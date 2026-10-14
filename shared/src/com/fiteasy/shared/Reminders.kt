package com.fiteasy.shared

import java.time.*
import java.time.temporal.ChronoUnit

fun validateReminder(r:Reminder) {
    require((r.scheduleId==null)==(r.rowId==null) && r.comment.length<=8192)
    require(r.soundKind in setOf("notification","alarm","ringtone")){"Unknown reminder sound"}
    require(r.volumePercent in 10..100 && r.rampSeconds in 0..300 && (r.soundUri==null || r.soundUri.length in 1..2048) && (r.noteId==null || r.noteId.length in 1..128)){"Invalid reminder melody"}
    require(r.name.length<=4096 && r.times.size<=1440 && (r.scheduleId==null || (r.scheduleId.isNotBlank() && r.rowId!!.isNotBlank())))
    require(r.repeatMinutes in 1..1440 && r.repeatCount in 0..100 && (!r.repeatUntilConfirmed || r.requiresConfirmation))
    require(r.id.isNotBlank() && r.name.isNotBlank() && r.everyDays in 1..3660 && r.snoozeMinutes in 1..1440)
    require(r.weekdays.all {it in 1..7} && r.monthDays.all {it in 1..31})
    (r.times.ifEmpty {listOf(r.time)}).forEach {LocalTime.parse(it)}
    LocalDate.parse(r.startDate);r.endDate?.let {require(LocalDate.parse(it)>=LocalDate.parse(r.startDate))}
    require((r.quietStart==null)==(r.quietEnd==null))
    r.quietStart?.let {LocalTime.parse(it)};r.quietEnd?.let {LocalTime.parse(it)}
}
/** Device-zone wall clock; invalid month dates and occurrences in quiet hours are skipped. */
/** System sound a reminder plays: the notification tone, the alarm tone or the ringtone. */
val reminderSounds=listOf("notification" to "Звук уведомления","alarm" to "Звук будильника","ringtone" to "Мелодия звонка")
/** A reminder with its own melody, volume or fade-in is played by the device's ring player; a plain one uses the system tone. */
fun ownMelody(r:Reminder)=r.sound && (r.soundUri!=null || r.volumePercent!=100 || r.rampSeconds>0)
const val RING_MILLIS=60_000L
const val RING_START_SHARE=.05f
/**
 * How long the alarm screen of a reminder stays before it closes itself (D121, rule 10 of docs/BATTERY.md, as in Notio):
 * a minute at the first ring, fifteen seconds at the second unanswered one, five after that. The screen is the dearest
 * thing a phone has; the reminder itself stays in the notification with the same buttons.
 */
fun alarmScreenMillis(count:Int):Long = when {count<=1->60_000L;count==2->15_000L;else->5_000L}
/** Deep sleep hands one app its alarms no more often than this (rule 4 of docs/BATTERY.md). */
const val DOZE_ALARM_MINUTES=9
/** Gain 0..1 of a melody [elapsedMillis] after it started: a straight climb from a quiet start to the chosen volume (after Notio's RingRamp). */
fun ringGain(elapsedMillis:Long,rampSeconds:Int,volumePercent:Int):Float {
    val target=volumePercent.coerceIn(10,100)/100f
    if(rampSeconds<=0)return target
    val progress=(elapsedMillis.coerceAtLeast(0).toFloat()/(rampSeconds*1000f)).coerceIn(0f,1f)
    return target*(RING_START_SHARE+(1f-RING_START_SHARE)*progress)
}
/**
 * When a playing melody needs attention next, [elapsedMillis] after it started (D115): every quarter of a second only
 * while the volume still climbs, then once, at the end of the ring. A melody without fade-in costs one timer, not 240.
 */
fun ringTickDelay(elapsedMillis:Long,rampSeconds:Int,limitMillis:Long=RING_MILLIS):Long {
    val left=limitMillis-elapsedMillis
    return (if(elapsedMillis<rampSeconds*1000L)minOf(250L,left) else left).coerceAtLeast(1)
}
/**
 * Texts opened from their sealed form, kept for the life of the process (D115). Opening one asks the device keystore, and
 * a reminder that goes off read the same few values a dozen times; a sealed text never changes its meaning, so it is opened once.
 */
class OpenedTexts(private val limit:Int=32) {
    private val opened=object:LinkedHashMap<String,String>(16,.75f,true) {override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,String>?)=size>limit}
    @Synchronized fun get(sealed:String,open:(String)->String):String = opened[sealed] ?: open(sealed).also {opened[sealed]=it}
    @Synchronized fun put(sealed:String,text:String){opened[sealed]=text}
    @Synchronized fun clear()=opened.clear()
}
/** How the days of a reminder come back, as the editor offers them. */
fun reminderRepeatKind(r:Reminder):String = when {
    r.endDate==r.startDate->"once"
    r.monthDays.isNotEmpty()->"monthly"
    r.weekdays.size in 1..6->"weekdays"
    else->"daily"
}
/** Short weekday name in the app language: Пн, Mon, 月 … */
fun weekdayLabel(day:Int,language:String):String = java.time.DayOfWeek.of(day).getDisplayName(java.time.format.TextStyle.SHORT,java.util.Locale.forLanguageTag(language)).replaceFirstChar {it.uppercase()}
fun nextReminder(r:Reminder,after:Instant=Instant.now(),zone:ZoneId=ZoneId.systemDefault()):Instant? {
    validateReminder(r);if(!r.enabled)return null
    val anchor=LocalDate.parse(r.startDate);val today=after.atZone(zone).toLocalDate()
    val start=if(anchor>today)anchor else today
    val end=r.endDate?.let(LocalDate::parse)
    val times=r.times.ifEmpty {listOf(r.time)}.map(LocalTime::parse).distinct().sorted()
    fun quiet(time:LocalTime):Boolean {
        val from=r.quietStart?.let(LocalTime::parse) ?: return false
        val to=LocalTime.parse(r.quietEnd)
        return if(from==to)false else if(from<to)time>=from && time<to else time>=from || time<to
    }
    for(offset in 0..3660){
        val date=start.plusDays(offset.toLong());if(end!=null && date>end)return null
        if(r.weekdays.isNotEmpty() && date.dayOfWeek.value !in r.weekdays)continue
        if(r.monthDays.isNotEmpty() && date.dayOfMonth !in r.monthDays)continue
        if(ChronoUnit.DAYS.between(anchor,date)%r.everyDays!=0L)continue
        for(time in times){if(quiet(time))continue;val instant=date.atTime(time).atZone(zone).toInstant();if(instant>after)return instant}
    }
    return null
}
