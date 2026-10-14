package com.fiteasy.desktop
import com.fiteasy.shared.*
import java.awt.*
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.concurrent.*

class DesktopReminders(private val readStates:()->Map<String,ReminderDelivery>,private val writeStates:(Map<String,ReminderDelivery>)->Unit){
 private val executor=Executors.newSingleThreadScheduledExecutor {r->Thread(r,"FitEasy-reminders").apply {isDaemon=true}}
 private var current:List<Reminder>?=null
 private var deliveries=readStates()
 private val primary=mutableMapOf<String,ScheduledFuture<*>>()
 private val following=mutableMapOf<String,ScheduledFuture<*>>()
 private var generation=0L
 // When each reminder is due next, by the clock; and when the clock was last looked at (D115).
 private val due=mutableMapOf<String,Long>()
 private var lastBeat=System.currentTimeMillis()
 init {executor.scheduleWithFixedDelay({runCatching {beat(System.currentTimeMillis())}},BEAT_MILLIS,BEAT_MILLIS,TimeUnit.MILLISECONDS)}
 /**
  * The timers count running time, so after the computer slept, or its clock was changed, they are late by as much. Once a
  * minute the clock is compared with the last look: after a jump every timer is set again from the clock, and a reminder
  * that fell due meanwhile, not longer than half an hour ago, is shown once. Returns whether a jump was seen (D115).
  */
 @Synchronized internal fun beat(now:Long):Boolean {
  val jumped=now-lastBeat !in 0..BEAT_MILLIS+BEAT_SLACK
  lastBeat=now
  if(!jumped || executor.isShutdown)return jumped
  val list=current ?: return true
  val missed=due.filterValues {it<=now && now-it<=MISSED_MILLIS}.keys.toList()
  generation++
  primary.values.forEach {it.cancel(false)};primary.clear();due.clear()
  following.values.forEach {it.cancel(false)};following.clear()
  list.filter {it.enabled}.forEach {schedule(it,generation);queueFollow(it)}
  missed.forEach {fire(it,false)}
  AppJournal.record("windows.reminders.rearmed")
  return true
 }
 private var tray:TrayIcon?=null
 private var popup:PopupMenu?=null
 private var showWindow:(()->Unit)?=null
 private var exitWindow:(()->Unit)?=null
 private var navigate:((AppNavigation)->Unit)?=null
 @Volatile private var language="ru"
 @Volatile var summaryEnabled=true
 fun refresh()=rebuildMenu()
    fun attach(show:()->Unit,exit:()->Unit,language:String,onNavigate:(AppNavigation)->Unit):Boolean {
        if(!SystemTray.isSupported())return false
        showWindow=show;exitWindow=exit;navigate=onNavigate;this.language=language
        return runCatching {
            val create=Runnable {
                if(tray==null){
                    val image=DesktopReminders::class.java.getResource("/icons/fiteasy.png")?.let {javax.imageio.ImageIO.read(it)} ?: BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB)
                    popup=PopupMenu()
                    tray=TrayIcon(image,com.fiteasy.shared.appSystemName(java.util.Locale.getDefault().language),popup).apply {isImageAutoSize=true;addActionListener {show()};SystemTray.getSystemTray().add(this)}
                    rebuildMenu()
                    DesktopSound.onChange={rebuildMenu()}
                }
            }
            if(EventQueue.isDispatchThread())create.run()else EventQueue.invokeAndWait(create)
            true
        }.getOrDefault(false)
    }

 @Synchronized fun states()=deliveries.toMap()
 private fun persist(){writeStates(deliveries);rebuildMenu()}
 fun setLanguage(value:String){if(language!=value){language=value;rebuildMenu()}}
 private fun rebuildMenu(){
  val active=synchronized(this){activeTrayReminders(deliveries)}
  val summary=synchronized(this){if(summaryEnabled)reminderSummary(current.orEmpty(),deliveries)else null}
  EventQueue.invokeLater {
   if(executor.isShutdown)return@invokeLater
   tray?.toolTip=(listOf(com.fiteasy.shared.appSystemName(java.util.Locale.getDefault().language))+summary?.let {reminderSummaryLines(it,language)}.orEmpty()).joinToString("\n").take(120)
   popup?.let {menu->
    menu.removeAll()
    fun item(action:String,run:()->Unit){menu.add(MenuItem(trayLabel(action,language)).apply {addActionListener {run()}})}
    item("open"){showWindow?.invoke()}
    // While a melody plays the menu offers to end it without opening the window (D104).
    if(DesktopSound.ringing.value!=null)item("silence"){DesktopSound.stop()}
    menu.addSeparator()
    listOf("today","food","water","activity","progress","notes","profile","settings").forEach {action->item(action){navigate?.invoke(AppNavigation(destination=action))}}
    menu.addSeparator()
    listOf("calendar","schedules","reminders").forEach {action->item(action){navigate?.invoke(AppNavigation(destination=action))}}
    menu.add(Menu(trayLabel("active",language)+" (${active.size})").apply {
     isEnabled=active.isNotEmpty()
     active.forEachIndexed {index,id->add(MenuItem("${trayLabel("reminders",language)} ${index+1}").apply {addActionListener {navigate?.invoke(AppNavigation(destination="reminder-status",reminderId=id))}})}
    })
    menu.addSeparator();item("help"){navigate?.invoke(AppNavigation(destination="help"))};item("about"){navigate?.invoke(AppNavigation(destination="about"))}
    menu.addSeparator();item("exit"){exitWindow?.invoke()}
   }
  }
 }
 @Synchronized fun sync(reminders:List<Reminder>){
  reminders.forEach(::validateReminder);if(current==reminders)return
  val old=current.orEmpty();current=reminders;generation++
  primary.values.forEach {it.cancel(false)};primary.clear();due.clear()
  following.values.forEach {it.cancel(false)};following.clear()
  val changed=old.filter {r->reminders.none {it==r}}.map {it.id}.toSet()
  deliveries=deliveries.filter {it.key !in changed && reminders.any {r->r.id==it.key && r.enabled}};persist()
  reminders.filter {it.enabled}.forEach {schedule(it,generation);queueFollow(it)}
 }
 private fun schedule(r:Reminder,version:Long){
  val next=nextReminder(r) ?: run {due.remove(r.id);return}
  due[r.id]=next.toEpochMilli()
  primary[r.id]=executor.schedule({synchronized(this){if(version!=generation)return@schedule;fire(r.id,false);current.orEmpty().firstOrNull {it.id==r.id && it.enabled}?.let {schedule(it,version)}}},(next.toEpochMilli()-System.currentTimeMillis()).coerceAtLeast(1),TimeUnit.MILLISECONDS)
 }
 private fun queueFollow(r:Reminder){
  following.remove(r.id)?.cancel(false)
  var d=deliveries[r.id]?.takeIf {it.nextAt>0} ?: return
  val adjusted=attentionTime(r,Instant.ofEpochMilli(d.nextAt).coerceAtLeast(Instant.now())).toEpochMilli()
  if(adjusted!=d.nextAt){d=d.copy(nextAt=adjusted);deliveries=deliveries+(r.id to d);persist()}
  following[r.id]=executor.schedule({synchronized(this){if(deliveries[r.id]?.episode!=d.episode || deliveries[r.id]?.nextAt!=d.nextAt)return@schedule;fire(r.id,true)}},(d.nextAt-System.currentTimeMillis()).coerceAtLeast(1),TimeUnit.MILLISECONDS)
 }
 private fun fire(id:String,follow:Boolean){
  val r=current.orEmpty().firstOrNull {it.id==id && it.enabled} ?: return
  val old=deliveries[id]
  if(follow && (old==null || old.nextAt==0L || old.status !in setOf("awaiting","snoozed","delivered")))return
  val now=Instant.now();val allowed=attentionTime(r,now)
  if(allowed>now){if(follow){deliveries=deliveries+(id to old!!.copy(nextAt=allowed.toEpochMilli()));persist();queueFollow(r)};return}
  val d=fired(r,old,follow).let {if(it.nextAt>0)it.copy(nextAt=attentionTime(r,Instant.ofEpochMilli(it.nextAt)).toEpochMilli())else it}
  deliveries=deliveries+(id to d);persist();queueFollow(r)
  EventQueue.invokeLater {show(r,d)}
 }
 @Synchronized fun control(id:String,action:String){
  require(action in setOf("ack","interrupt","snooze"))
  val r=current.orEmpty().firstOrNull {it.id==id && it.enabled} ?: return
  val old=deliveries[id] ?: return
  if(DesktopSound.ringing.value==id)DesktopSound.stop()
  val d=when(action){"ack"->old.copy(status="completed",nextAt=0);"interrupt"->old.copy(status="interrupted",nextAt=0);else->old.copy(status="snoozed",nextAt=attentionTime(r,Instant.now().plusSeconds(r.snoozeMinutes*60L)).toEpochMilli())}
  deliveries=deliveries+(id to d);persist();queueFollow(r)
 }
 @Synchronized private fun show(r:Reminder,d:ReminderDelivery){
  if(executor.isShutdown || deliveries[r.id]!=d)return
  if(r.sound){if(ownMelody(r))DesktopSound.play(r.soundUri?.let {DesktopSound.own(it,desktopDataDirectory())},r.soundKind,r.volumePercent,r.rampSeconds,r.id) else Toolkit.getDefaultToolkit().beep()}
  if(r.popup)tray?.displayMessage(r.name,r.comment.ifBlank {"FitEasy"},TrayIcon.MessageType.INFO)
 }
 @Synchronized fun close(){DesktopSound.stop();executor.shutdownNow();tray?.let {SystemTray.getSystemTray().remove(it)}}
 private companion object {
  const val BEAT_MILLIS=60_000L
  const val BEAT_SLACK=15_000L
  const val MISSED_MILLIS=30*60_000L
 }
}
