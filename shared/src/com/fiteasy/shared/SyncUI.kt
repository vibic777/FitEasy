package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiteasy.shared.sync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** The Russian text of a Notio resource: the key every FitEasy dictionary translates. */
fun nt(name:String):String = notioKeys[name] ?: name
private val sectionNames=mapOf("nutrition" to "Питание","water" to "Напитки","activity" to "Активность","progress" to "Замеры","catalogues" to "Каталоги","recipes" to "Рецепты","profile" to "Профиль","goals" to "Цели","schedules" to "Расписания","notes" to "Заметки","reminders" to "Напоминания")
private fun modeTitle(mode:ImportMode)=nt(when(mode){ImportMode.MERGE->"sync_mode_merge";ImportMode.COPIES->"sync_mode_copies";ImportMode.REPLACE->"sync_mode_replace"})
private fun modeHint(mode:ImportMode)=when(mode){
    ImportMode.MERGE->"Одинаковые записи сопоставляются, у каждой побеждает более новое изменение; недостающее добавляется."
    ImportMode.COPIES->nt("sync_mode_copies_hint")
    ImportMode.REPLACE->"В выбранных разделах принимающее устройство становится точной копией отправляющего: его записи этих разделов сначала удаляются. Остальные разделы не меняются."
}
/** Why a sync or pairing failed, in words (Notio's reasons). */
private fun reasonText(e:Throwable,language:String):String {
    val reason=(e as? SyncException)?.reason ?: return SyncEngine.describe(e)
    return translate(when(reason){
        SyncEngine.NOT_FOUND->nt("sync_err_not_found");SyncReasons.NOT_WAITING->nt("sync_err_not_waiting");SyncReasons.UNKNOWN_PAIRING->nt("sync_err_unknown_pairing")
        SyncReasons.VERSION->nt("sync_err_version");SyncReasons.WRONG_CODE->nt("sync_err_wrong_code");SyncReasons.DENIED->nt("sync_err_denied");else->return SyncEngine.describe(e)
    },language)
}
private fun statsText(counts:List<Int>,language:String)=translate("новых записей {0}, обновлено {1}, без изменений {2}",language).replace("{0}",counts.getOrElse(2){0}.toString()).replace("{1}",counts.getOrElse(3){0}.toString()).replace("{2}",counts.getOrElse(4){0}.toString())
private val stamp=java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private fun stampText(millis:Long)=stamp.format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))

