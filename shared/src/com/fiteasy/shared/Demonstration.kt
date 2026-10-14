package com.fiteasy.shared

import java.security.MessageDigest
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private fun demoHash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it.toInt() and 255)}
private inline fun <reified T> fingerprint(value:T)=demoHash(codec.encodeToString(value))
/** A reminder is told from the demonstration's own by everything but its time of change, added in 0.4.29: one made before it keeps its mark. */
private fun fingerprint(value:Reminder)=demoHash(JsonObject(codec.encodeToJsonElement(Reminder.serializer(),value).jsonObject-"updated").toString())

fun createDemonstration(current:Snapshot,date:LocalDate=LocalDate.now(),days:Int=7):Snapshot {
    require(current.local.demo.isEmpty()) {"Демонстрация уже создана"}
    require(days in setOf(7,30))
    val entries=(0 until days).flatMap {offset->val day=date.minusDays(offset.toLong()).toString();listOf(
        Entry(date=day,type="food",name="Демо · Овсяная каша с яблоком",amount=250.0,unit="g",meal="Завтрак",nutrients=Nutrients(230.0,7.0,5.0,39.0,4.0)),
        Entry(date=day,type="drink",name="Демо · Вода",amount=500.0,unit="ml",plainWater=true),
        Entry(date=day,type="drink",name="Демо · Капучино",amount=200.0,unit="ml",nutrients=Nutrients(85.0,4.0,4.0,8.0)),
        Entry(date=day,type="activity",name="Демо · Спокойная прогулка",amount=20.0+offset%7,unit="min",distanceKm=1.2),
        Entry(date=day,type="steps",name="Демо · Шаги",amount=3500.0+(offset%7)*300,unit="steps"))}
    val notes=listOf(
        Note(created=System.currentTimeMillis(),title="Демо · Дневник самочувствия",text="Сегодня нашла время для спокойной прогулки. Здесь можно вести личные записи."),
        Note(created=System.currentTimeMillis(),kind="list",title="Демо · Список покупок",items=listOf(NoteItem(text="Яблоки",checked=true),NoteItem(text="Овсяные хлопья"),NoteItem(text="Молоко",comment="Для кофе"))),
        Note(created=System.currentTimeMillis(),kind="drawing",title="Демо · Рисунок",strokes=listOf(InkStroke(points=listOf(InkPoint(0.15f,0.7f),InkPoint(0.4f,0.3f),InkPoint(0.65f,0.7f),InkPoint(0.85f,0.4f))))))
    val schedule=DaySchedule(name="Демо · Понедельник",weekdays=listOf(1),rows=listOf(
        ScheduleRow(time="09:00",title="Завтрак",comment="Спокойное начало дня"),
        ScheduleRow(time="13:00",title="Вода",comment="Пример напоминания"),
        ScheduleRow(time="18:00",title="Прогулка",comment="Удобный темп")))
    val reminders=schedule.rows.map {Reminder(name="Демо · ${it.title}",enabled=false,scheduleId=schedule.id,rowId=it.id,comment="Пример: включите при необходимости")}
    val registry=buildMap {entries.forEach {put("entry:${it.id}",fingerprint(it))};notes.forEach {put("note:${it.id}",fingerprint(it))};put("schedule:${schedule.id}",fingerprint(schedule));reminders.forEach {put("reminder:${it.id}",fingerprint(it))}}
    return current.copy(shared=current.shared.copy(entries=current.shared.entries+entries,notes=current.shared.notes+notes,schedules=current.shared.schedules+schedule),local=current.local.copy(reminders=current.local.reminders+reminders,demo=registry))
}

fun removeDemonstration(current:Snapshot,now:Long=System.currentTimeMillis()):Snapshot {
    val registry=current.local.demo
    fun original(key:String,hash:String)=registry[key]==hash
    fun originalNote(note:Note):Boolean {
        if(original("note:${note.id}",fingerprint(note)))return true
        if(note.created!=null || note.folderId!=null)return false
        // Old registries hashed notes before the optional folder/creation fields existed.
        val fields=codec.parseToJsonElement(codec.encodeToString(note)).jsonObject.filterKeys {it !in setOf("folderId","created")}
        return original("note:${note.id}",demoHash(JsonObject(fields).toString()))
    }
    val reminders=current.local.reminders.filterNot {original("reminder:${it.id}",fingerprint(it))}
    val shared=current.shared
    return current.copy(shared=shared.copy(
        entries=shared.entries.map {if(it.deleted==null && original("entry:${it.id}",fingerprint(it)))it.copy(deleted=now,updated=now)else it},
        notes=shared.notes.map {if(it.deleted==null && originalNote(it))it.copy(deleted=now,updated=now)else it},
        schedules=shared.schedules.map {if(it.deleted==null && reminders.none {r->r.scheduleId==it.id} && original("schedule:${it.id}",fingerprint(it)))it.copy(deleted=now,updated=now)else it}),
        local=current.local.copy(reminders=reminders))
}

fun forceRemoveDemonstration(current:Snapshot):Snapshot {
    val registry=current.local.demo
    val removedPlans=current.shared.schedules.filter {"schedule:${it.id}" in registry}.map {it.id}.toSet()
    return current.copy(shared=current.shared.copy(
        entries=current.shared.entries.filterNot {"entry:${it.id}" in registry},
        notes=current.shared.notes.filterNot {"note:${it.id}" in registry},
        schedules=current.shared.schedules.filterNot {"schedule:${it.id}" in registry}),
        local=current.local.copy(demo=emptyMap(),reminders=current.local.reminders.filterNot {"reminder:${it.id}" in registry}.map {
            if(it.scheduleId in removedPlans)it.copy(enabled=false,scheduleId=null,rowId=null)else it
        }))
}
