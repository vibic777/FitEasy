package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.lock.DeviceLock
import com.fiteasy.shared.lock.mode
import com.fiteasy.shared.sync.Transfer
import com.fiteasy.shared.sync.applySync
import com.fiteasy.shared.sync.syncSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The settings pages "Security and privacy" and "Backup" repeat Notio's (D106): a titled block, a line that says what it is
// for, and buttons as wide as the page with their names written on them. What FitEasy has and Notio does not — products
// and dishes, reports, the demonstration, resetting — are further blocks of the same kind.

/** One setting of a page: its title and what belongs to it (Notio's SettingBlock). */
@Composable fun SettingBlock(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics {heading()})
        Column(verticalArrangement=Arrangement.spacedBy(4.dp),content=content)
    }
}
@Composable fun Hint(text:String) {Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
/** A button as wide as the page with its name on it, as on Notio's settings pages. */
@Composable fun WideButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    androidx.compose.material3.OutlinedButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth()) {androidx.compose.material3.Text(translate(text,LocalLanguage.current))}
}
/** One of several choices with a line under it that says what it does (Notio's ModeRow). */
@Composable fun ModeRow(label:String,hint:String?,selected:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected=selected,onClick=onClick,role=Role.RadioButton).padding(vertical=4.dp),verticalAlignment=Alignment.Top) {
        RadioButton(selected=selected,onClick=null)
        Column(Modifier.padding(start=12.dp)) {Text(label,style=MaterialTheme.typography.bodyLarge);if(hint!=null)Hint(hint)}
    }
}
@Composable fun ImportModeDialog(appearance:Boolean,onDismiss:()->Unit,onConfirm:(ImportMode,Boolean)->Unit) {
    var mode by remember {mutableStateOf(ImportMode.MERGE)}
    var applyAppearance by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(nt("import_mode_title"))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ModeRow(nt("import_mode_merge"),"Всё имеющееся сохраняется. Если запись есть и там и там, побеждает более новая.",mode==ImportMode.MERGE){mode=ImportMode.MERGE}
        ModeRow(nt("import_mode_new"),nt("import_mode_new_hint"),mode==ImportMode.COPIES){mode=ImportMode.COPIES}
        ModeRow(nt("import_mode_replace"),nt("import_mode_replace_hint"),mode==ImportMode.REPLACE){mode=ImportMode.REPLACE}
        // FitEasy only: a copy may carry the appearance of the device it was made on.
        if(appearance)Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(applyAppearance,{applyAppearance=it});Text("Применить оформление")}
    }},confirmButton={androidx.compose.material3.TextButton(onClick={onConfirm(mode,applyAppearance)}){Text(nt("import_confirm"))}},dismissButton={androidx.compose.material3.TextButton(onClick=onDismiss){Text(nt("action_cancel"))}})
}

/** The password that protects exported copies; kept with the device secrets, never in a copy or a transfer. */
data class BackupPassword(val enabled:Boolean,val password:String)
fun backupPassword(platform:Platform)=BackupPassword(platform.readSecret("backup-password-on")=="true",platform.readSecret("backup-password").orEmpty())
fun saveBackupPassword(platform:Platform,value:BackupPassword){platform.writeSecret("backup-password",value.password.ifEmpty {null});platform.writeSecret("backup-password-on",value.enabled.toString())}
@Composable fun BackupPasswordOptions(platform:Platform) {
    val stored=remember {runCatching {backupPassword(platform)}.getOrDefault(BackupPassword(false,""))}
    var enabled by remember {mutableStateOf(stored.enabled)}
    var saved by remember {mutableStateOf(stored.password)}
    var typed by remember {mutableStateOf(stored.password)}
    var message by remember {mutableStateOf("")}
    fun store(value:BackupPassword)=runCatching {saveBackupPassword(platform,value)}.onFailure {AppJournal.record("backup.password.save.failed",JournalLevel.ERROR,it)}.isSuccess
    Hint(nt("settings_backup_password_hint"))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(nt("backup_password_enable"),Modifier.weight(1f))
        // Switching on needs a password: nothing is stored until one is saved.
        Switch(enabled,{on->enabled=on;if(on && saved.isEmpty())message=nt("backup_password_type_first") else {message="";store(BackupPassword(on,saved))}})
    }
    if(enabled) {
        OutlinedTextField(typed,{typed=it.take(128);message=""},label={Text(nt("backup_password_field"))},singleLine=true,visualTransformation=PasswordVisualTransformation(),supportingText={Text(nt("backup_password_warning"))},modifier=Modifier.fillMaxWidth())
        androidx.compose.material3.Button(enabled=typed.isNotEmpty() && typed!=saved,onClick={if(store(BackupPassword(true,typed))){saved=typed;message=nt("backup_password_saved")}}){Text(nt("backup_password_save"))}
        if(message.isNotEmpty())Text(message,color=MaterialTheme.colorScheme.primary)
    }
}