/**
 * Settings → Synchronization, repeating Notio's page and engine (D105): this device, a new connection (wait with a code /
 * find and type the code), the kept connections with Synchronize, Test, Address and Remove, and the log. The device listens
 * and announces itself only while this page is on the screen. A paired device needs no code again: a sync is started on
 * one device and the other asks its person. Appearance, language and protection never travel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SyncPanel(platform:Platform,onUpdated:()->Unit,onClose:()->Unit,host:SyncHost?=null) {
    val language=LocalLanguage.current
    val scope=rememberCoroutineScope()
    val clipboard=LocalClipboardManager.current
    // The app keeps one sync for as long as it runs; a page opened on its own (a preview, a test) makes and closes its own.
    val own=host==null
    val syncHost=remember(host){host ?: SyncHost(platform)}
    val store=syncHost.store;val engine=syncHost.engine
    val foreground by platform.foreground.collectAsState(initial=true)
    DisposableEffect(syncHost){syncHost.page(true);onDispose {syncHost.page(false);if(own)syncHost.close()}}
    LaunchedEffect(syncHost,foreground){if(own)syncHost.foreground(foreground)}
    LaunchedEffect(syncHost){if(own){var seen=syncHost.imported.value;syncHost.imported.collect {if(it!=seen){seen=it;onUpdated()}}}}
    var always by remember {mutableStateOf(syncHost.always)}
    val listening by engine.listening.collectAsState()
    val code by engine.pairingCode.collectAsState()
    val nearby by engine.nearby.collectAsState()
    val pairings by store.pairings.collectAsState()
    val log by store.log.collectAsState()
    val pairedEvent by engine.pairedEvent.collectAsState()
    var deviceName by remember {mutableStateOf(store.deviceName)}
    var renaming by remember {mutableStateOf(false)}
    val portBusy by engine.portBusy.collectAsState()
    var manualPort by remember {mutableIntStateOf(store.port)}
    var choosingPort by remember {mutableStateOf(false)}
    var finding by remember {mutableStateOf(false)}
    var syncing by remember {mutableStateOf<Pairing?>(null)}
    var working by remember {mutableStateOf<String?>(null)}
    var outcome by remember {mutableStateOf<Triple<String,SyncOutcome,ImportMode>?>(null)}
    var checks by remember {mutableStateOf<Pair<String,List<SyncCheck>>?>(null)}
    var removing by remember {mutableStateOf<Pairing?>(null)}
    var addressing by remember {mutableStateOf<Pairing?>(null)}
    var showLog by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf("")}
    var preset by remember {mutableStateOf<Pair<String,String>?>(null)}
    fun text(name:String,value:Any?=null)=translate(nt(name),language).let {if(value==null)it else it.replace("{0}",value.toString())}
    val hint=MaterialTheme.typography.bodySmall

    fun runSync(p:Pairing,direction:SyncDirection,mode:ImportMode,sections:Set<String>?,preset:Boolean=false) {
        working=text("sync_running",p.peerName)
        scope.launch {engine.sync(p.id,direction,mode,sections,preset).onSuccess {outcome=Triple(p.peerName,it,mode);notice=""}.onFailure {notice=text("sync_failed",reasonText(it,language))};working=null}
    }
    /** Everything set beforehand: start at once. Otherwise ask for what is left to choose. */
    fun startSync(p:Pairing) {
        if(!p.allSetBeforehand){syncing=p;return}
        val mode=runCatching {ImportMode.valueOf(p.mode)}.getOrDefault(ImportMode.MERGE);val sections=if(p.allFolders)null else p.folderIds.filter {it in syncSections}.toSet()
        if(!SyncEngine.allowed(p.direction,mode,sections?.toList()))notice=translate("Замещение возможно только в одну сторону.",language) else runSync(p,p.direction,mode,sections,preset=true)
    }
    SettingsPage(onDismissRequest={if(working==null)onClose()},title={Text(nt("settings_sync"))},text={Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(nt("settings_sync_hint"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
        // ---- this device ----
        Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.Text(text("sync_this_device",deviceName),style=MaterialTheme.typography.titleSmall)
            val l=listening
            androidx.compose.material3.Text(if(l==null)text("sync_not_listening") else if(l.addresses.isEmpty())text("sync_no_wifi") else text("sync_listening",l.addresses.joinToString(", "){"$it:${l.port}"}),style=hint,color=if(l==null || l.addresses.isEmpty())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            // The port this device waits on (D132): chosen automatically or set by hand, and whether it is taken.
            portStatus(manualPort,l?.port,portBusy,language).let {(line,problem)->androidx.compose.material3.Text(line,style=hint,color=if(problem)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextButton(onClick={renaming=true}){androidx.compose.material3.Text(text("sync_rename_device"))}
                androidx.compose.material3.TextButton(onClick={choosingPort=true}){androidx.compose.material3.Text(translate("Порт этого устройства",language))}
            }
        }}
        // Ready without this page (D112): off unless the person turns it on.
        Card(Modifier.fillMaxWidth()) {Row(Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Постоянно ждать синхронизацию",style=MaterialTheme.typography.titleSmall)
                Text(if(platform.name=="Windows")"Пока приложение запущено, в том числе в трее, это устройство принимает синхронизацию без открытой страницы." else "Пока приложение на экране, это устройство принимает синхронизацию без открытой страницы. В фоне телефон не ждёт: так бережётся батарея.",style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(always,{always=it;syncHost.setAlways(it)})
        }}
        // ---- a new connection ----
        Text(nt("sync_new_connection"),style=MaterialTheme.typography.titleSmall)
        val c=code
        if(c==null) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(onClick={engine.startPairing()},enabled=listening!=null){androidx.compose.material3.Text(text("sync_wait"))}
                androidx.compose.material3.OutlinedButton(onClick={finding=true}){androidx.compose.material3.Text(text("sync_find"))}
            }
            Text(nt("sync_roles_hint"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } else Card(Modifier.fillMaxWidth()) {Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(nt("sync_code_title"),style=MaterialTheme.typography.bodyMedium)
            SelectionContainer {androidx.compose.material3.Text(c,fontFamily=FontFamily.Monospace,fontSize=28.sp,style=MaterialTheme.typography.headlineMedium)}
            Text(nt("sync_code_hint"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(Modifier.fillMaxWidth())
            androidx.compose.material3.TextButton(onClick={engine.stopPairing()}){androidx.compose.material3.Text(text("action_cancel"))}
        }}
        pairedEvent?.let {name->LaunchedEffect(name){notice=text("sync_paired_with",name);engine.clearPairedEvent()}}
        if(notice.isNotEmpty())androidx.compose.material3.Text(notice,color=MaterialTheme.colorScheme.primary)
        // ---- kept connections ----
        Text(nt("sync_connections"),style=MaterialTheme.typography.titleSmall)
        if(pairings.isEmpty())Text(nt("sync_no_connections"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
        pairings.forEach {p->
            val online=nearby.any {it.deviceId==p.peerDeviceId}
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.Text(p.peerName,style=MaterialTheme.typography.titleSmall)
                androidx.compose.material3.Text(text(if(online)"sync_online" else "sync_offline"),style=hint,color=if(online)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.Text(if(p.lastSyncAt>0)text("sync_last",stampText(p.lastSyncAt)) else text("sync_never"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.Text(p.lastHost?.let {text("sync_address_line",it+":"+(p.lastPort.takeIf {port->port>0} ?: SyncEngine.DEFAULT_PORT))} ?: text("sync_address_unknown"),style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
                // What Synchronize does for this connection: each of the three is asked every time or set beforehand (D109).
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    val asked=translate("выбирается",language)
                    androidx.compose.material3.OutlinedButton(onClick={preset=p.id to "direction"},enabled=working==null){androidx.compose.material3.Text(text("sync_direction")+": "+if(p.askDirection)asked else translate(directionShort(p.direction),language))}
                    androidx.compose.material3.OutlinedButton(onClick={preset=p.id to "mode"},enabled=working==null){androidx.compose.material3.Text(text("sync_mode")+": "+if(p.askMode)asked else translate(modeTitle(runCatching {ImportMode.valueOf(p.mode)}.getOrDefault(ImportMode.MERGE)),language))}
                    androidx.compose.material3.OutlinedButton(onClick={preset=p.id to "sections"},enabled=working==null){androidx.compose.material3.Text(translate("Разделы",language)+": "+if(p.askFolders)asked else if(p.allFolders)translate("Все данные",language) else p.folderIds.count {it in syncSections}.toString())}
                }
                // Which of the two rules applies to this connection now is always written out (D116).
                Text(if(p.allSetBeforehand)"Заданы все три: синхронизация начинается сразу и идёт без подтверждения на другом устройстве." else "Пока хотя бы один выбор делается при синхронизации, другое устройство обязательно спрашивает подтверждение.",style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(onClick={startSync(p)},enabled=working==null){androidx.compose.material3.Text(text("sync_sync"))}
                    androidx.compose.material3.OutlinedButton(onClick={working=text("sync_testing");scope.launch {checks=p.peerName to engine.test(p.id);working=null}},enabled=working==null){androidx.compose.material3.Text(text("sync_test"))}
                    androidx.compose.material3.TextButton(onClick={addressing=p},enabled=working==null){androidx.compose.material3.Text(text("sync_address_edit"))}
                    androidx.compose.material3.TextButton(onClick={removing=p},enabled=working==null){androidx.compose.material3.Text(text("sync_remove"))}
                }
            }}
        }
        androidx.compose.material3.TextButton(onClick={showLog=true}){androidx.compose.material3.Text(text("sync_log")+" (${log.size})")}
    }},confirmButton={})

    if(renaming) {
        var name by remember {mutableStateOf(deviceName)}
        AlertDialog(onDismissRequest={renaming=false},title={Text(nt("sync_rename_device"))},text={OutlinedTextField(name,{name=it.take(40)},singleLine=true,label={Text(nt("sync_device_name"))})},
            confirmButton={androidx.compose.material3.TextButton(enabled=name.isNotBlank(),onClick={store.deviceName=name;deviceName=store.deviceName;renaming=false;if(listening!=null){engine.stopListening();engine.startListening()}}){androidx.compose.material3.Text(text("action_save"))}},
            dismissButton={androidx.compose.material3.TextButton(onClick={renaming=false}){androidx.compose.material3.Text(text("action_cancel"))}})
    }
    if(choosingPort)PortDialog(manualPort,listening?.port,onDismiss={choosingPort=false},onSave={port->
        store.port=port;manualPort=store.port;choosingPort=false;store.log(if(port>0)"port of this device set by hand: $port" else "port of this device: automatic");syncHost.restart()
    })
    if(finding)FindDeviceDialog(nearby,onDismiss={finding=false},onPair={host,port,typed->
        finding=false;working=text("sync_pairing_progress")
        scope.launch {engine.pair(host,port,typed).onSuccess {notice=text("sync_paired_with",it.peerName)}.onFailure {notice=text("sync_failed",reasonText(it,language))};working=null}
    })
    syncing?.let {p->SyncDialog(p,onDismiss={syncing=null},onStart={direction,mode,sections->syncing=null;runSync(p,direction,mode,sections)})}
    preset?.let {(id,kind)->pairings.firstOrNull {it.id==id}?.let {p->PresetDialog(p,kind,onDismiss={preset=null},onSave={change->store.update(p.id,change);preset=null})}}
    addressing?.let {p->AddressDialog(p,onDismiss={addressing=null},onSave={host,port->store.update(p.id){it.copy(lastHost=host,lastPort=port)};store.log("address of ${p.peerName} set by hand: $host:$port");addressing=null;notice=text("sync_address_saved","$host:$port")})}
    removing?.let {p->AlertDialog(onDismissRequest={removing=null},title={androidx.compose.material3.Text(text("sync_remove_title",p.peerName))},
        text={Text("Соединение разрывается на обоих устройствах, если другое сейчас на связи, иначе только здесь. Данные остаются как есть. Чтобы снова синхронизироваться, соединитесь новым кодом.")},
        confirmButton={androidx.compose.material3.TextButton(onClick={removing=null;scope.launch {val both=engine.unpair(p.id);notice=text(if(both)"sync_removed_both" else "sync_removed_here")}}){androidx.compose.material3.Text(text("sync_remove"))}},
        dismissButton={androidx.compose.material3.TextButton(onClick={removing=null}){androidx.compose.material3.Text(text("action_cancel"))}})}
    working?.let {title->AlertDialog(onDismissRequest={},title={androidx.compose.material3.Text(title)},text={LinearProgressIndicator(Modifier.fillMaxWidth())},confirmButton={})}
    outcome?.let {(name,o,mode)->AlertDialog(onDismissRequest={outcome=null},title={androidx.compose.material3.Text(text("sync_done_title",name))},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        o.received?.let {androidx.compose.material3.Text(text("sync_received")+" "+statsText(it,language))}
        o.sent?.let {androidx.compose.material3.Text(text("sync_sent")+" "+statsText(it,language))}
        // Said in words when a merge found nothing newer and nothing missing: it is not a failure, and replacement is the other way.
        if(mode==ImportMode.MERGE && listOfNotNull(o.received,o.sent).all {it.getOrElse(2){0}+it.getOrElse(3){0}==0})Text("Ничего не изменилось: при слиянии запись заменяется только более новой, а недостающее добавляется. Чтобы разделы стали такими же, как на отправляющем устройстве, выберите замещение.",style=hint,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(kotlin.math.abs(o.clockOffset)>=SyncEngine.CLOCK_TOLERANCE)androidx.compose.material3.Text(text("sync_clock_warning",o.clockOffset/60_000),color=MaterialTheme.colorScheme.error)
    }},confirmButton={androidx.compose.material3.TextButton(onClick={outcome=null}){androidx.compose.material3.Text(text("action_close"))}})}
    checks?.let {(name,list)->
        val lines=list.map {(if(it.ok)"✓ " else "✗ ")+text("sync_step_"+it.step).let {t->if(t.startsWith("sync_step_"))it.step else t}+": "+it.detail}
        AlertDialog(onDismissRequest={checks=null},title={androidx.compose.material3.Text(text("sync_test")+" — "+name)},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            list.forEachIndexed {i,check->androidx.compose.material3.Text(lines[i],fontFamily=FontFamily.Monospace,style=hint,color=if(check.ok)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)}
            Text(nt(if(list.all {it.ok})"sync_test_ok" else "sync_test_failed"),style=MaterialTheme.typography.bodyMedium)
        }},confirmButton={androidx.compose.material3.TextButton(onClick={clipboard.setText(AnnotatedString("FitEasy sync, $name\n"+lines.joinToString("\n")))}){androidx.compose.material3.Text(text("sync_copy_report"))}},
            dismissButton={androidx.compose.material3.TextButton(onClick={checks=null}){androidx.compose.material3.Text(text("action_close"))}})
    }
    if(showLog)AlertDialog(onDismissRequest={showLog=false},title={Text(nt("sync_log"))},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
        if(log.isEmpty())Text(nt("sync_log_empty"))
        SelectionContainer {androidx.compose.material3.Text(log.joinToString("\n"),fontFamily=FontFamily.Monospace,style=hint)}
    }},confirmButton={androidx.compose.material3.TextButton(onClick={clipboard.setText(AnnotatedString(log.joinToString("\n")))}){androidx.compose.material3.Text(text("sync_copy_report"))}},
        dismissButton={Row {androidx.compose.material3.TextButton(onClick={store.clearLog()}){androidx.compose.material3.Text(text("sync_log_clear"))};androidx.compose.material3.TextButton(onClick={showLog=false}){androidx.compose.material3.Text(text("action_close"))}}})
    if(own)SyncPrompts(engine,busy=working!=null)
}

