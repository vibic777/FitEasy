package com.fiteasy.shared

import java.time.LocalDate

val reportTypes=listOf("food","drink","steps","activity","measurement")
data class DiaryReport(val from:String,val to:String,val types:Set<String>,val entries:List<Entry>)
fun diaryReport(data:SharedData,from:String,to:String,types:Set<String>):DiaryReport {
    LocalDate.parse(from);LocalDate.parse(to);require(from<=to && types.isNotEmpty() && types.all {it in reportTypes})
    val entries=data.entries.filter {it.deleted==null && it.date in from..to && it.type in types}.sortedWith(compareBy<Entry>{it.date}.thenBy {it.type}.thenBy {it.updated})
    require(entries.size<=10000){"Report exceeds 10000 records; choose a shorter period"}
    return DiaryReport(from,to,types,entries)
}
fun reportCsv(report:DiaryReport):ByteArray {
    fun cell(s:String)="\""+s.replace("\"","\"\"").let {if(it.trimStart().firstOrNull() in listOf('=','+','-','@') || it.firstOrNull() in listOf('\t','\r'))"'$it" else it}+"\""
    val header="date,type,name,amount,unit,kcal,protein_g,fat_g,carbs_g,fiber_g,meal,source,workout_seconds,workout_finished,distance_km"
    val rows=report.entries.map {e->listOf(e.date,e.type,e.name,e.amount.toString(),e.unit,e.nutrients.kcal?.toString() ?: "",e.nutrients.protein?.toString() ?: "",e.nutrients.fat?.toString() ?: "",e.nutrients.carbs?.toString() ?: "",e.nutrients.fiber?.toString() ?: "",e.meal,e.source,e.workout?.elapsedSeconds?.toString() ?: "",e.workout?.finished?.toString() ?: "",e.distanceKm?.toString() ?: "").joinToString(",",transform=::cell)}
    return ("\uFEFF"+header+"\r\n"+rows.joinToString("\r\n")).toByteArray(Charsets.UTF_8)
}
fun reportLines(report:DiaryReport)=listOf("FitEasy", "${report.from} — ${report.to}",report.types.joinToString(" / "),"Records: ${report.entries.size}","")+report.entries.flatMap {e->
    listOf("${e.date} | ${e.type} | ${e.name}","${number(e.amount)} ${e.unit} | ${e.meal} | ${e.source}","kcal=${number(e.nutrients.kcal)}; P=${number(e.nutrients.protein)} g; F=${number(e.nutrients.fat)} g; C=${number(e.nutrients.carbs)} g; fiber=${number(e.nutrients.fiber)} g")+
    (e.distanceKm?.let {listOf("Distance: ${number(it)} km")} ?: emptyList())+(e.workout?.let {w->listOf("Workout: ${w.elapsedSeconds} s; finished=${w.finished}","Completed=${w.completed.map {it+1}}; skipped=${w.skipped.map {it+1}}")+w.routine.exercises.mapIndexed {i,x->"${i+1}. ${x.name}: ${x.seconds} s / ${x.repetitions}; rest=${x.restSeconds} s"}} ?: emptyList())+listOf("")
}
/** Break at Unicode code points so long imported names cannot overflow the page. */
fun wrapReportLines(lines:List<String>,width:Float,measure:(String)->Float):List<String> = lines.flatMap {line->line.split('\n').flatMap {part->
    val rows=mutableListOf<String>();var row=""
    part.codePoints().toArray().forEach {point->val next=String(Character.toChars(point));if(row.isNotEmpty() && measure(row+next)>width){rows+=row;row=""};row+=next}
    rows+=row;rows
}}