/** What a reminder shows where anybody can read it — on the lock screen, in a pop-up: device-local, as in Notio. */
fun reminderPrivacy(platform:Platform)=platform.readSecret("reminder-privacy")?.takeIf {it in setOf("generic","title","full")} ?: "generic"
/** Title and text of a reminder as the chosen privacy allows. */
fun reminderShown(r:Reminder,privacy:String,language:String):Pair<String,String> = when(privacy) {
    "generic"->translate(nt("reminder_generic_title"),language) to translate(nt("reminder_generic_text"),language)
    "title"->r.name to ""
    else->r.name to r.comment
}

/** Settings → Security and privacy, as in Notio: the app lock, then what reminders show. */
@Composable fun SecurityPage(platform:Platform,onLockSettings:()->Unit,onLock:(()->Unit)?) {
    val language=LocalLanguage.current
    val lock=remember {DeviceLock(platform)}
    var privacy by remember {mutableStateOf(reminderPrivacy(platform))}
    SettingBlock(nt("settings_lock")) {
        Hint(nt("settings_lock_hint"))
        if(platform.name=="Android")Hint("Пока блокировка включена, снимки и запись экрана приложения запрещены, а в списке недавних приложений его содержимое скрыто.")
        androidx.compose.material3.Text(if(lock.settings.enabled)translate(nt("lock_status_on"),language).replace("{0}",translate(lockMethods.firstOrNull {it.first==lock.settings.mode}?.second ?: "Пароль",language)) else translate(nt("lock_status_off"),language),style=MaterialTheme.typography.bodyMedium)
        WideButton(nt("settings_lock_open"),onClick=onLockSettings)
        if(onLock!=null)WideButton("Заблокировать",onClick=onLock)
    }
    SettingBlock(nt("settings_privacy")) {
        Hint(nt("settings_privacy_hint"))
        listOf("generic" to "settings_privacy_generic","title" to "settings_privacy_title_only","full" to "settings_privacy_full").forEach {(id,name)->
            ModeRow(nt(name),null,privacy==id){privacy=id;runCatching {platform.writeSecret("reminder-privacy",id);platform.notificationSettingsChanged()}.onFailure {AppJournal.record("reminder.privacy.save.failed",JournalLevel.ERROR,it)}}
        }
    }
}

/**
 * Settings → Backup, as in Notio: your data (export, import), the password of copies, database encryption. Then what only
 * FitEasy has, in the same blocks: products and dishes, the database file, reports, the trash, the demonstration, resetting.
 */