/**
 * What another device asks of this one, shown wherever the person is in the app (D112): pairing, or a sync that changes or
 * sends this device's data; and what an incoming connection is doing now.
 */
@Composable fun SyncPrompts(engine:SyncEngine,busy:Boolean=false) {
    val language=LocalLanguage.current
    val prompt by engine.prompt.collectAsState()
    val status by engine.status.collectAsState()
    fun text(name:String,value:Any?=null)=translate(nt(name),language).let {if(value==null)it else it.replace("{0}",value.toString())}
    if(!busy && prompt==null)status?.let {s->AlertDialog(onDismissRequest={},title={Text(nt(when(s){SyncStatus.RECEIVING->"sync_status_receiving";SyncStatus.SENDING->"sync_status_sending";else->"sync_pairing_progress"}))},text={LinearProgressIndicator(Modifier.fillMaxWidth())},confirmButton={})}
    prompt?.let {p->AlertDialog(onDismissRequest={},title={androidx.compose.material3.Text(when(p){is SyncPrompt.Pair->text("sync_prompt_pair",p.peerName);is SyncPrompt.Request->text("sync_prompt_sync",p.peerName)})},text={
        if(p is SyncPrompt.Request)Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(nt(when(p.direction){SyncDirection.SEND->"sync_prompt_send";SyncDirection.RECEIVE->"sync_prompt_receive";SyncDirection.BOTH->"sync_prompt_both"}))
            androidx.compose.material3.Text(text("sync_mode_label")+" "+translate(modeTitle(p.mode),language))
            androidx.compose.material3.Text(translate("Разделы",language)+": "+(p.folderNames?.joinToString(", "){translate(sectionNames[it] ?: it,language)} ?: translate("Все данные",language)))
            if(p.mode==ImportMode.REPLACE && p.direction!=SyncDirection.SEND)Text(if(p.folderNames==null)nt("sync_replace_warning") else "Замещение сначала удалит на принимающем устройстве все записи выбранных разделов.",color=MaterialTheme.colorScheme.error)
            if(kotlin.math.abs(p.clockOffset)>=SyncEngine.CLOCK_TOLERANCE)androidx.compose.material3.Text(text("sync_clock_warning",p.clockOffset/60_000),color=MaterialTheme.colorScheme.error)
        } else Text(nt("sync_prompt_pair_hint"))
    },confirmButton={androidx.compose.material3.TextButton(onClick={p.answer.complete(true)}){androidx.compose.material3.Text(text("sync_allow"))}},dismissButton={androidx.compose.material3.TextButton(onClick={p.answer.complete(false)}){androidx.compose.material3.Text(text("sync_deny"))}})}
}

