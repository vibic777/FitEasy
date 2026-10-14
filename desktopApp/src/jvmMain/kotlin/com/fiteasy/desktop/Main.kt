package com.fiteasy.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.fiteasy.shared.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.sql.DriverManager
import java.util.Base64
import java.security.SecureRandom
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import androidx.compose.ui.graphics.toComposeImageBitmap

fun desktopDataDirectory():File {
    val explicit=System.getProperty("fiteasy.home")
    if(explicit!=null)return File(explicit).apply {mkdirs();check(isDirectory && canWrite())}
    val appRoot=File(DesktopPlatform::class.java.protectionDomain.codeSource.location.toURI()).parentFile?.parentFile
    val portable=appRoot!=null && File(appRoot,"portable.txt").isFile
    val directory=if(portable)File(appRoot,"data")else File((System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"))+"/FitEasy")
    return directory.apply {mkdirs();check(isDirectory && canWrite()) {"FitEasy data directory is not writable"}}
}

fun main() {
    val directory=desktopDataDirectory()
    AppJournal.install(File(directory,"logs"),"windows")
    configureWindowRendering(directory)
    val instance=SingleInstance(directory)
    if(!instance.claimed)return
    try {runFitEasy(instance)} finally {instance.close()}
}

private fun runFitEasy(instance:SingleInstance) = application {
    val platform=remember {DesktopPlatform()}
    var visible by remember {mutableStateOf(true)}
    val state=rememberWindowState(width=1080.dp,height=800.dp)
    val windowRef=remember {arrayOfNulls<java.awt.Window>(1)}
    fun show(){visible=true;state.isMinimized=false;platform.foregroundState.value=true;windowRef[0]?.toFront();windowRef[0]?.requestFocus()}
    val trayAvailable=remember {platform.attachTray(show=::show,exit={if(platform.confirmExit()){platform.close();exitApplication()}})}
    DisposableEffect(instance){instance.ready(show=::show,quit={if(platform.confirmExit()){platform.close();exitApplication()}});onDispose {instance.onShow={};instance.onQuit={}}}
    Window(onCloseRequest={if(platform.confirmExit()){if(trayAvailable && platform.closeToTray()){platform.foregroundState.value=false;visible=false}else {platform.close();exitApplication()}}},visible=visible,title=appSystemName(java.util.Locale.getDefault().language)+" · Vibic777",state=state) {
        DisposableEffect(window){
            windowRef[0]=window
            val listener=java.awt.event.WindowStateListener {event->platform.foregroundState.value=(event.newState and java.awt.Frame.ICONIFIED)==0}
            window.addWindowStateListener(listener)
            onDispose {windowRef[0]=null;window.removeWindowStateListener(listener);platform.close()}
        }
        val bar=rememberWindowBar()
        androidx.compose.runtime.CompositionLocalProvider(LocalWindowBar provides bar,LocalDesktopWindow provides true){FitEasyApp(platform)}
    }
}

class DesktopPlatform : Platform {
    override fun renderMode()=storedRenderMode(directory)
    override fun setRenderMode(mode:String)=storeRenderMode(directory,mode)
    @Volatile private var dirtyEditor=false
    override fun editingChanged(dirty:Boolean){dirtyEditor=dirty}
    fun confirmExit()=!dirtyEditor || javax.swing.JOptionPane.showConfirmDialog(null,"В заметке есть несохранённые изменения. Закрыть без сохранения?","FitEasy",javax.swing.JOptionPane.YES_NO_OPTION)==javax.swing.JOptionPane.YES_OPTION
    private val fileTasks=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.Main)
    val foregroundState=kotlinx.coroutines.flow.MutableStateFlow(true)
    override val foreground:kotlinx.coroutines.flow.Flow<Boolean> get()=foregroundState
    private val reminderChanges=kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity=1)
    override val dataChanged:kotlinx.coroutines.flow.Flow<Unit> get()=reminderChanges
    fun close(){AppJournal.record("windows.close");fileTasks.coroutineContext[kotlinx.coroutines.Job]?.cancel();reminders.close();synchronized(this){if(!writer.flush())AppJournal.fatal("windows.database.unsaved");release()}}
    private val navigationState=kotlinx.coroutines.flow.MutableStateFlow<AppNavigation?>(null)
    override val navigation:kotlinx.coroutines.flow.Flow<AppNavigation?> get()=navigationState
    override fun navigationHandled(token:String){val pending=navigationState.value;if(pending?.token==token)navigationState.compareAndSet(pending,null)}
    fun attachTray(show:()->Unit,exit:()->Unit)=reminders.attach(show,exit,readSecret("language") ?: "ru") {request->
        val language=readSecret("language") ?: "ru"
        val confirmed=!dirtyEditor || javax.swing.JOptionPane.showOptionDialog(null,trayDiscardMessage(language),"FitEasy",javax.swing.JOptionPane.DEFAULT_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE,null,arrayOf(translate("Продолжить",language),translate("Отмена",language)),translate("Отмена",language))==0
        if(confirmed){navigationState.value=request;show()}
    }
    override val name="Windows"
    override fun deviceName()=System.getenv("COMPUTERNAME") ?: runCatching {java.net.InetAddress.getLocalHost().hostName}.getOrNull() ?: "PC"
    override fun notificationSettingsChanged(){reminders.summaryEnabled=statusNotificationEnabled(this);reminders.refresh()}
    // Melody of a reminder (D097, D123): Windows sounds or an MP3/OGG/WAV/AIFF/AU file, copied into the data folder.
    // Settings → Rights (D127): Windows gives the app no rights of its own; the two system pages that affect it are opened from there.
    override fun rights()=listOf(AppRight("windows-notifications",null),AppRight("windows-network",null))
    override fun grantRight(id:String){openSystemPage(id)}
    override fun revokeRight(id:String):Boolean {openSystemPage(id);return false}
    private fun openSystemPage(id:String) {
        AppJournal.record("windows.right.page")
        when(id) {
            "windows-notifications"->ProcessBuilder("rundll32.exe","url.dll,FileProtocolHandler","ms-settings:notifications").start()
            "windows-network"->ProcessBuilder("control.exe","/name","Microsoft.WindowsFirewall","/page","pageConfigureApps").start()
            else->error("Unknown right")
        }
    }
    override fun reminderSoundPickers()=listOf("system","file")
    override fun pickReminderSound(kind:String,current:String?,onResult:(String?)->Unit){val (chosen,path)=DesktopSound.pick(kind=="system",directory,current);if(chosen)onResult(path)}
    override fun reminderSoundTitle(uri:String)=DesktopSound.title(uri)
    override fun previewReminderSound(uri:String?,kind:String,volumePercent:Int){DesktopSound.play(DesktopSound.own(uri,directory),kind,volumePercent,0,null,15_000)}
    override fun stopReminderPreview(){if(DesktopSound.ringing.value==null)DesktopSound.stop()}
    override val ringing:kotlinx.coroutines.flow.Flow<String?> get()=DesktopSound.ringing
    override fun stopRinging(){DesktopSound.stop()}
    private val directory=desktopDataDirectory()
    private val file=File(directory,"fiteasy.db")
    private val keyFile=File(directory,"database-key.dpapi")
    private val journal=File(directory,"conversion.pending")
    private val oldKeyFile=File(directory,"conversion-old-key.dpapi")
    private val reminders=DesktopReminders({readSecret("reminder-runtime")?.let {codec.decodeFromString<Map<String,ReminderDelivery>>(it)} ?: emptyMap()},{writeSecret("reminder-runtime",codec.encodeToString(it));reminderChanges.tryEmit(Unit)})
    init {
        if(journal.exists()) {
            val safety=File(directory,"fiteasy-conversion-safety.db")
            if(safety.exists()) { check(!file.exists() || file.delete()); check(safety.renameTo(file)) }
            if(journal.readText()=="encrypted") { check(oldKeyFile.exists()); oldKeyFile.copyTo(keyFile,true) } else if(keyFile.exists()) check(keyFile.delete())
            check(journal.delete()); oldKeyFile.delete()
        } else {
            val safety=File(directory,"fiteasy-conversion-safety.db")
            if(safety.exists()){open().use {c->c.createStatement().use {s->s.executeQuery("PRAGMA integrity_check").use {r->check(r.next() && r.getString(1)=="ok")}}};check(safety.delete())}
            oldKeyFile.delete()
        }
    }
    private fun key()=if(keyFile.isFile)String(Crypt32Util.cryptUnprotectData(keyFile.readBytes()))else ""
    private fun open(target: File=file,password: String=key()): java.sql.Connection {
        Class.forName("org.sqlite.JDBC")
        val connection=org.sqlite.mc.SQLiteMCSqlCipherConfig.getV4Defaults().withKey(password).build().createConnection("jdbc:sqlite:${target.absolutePath}")
        try {SqlStateStore.migrate(jdbcStateSql(connection));return connection}catch(t:Throwable){connection.close();throw t}
    }
    @Volatile private var cachedSnapshot:Snapshot?=null
    // One connection while the app runs (D080): SQLCipher derives the key once, not on every save.
    // Windows refuses to rename or delete an open file, so release() precedes every such step.
    // The connection has its own lock (D125): the thread that writes holds it, never the lock of the platform.
    private val databaseLock=Any()
    private var held:java.sql.Connection?=null
    private fun release(){synchronized(databaseLock){runCatching {held?.close()};held=null}}
    private fun <T> connected(action:(java.sql.Connection)->T):T = synchronized(databaseLock) {
        if(held?.isClosed!=false || !file.exists()){release();held=open()}
        try {action(held!!)}catch(t:Throwable){release();throw t}
    }
    private val saveFailures=kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity=1)
    override val saveFailed:kotlinx.coroutines.flow.Flow<Unit> get()=saveFailures
    // Saves are written off the interface thread (D125); a failed write puts back what the disk holds and says so.
    private val writer=SaveQueue<Snapshot>("FitEasy-save",{value,persisted->connected {c->val driver=jdbcStateSql(c);driver.transaction {SqlStateStore.write(driver,value,persisted)}}}) {value,persisted,error->
        AppJournal.record("windows.database.save.failed",JournalLevel.ERROR,error)
        synchronized(this){if(cachedSnapshot===value)cachedSnapshot=persisted}
        saveFailures.tryEmit(Unit);reminderChanges.tryEmit(Unit)
    }
    override fun flushSaves()=writer.flush()
    private fun readSnapshot(): Snapshot {writer.flush();return open().use {SqlStateStore.read(jdbcStateSql(it))}}
    @Synchronized override fun load():Snapshot=cachedSnapshot ?: run {writer.flush();connected {SqlStateStore.read(jdbcStateSql(it))}}.also {AppJournal.record("windows.database.loaded");cachedSnapshot=it;writer.known(it);runCatching {reminders.sync(effectiveReminders(it))}}
    @Synchronized override fun save(snapshot:Snapshot){if(cachedSnapshot==snapshot)return;AppJournal.record("windows.database.save");validate(snapshot.shared);validateLocal(snapshot.local);if(cachedSnapshot==null)load();cachedSnapshot=snapshot;writer.submit(snapshot);runCatching {reminders.sync(effectiveReminders(snapshot));DesktopSound.prune(directory,snapshot.local.reminders.mapNotNull {it.soundUri}.toSet())}.onFailure {AppJournal.record("windows.reminders.schedule.failed",JournalLevel.ERROR,it)}}
    override fun readCatalogue(): List<Food> = listOf("products.json","dishes.json").flatMap {name -> javaClass.classLoader.getResourceAsStream(name)?.bufferedReader()?.use {codec.decodeFromString<List<Food>>(it.readText())} ?: emptyList()}
    override fun exportFile(name:String,mime:String,bytes:ByteArray) {AppJournal.record("file.export.request");val chooser=JFileChooser().apply {selectedFile=File(name)};if(chooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION){val target=chooser.selectedFile;if(!target.exists() || javax.swing.JOptionPane.showConfirmDialog(null,"Replace existing file?","FitEasy",javax.swing.JOptionPane.YES_NO_OPTION)==javax.swing.JOptionPane.YES_OPTION)target.writeBytes(bytes)}}
    override fun importFile(onResult:(ByteArray)->Unit) {AppJournal.record("file.import.request");val chooser=JFileChooser();if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){require(chooser.selectedFile.length()<=64*1024*1024);onResult(chooser.selectedFile.readBytes())}}
    override fun launchFileTask(task:suspend ()->Unit){fileTasks.launch {task()}}
    override fun encryptionEnabled()=keyFile.isFile
    private fun secretFile(name:String): File { require(name.matches(Regex("[a-z-]+"))); return File(directory,"secret-$name.dpapi") }
    override fun readSecret(name:String):String?=secretFile(name).takeIf {it.isFile}?.let {String(Crypt32Util.cryptUnprotectData(it.readBytes()))}
    override fun writeSecret(name:String,value:String?) { val target=secretFile(name);if(value==null){if(target.exists())check(target.delete())}else target.writeBytes(Crypt32Util.cryptProtectData(value.toByteArray()));if(name=="language")reminders.setLanguage(value ?: "ru") }
    override fun temporaryFile():File=File.createTempFile("fiteasy-transfer-",".tmp",directory)
    override fun reminderStates()=reminders.states()
    override fun controlReminder(id:String,action:String){reminders.control(id,action)}
    override fun reportPdf(report:DiaryReport)=desktopReportPdf(report)
    override fun prepareBackground(bytes:ByteArray)=desktopBackground(bytes)
    override fun decodeBackground(bytes:ByteArray)=org.jetbrains.skia.Image.makeFromEncoded(desktopBackground(bytes)).toComposeImageBitmap()
    @Synchronized override fun wipeLocal(){
        WorkoutDraftStore.invalidate(this)
        writer.flush();cachedSnapshot=null;release();writer.known(null)
        reminders.sync(emptyList())
        val known=setOf("fiteasy.db","fiteasy.db-journal","fiteasy.db-wal","fiteasy.db-shm","fiteasy-converting.db","fiteasy-conversion-safety.db","database-key.dpapi","conversion-old-key.dpapi","conversion.pending")
        directory.listFiles().orEmpty().filter {it.name in known || (it.name.startsWith("secret-") && it.name.endsWith(".dpapi"))}.forEach {check(it.delete())}
    }
    @Synchronized override fun exportDatabase(password:String):ByteArray {
        val snapshot=load();val target=temporaryFile();check(target.delete())
        try {open(target,password).use {c->val driver=jdbcStateSql(c);driver.transaction {SqlStateStore.write(driver,snapshot)}};return target.readBytes()}finally{target.delete();File(target.path+"-journal").delete()}
    }
    override fun readDatabase(bytes:ByteArray,password:String):Snapshot {
        require(bytes.size in 100..64*1024*1024);val target=temporaryFile()
        try {target.writeBytes(bytes);Class.forName("org.sqlite.JDBC");return org.sqlite.mc.SQLiteMCSqlCipherConfig.getV4Defaults().withKey(password).build().createConnection("jdbc:sqlite:${target.absolutePath}").use {c->c.createStatement().use {s->
            // The file comes from outside (D130): its views and triggers may not call anything beyond plain SQL.
            s.execute("PRAGMA trusted_schema=OFF")
            s.execute("PRAGMA query_only=ON")
            s.executeQuery("PRAGMA integrity_check").use {require(it.next() && it.getString(1)=="ok")}
            SqlStateStore.read(jdbcStateSql(c))
        }}}finally{target.delete()}
    }
    override fun databasePassword()=key()
    override fun changeDatabasePassword(password:String){require(password.length in 8..256);convertEncryption(true,password)}
    override fun setEncryption(enabled:Boolean){convertEncryption(enabled,null)}
    @Synchronized private fun convertEncryption(enabled:Boolean,supplied:String?) {
        AppJournal.record("windows.database.encryption.change")
        if(enabled==encryptionEnabled() && supplied==null)return
        val snapshot=load();check(writer.flush()){"The last change is not saved yet"};release();val previous=if(keyFile.exists())keyFile.readBytes()else null
        val newKey=if(enabled)supplied ?: Base64.getEncoder().encodeToString(ByteArray(32).also {SecureRandom().nextBytes(it)})else ""
        val work=File(directory,"fiteasy-converting.db");check(!work.exists() || work.delete())
        open(work,newKey).use {c->val driver=jdbcStateSql(c);driver.transaction {SqlStateStore.write(driver,snapshot)};c.createStatement().use {s->s.executeQuery("PRAGMA integrity_check").use {r->check(r.next()&&r.getString(1)=="ok")}}}
        val safety=File(directory,"fiteasy-conversion-safety.db");check(!safety.exists())
        if(previous!=null)oldKeyFile.writeBytes(previous)
        journal.writeText(if(previous!=null)"encrypted" else "plain")
        check(file.renameTo(safety))
        try {check(work.renameTo(file));if(enabled)keyFile.writeBytes(Crypt32Util.cryptProtectData(newKey.toByteArray()))else check(keyFile.delete());check(readSnapshot()==snapshot)}catch(t:Throwable){file.delete();safety.renameTo(file);if(previous!=null)keyFile.writeBytes(previous)else keyFile.delete();throw t}
        check(journal.delete());check(safety.delete());oldKeyFile.delete()
        // The new file was written whole from this state and read back equal.
        writer.known(snapshot)
    }
    override fun trackSteps(enabled:Boolean) {if(enabled)error("Phone tracking is available on Android; Windows displays imported records")}
}



