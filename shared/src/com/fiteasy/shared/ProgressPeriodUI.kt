package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun ProgressPeriodEditor(period:String,anchor:String,forward:Boolean,years:Int,from:String,to:String,onCalendar:(String)->Unit,onApply:(String,Boolean,Int,String,String)->Boolean,onDismiss:()->Unit) {
    var selected by remember {mutableStateOf(period)}
    var direction by remember {mutableStateOf(forward)}
    var count by remember {mutableStateOf(years.toString())}
    // Parent dates refresh after a calendar selection; keep the other draft fields.
    var start by remember(from) {mutableStateOf(from)}
    var end by remember(to) {mutableStateOf(to)}
    var invalid by remember {mutableStateOf(false)}
    SettingsPage(onDismissRequest=onDismiss,title={Text("Период прогресса")},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        FlowRow {progressPeriods.forEach {(id,label)->FilterChip(selected==id,{selected=id},{Text(label)})}}
        if(selected !in setOf("range","all")) {
            OutlinedButton(onClick={onCalendar("anchor")}){FitIcon("calendar");Spacer(Modifier.width(8.dp));Text("${translate("Точка отсчёта",LocalLanguage.current)}: $anchor")}
            FlowRow {FilterChip(!direction,{direction=false},{Text("До точки отсчёта")});FilterChip(direction,{direction=true},{Text("От точки отсчёта")})}
        }
        if(selected=="years")OutlinedTextField(count,{if(it.length<=3 && it.all(Char::isDigit))count=it},label={Text("Число лет · 1–100")},singleLine=true)
        if(selected=="range") {
            Row {OutlinedTextField(start,{start=it},label={Text("Начало · YYYY-MM-DD")},singleLine=true,modifier=Modifier.weight(1f));IconAction("calendar","Начало периода",onClick={onCalendar("from")})}
            Row {OutlinedTextField(end,{end=it},label={Text("Конец · YYYY-MM-DD")},singleLine=true,modifier=Modifier.weight(1f));IconAction("calendar","Конец периода",onClick={onCalendar("to")})}
        }
        Text("Границы включены. Длинные диапазоны группируются по месяцам или годам.")
        if(invalid)Text("Проверьте даты и предел 100 лет")
    }},confirmButton={TextButton(enabled=(count.toIntOrNull() ?: 0) in 1..100,onClick={if(onApply(selected,direction,count.toInt(),start,end))onDismiss()else invalid=true}){Text("Применить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
