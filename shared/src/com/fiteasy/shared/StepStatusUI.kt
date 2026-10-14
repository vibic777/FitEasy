package com.fiteasy.shared

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable fun StepStatusUI(platform:Platform) {
    val foreground by platform.foreground.collectAsState(initial=true)
    var status by remember {mutableStateOf("")}
    var mode by remember {mutableStateOf(platform.stepMode())}
    var enabled by remember {mutableStateOf(platform.stepsEnabled())}
    var diagnostics by remember {mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val clipboard=LocalClipboardManager.current
    LaunchedEffect(platform,foreground){if(foreground)while(true){status=withContext(Dispatchers.IO){platform.stepStatus()};enabled=platform.stepsEnabled();if(platform.name!="Android")break;delay(2000)}}
    if(status.isNotEmpty())Text(status)
    if(platform.name=="Android") {
        // One switch for both states; the label always names what a press will do.
        Button(onClick={val target=!enabled;runCatching {platform.trackSteps(target)}.onSuccess {enabled=platform.stepsEnabled() || target}.onFailure {AppJournal.record("ui.action.failed",JournalLevel.ERROR,it);status="Step counter error; open journal"}},
            colors=if(enabled)ButtonDefaults.outlinedButtonColors()else ButtonDefaults.buttonColors(),border=if(enabled)ButtonDefaults.outlinedButtonBorder()else null){Text(if(enabled)"Выключить шагомер" else "Включить шагомер телефона")}
        Text("Step counting mode")
        FlowRow {listOf("auto" to "Automatic","counter" to "System counter","detector" to "System detector","software" to "Software pedometer").forEach {(id,label)->FilterChip(mode==id,{runCatching {platform.setStepMode(id);mode=id}.onFailure {AppJournal.record("steps.mode.failed",JournalLevel.ERROR,it);status="Step counter error; open journal"}},{Text(label)})}}
        if(mode=="software")Text("Software steps: approximate; higher battery use")
        TextButton(onClick={scope.launch {diagnostics=withContext(Dispatchers.IO){runCatching {platform.stepDiagnostics()}.getOrElse {AppJournal.record("steps.diagnostics.failed",JournalLevel.ERROR,it);"Step counter error; open journal"}}}}){Text("Step diagnostics")}
    }
    diagnostics?.let {report->AlertDialog(onDismissRequest={diagnostics=null},title={Text("Step diagnostics")},text={Column(Modifier.verticalScroll(rememberScrollState())){androidx.compose.foundation.text.selection.SelectionContainer {androidx.compose.material3.Text(report)}}},confirmButton={TextButton(onClick={clipboard.setText(AnnotatedString(report))}){Text("Копировать")}},dismissButton={TextButton(onClick={diagnostics=null}){Text("Закрыть")}})}
}
