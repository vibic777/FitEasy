package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun JournalDialog(platform:Platform,onDismiss:()->Unit) {
    var text by remember {mutableStateOf("")}
    var options by remember {mutableStateOf(AppJournal.options())}
    var hours by remember {mutableStateOf(options.retentionHours.toString())}
    var revision by remember {mutableIntStateOf(0)}
    var error by remember {mutableStateOf(false)}
    var clearing by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val clipboard=LocalClipboardManager.current
    LaunchedEffect(revision){withContext(Dispatchers.IO){runCatching {AppJournal.read()}}.onSuccess {text=it}.onFailure {error=true}}
    fun save(next:JournalOptions){val previous=options;options=next;scope.launch {withContext(Dispatchers.IO){runCatching {AppJournal.setOptions(next)}}.onFailure {options=previous;error=true}}}
    SettingsPage(onDismissRequest=onDismiss,title={Text("App journal")},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row {Text("App journal",Modifier.weight(1f));Switch(options.enabled,{save(options.copy(enabled=it))})}
        FlowRow {JournalLevel.entries.filter {it!=JournalLevel.FATAL}.forEach {level->FilterChip(level.name in options.levels,{save(options.copy(levels=if(level.name in options.levels)options.levels-level.name else options.levels+level.name))},{Text(level.name)})}}
        Text("Fatal errors are always recorded")
        OutlinedTextField(hours,{if(it.length<=4 && it.all(Char::isDigit))hours=it},label={Text("Retention hours (1–8760)")},singleLine=true)
        TextButton(enabled=hours.toIntOrNull()?.let {it in 1..8760}==true,onClick={save(options.copy(retentionHours=hours.toInt()))}){Text("Сохранить")}
        Text("Automatic cleanup")
        FlowRow {listOf(0 to "Never",1 to "Daily",7 to "Weekly",30 to "Monthly").forEach {(days,label)->FilterChip(options.clearDays==days,{save(options.copy(clearDays=days))},{Text(label)})}}
        Text("Journal rotates at 2 × 512 KiB; no private record contents")
        FlowRow {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={revision++}){Text("Refresh")}
            TextButton(enabled=text.isNotEmpty(),onClick={clipboard.setText(AnnotatedString(text))}){Text("Copy journal")}
            TextButton(onClick={clearing=true}){Text("Clear journal")}
            }
        }
        FlowRow {JournalExportType.entries.forEach {type->OutlinedButton(onClick={scope.launch {runCatching {val bytes=withContext(Dispatchers.IO){journalExport(AppJournal.read(),type)};platform.exportFile("FitEasy-journal-${BuildInfo.VERSION}.${type.extension}",type.mime,bytes)}.onFailure {error=true;AppJournal.record("journal.export.failed",JournalLevel.ERROR,it)}}}){Text(type.name)}}}
        if(error)Text("Journal operation failed")
        if(text.isEmpty())Text("Journal is empty")else androidx.compose.foundation.text.selection.SelectionContainer {Text(text.takeLast(32768))}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
    if(clearing)AlertDialog(onDismissRequest={clearing=false},title={Text("Clear journal?")},confirmButton={TextButton(onClick={scope.launch {withContext(Dispatchers.IO){runCatching {AppJournal.clear()}}.onSuccess {revision++;clearing=false}.onFailure {error=true}}}){Text("Clear journal")}},dismissButton={TextButton(onClick={clearing=false}){Text("Отмена")}})
}
