package com.fiteasy.shared

import java.time.LocalDate
import java.time.temporal.ChronoUnit

val progressPeriods=linkedMapOf("day" to "День","week" to "Неделя","month" to "Месяц","year" to "Год","years" to "Несколько лет","all" to "Всё время","range" to "Диапазон")
data class ProgressRange(val start:LocalDate,val end:LocalDate) {
    init {require(start.year in 1..9999 && end.year in 1..9999);require(!end.isBefore(start)) {"Начало позже конца"};require(!end.isAfter(start.plusYears(100))) {"Максимум 100 лет"}}
}
fun progressRange(period:String,anchor:LocalDate,forward:Boolean,years:Int,customStart:String,customEnd:String,entries:List<Entry>):ProgressRange {
    require(period in progressPeriods && years in 1..100)
    if(period=="range")return ProgressRange(LocalDate.parse(customStart),LocalDate.parse(customEnd))
    if(period=="all") {
        val dates=entries.filter {it.deleted==null}.map {LocalDate.parse(it.date)}
        return ProgressRange(dates.minOrNull() ?: anchor,dates.maxOrNull() ?: anchor)
    }
    fun shift(date:LocalDate,sign:Long)=when(period){"day"->date.plusDays(sign);"week"->date.plusWeeks(sign);"month"->date.plusMonths(sign);"year"->date.plusYears(sign);else->date.plusYears(years*sign)}
    return if(forward)ProgressRange(anchor,shift(anchor,1).minusDays(1))else ProgressRange(shift(anchor,-1).plusDays(1),anchor)
}

val calendarEntryTypes=linkedMapOf("food" to "Питание","drink" to "Вода","steps" to "Шаги","activity" to "Активность","measurement" to "Замеры")
fun calendarDataIndex(entries:List<Entry>)=entries.asSequence().filter {it.deleted==null}.groupBy {it.date}.mapValues {(_,rows)->rows.groupingBy {it.type}.eachCount().filterKeys {it in calendarEntryTypes}}

data class ProgressBucket(val start:LocalDate,val end:LocalDate)
fun progressBuckets(range:ProgressRange):List<ProgressBucket> {
    val days=ChronoUnit.DAYS.between(range.start,range.end)+1
    val annual=range.end.isAfter(range.start.plusYears(10))
    val result=mutableListOf<ProgressBucket>()
    var start=range.start
    while(!start.isAfter(range.end)) {
        val last=when {annual->LocalDate.of(start.year,12,31);days>120->start.withDayOfMonth(start.lengthOfMonth());else->start}
        val end=minOf(last,range.end)
        result+=ProgressBucket(start,end);start=end.plusDays(1)
    }
    return result
}
fun progressSeries(metric:String,buckets:List<ProgressBucket>,entries:List<Entry>,appearance:Appearance,stepSource:String,deviceId:String):List<Double?> {
    require(metric in chartMetrics)
    if(buckets.isEmpty())return emptyList()
    val daily=entries.asSequence().filter {it.deleted==null && it.date>=buckets.first().start.toString() && it.date<=buckets.last().end.toString()}.groupBy {LocalDate.parse(it.date)}
    val bucketRows=Array(buckets.size){mutableListOf<Entry>()}
    val stepTotals=DoubleArray(buckets.size)
    // Boundaries are ordered; binary search keeps multi-year views linear in records plus buckets.
    daily.forEach {(date,rows)->
        var lo=0;var hi=buckets.lastIndex
        while(lo<hi){val mid=(lo+hi)/2;if(date>buckets[mid].end)lo=mid+1 else hi=mid}
        bucketRows[lo].addAll(rows);stepTotals[lo]+=stepTotal(rows,stepSource,deviceId)
    }
    return buckets.mapIndexed {i,_->val rows=bucketRows[i]
        when(metric) {
            "Вес","Талия","Бёдра","Грудь"->rows.filter {it.type=="measurement" && it.name==metric}.maxWithOrNull(compareBy<Entry> {it.date}.thenBy {it.updated})?.let {displayAmount(it.amount,it.unit,appearance.units)}
            "Калории"->rows.filter {it.type in setOf("food","drink") && it.nutrients.kcal!=null}.takeIf {it.isNotEmpty()}?.sumOf {it.nutrients.kcal!!}?.let {displayAmount(it,"kcal",appearance.units)}
            "Вода"->rows.filter {it.type=="drink" && it.plainWater}.takeIf {it.isNotEmpty()}?.sumOf {it.amount}?.let {displayAmount(it,"ml",appearance.units)}
            "Шаги"->if(rows.any {it.type=="steps"})stepTotals[i]else null
            else->rows.filter {it.type=="activity"}.takeIf {it.isNotEmpty()}?.sumOf {it.amount}
        }
    }
}
