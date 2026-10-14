package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable fun ReportEditor(platform:Platform,data:SharedData,date:String,onDismiss:()->Unit) {
    var from by remember {mutableStateOf(LocalDate.parse(date).minusDays(29).toString())}
    var to by remember {mutableStateOf(date)}
    var types by remember {mutableStateOf(reportTypes.toSet())}
    var message by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun export(pdf:Boolean){scope.launch {
        busy=true
        runCatching {
            val report=diaryReport(data,from.trim(),to.trim(),types)
            val bytes=withContext(Dispatchers.Default){if(pdf)platform.reportPdf(report)else reportCsv(report)}
            platform.exportFile("FitEasy-${report.from}-${report.to}.${if(pdf)"pdf" else "csv"}",if(pdf)"application/pdf" else "text/csv",bytes)
        }.onFailure {message=it.message ?: "Не удалось создать отчёт"}
        busy=false
    }}
    SettingsPage(onDismissRequest={if(!busy)onDismiss()},title={Text("Отчёт")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(from,{from=it},enabled=!busy,label={Text("С YYYY-MM-DD")})
        OutlinedTextField(to,{to=it},enabled=!busy,label={Text("По YYYY-MM-DD")})
        reportTypes.zip(listOf("Питание","Напитки","Шаги","Активность","Замеры")).forEach {(id,label)->Row {Checkbox(id in types,{checked->types=if(checked)types+id else types-id},enabled=!busy);Text(label)}}
        Text("Единицы: г, мл, кг, см, минуты. Неизвестные нутриенты не заменяются нулями. PDF — отчёт для чтения; резервная копия создаётся отдельно.")
        if(message.isNotEmpty())Text(message)
        if(busy)CircularProgressIndicator()
    }},confirmButton={Row {TextButton(enabled=!busy && types.isNotEmpty(),onClick={export(false)}){Text("CSV")};TextButton(enabled=!busy && types.isNotEmpty(),onClick={export(true)}){Text("PDF")}}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Закрыть")}})
}