/**
 * The sync of this device for as long as the app runs (D112): the engine, its store, and when it listens. It listens while
 * the Sync page is on the screen, as before; with "always ready" on it also listens without the page — on a computer while
 * the app runs, in the tray too; on a phone while the app is on the screen, because listening in the background would need
 * a permanent service and its battery cost.
 */
class SyncHost(private val platform:Platform) {
    val imported=MutableStateFlow(0)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    val store=SyncStore(platform)
    val engine=SyncEngine(store,JmdnsDiscovery {line->store.log(line)},FitSyncData(platform){imported.value++},platform.temporaryFile().let {file->file.delete();java.io.File(file.parentFile,"sync-incoming")},scope,multicast={platform.multicast(it)})
    var always=runCatching {platform.readSecret("sync-always")=="true"}.getOrDefault(false)
        private set
    private var pageOpen=false
    private var foreground=true
    fun setAlways(on:Boolean){runCatching {platform.writeSecret("sync-always",on.toString())}.onFailure {AppJournal.record("sync.always.save.failed",JournalLevel.ERROR,it)};always=on;apply()}
    fun page(open:Boolean){pageOpen=open;apply()}
    fun foreground(active:Boolean){foreground=active;apply()}
    @Synchronized private fun apply() {
        val listen=(pageOpen && foreground) || (always && (foreground || platform.name=="Windows"))
        if(listen)engine.startListening() else engine.stopListening()
    }
    /** Listens again from the start: the port of this device was changed (D132). */
    fun restart(){engine.stopListening();apply()}
    fun close(){engine.stopListening();scope.launch {delay(3000);scope.cancel()}}
}

