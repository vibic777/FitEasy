package com.fiteasy.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.fiteasy.shared.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.asImageBitmap

class MainActivity : ComponentActivity(), Platform {
    private val reminderRequest=kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    override val reminderOpen:kotlinx.coroutines.flow.Flow<String?> get()=reminderRequest
    override fun reminderOpened(){reminderRequest.value=null}
    private fun acceptReminderIntent(value:android.content.Intent?){value?.getStringExtra("reminderId")?.takeIf {it=="status" || runCatching {java.util.UUID.fromString(it)}.isSuccess}?.let {reminderRequest.value=it}}
    override fun onNewIntent(intent:android.content.Intent){super.onNewIntent(intent);setIntent(intent);acceptReminderIntent(intent)}
    override fun reminderReadiness()=ReminderReadiness(
        androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled(),
        android.os.Build.VERSION.SDK_INT<31 || getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms(),
        getSystemService(android.app.ActivityManager::class.java).isBackgroundRestricted,
        getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
        android.os.Build.VERSION.SDK_INT<34 || getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent(),
        android.provider.Settings.canDrawOverlays(this)
    )
    override fun openReminderAccess(action:String){
        require(action in setOf("notifications","exact","battery","fullscreen","overlay"))
        runOnUiThread {runCatching {
            if(action=="notifications" && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && (!prefs.getBoolean("notificationsAsked",false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))){prefs.edit().putBoolean("notificationsAsked",true).apply();notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);return@runCatching}
            startActivity(accessPage(action))
        }.onFailure {AppJournal.record("android.reminder.access.failed",JournalLevel.ERROR,it)}}
    }
    // The page of the system settings where the right named by the action is switched on and off.
    private fun accessPage(action:String)=when(action){
        "notifications"->android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName)
        "fullscreen"->if(android.os.Build.VERSION.SDK_INT>=34)android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,android.net.Uri.parse("package:$packageName"))else android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName)
        "overlay"->android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName"))
        "exact"->if(android.os.Build.VERSION.SDK_INT>=31)android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName"))else android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName"))
        else->android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName"))
    }
    // Rights in one place (D127): Settings → Rights shows what the system gave, asks for it and gives it back.
    private val rightIds=setOf("notifications","exact","fullscreen","overlay")
    override fun rights():List<AppRight> {val r=reminderReadiness();return listOf(AppRight("notifications",r.notifications),AppRight("exact",r.exact),AppRight("fullscreen",r.fullScreen),AppRight("overlay",r.overlay))}
    override fun grantRight(id:String){require(id in rightIds);openReminderAccess(id)}
    // The notification permission is the only one Android lets an app give up itself (13+, when the app closes); the others are switched off on their system page.
    private fun giveUpNotifications():Boolean {
        if(android.os.Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS);AppJournal.record("android.right.given-up.notifications");return true
    }
    override fun revokeRight(id:String):Boolean {
        require(id in rightIds)
        if(id=="notifications" && giveUpNotifications())return true
        runOnUiThread {runCatching {startActivity(accessPage(id))}.onFailure {AppJournal.record("android.right.page.failed",JournalLevel.ERROR,it)}}
        return false
    }
    override fun revokeAllRights(){
        giveUpNotifications();AppJournal.record("android.right.all")
        runOnUiThread {runCatching {startActivity(accessPage("app"))}.onFailure {AppJournal.record("android.right.page.failed",JournalLevel.ERROR,it)}}
    }
    override fun testAlarm(){AndroidReminders.test(this)}
    // Melody of a reminder (D097): the phone's own sounds through the system picker, or an audio file copied into the app.
    @Volatile private var pickerActive=false
    override fun pickerOpen()=pickerActive
    private var soundCallback:((String?)->Unit)?=null
    private val systemSound=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result->
        val callback=soundCallback;soundCallback=null;pickerActive=false
        @Suppress("DEPRECATION") val picked=result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if(result.resultCode==RESULT_OK && picked!=null)callback?.invoke(picked.toString())
    }
    private val fileSound=registerForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        val callback=soundCallback;soundCallback=null;pickerActive=false
        if(uri!=null)lifecycleScope.launch {callback?.invoke(withContext(Dispatchers.IO){ReminderSounds.importFile(this@MainActivity,uri)})}
    }
    override fun reminderSoundPickers()=listOf("system","file")
    override fun pickReminderSound(kind:String,current:String?,onResult:(String?)->Unit){
        soundCallback=onResult;pickerActive=true
        runCatching {
            if(kind=="file")fileSound.launch(arrayOf("audio/*"))
            else systemSound.launch(android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,android.media.RingtoneManager.TYPE_ALL)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false)
                .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,current?.takeIf {!it.startsWith("file:")}?.let(android.net.Uri::parse) ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)))
        }.onFailure {soundCallback=null;pickerActive=false;AppJournal.record("reminder.sound.picker.failed",JournalLevel.ERROR,it)}
    }
    override fun reminderSoundTitle(uri:String)=ReminderSounds.title(this,uri)
    override fun previewReminderSound(uri:String?,kind:String,volumePercent:Int){ReminderSounds.preview(this,uri,kind,volumePercent)}
    override fun stopReminderPreview(){ReminderSounds.stopPreview()}
    override val ringing:kotlinx.coroutines.flow.Flow<String?> get()=ReminderRingService.ringing
    override fun stopRinging(){ReminderRingService.stop(this)}
    /** Reschedules with the new exactness, refreshes the shade line and lets the step service pick up the battery saver. */
    override fun notificationSettingsChanged(){
        lifecycleScope.launch(Dispatchers.IO){runCatching {AndroidReminders.sync(this@MainActivity,AndroidReminders.read(this@MainActivity),true);AndroidReminders.refreshStatus(this@MainActivity)}.onFailure {AppJournal.record("reminder.settings.failed",JournalLevel.ERROR,it)}}
    }
    private val foregroundState=kotlinx.coroutines.flow.MutableStateFlow(true)
    override val foreground:kotlinx.coroutines.flow.Flow<Boolean> get()=foregroundState
    override fun onStart(){AppJournal.record("android.foreground");super.onStart();foregroundState.value=true;lifecycleScope.launch(Dispatchers.IO){runCatching {AndroidReminders.refreshStatus(this@MainActivity)}.onFailure {AppJournal.record("reminder.status.failed",JournalLevel.WARN,it)}}}
    // What was saved is on the disk before the app leaves the screen (D125): after that Android may end the process at any moment.
    override fun onStop(){AppJournal.record("android.background");foregroundState.value=false;ReminderSounds.stopPreview();if(!AndroidDatabase.flush())AppJournal.fatal("android.database.unsaved");super.onStop()}
    override val saveFailed:kotlinx.coroutines.flow.Flow<Unit> get()=AndroidDatabase.saveFailed
    override fun flushSaves()=AndroidDatabase.flush()
    override val name = "Android"
    override fun deviceName()=listOf(android.os.Build.MANUFACTURER,android.os.Build.MODEL).filter {!it.isNullOrBlank()}.joinToString(" ").ifBlank {"Android"}
    override fun biometricAvailable()=BiometricAuth.available(this)
    override fun authenticateBiometric(onResult:(Boolean)->Unit){BiometricAuth.prompt(this,onResult)}
    private val databaseFile by lazy { getDatabasePath("fiteasy.db").also { it.parentFile?.mkdirs() } }
    private val prefs by lazy { getSharedPreferences("local-security", Context.MODE_PRIVATE) }
    private var importCallback: ((ByteArray)->Unit)? = null
    private var exportBytes = ByteArray(0)


    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private fun showFailure(error:Throwable){AppJournal.record("android.operation.failed",JournalLevel.ERROR,error);android.widget.Toast.makeText(this,error.message ?: "Не удалось выполнить действие",android.widget.Toast.LENGTH_LONG).show()}
    private val createFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes=exportBytes;exportBytes=ByteArray(0);pickerActive=false
        if(uri!=null)lifecycleScope.launch {runCatching {withContext(Dispatchers.IO){contentResolver.openOutputStream(uri)?.use {it.write(bytes)} ?: error("Не удалось открыть файл")}}.onFailure(::showFailure)}
    }
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback=importCallback;importCallback=null;pickerActive=false
        if(uri!=null)lifecycleScope.launch {
            runCatching {withContext(Dispatchers.IO){contentResolver.openInputStream(uri)?.use {stream ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
                while(true){val count=stream.read(buffer);if(count<0)break;total+=count;require(total<=64*1024*1024){"Файл превышает 64 МБ"};output.write(buffer,0,count)}
                output.toByteArray()
            } ?: error("Не удалось открыть файл")}}.onSuccess {bytes->runCatching {callback?.invoke(bytes)}.onFailure(::showFailure)}.onFailure(::showFailure)
        }
    }
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {AndroidReminders.changed.tryEmit(Unit);runCatching {AndroidReminders.refreshStatus(this)}.onFailure {AppJournal.record("reminder.status.failed",JournalLevel.ERROR,it)}}
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); System.loadLibrary("sqlcipher"); recoverConversion();acceptReminderIntent(intent);guardScreen(); setContent { FitEasyApp(this) }
        // Alarms lost while the app was stopped by force come back when it is opened (D115).
        val app=applicationContext;lifecycleScope.launch(Dispatchers.IO){runCatching {AndroidReminders.armOnce(app)}.onFailure {AppJournal.record("reminder.arm.failed",JournalLevel.WARN,it)}}
    }
    private fun recoverConversion(){AndroidDatabase.recover(this)}
    private fun key(): String = if(encryptionEnabled()) prefs.getString("databaseKey",null)?.let(::unseal) ?: error("Database key unavailable") else ""
    private fun open(file: File=databaseFile,password:String=key())=AndroidDatabase.open(file,password)
    override val dataChanged:kotlinx.coroutines.flow.Flow<Unit> get()=AndroidDatabase.changed
    override val reminderChanged:kotlinx.coroutines.flow.Flow<Unit> get()=AndroidReminders.changed
    @Synchronized override fun load():Snapshot=AndroidDatabase.load(this)
    @Synchronized override fun save(snapshot: Snapshot) {
        validate(snapshot.shared);validateLocal(snapshot.local)
        AndroidDatabase.save(this,snapshot)
        afterSave(snapshot)
    }
    override fun mutateSnapshot(transform:(Snapshot)->Snapshot):Snapshot=AndroidDatabase.mutate(this,transform).also(::afterSave)
    private fun afterSave(snapshot:Snapshot){
        // The database commit succeeded; scheduling failures must not report a failed save.
        runCatching {
            val reminders=effectiveReminders(snapshot)
            AndroidReminders.sync(this,reminders)
            if(reminders.any {it.enabled} && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !prefs.getBoolean("notificationsAsked",false)){
                prefs.edit().putBoolean("notificationsAsked",true).apply();runOnUiThread {notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)}
            }
        }.onFailure {prefs.edit().putString("reminders-error",it.javaClass.simpleName).apply();showFailure(it)}
    }
    override fun readCatalogue(): List<Food> = listOf("products.json","dishes.json").flatMap { filename -> assets.open(filename).bufferedReader().use { codec.decodeFromString<List<Food>>(it.readText()) } }
    override fun exportFile(name: String,mime: String,bytes: ByteArray) {AppJournal.record("file.export.request"); exportBytes=bytes;pickerActive=true; createFile.launch(name) }
    override fun importFile(onResult: (ByteArray)->Unit) {AppJournal.record("file.import.request"); importCallback=onResult;pickerActive=true; openFile.launch(arrayOf("application/octet-stream","application/json","*/*")) }
    override fun launchFileTask(task:suspend ()->Unit){lifecycleScope.launch {task()}}
    override fun encryptionEnabled() = prefs.getBoolean("encrypted",false)
    override fun readSecret(name: String): String? = prefs.getString("secret-$name",null)?.let(::unseal)
    override fun writeSecret(name: String,value: String?) { check(prefs.edit().putString("secret-$name",value?.let(::seal)).commit());if(name=="app-lock")guardScreen() }
    // While the app lock is on the window is kept out of screenshots, screen recording and the recent-apps picture (D130), as in Notio.
    private fun guardScreen() {
        val locked=runCatching {readSecret("app-lock")?.let {codec.decodeFromString<com.fiteasy.shared.lock.LockSettings>(it).enabled}==true}.getOrDefault(false)
        runOnUiThread {val flag=android.view.WindowManager.LayoutParams.FLAG_SECURE;if(locked)window.setFlags(flag,flag) else window.clearFlags(flag)}
    }
    override fun temporaryFile(): File = File.createTempFile("fiteasy-transfer-",".tmp",cacheDir)
    override fun reminderStates()=AndroidReminders.states(this)
    override fun controlReminder(id:String,action:String){AndroidReminders.control(this,id,action)}
    override fun reportPdf(report:DiaryReport)=androidReportPdf(report)
    override fun prepareBackground(bytes:ByteArray)=androidBackground(bytes)
    override fun decodeBackground(bytes:ByteArray)=androidBackground(bytes).let {safe->android.graphics.BitmapFactory.decodeByteArray(safe,0,safe.size)?.asImageBitmap() ?: error("Image unavailable")}
    @Synchronized override fun wipeLocal():Unit=synchronized(AndroidDatabase.lock){
        WorkoutDraftStore.invalidate(this)
        trackSteps(false);AndroidReminders.clear(this);AndroidDatabase.invalidate()
        listOf("fiteasy.db","fiteasy.db-journal","fiteasy.db-wal","fiteasy.db-shm","fiteasy-converting.db","fiteasy-conversion-safety.db").forEach {name->val target=File(databaseFile.parentFile,name);check(!target.exists() || target.delete())}
        check(prefs.edit().clear().commit())
        Unit
    }
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
    override fun multicast(enabled: Boolean) {
        if(enabled && multicastLock==null)multicastLock=(applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager).createMulticastLock("fiteasy-discovery").apply {setReferenceCounted(false);acquire()}
        if(!enabled){multicastLock?.let {if(it.isHeld)it.release()};multicastLock=null}
    }
    override fun exportDatabase(password:String):ByteArray=synchronized(AndroidDatabase.lock) {
        val snapshot=load();val target=temporaryFile();check(target.delete())
        try {open(target,password).use {db->val driver=androidStateSql(db);driver.transaction {SqlStateStore.write(driver,snapshot)}};target.readBytes()}finally{target.delete();File(target.path+"-journal").delete()}
    }
    override fun readDatabase(bytes:ByteArray,password:String):Snapshot {
        require(bytes.size in 100..64*1024*1024);val target=temporaryFile()
        try {target.writeBytes(bytes);return SQLiteDatabase.openDatabase(target.absolutePath,password,null,SQLiteDatabase.OPEN_READONLY,null,null).use {db->
            // The file comes from outside (D130): its views and triggers may not call anything beyond plain SQL.
            runCatching {db.execSQL("PRAGMA trusted_schema=OFF")}.onFailure {AppJournal.record("android.database.import.schema-guard.failed",JournalLevel.WARN,it)}
            db.rawQuery("PRAGMA integrity_check",null).use {require(it.moveToFirst() && it.getString(0)=="ok")}
            SqlStateStore.read(androidStateSql(db))
        }}finally{target.delete()}
    }
    override fun databasePassword()=key()
    override fun changeDatabasePassword(password:String){require(password.length in 8..256);convertEncryption(true,password)}
    override fun setEncryption(enabled:Boolean){convertEncryption(enabled,null)}
    @Synchronized private fun convertEncryption(enabled: Boolean,supplied:String?):Unit = synchronized(AndroidDatabase.lock) {
        if(enabled==encryptionEnabled() && supplied==null)return@synchronized
        val snapshot=load();AndroidDatabase.invalidate()
        val newKey=if(enabled)supplied ?: Base64.getEncoder().encodeToString(ByteArray(32).also {SecureRandom().nextBytes(it)}) else ""
        val staged=File(databaseFile.parentFile,"fiteasy-converting.db")
        if(staged.exists())check(staged.delete())
        open(staged,newKey).use {db->val driver=androidStateSql(db);driver.transaction {SqlStateStore.write(driver,snapshot)};db.rawQuery("PRAGMA integrity_check",null).use {check(it.moveToFirst() && it.getString(0)=="ok")}}
        val safety=File(databaseFile.parentFile,"fiteasy-conversion-safety.db")
        check(!safety.exists()) {"Unresolved previous conversion"}
        check(prefs.edit().putBoolean("conversionPending",true).putBoolean("oldEncrypted",encryptionEnabled()).putString("oldDatabaseKey",prefs.getString("databaseKey",null)).commit())
        check(databaseFile.renameTo(safety))
        try {
            check(staged.renameTo(databaseFile))
            check(prefs.edit().putBoolean("encrypted",enabled).putString("databaseKey",if(enabled)seal(newKey)else null).commit())
            check(AndroidDatabase.load(this,false)==snapshot)
        } catch(t: Throwable) { recoverConversion(); throw t }
        AndroidDatabase.invalidate()
        check(prefs.edit().remove("conversionPending").remove("oldEncrypted").remove("oldDatabaseKey").commit())
        check(safety.delete())
    }
    private fun seal(text:String)=AndroidVault.seal(text)
    private fun unseal(text:String)=AndroidVault.unseal(text)
    // The phone pedometer is parked (D091): nothing to start or stop, records already in the diary stay.
    override fun trackSteps(enabled:Boolean) {}
    override fun onDestroy(){multicast(false);super.onDestroy()}
}





