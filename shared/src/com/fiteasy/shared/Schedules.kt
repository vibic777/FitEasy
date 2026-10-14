package com.fiteasy.shared

import kotlinx.serialization.Serializable
import java.time.*
import java.time.temporal.ChronoUnit

@Serializable data class ScheduleRow(val id:String=newId(),val time:String="09:00",val title:String="",val comment:String="")
@Serializable data class DaySchedule(val id:String=newId(),val name:String,val rows:List<ScheduleRow> = emptyList(),val weekdays:List<Int> = (1..7).toList(),val monthDays:List<Int> = emptyList(),val everyDays:Int=1,val startDate:String=LocalDate.now().toString(),val endDate:String?=null,val enabled:Boolean=true,val updated:Long=System.currentTimeMillis(),val deleted:Long?=null)
fun validateSchedule(s:DaySchedule){
    require(s.id.isNotBlank() && s.name.isNotBlank() && s.name.length<=512 && s.rows.size<=500 && s.rows.map {it.id}.distinct().size==s.rows.size)
    validateReminder(Reminder(name=s.name,weekdays=s.weekdays,monthDays=s.monthDays,everyDays=s.everyDays,startDate=s.startDate,endDate=s.endDate))
    s.rows.forEach {require(it.id.isNotBlank() && it.title.isNotBlank() && it.title.length<=4096 && it.comment.length<=8192);LocalTime.parse(it.time)}
}
fun copySchedule(s:DaySchedule)=s.copy(id=newId(),rows=s.rows.map {it.copy(id=newId())},updated=System.currentTimeMillis())
fun calendarMatches(date:LocalDate,start:String,end:String?,weekdays:List<Int>,monthDays:List<Int>,every:Int):Boolean {
    val anchor=LocalDate.parse(start)
    return date>=anchor && (end==null || date<=LocalDate.parse(end)) && (weekdays.isEmpty() || date.dayOfWeek.value in weekdays) && (monthDays.isEmpty() || date.dayOfMonth in monthDays) && ChronoUnit.DAYS.between(anchor,date)%every==0L
}
fun scheduleOn(s:DaySchedule,date:LocalDate)=s.deleted==null && calendarMatches(date,s.startDate,s.endDate,s.weekdays,s.monthDays,s.everyDays)
fun effectiveReminders(snapshot:Snapshot):List<Reminder> = snapshot.local.reminders.map {r->
    if(snapshot.local.remindersOff)r.copy(enabled=false) else if(r.scheduleId==null)r else {
        val schedule=snapshot.shared.schedules.firstOrNull {it.id==r.scheduleId && it.deleted==null}
        val row=schedule?.rows?.firstOrNull {it.id==r.rowId}
        if(schedule==null || row==null)r.copy(enabled=false)else r.copy(name="${schedule.name}: ${row.title}",comment=r.comment.ifBlank {row.comment},time=row.time,times=listOf(row.time),weekdays=schedule.weekdays,monthDays=schedule.monthDays,everyDays=schedule.everyDays,startDate=schedule.startDate,endDate=schedule.endDate,enabled=r.enabled && schedule.enabled)
    }
}
fun remindersOn(r:Reminder,date:LocalDate):List<LocalTime> {
    if(!calendarMatches(date,r.startDate,r.endDate,r.weekdays,r.monthDays,r.everyDays))return emptyList()
    return r.times.ifEmpty {listOf(r.time)}.map(LocalTime::parse).distinct().sorted()
}
fun attentionTime(r:Reminder,time:Instant,zone:ZoneId=ZoneId.systemDefault()):Instant {
    val start=r.quietStart?.let(LocalTime::parse) ?: return time
    val end=LocalTime.parse(r.quietEnd);if(start==end)return time
    val local=time.atZone(zone);val quiet=if(start<end)local.toLocalTime()>=start && local.toLocalTime()<end else local.toLocalTime()>=start || local.toLocalTime()<end
    if(!quiet)return time
    val day=if(start>end && local.toLocalTime()>=start)local.toLocalDate().plusDays(1)else local.toLocalDate()
    return day.atTime(end).atZone(zone).toInstant()
}
@Serializable data class ReminderDelivery(val episode:String=newId(),val status:String="completed",val at:Long=System.currentTimeMillis(),val count:Int=1,val nextAt:Long=0)
fun canFollow(r:Reminder,d:ReminderDelivery)=d.status in setOf("awaiting","delivered","snoozed") && (r.repeatUntilConfirmed || d.count<=r.repeatCount)
fun fired(r:Reminder,previous:ReminderDelivery?,follow:Boolean,now:Long=System.currentTimeMillis()):ReminderDelivery {
    val d=if(follow && previous!=null)previous.copy(count=previous.count+if(previous.status=="snoozed")0 else 1,nextAt=0)else ReminderDelivery(at=now)
    val status=if(r.requiresConfirmation)"awaiting" else if(r.repeatCount>=d.count)"delivered" else "completed"
    val result=d.copy(status=status)
    return if(canFollow(r,result))result.copy(nextAt=now+r.repeatMinutes*60000L)else result
}
fun reminderStateLabel(r:Reminder,d:ReminderDelivery?):String=when {
    !r.enabled->"Выключено"
    d?.status=="awaiting"->"Ожидает подтверждения"
    d?.status=="snoozed"->"Отложено"
    d?.status=="interrupted"->"Прервано"
    d?.status=="delivered"->"Выдано, повторяется"
    d?.status=="completed"->"Завершено"
    nextReminder(r)!=null->"Запланировано"
    else->"Нет будущих срабатываний"
}
/** What the quiet status line shows (D085): enabled reminders, how many still fall today, the nearest time, and series waiting for an answer. */
data class ReminderSummary(val enabled:Int,val today:Int,val next:Instant?,val waiting:Int,val tasks:Int=0)
val waitingStatuses=setOf("awaiting","delivered","snoozed")
fun reminderSummary(reminders:List<Reminder>,deliveries:Map<String,ReminderDelivery>,now:Instant=Instant.now(),zone:java.time.ZoneId=java.time.ZoneId.systemDefault()):ReminderSummary? {
    val enabled=reminders.filter {it.enabled};if(enabled.isEmpty())return null
    val day=now.atZone(zone).toLocalDate()
    // The next time of each reminder is worked out once: it is the costly part of the summary (D115).
    val next=enabled.map {it to nextReminder(it,now,zone)}
    val upcoming=next.mapNotNull {it.second}
    val follow=enabled.mapNotNull {r->deliveries[r.id]?.takeIf {it.status in waitingStatuses && it.nextAt>0}?.let {Instant.ofEpochMilli(it.nextAt)}}
    // A reminder bound to a schedule row is a planned task, such as a workout by schedule.
    val tasks=next.count {(r,at)->r.scheduleId!=null && at?.atZone(zone)?.toLocalDate()==day}
    return ReminderSummary(enabled.size,upcoming.count {it.atZone(zone).toLocalDate()==day},(upcoming+follow).minOrNull(),enabled.count {deliveries[it.id]?.status in waitingStatuses},tasks)
}
fun reminderSummaryLines(s:ReminderSummary,language:String,zone:java.time.ZoneId=java.time.ZoneId.systemDefault()):List<String> = listOfNotNull(
    reminderVisualText("Включённых напоминаний: {0}",language,s.enabled),
    extraText("Запланировано сегодня: {0}",language).replace("{0}",s.today.toString()),
    if(s.tasks>0)extraText("Задач по расписанию сегодня: {0}",language).replace("{0}",s.tasks.toString())else null,
    s.next?.let {reminderVisualText("Ближайшее: {0}",language,java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm").format(it.atZone(zone)))},
    if(s.waiting>0)extraText("Ждут ответа: {0}",language).replace("{0}",s.waiting.toString())else null)
