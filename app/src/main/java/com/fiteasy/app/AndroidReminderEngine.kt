package com.fiteasy.app

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant

object AndroidReminders {
    val changed=kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity=1,onBufferOverflow=kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    private var lastStatusText:String?=null
    /** Set by the running step service: one shade entry then carries steps and reminders together. */
    @Volatile var onStatus:(()->Unit)?=null
    data class Status(val lines:List<String>,val waiting:Int)
    // What a going-off reminder reads is opened with the keystore once per process and decoded once per change (D115).
    private val opened=OpenedTexts()
    private var remindersRead:Pair<String,List<Reminder>>?=null
    private var statesRead:Pair<String,Map<String,ReminderDelivery>>?=null
    private val channels=HashSet<String>()
    private var armed=false
    private fun open(sealed:String)=opened.get(sealed,AndroidVault::unseal)
    private fun secret(c:Context,name:String):String? = runCatching {c.getSharedPreferences("local-security",Context.MODE_PRIVATE).getString("secret-$name",null)?.let(::open)}.getOrNull()
    /** Device-local switch written by Platform.writeSecret. */
    fun flag(c:Context,name:String,default:Boolean):Boolean = secret(c,name)?.toBooleanStrictOrNull() ?: default
    @Synchronized fun status(c:Context):Status? = if(!flag(c,"status-notification",true))null else reminderSummary(read(c),states(c))?.let {Status(reminderSummaryLines(it,language(c)),it.waiting)}
    fun interruptAll(c:Context)=pending(c,"all","interrupt-all")
    fun test(c:Context){c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+10_000,pending(c,"test","test"))}
    private fun language(c:Context)=secret(c,"language") ?: java.util.Locale.getDefault().language
    private fun prefs(c:Context)=c.getSharedPreferences("scheduled-reminders",Context.MODE_PRIVATE)
    @Synchronized fun read(c:Context):List<Reminder> {
        val sealed=prefs(c).getString("reminders",null) ?: return emptyList()
        remindersRead?.takeIf {it.first==sealed}?.let {return it.second}
        return codec.decodeFromString<List<Reminder>>(open(sealed)).also {remindersRead=sealed to it}
    }
    @Synchronized fun states(c:Context):Map<String,ReminderDelivery> {
        val sealed=prefs(c).getString("deliveries",null) ?: return emptyMap()
        statesRead?.takeIf {it.first==sealed}?.let {return it.second}
        return codec.decodeFromString<Map<String,ReminderDelivery>>(open(sealed)).also {statesRead=sealed to it}
    }
    private fun write(c:Context,states:Map<String,ReminderDelivery>){
        val sealed=AndroidVault.seal(codec.encodeToString(states))
        check(prefs(c).edit().putString("deliveries",sealed).commit());statesRead=sealed to states
        changed.tryEmit(Unit);refreshStatus(c)
    }
    private fun channel(c:Context,id:String,make:()->NotificationChannel):String {if(channels.add(id))c.getSystemService(NotificationManager::class.java).createNotificationChannel(make());return id}
    @Synchronized fun refreshStatus(c:Context){
        val manager=NotificationManagerCompat.from(c)
        val merged=onStatus
        if(merged!=null){lastStatusText=null;manager.cancel("reminder-status",0);merged();return}
        val status=status(c)
        if(status==null){lastStatusText=null;manager.cancel("reminder-status",0);return}
        if(!manager.areNotificationsEnabled()){lastStatusText=null;return}
        // The same text is not posted again, and nothing is prepared for it.
        val text=status.lines.joinToString(" · ")
        if(text==lastStatusText)return
        val channel=channel(c,"reminder-status"){NotificationChannel("reminder-status","FitEasy",NotificationManager.IMPORTANCE_LOW).apply {setShowBadge(false);setSound(null,null);enableVibration(false)}}
        val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java).setData(Uri.parse("fiteasy://open-reminder/status")).putExtra("reminderId","status").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=NotificationCompat.Builder(c,channel).setSmallIcon(R.drawable.ic_notification).setContentTitle("FitEasy").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(status.lines.joinToString("\n"))).setContentIntent(open).setSilent(true).setOnlyAlertOnce(true).setOngoing(true).setShowWhen(false).setVisibility(NotificationCompat.VISIBILITY_SECRET).setCategory(NotificationCompat.CATEGORY_STATUS)
        if(status.waiting>0)builder.addAction(0,translate("Прервать все",language(c)),interruptAll(c))
        manager.notify("reminder-status",0,builder.build())
        lastStatusText=text
    }
    private fun pending(c:Context,id:String,action:String)=PendingIntent.getBroadcast(c,0,Intent(c,ReminderReceiver::class.java).setAction(action).setData(Uri.parse("fiteasy://reminder/$id")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun cancel(c:Context,id:String,all:Boolean){val alarm=c.getSystemService(AlarmManager::class.java);(if(all)listOf("fire","follow")else listOf("follow")).forEach {alarm.cancel(pending(c,id,it))};NotificationManagerCompat.from(c).cancel(id,0);NotificationManagerCompat.from(c).cancel(id.hashCode());if(ReminderRingService.ringing.value==id)ReminderRingService.stopAndRemove(c)}
    private fun schedule(c:Context,id:String,time:Instant,action:String="fire"){
        val alarm=c.getSystemService(AlarmManager::class.java);val intent=pending(c,id,action)
        // Battery saver: let the system batch the wake-up with others; the reminder may come a few minutes late.
        if(flag(c,"economy",false))alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.toEpochMilli(),intent)
        else if(Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms())alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.toEpochMilli(),intent)else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.toEpochMilli(),intent)
    }
    @Synchronized fun clear(c:Context){lastStatusText=null;changed.tryEmit(Unit);read(c).forEach {cancel(c,it.id,true)};check(prefs(c).edit().clear().commit());remindersRead=null;statesRead=null;opened.clear();NotificationManagerCompat.from(c).cancel("reminder-status",0)}
    /**
     * Once per start of the app: the alarms of the enabled reminders are set again. Android drops them when the app is
     * stopped by force, and nothing else would bring them back until a reminder is changed (D115).
     */
    @Synchronized fun armOnce(c:Context){if(armed)return;armed=true;sync(c,read(c),true)}
    /**
     * Makes the alarms follow [reminders]. Only a reminder that was changed gets its alarm set again; [force] sets the alarm
     * of every enabled one (after a restart, a changed clock or exactness) without touching what is on the screen;
     * [restore] also shows again, silently, the reminders that still wait for an answer — a restart removes them (D115).
     */
    @Synchronized fun sync(c:Context,reminders:List<Reminder>,force:Boolean=false,restore:Boolean=false){
        val old=read(c);val same=old==reminders
        if(!force && same)return
        reminders.forEach(::validateReminder)
        val before=states(c);var states=before
        val changed=if(same)emptySet() else reminders.filter {r->old.none {it==r}}.map {it.id}.toSet()
        if(!same)old.filter {r->reminders.none {it==r}}.forEach {cancel(c,it.id,true);states=states-it.id}
        states=states.filterKeys {id->reminders.any {it.id==id && it.enabled}}
        if(!same){
            val sealed=AndroidVault.seal(codec.encodeToString(reminders))
            check(prefs(c).edit().putString("reminders",sealed).commit());remindersRead=sealed to reminders
        }
        if(states!=before)write(c,states) else refreshStatus(c)
        if(!same)runCatching {ReminderSounds.prune(c,reminders.mapNotNull {it.soundUri}.toSet())}
        var set=0
        reminders.forEach {r->
            val touched=r.id in changed
            if(!r.enabled){if(touched)cancel(c,r.id,true)}
            else if(force || touched){
                nextReminder(r)?.let {schedule(c,r.id,it);set++}
                states[r.id]?.takeIf {it.nextAt>0}?.let {schedule(c,r.id,attentionTime(r,Instant.ofEpochMilli(it.nextAt).coerceAtLeast(Instant.now().plusSeconds(1))),"follow")}
                if(restore && states[r.id]?.status=="awaiting")show(c,r,silent=true)
            }
        }
        if(set>0)AppJournal.record("reminder.scheduled")
    }
    @Synchronized fun control(c:Context,id:String,action:String){
        require(action in setOf("ack","interrupt","snooze"))
        val r=read(c).firstOrNull {it.id==id && it.enabled} ?: return
        val states=states(c);val previous=states[id] ?: return
        cancel(c,id,false)
        val result=when(action){
            "ack"->previous.copy(status="completed",nextAt=0)
            "interrupt"->previous.copy(status="interrupted",nextAt=0)
            else->previous.copy(status="snoozed",nextAt=attentionTime(r,Instant.now().plusSeconds(r.snoozeMinutes*60L)).toEpochMilli())
        }
        write(c,states+(id to result));if(result.nextAt>0)schedule(c,id,Instant.ofEpochMilli(result.nextAt),"follow")
    }
    @Synchronized private fun fire(c:Context,id:String,follow:Boolean){
        AppJournal.record(if(follow)"reminder.alarm.follow" else "reminder.alarm.fire")
        val r=read(c).firstOrNull {it.id==id && it.enabled} ?: return
        val states=states(c);val previous=states[id]
        if(follow && (previous==null || previous.nextAt==0L || previous.status !in setOf("awaiting","snoozed","delivered")))return
        if(follow && previous!!.nextAt>System.currentTimeMillis()+1000){schedule(c,id,Instant.ofEpochMilli(previous.nextAt),"follow");return}
        val now=Instant.now();val allowed=attentionTime(r,now)
        if(allowed>now){
            if(follow){val deferred=previous!!.copy(nextAt=allowed.toEpochMilli());write(c,states+(id to deferred));schedule(c,id,allowed,"follow")}
            else nextReminder(r,now)?.let {schedule(c,id,it)}
            return
        }
        cancel(c,id,false)
        val delivered=fired(r,previous,follow).let {if(it.nextAt>0)it.copy(nextAt=attentionTime(r,Instant.ofEpochMilli(it.nextAt)).toEpochMilli())else it}
        write(c,states+(id to delivered));show(c,r)
        if(delivered.nextAt>0)schedule(c,id,Instant.ofEpochMilli(delivered.nextAt),"follow")
        if(!follow)nextReminder(r)?.let {schedule(c,id,it)}
    }
    private fun show(c:Context,r:Reminder,silent:Boolean=false){
        val language=language(c)
        // Own melody, volume or fade-in: the ring service plays it and its channel stays silent (D097).
        val ringing=!silent && ownMelody(r)
        fun channel(ring:Boolean):String {
            val id=if(ring)"reminders-ring-${r.vibration}-${r.popup}" else "reminders-${r.sound}-${r.soundKind}-${r.vibration}-${r.popup}"
            return channel(c,id) {
                val tone=when(r.soundKind){"alarm"->RingtoneManager.TYPE_ALARM;"ringtone"->RingtoneManager.TYPE_RINGTONE;else->RingtoneManager.TYPE_NOTIFICATION}
                NotificationChannel(id,"FitEasy напоминания",if(r.popup)NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT).apply {
                    enableVibration(r.vibration);lockscreenVisibility=Notification.VISIBILITY_PRIVATE
                    setSound(if(r.sound && !ring)RingtoneManager.getDefaultUri(tone)else null,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                }
            }
        }
        val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java).setData(Uri.parse("fiteasy://open-reminder/${r.id}")).putExtra("reminderId",r.id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text=listOf(r.name,r.comment).filter {it.isNotBlank()}.joinToString("\n")
        val state=states(c)[r.id]
        val status=translate(reminderStateLabel(r,state),language)
        val detail=listOf(text,status,state?.nextAt?.takeIf {it>0}?.let {reminderVisualText("Ближайшее: {0}",language,Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())}).filterNotNull().joinToString("\n")
        fun build(ring:Boolean):Notification {
        val notification=NotificationCompat.Builder(c,channel(ring)).setSmallIcon(R.drawable.ic_notification).setContentTitle(r.name).setContentText(r.comment.ifBlank {"FitEasy"}).setSubText(status).setStyle(NotificationCompat.BigTextStyle().bigText(detail)).setContentIntent(open).setSilent(silent).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(!r.requiresConfirmation).setOngoing(r.requiresConfirmation).setPriority(if(r.popup)NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0,"${translate("Отложить",language)} ${r.snoozeMinutes}",pending(c,r.id,"snooze"))
            .addAction(0,translate("Прервать",language),pending(c,r.id,"interrupt"))
        if(r.requiresConfirmation)notification.addAction(0,translate("Подтвердить",language),pending(c,r.id,"ack"))
        // Alarm-clock mode: the reminder opens its own small screen over the lock screen.
        if(!silent && r.popup && flag(c,"alarm-screen",false)){
            val alarm=Intent(c,ReminderAlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("id",r.id).putExtra("name",r.name).putExtra("comment",r.comment).putExtra("snooze",r.snoozeMinutes).putExtra("confirm",r.requiresConfirmation).putExtra("language",language).putExtra("count",state?.count ?: 1)
            notification.setCategory(NotificationCompat.CATEGORY_ALARM).setFullScreenIntent(PendingIntent.getActivity(c,r.id.hashCode(),alarm,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),true)
            if(android.provider.Settings.canDrawOverlays(c))runCatching {c.startActivity(alarm)}.onFailure {AppJournal.record("reminder.alarm.start.failed",JournalLevel.WARN,it)}
        }
        if(ring)notification.addAction(0,translate("Заглушить",language),pending(c,r.id,"silence"))
        // What the lock screen may show is the person's choice (Settings → Security and privacy, as in Notio); unlocked, the reminder is shown whole.
        val (shownTitle,shownText)=reminderShown(r,secret(c,"reminder-privacy") ?: "generic",language)
        notification.setPublicVersion(NotificationCompat.Builder(c,channel(ring)).setSmallIcon(R.drawable.ic_notification).setContentTitle(shownTitle).setContentText(shownText.ifBlank {"FitEasy"}).build())
        return notification.build()
        }
        if(NotificationManagerCompat.from(c).areNotificationsEnabled() && (Build.VERSION.SDK_INT<33 || c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)){
            if(ringing && ReminderRingService.start(c,r.id.hashCode(),build(true),r.id,r.soundUri,r.soundKind,r.volumePercent,r.rampSeconds))AppJournal.record("reminder.ring.started")
            else NotificationManagerCompat.from(c).notify(r.id,0,build(false))
            AppJournal.record("reminder.notification.posted")
        }else AppJournal.record("reminder.notification.permission.blocked",JournalLevel.WARN)
    }
    @Synchronized fun handle(c:Context,intent:Intent){
        if(intent.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_MY_PACKAGE_REPLACED,AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)){
            // A restart or an update removed the alarms and what was on the screen; a changed clock moved only the alarms.
            armed=true;sync(c,read(c),true,restore=intent.action==Intent.ACTION_BOOT_COMPLETED || intent.action==Intent.ACTION_MY_PACKAGE_REPLACED)
        }else intent.data?.lastPathSegment?.let {id->when(intent.action){"fire"->fire(c,id,false);"follow"->fire(c,id,true);"ack","snooze","interrupt"->control(c,id,intent.action!!);"silence"->ReminderRingService.stop(c);"test"->show(c,Reminder(id="test",name=translate("Проверка будильника",language(c)),popup=true,sound=true,vibration=true));"interrupt-all"->states(c).filterValues {it.status in waitingStatuses}.keys.forEach {control(c,it,"interrupt")}}}
    }
}