private fun directionShort(direction:SyncDirection)=when(direction){SyncDirection.SEND->"С этого устройства";SyncDirection.RECEIVE->"На это устройство";SyncDirection.BOTH->"В обе стороны"}

/** A paragraph of the help about the port (D132), kept with the code (D120). */
val syncPortHelp=listOf("Порт, на котором устройство ждёт синхронизацию, выбирается автоматически: обычный 47821, а если он занят — любой свободный, и другое устройство находит его само. В блоке «Это устройство» кнопка «Порт этого устройства» позволяет задать порт вручную (от 1024 до 65535) и сразу показывает, свободен он или занят другой программой. Если заданный вручную порт занят, устройство не принимает синхронизацию и пишет об этом.")

/**
 * What the page says about the port this device waits on (D132) and whether it is a problem: [manual] is the port set by
 * hand (0 = automatic), [listening] the port open now, [busy] the port found taken when listening started.
 */
fun portStatus(manual:Int,listening:Int?,busy:Int?,language:String):Pair<String,Boolean> {
    fun say(text:String,vararg values:Int)=values.foldIndexed(translate(text,language)){i,line,value->line.replace("{$i}",value.toString())}
    return when {
        manual>0 && listening==null && busy==manual->say("Порт {0} занят другой программой: это устройство не принимает синхронизацию. Выберите другой порт или «Автоматически».",manual) to true
        manual>0->say("Порт: {0}, задан вручную.",manual) to false
        busy!=null && listening!=null->say("Обычный порт {0} занят другой программой, взят свободный {1}.",busy,listening) to false
        else->say("Порт: {0}, выбран автоматически.",listening ?: SyncEngine.DEFAULT_PORT) to false
    }
}
/** What the dialog says about a port typed by hand: free, taken, used by this app, or not a port at all. [free] is null while it is being checked. */
fun portCheckText(typed:String,listening:Int?,free:Boolean?,language:String):Pair<String,Boolean> {
    val port=typed.toIntOrNull()?.takeIf(SyncEngine::validPort) ?: return translate("Введите порт от 1024 до 65535.",language) to true
    fun say(text:String)=translate(text,language).replace("{0}",port.toString())
    return when {
        port==listening->say("Порт {0} сейчас использует это приложение.") to false
        free==null->say("Проверяется, свободен ли порт {0}…") to false
        free->say("Порт {0} свободен.") to false
        else->say("Порт {0} занят другой программой.") to true
    }
}