@Composable fun DataPage(platform:Platform,stock:List<Food>,demo:Boolean,mutate:((Snapshot)->Snapshot)->Boolean,onAppearance:(Appearance)->Unit,onDatabaseTools:()->Unit,onReport:()->Unit,onTrash:()->Unit,onDemoCreate:()->Unit,onDemoRemove:(Boolean)->Unit,onReset:()->Unit,onDatabaseImported:()->Unit={}) {
    val language=LocalLanguage.current
    val scope=rememberCoroutineScope()
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var withAppearance by remember {mutableStateOf(false)}
    var pendingBytes by remember {mutableStateOf<ByteArray?>(null)}
    var wrongPassword by remember {mutableStateOf(false)}
    var pending by remember {mutableStateOf<Backup?>(null)}
    fun read(bytes:ByteArray,password:String) {
        busy=true
        scope.launch {
            withContext(Dispatchers.IO){runCatching {readBackup(bytes,password)}}.onSuccess {pending=it;pendingBytes=null;wrongPassword=false}
                .onFailure {error->
                    AppJournal.record("backup.import.read.failed",JournalLevel.WARN,error)
                    // A protected copy asks for its password; anything else is not a copy FitEasy can read.
                    if(BackupCrypto.isProtected(bytes)){wrongPassword=pendingBytes!=null;pendingBytes=bytes}else {pendingBytes=null;message="Восстановление не выполнено. Проверьте файл, пароль и изменения данных во время импорта."}
                }
            busy=false
        }
    }
    SettingBlock(nt("settings_data")) {
        Hint("Копия содержит дневник, продукты и блюда, рецепты, комплексы, расписания, заметки, профиль и цели. Паролей и напоминаний в ней нет.")
        WideButton(nt("settings_export_all"),enabled=!busy) {
            busy=true;message=""
            scope.launch {
                runCatching {
                    val bytes=withContext(Dispatchers.IO){val snapshot=platform.load();val password=backupPassword(platform);prepareBackup(snapshot.shared,stock,if(withAppearance)snapshot.local.appearance else null,if(password.enabled)password.password else "")}
                    platform.exportFile("FitEasy-${java.time.LocalDate.now()}.fiteasy","application/octet-stream",bytes)
                }.onSuccess {message="Резервная копия подготовлена"}.onFailure {AppJournal.record("backup.export.failed",JournalLevel.ERROR,it);message=nt("backup_export_failed")}
                busy=false
            }
        }
        WideButton(nt("settings_import_all"),enabled=!busy) {message="";runCatching {platform.importFile {bytes->val password=backupPassword(platform);read(bytes,if(password.enabled)password.password else "")}}.onFailure {AppJournal.record("backup.import.open.failed",JournalLevel.ERROR,it)}}
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Включить оформление",Modifier.weight(1f));Switch(withAppearance,{withAppearance=it})}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotEmpty())Text(message,color=MaterialTheme.colorScheme.primary)
    }
    SettingBlock(nt("settings_backup_password")) {BackupPasswordOptions(platform)}
    SettingBlock(nt("settings_db_encryption")) {
        EncryptionSetting(platform,onDatabaseImported)
        // FitEasy only: its own password for the database, and sending the file by mail.
        WideButton("Сменить пароль базы",onClick=onDatabaseTools)
    }
    SettingBlock(catalogueTransferText(key="Catalogue exchange",language=language)) {CatalogueTransferPanel(platform,stock)}
    SettingBlock("PDF / CSV отчёт") {
        Hint("Отчёт — это выборка дневника за период в PDF или CSV, а не резервная копия.")
        WideButton("PDF / CSV отчёт",onClick=onReport)
    }
    SettingBlock("Демонстрация и обнуление") {
        Hint("Демонстрация добавляет учебные записи и убирается одной кнопкой. Обнуление удаляет данные на этом устройстве без корзины.")
        WideButton("Создать демонстрацию",enabled=!demo,onClick=onDemoCreate)
        WideButton("Удалить демонстрацию",enabled=demo){onDemoRemove(false)}
        WideButton("Удалить демо принудительно",enabled=demo){onDemoRemove(true)}
        WideButton("Обнуление данных",onClick=onReset)
    }
    pendingBytes?.let {bytes->
        var typed by remember(bytes) {mutableStateOf("")}
        AlertDialog(onDismissRequest={pendingBytes=null;wrongPassword=false},title={Text(nt("import_password_title"))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Hint(nt("import_password_hint"))
            OutlinedTextField(typed,{typed=it;wrongPassword=false},label={Text(nt("backup_password_field"))},singleLine=true,isError=wrongPassword,visualTransformation=PasswordVisualTransformation(),supportingText=if(wrongPassword)({Text("Неверный пароль")}) else null,modifier=Modifier.fillMaxWidth())
        }},confirmButton={androidx.compose.material3.TextButton(enabled=typed.isNotEmpty() && !busy,onClick={read(bytes,typed)}){Text(nt("action_continue"))}},dismissButton={androidx.compose.material3.TextButton(onClick={pendingBytes=null;wrongPassword=false}){Text(nt("action_cancel"))}})
    }
    pending?.let {backup->ImportModeDialog(backup.appearance!=null,onDismiss={pending=null},onConfirm={mode,applyAppearance->
        pending=null
        var counts=com.fiteasy.shared.sync.SyncCounts()
        val saved=mutate {current->val (next,changed)=applySync(current.shared,Transfer(sections=syncSections,data=backup.data),mode);counts=changed;current.copy(shared=next)}
        if(saved){if(applyAppearance)backup.appearance?.let(onAppearance);message=translate("Импортировано",language)+": "+translate("новых записей {0}, обновлено {1}, без изменений {2}",language).replace("{0}",counts.added.toString()).replace("{1}",counts.updated.toString()).replace("{2}",counts.kept.toString())}
        else message="Восстановление не выполнено. Проверьте файл, пароль и изменения данных во время импорта."
    })}
}