/** The port this device waits on (D132): automatic, or typed by hand with a check whether it is free. */
@Composable private fun PortDialog(manual:Int,listening:Int?,onDismiss:()->Unit,onSave:(Int)->Unit) {
    val language=LocalLanguage.current
    var auto by remember {mutableStateOf(manual==0)}
    var typed by remember {mutableStateOf((manual.takeIf {it>0} ?: listening ?: SyncEngine.DEFAULT_PORT).toString())}
    var free by remember {mutableStateOf<Boolean?>(null)}
    val port=typed.toIntOrNull()?.takeIf(SyncEngine::validPort)
    // Checked a moment after the last digit, off the interface thread; the port this app holds is not asked about.
    LaunchedEffect(typed,auto,listening){free=null;if(!auto && port!=null && port!=listening){delay(300);free=withContext(Dispatchers.IO){SyncEngine.portFree(port)}}}
    val usable=port!=null && (port==listening || free==true)
    AlertDialog(onDismissRequest=onDismiss,title={androidx.compose.material3.Text(translate("Порт этого устройства",language))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ChoiceRow(translate("Автоматически",language),translate("Обычный порт {0}, а если он занят — любой свободный. Другое устройство находит порт само.",language).replace("{0}",SyncEngine.DEFAULT_PORT.toString()),auto){auto=true}
        ChoiceRow(translate("Вручную",language),translate("Свой порт нужен, когда в сети или в брандмауэре открыт только определённый порт. Если другое устройство подключается по адресу, введённому вручную, укажите на нём этот же порт.",language),!auto){auto=false}
        if(!auto) {
            OutlinedTextField(typed,{typed=it.filter(Char::isDigit).take(5)},singleLine=true,label={Text(nt("sync_address_port"))})
            portCheckText(typed,listening,free,language).let {(line,problem)->androidx.compose.material3.Text(line,style=MaterialTheme.typography.bodySmall,color=if(problem)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}
        }
    }},confirmButton={androidx.compose.material3.TextButton(enabled=auto || usable,onClick={onSave(if(auto)0 else port!!)}){androidx.compose.material3.Text(translate(nt("action_save"),language))}},
        dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){androidx.compose.material3.Text(translate(nt("action_cancel"),language))}})
}

/** One of the three things set for a connection beforehand: asked at every sync, or a fixed choice. */
@Composable private fun PresetDialog(pairing:Pairing,kind:String,onDismiss:()->Unit,onSave:((Pairing)->Pairing)->Unit) {
    val language=LocalLanguage.current
    fun text(name:String)=translate(nt(name),language)
    val ask=translate("Выбирается при синхронизации",language)
    var askSections by remember {mutableStateOf(pairing.askFolders)}
    var all by remember {mutableStateOf(pairing.allFolders)}
    var sections by remember {mutableStateOf(pairing.folderIds.filter {it in syncSections}.toSet().ifEmpty {syncSections})}
    AlertDialog(onDismissRequest=onDismiss,title={androidx.compose.material3.Text(when(kind){"direction"->text("sync_direction");"mode"->text("sync_mode");else->translate("Разделы",language)}+" — "+pairing.peerName)},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(2.dp)) {
        when(kind) {
            "direction"->{
                ChoiceRow(ask,null,pairing.askDirection){onSave {it.copy(askDirection=true)}}
                ChoiceRow(text("sync_dir_send").replace("{0}",pairing.peerName),null,!pairing.askDirection && pairing.direction==SyncDirection.SEND){onSave {it.copy(askDirection=false,direction=SyncDirection.SEND)}}
                ChoiceRow(text("sync_dir_receive").replace("{0}",pairing.peerName),null,!pairing.askDirection && pairing.direction==SyncDirection.RECEIVE){onSave {it.copy(askDirection=false,direction=SyncDirection.RECEIVE)}}
                ChoiceRow(text("sync_dir_both"),null,!pairing.askDirection && pairing.direction==SyncDirection.BOTH){onSave {it.copy(askDirection=false,direction=SyncDirection.BOTH)}}
            }
            "mode"->{
                ChoiceRow(ask,null,pairing.askMode){onSave {it.copy(askMode=true)}}
                ImportMode.entries.forEach {m->ChoiceRow(translate(modeTitle(m),language),translate(modeHint(m),language),!pairing.askMode && pairing.mode==m.name){onSave {it.copy(askMode=false,mode=m.name)}}}
            }
            else->{
                ChoiceRow(ask,null,askSections){askSections=true}
                ChoiceRow(translate("Все данные",language),null,!askSections && all){askSections=false;all=true}
                ChoiceRow(translate("Выбранные разделы",language),null,!askSections && !all){askSections=false;all=false}
                if(!askSections && !all)syncSections.forEach {s->Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(s in sections,{sections=if(it)sections+s else sections-s});Text(sectionNames.getValue(s))}}
            }
        }
    }},confirmButton={if(kind=="sections")androidx.compose.material3.TextButton(enabled=askSections || all || sections.isNotEmpty(),onClick={onSave {it.copy(askFolders=askSections,allFolders=if(askSections)it.allFolders else all,folderIds=if(askSections || all)it.folderIds else sections.toList())}}){androidx.compose.material3.Text(text("action_save"))}},
        dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){androidx.compose.material3.Text(text("action_cancel"))}})
}

/** Client role of pairing: a device found on the network (or an address typed), then the code shown there. */
@Composable private fun FindDeviceDialog(nearby:List<FoundDevice>,onDismiss:()->Unit,onPair:(String,Int,String)->Unit) {
    val language=LocalLanguage.current
    fun text(name:String)=translate(nt(name),language)
    var target by remember {mutableStateOf<Pair<String,Int>?>(null)}
    var targetName by remember {mutableStateOf("")}
    var address by remember {mutableStateOf("")}
    var code by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(nt("sync_find"))},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(target==null) {
            // Every FitEasy device seen, the ones waiting with a code first; one that does not wait answers "not waiting".
            val devices=nearby.sortedByDescending {it.waitingToPair}
            if(devices.none {it.waitingToPair}){Text(nt("sync_searching_all"));LinearProgressIndicator(Modifier.fillMaxWidth())}
            devices.forEach {d->androidx.compose.material3.OutlinedButton(onClick={target=d.host to d.port;targetName=d.name},modifier=Modifier.fillMaxWidth()) {Column(Modifier.fillMaxWidth()) {
                androidx.compose.material3.Text(d.name+"  ("+d.host+")")
                androidx.compose.material3.Text(if(d.waitingToPair)text("sync_found_waiting") else text("sync_found_not_waiting").replace("{0}",text("sync_wait")),style=MaterialTheme.typography.bodySmall,color=if(d.waitingToPair)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }}}
            Text(nt("sync_manual_hint"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(address,{address=it.trim()},singleLine=true,label={Text(nt("sync_address"))},placeholder={androidx.compose.material3.Text("192.168.1.20:47821")})
            androidx.compose.material3.TextButton(enabled=address.isNotBlank(),onClick={val host=address.substringBefore(':');target=host to (address.substringAfter(':',"").toIntOrNull() ?: SyncEngine.DEFAULT_PORT);targetName=host}){androidx.compose.material3.Text(text("sync_use_address"))}
        } else {
            androidx.compose.material3.Text(text("sync_enter_code").replace("{0}",targetName))
            OutlinedTextField(code,{code=it.uppercase().take(16)},singleLine=true,label={Text(nt("sync_code"))},placeholder={androidx.compose.material3.Text(text="XXXX-XXXX-XXXX")})
        }
    }},confirmButton={target?.let {t->androidx.compose.material3.TextButton(enabled=SyncCrypto.normalizeCode(code).length==SyncCrypto.CODE_LENGTH,onClick={onPair(t.first,t.second,code)}){androidx.compose.material3.Text(text("sync_connect"))}}},
        dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){androidx.compose.material3.Text(text("action_cancel"))}})
}

@Composable private fun ChoiceRow(label:String,hint:String?,selected:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected,onClick)
        Column(Modifier.weight(1f)) {androidx.compose.material3.Text(label);if(hint!=null)androidx.compose.material3.Text(hint,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

/** Before every sync: which way, how the receiving side takes it, and which sections. The choices of last time are offered. */
@Composable private fun SyncDialog(pairing:Pairing,onDismiss:()->Unit,onStart:(SyncDirection,ImportMode,Set<String>?)->Unit) {
    val language=LocalLanguage.current
    fun text(name:String)=translate(nt(name),language)
    var direction by remember {mutableStateOf(pairing.direction)}
    var mode by remember {mutableStateOf(runCatching {ImportMode.valueOf(pairing.mode)}.getOrDefault(ImportMode.MERGE))}
    var all by remember {mutableStateOf(pairing.allFolders)}
    var sections by remember {mutableStateOf(pairing.folderIds.filter {it in syncSections}.toSet().ifEmpty {syncSections})}
    val chosen=if(all)null else sections
    val allowed=SyncEngine.allowed(direction,mode,chosen?.toList()) && (all || sections.isNotEmpty())
    AlertDialog(onDismissRequest=onDismiss,title={androidx.compose.material3.Text(text("sync_with").replace("{0}",pairing.peerName))},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(2.dp)) {
        // Only what was not set for this connection beforehand is asked; the rest is shown as it will be used.
        Text(nt("sync_direction"),style=MaterialTheme.typography.titleSmall)
        if(pairing.askDirection) {
            ChoiceRow(text("sync_dir_send").replace("{0}",pairing.peerName),null,direction==SyncDirection.SEND){direction=SyncDirection.SEND}
            ChoiceRow(text("sync_dir_receive").replace("{0}",pairing.peerName),null,direction==SyncDirection.RECEIVE){direction=SyncDirection.RECEIVE}
            ChoiceRow(text("sync_dir_both"),null,direction==SyncDirection.BOTH){direction=SyncDirection.BOTH}
        } else Text(directionShort(direction))
        Text(nt("sync_mode"),style=MaterialTheme.typography.titleSmall)
        if(pairing.askMode)ImportMode.entries.forEach {m->ChoiceRow(translate(modeTitle(m),language),translate(modeHint(m),language),mode==m){mode=m}} else Text(modeTitle(mode))
        Text("Разделы",style=MaterialTheme.typography.titleSmall)
        if(pairing.askFolders)Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(all,{all=it});Text("Все данные")} else androidx.compose.material3.Text(if(all)translate("Все данные",language) else sections.joinToString(", ") {translate(sectionNames.getValue(it),language)})
        if(pairing.askFolders && !all)syncSections.forEach {s->Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(s in sections,{sections=if(it)sections+s else sections-s});Text(sectionNames.getValue(s))}}
        if(mode==ImportMode.REPLACE)Text("Замещение возможно только в одну сторону.",color=if(allowed)MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    }},confirmButton={androidx.compose.material3.TextButton(enabled=allowed,onClick={onStart(direction,mode,chosen)}){androidx.compose.material3.Text(text("sync_start"))}},
        dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){androidx.compose.material3.Text(text("action_cancel"))}})
}

/** Where to knock when the device is not found by itself; the pairing key stays, so a stranger at that address is refused. */
@Composable private fun AddressDialog(pairing:Pairing,onDismiss:()->Unit,onSave:(String,Int)->Unit) {
    val language=LocalLanguage.current
    fun text(name:String)=translate(nt(name),language)
    var host by remember {mutableStateOf(pairing.lastHost.orEmpty())}
    var port by remember {mutableStateOf((pairing.lastPort.takeIf {it>0} ?: SyncEngine.DEFAULT_PORT).toString())}
    val validHost=SyncAddress.host(host);val validPort=SyncAddress.port(port)
    AlertDialog(onDismissRequest=onDismiss,title={androidx.compose.material3.Text(text("sync_address_title").replace("{0}",pairing.peerName))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(nt("sync_address_hint"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(host,{host=it.trim()},singleLine=true,label={Text(nt("sync_address_host"))})
        OutlinedTextField(port,{port=it.filter(Char::isDigit).take(5)},singleLine=true,label={Text(nt("sync_address_port"))})
        if(validHost==null || validPort==null)Text(nt("sync_address_invalid"),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    }},confirmButton={androidx.compose.material3.TextButton(enabled=validHost!=null && validPort!=null,onClick={onSave(validHost!!,validPort!!)}){androidx.compose.material3.Text(text("action_save"))}},
        dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){androidx.compose.material3.Text(text("action_cancel"))}})
}