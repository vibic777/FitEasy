package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.lock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun FitEasyApp(platform:Platform) {
    val lock=remember {DeviceLock(platform)}
    var unlocked by remember {mutableStateOf(!lock.settings.enabled)}
    var configure by remember {mutableStateOf(false)}
    var sync by remember {mutableStateOf(false)}
    var generation by remember {mutableStateOf(0)}
    var dialogAppearance by remember {mutableStateOf(Appearance())}
    var language by remember {mutableStateOf(platform.readSecret("language") ?: "ru")}
    // One sync for as long as the app runs (D112): it listens while its page is open, or always when the person chose so.
    val syncHost=remember {SyncHost(platform)}
    DisposableEffect(syncHost){onDispose {syncHost.close()}}
    val active by platform.foreground.collectAsState(initial=true)
    LaunchedEffect(syncHost,active){syncHost.foreground(active)}
    LaunchedEffect(syncHost){var seen=syncHost.imported.value;syncHost.imported.collect {if(it!=seen){seen=it;generation++}}}
    // Auto-lock (D099): at once, or only after the chosen time in the background. A file or sound picker the person opened
    // from the app keeps it unlocked for at most five minutes, so the form that asked for the file is still there.
    var awaySince by remember {mutableStateOf<Long?>(null)}
    var awayPicker by remember {mutableStateOf(false)}
    LaunchedEffect(platform){platform.foreground.collect {active->
        val s=lock.settings
        fun close(){unlocked=false;configure=false;sync=false}
        if(!s.enabled){awaySince=null;return@collect}
        if(!active){
            awayPicker=platform.pickerOpen()
            if(s.timeoutSeconds==0 && !awayPicker)close() else if(awaySince==null)awaySince=System.currentTimeMillis()
        } else awaySince?.let {since->
            awaySince=null
            val timeout=if(awayPicker && s.timeoutSeconds in 0..299)300 else s.timeoutSeconds
            if(lockAfterBackground(timeout,System.currentTimeMillis()-since))close()
        }
    }}
    LaunchedEffect(platform){platform.navigation.collect {request->if(request!=null){configure=false;sync=false}}}
    CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {
        if(!unlocked) LockGate(lock,active) {unlocked=true}
        else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)){key(generation){FitEasyContent(platform,onSecurity={dialogAppearance=platform.load().local.appearance;configure=true},onSync={dialogAppearance=platform.load().local.appearance;sync=true},onLock=if(lock.settings.enabled)({unlocked=false})else null,onLanguageChanged={language=it})}}

        }
        // A question from a paired device reaches the person wherever they are in the app, not only on the Sync page.
        if(unlocked)SyncPrompts(syncHost.engine)
        if(configure || sync) {
            val dark=dialogAppearance.theme=="dark" || (dialogAppearance.theme=="system" && isSystemInDarkTheme())
            FitTheme(dialogAppearance,fitColorScheme(dialogAppearance.accent,dark)) {
                if(configure) LockConfiguration(lock) {configure=false}
                if(sync) SyncPanel(platform,{generation++},{sync=false},host=syncHost)
            }
        }
    }
    }
}

/** What went wrong with the mail, in words the person can act on (after Notio's mail errors). */
fun mailResultText(result:MailResult):String = when(result) {
    is MailResult.Sent->"Код отправлен"
    is MailResult.Failed->when(result.error) {
        MailError.NOT_CONFIGURED->"Почта настроена не полностью."
        MailError.CONNECTION->"Не удалось подключиться к почтовому серверу. Проверьте имя сервера, порт и интернет."
        MailError.TIMEOUT->"Почтовый сервер не ответил вовремя."
        MailError.TLS->"Защищённое соединение не удалось. Проверьте тип шифрования и порт (587 для STARTTLS, 465 для SSL/TLS)."
        MailError.AUTH->"Сервер не принял логин или пароль."
        MailError.REJECTED->"Сервер отклонил письмо."
        MailError.PROTOCOL->"Сервер ответил неожиданно."
    }
    else->"Не удалось отправить код"
}
/**
 * Settings → Protection: database encryption, after Notio. The switch asks first, the file is converted off the window's
 * thread behind a notice that cannot be dismissed, and a failure changes nothing.
 */
@Composable fun EncryptionSetting(platform:Platform,onImported:()->Unit={}) {
    val language=LocalLanguage.current
    val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
    var encrypted by remember {mutableStateOf(platform.encryptionEnabled())}
    var passphrase by remember {mutableStateOf(if(encrypted)runCatching {platform.databasePassword()}.getOrNull() else null)}
    var shown by remember {mutableStateOf(false)}
    var asking by remember {mutableStateOf<Boolean?>(null)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var failed by remember {mutableStateOf(false)}
    var confirmEmail by remember {mutableStateOf<SmtpSettings?>(null)}
    var importBytes by remember {mutableStateOf<ByteArray?>(null)}
    var importPassword by remember {mutableStateOf<String?>(null)}
    var importWrong by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun say(text:String,error:Boolean=false){message=text;failed=error}
    fun text(name:String)=translate(nt(name),language)
    fun runImport(bytes:ByteArray,password:String?) {scope.launch {
        busy=true
        val result=withContext(Dispatchers.IO){runCatching {
            // An open file first, then this device's own key, then the password the person typed.
            val candidates=listOfNotNull(password,"",runCatching {platform.databasePassword()}.getOrNull()).distinct()
            val imported=candidates.firstNotNullOfOrNull {key->runCatching {platform.readDatabase(bytes,key)}.getOrNull()} ?: error("password")
            platform.save(imported.copy(local=imported.local.copy(phoneCounter=null,demo=emptyMap(),legalNoticeVersion=0)))
        }}
        busy=false
        result.onSuccess {importBytes=null;importPassword=null;importWrong=false;say(text("backup_import_done").substringBefore(':'));onImported()}
            .onFailure {if(importPassword==null)importPassword="" else importWrong=true;AppJournal.record("database.file.import.failed",JournalLevel.WARN,it)}
    }}
    Hint("Весь файл базы на этом устройстве шифруется. Файл преобразуется на месте, данные не теряются.")
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(nt("db_encryption_enable"),Modifier.weight(1f))
        Switch(encrypted,{asking=it},enabled=!busy)
    }
    val key=passphrase
    if(encrypted && key!=null) {
        OutlinedTextField(key,{},readOnly=true,singleLine=true,label={Text(nt("db_encryption_password_field"))},visualTransformation=if(shown)androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon={NotioIconAction(if(shown)NotioIcons.VisibilityOff else NotioIcons.Visibility,if(shown)"Скрыть" else "Показать пароль базы",{shown=!shown})},modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString(key));say(text("db_encryption_copied"))}){androidx.compose.material3.Text(text("action_copy"))}
            androidx.compose.material3.OutlinedButton(enabled=!busy,onClick={val smtp=runCatching {DeviceLock(platform).settings.smtp}.getOrNull();if(smtp==null || !smtp.isComplete)say(text("db_encryption_email_not_configured"),true) else confirmEmail=smtp}){androidx.compose.material3.Text(text("db_encryption_email_button"))}
        }
        Hint(nt("db_encryption_password_warning"))
    }
    if(message.isNotEmpty())androidx.compose.material3.Text(translate(message,language),style=MaterialTheme.typography.bodyMedium,color=if(failed)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    HorizontalDivider(Modifier.padding(vertical=8.dp))
    Hint(nt("db_file_transfer_hint"))
    WideButton(nt("db_file_export"),enabled=!busy) {scope.launch {
        busy=true
        runCatching {val bytes=withContext(Dispatchers.IO){platform.exportDatabase(platform.databasePassword())};platform.exportFile("FitEasy-${java.time.LocalDate.now()}.db","application/octet-stream",bytes)}
            .onSuccess {say(text("db_file_export_done"))}.onFailure {AppJournal.record("database.file.export.failed",JournalLevel.ERROR,it);say(text("db_file_export_failed"),true)}
        busy=false
    }}
    WideButton(nt("db_file_import"),enabled=!busy) {runCatching {platform.importFile {bytes->importBytes=bytes;importPassword=null;importWrong=false}}.onFailure {AppJournal.record("database.file.open.failed",JournalLevel.ERROR,it)}}
    asking?.let {enable->AlertDialog(onDismissRequest={asking=null},title={Text(nt(if(enable)"db_encryption_confirm_on_title" else "db_encryption_confirm_off_title"))},
        text={Text(if(enable)"Файл базы преобразуется на месте, данные не теряются. Обычная программа для SQLite больше не откроет файл напрямую; резервная копия .fiteasy по-прежнему открывается." else "Файл базы снова станет обычным файлом SQLite. Данные не теряются.")},
        confirmButton={androidx.compose.material3.TextButton(onClick={
            asking=null;busy=true;message=""
            scope.launch {
                val result=withContext(Dispatchers.IO){runCatching {platform.setEncryption(enable)}}
                encrypted=platform.encryptionEnabled();passphrase=if(encrypted)runCatching {platform.databasePassword()}.getOrNull() else null;shown=false
                result.onFailure {AppJournal.record("database.encryption.failed",JournalLevel.ERROR,it);say(text(if(enable)"db_encryption_enable_failed" else "db_encryption_disable_failed"),true)}
                busy=false
            }
        }){androidx.compose.material3.Text(text("action_continue"))}},dismissButton={androidx.compose.material3.TextButton(onClick={asking=null}){androidx.compose.material3.Text(text("action_cancel"))}})}
    if(busy)AlertDialog(onDismissRequest={},title={Text(nt("db_encryption_busy_title"))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text(nt("db_encryption_busy_message"));LinearProgressIndicator(Modifier.fillMaxWidth())}},confirmButton={})
    confirmEmail?.let {smtp->AlertDialog(onDismissRequest={confirmEmail=null},title={Text(nt("db_encryption_email_confirm_title"))},text={androidx.compose.material3.Text(text("db_encryption_email_confirm_message").replace("{0}",smtp.to))},
        confirmButton={androidx.compose.material3.TextButton(onClick={
            confirmEmail=null
            scope.launch {
                busy=true
                val result=runCatching {SmtpClient().send(smtp,text("db_encryption_mail_subject"),text("db_encryption_mail_body").replace("{0}",passphrase.orEmpty()))}.getOrElse {MailResult.Failed(MailError.CONNECTION)}
                busy=false
                if(result is MailResult.Sent)say(text("db_encryption_email_sent")) else say(mailResultText(result),true)
            }
        }){androidx.compose.material3.Text(text("action_continue"))}},dismissButton={androidx.compose.material3.TextButton(onClick={confirmEmail=null}){androidx.compose.material3.Text(text("action_cancel"))}})}
    importBytes?.let {bytes->AlertDialog(onDismissRequest={importBytes=null;importPassword=null;importWrong=false},title={Text(nt("db_file_import_confirm_title"))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Импорт файла базы данных заменяет всё, что сейчас есть на этом устройстве: дневник, заметки и настройки. Это необратимо. Защита этого устройства сохраняется.")
        if(importPassword!=null)OutlinedTextField(importPassword.orEmpty(),{importPassword=it;importWrong=false},label={Text(nt("db_encryption_password_field"))},singleLine=true,isError=importWrong,visualTransformation=PasswordVisualTransformation(),supportingText=if(importWrong)({Text(nt("transfer_error_wrong_password"))}) else null,modifier=Modifier.fillMaxWidth())
    }},confirmButton={androidx.compose.material3.TextButton(enabled=!busy,onClick={runImport(bytes,importPassword?.takeIf {it.isNotEmpty()})}){androidx.compose.material3.Text(text("action_continue"),color=MaterialTheme.colorScheme.error)}},
        dismissButton={androidx.compose.material3.TextButton(onClick={importBytes=null;importPassword=null;importWrong=false}){androidx.compose.material3.Text(text("action_cancel"))}})}
}
/** Name of a way to sign in, in the order they are offered everywhere: PIN, pattern, password, login. */
val lockMethods=listOf("pin" to "PIN-код","pattern" to "Графический ключ","password" to "Пароль","login" to "Логин и пароль")
private val lockMethodIcons=mapOf("pin" to "keypad","pattern" to "pattern","password" to "key","login" to "profile")
private fun lockMethodName(mode:String)=lockMethods.firstOrNull {it.first==mode}?.second ?: "Пароль"
/** Ready security questions, as in Notio; the chosen one is stored as its text in the language it was chosen in. */
val securityQuestions=listOf("Как звали вашего первого питомца?","В каком городе вы родились?","Какая девичья фамилия вашей матери?","Как называлась ваша первая школа?","Какая ваша любимая книга?")
private val lockTimeoutNames=mapOf(0 to "Сразу",30 to "Через 30 секунд",60 to "Через 1 минуту",300 to "Через 5 минут",900 to "Через 15 минут",LOCK_NEVER to "Только при запуске приложения")
fun lockTimeoutLabel(seconds:Int,language:String):String = lockTimeoutNames[seconds]?.let {translate(it,language)} ?: translate("{0} мин {1} с",language).replace("{0}",(seconds/60).toString()).replace("{1}",(seconds%60).toString())

/**
 * Asks for the secret of one way to sign in (D096, after Notio's lock screens). Unlocking asks once; creating asks twice.
 * A PIN whose length is known submits itself at the last digit; the pad's Enter key submits at any length. A pattern
 * submits when the finger lifts. The result is the credential text DeviceLock expects.
 */
@Composable fun SecretForm(mode:String,creating:Boolean,onSubmit:(String)->Unit,pinLength:Int?=null,enabled:Boolean=true,message:String="",submitLabel:String="Продолжить") {
    var login by remember(mode,creating) {mutableStateOf("")}
    var value by remember(mode,creating) {mutableStateOf("")}
    var confirm by remember(mode,creating) {mutableStateOf("")}
    var first by remember(mode,creating) {mutableStateOf<String?>(null)}
    var local by remember(mode,creating) {mutableStateOf("")}
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        when(mode) {
            "pin"->{
                val chosen=first
                if(!creating) {Text("Введите PIN-код");PinPad(value,{value=it;local=""},expectedLength=pinLength,enabled=enabled) {val typed=value;value="";if(typed.isNotEmpty())onSubmit(typed)}}
                else if(chosen==null) {Text("Придумайте PIN-код: от 4 до 12 цифр");PinPad(value,{value=it;local=""},enabled=enabled) {if(value.length in 4..12){first=value;value=""}else local="PIN-код: только цифры, от 4 до 12"}}
                else {Text("Повторите PIN-код");PinPad(confirm,{confirm=it},expectedLength=chosen.length,maxLength=chosen.length,enabled=enabled) {if(confirm==chosen)onSubmit(chosen)else {local="PIN-коды не совпадают";first=null;value=""};confirm=""}}
            }
            "pattern"->{
                Text(if(!creating)"Нарисуйте графический ключ" else if(first==null)"Соедините не менее четырёх точек" else "Нарисуйте ключ ещё раз")
                PatternInput({local=""}) {drawn->
                    val chosen=first
                    when {
                        drawn.split(',').count {it.isNotEmpty()}<4->{local="Соедините не менее четырёх точек";first=null}
                        !creating->onSubmit(drawn)
                        chosen==null->first=drawn
                        chosen==drawn->onSubmit(drawn)
                        else->{local="Ключи не совпадают";first=null}
                    }
                }
            }
            else->{
                if(mode=="login")OutlinedTextField(login,{login=it;local=""},label={Text("Логин")},singleLine=true,enabled=enabled)
                OutlinedTextField(value,{value=it;local=""},label={Text("Пароль")},singleLine=true,enabled=enabled,visualTransformation=PasswordVisualTransformation())
                if(creating)OutlinedTextField(confirm,{confirm=it;local=""},label={Text("Повторите пароль")},singleLine=true,enabled=enabled,visualTransformation=PasswordVisualTransformation())
                Button(enabled=enabled,onClick={
                    when {
                        mode=="login" && login.isBlank()->local="Укажите логин"
                        creating && value.length<4->local="Пароль: не менее 4 символов"
                        creating && value!=confirm->local="Пароли не совпадают"
                        value.isNotEmpty()->{val typed=if(mode=="login")login.trim()+LOGIN_SEPARATOR+value else value;value="";confirm="";onSubmit(typed)}
                    }
                }){Text(submitLabel)}
            }
        }
        val shown=local.ifEmpty {message}
        if(shown.isNotEmpty())Text(shown,color=MaterialTheme.colorScheme.error)
    }
}

/** The screen over a locked app: one centred form, the wait after wrong tries, biometrics, and "forgot" leading to recovery. */
@Composable fun LockGate(lock:DeviceLock,foreground:Boolean=true,onUnlock:()->Unit) {
    val language=LocalLanguage.current
    val recoveries=listOf("question" to "Ответить на контрольный вопрос","email" to "Отправить код на мою почту","totp" to "Использовать приложение-аутентификатор").filter {(id,_)->(id=="question" && lock.settings.answerHash.isNotEmpty()) || (id=="totp" && lock.settings.totp.isNotEmpty()) || (id=="email" && lock.settings.smtp.isComplete)}
    var forgot by remember {mutableStateOf(lock.settings.recoveryOnly)}
    var recovery by remember {mutableStateOf(recoveries.firstOrNull()?.first ?: "question")}
    var value by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var blockedUntil by remember {mutableLongStateOf(lock.settings.blockedUntil)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    val scope=rememberCoroutineScope()
    // The countdown of a delay runs only while the lock screen is looked at (D121, rule 1): in the background nothing ticks.
    LaunchedEffect(blockedUntil,foreground) {if(foreground){while(blockedUntil>System.currentTimeMillis()){now=System.currentTimeMillis();kotlinx.coroutines.delay(500)}};now=System.currentTimeMillis()}
    fun attempt(kind:String,typed:String) {scope.launch {
        busy=true
        runCatching {withContext(Dispatchers.Default){if(!lock.settings.enabled)true else if(kind=="password")lock.unlock(typed)else lock.recover(kind,typed)}}
            .onSuccess {if(it)onUnlock()else message="Неверный пароль, ответ или код"}.onFailure {message=it.message ?: "Ошибка"}
        blockedUntil=lock.settings.blockedUntil;now=System.currentTimeMillis();if(lock.settings.recoveryOnly)forgot=true;value="";busy=false
    }}
    val biometric=lock.settings.biometric && lock.biometricAvailable()
    fun askBiometric() {runCatching {lock.biometric {if(it)onUnlock()else message="Биометрия не подтверждена"}}.onFailure {message=it.message ?: "Ошибка"}}
    LaunchedEffect(Unit) {if(biometric && !lock.settings.recoveryOnly && System.currentTimeMillis()>=lock.settings.blockedUntil)askBiometric()}
    Surface(Modifier.fillMaxSize()) {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp,Alignment.CenterVertically),horizontalAlignment=Alignment.CenterHorizontally) {
        WindowBarSpace()
        Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(14.dp)){RigSymbol("lock",MaterialTheme.colorScheme.onPrimaryContainer)}}
        Text("FitEasy",style=MaterialTheme.typography.headlineMedium)
        val waiting=blockedUntil>now
        if(waiting)androidx.compose.material3.Text(translate("Подождите, секунд",language)+": "+((blockedUntil-now+999)/1000),color=MaterialTheme.colorScheme.error)
        if(!forgot) {
            SecretForm(lock.settings.mode,creating=false,onSubmit={attempt("password",it)},pinLength=lock.settings.pinLength.takeIf {it>0},enabled=!waiting && !busy,message=message)
            if(biometric)RigAction("fingerprint",translate("Биометрия",language),{askBiometric()},translated=false)
            if(recoveries.isNotEmpty())androidx.compose.material3.TextButton(onClick={forgot=true;message="";value=""}){Text("Забыли код или пароль?")}
        } else {
            Text("Восстановление доступа",style=MaterialTheme.typography.titleMedium)
            if(recoveries.isEmpty())Text("Способ восстановления не настроен",color=MaterialTheme.colorScheme.error)
            else {
                Text("Как вы хотите вернуть доступ?")
                recoveries.forEach {(id,label)->SecurityRow(when(id){"question"->"help";"email"->"send";else->"time"},label,"",selected=recovery==id,arrow=false){recovery=id;value="";message=""}}
                if(recovery=="question")Text(lock.settings.question)
                if(recovery=="email")androidx.compose.material3.OutlinedButton(enabled=!busy,onClick={scope.launch {busy=true;runCatching {lock.sendRecovery()}.onSuccess {message=mailResultText(it)}.onFailure {message=it.message ?: "Ошибка"};busy=false}}){Text("Отправить код")}
                OutlinedTextField(value,{value=it},label={Text("Ответ или код")},singleLine=true,enabled=!waiting && !busy)
                Button(enabled=!waiting && !busy && value.isNotEmpty(),onClick={attempt(recovery,value)}){Text("Продолжить")}
                if(message.isNotEmpty())Text(message,color=MaterialTheme.colorScheme.error)
            }
            if(!lock.settings.recoveryOnly)TextButton(onClick={forgot=false;message="";value=""}){Text("Назад")}
        }
    }}
}

/** A row of the security menu: icon in a tinted circle, title and state; the arrow was dropped as noise (D104). */
@Composable private fun SecurityRow(icon:String,title:String,summary:String,selected:Boolean=false,arrow:Boolean=true,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large,color=if(selected)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,tonalElevation=1.dp) {
        Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(9.dp)){RigSymbol(icon,MaterialTheme.colorScheme.onPrimaryContainer)}}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.titleMedium);if(summary.isNotEmpty())Text(summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}

/**
 * Settings → Protection (D096, after Notio): while off, choose a way to sign in and set it; while on, a menu whose items
 * open one small page each — change the way to sign in, security question, authenticator, mail, wrong-try limits, turn off.
 * Changing or turning off asks for the current secret first. Every page saves on its own.
 */
@Composable fun LockConfiguration(lock:DeviceLock,onClose:()->Unit) {
    var s by remember {mutableStateOf(lock.settings)}
    var section by remember {mutableStateOf("")}
    var mode by remember {mutableStateOf(if(lock.settings.enabled)lock.settings.mode else "pin")}
    var verified by remember {mutableStateOf(false)}
    var question by remember {mutableStateOf(s.question)}
    var answer by remember {mutableStateOf("")}
    var totp by remember {mutableStateOf(s.totp)}
    var totpCode by remember {mutableStateOf("")}
    var host by remember {mutableStateOf(s.smtp.host)}
    var port by remember {mutableStateOf(s.smtp.port.toString())}
    var username by remember {mutableStateOf(s.smtp.username)}
    var smtpPassword by remember {mutableStateOf(s.smtp.password)}
    var recipient by remember {mutableStateOf(s.smtp.to)}
    var smtpSecurity by remember {mutableStateOf(s.smtp.security)}
    var freeAttempts by remember {mutableStateOf(s.freeAttempts.toString())}
    var delay by remember {mutableStateOf(s.delaySeconds.toString())}
    var maximum by remember {mutableStateOf(s.maxAttempts.toString())}
    var wipe by remember {mutableStateOf(s.wipeAfterLimit)}
    var wipeConfirmation by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var customMinutes by remember {mutableStateOf((s.timeoutSeconds.coerceAtLeast(0)/60).toString())}
    var customSeconds by remember {mutableStateOf((s.timeoutSeconds.coerceAtLeast(0)%60).toString())}
    val language=LocalLanguage.current
    val scope=rememberCoroutineScope()
    fun open(id:String) {section=id;message="";verified=false}
    /** Runs a change off the main thread, stores it, and returns to the menu when it worked. */
    fun apply(stay:Boolean=false,change:suspend ()->LockSettings) {scope.launch {
        busy=true
        runCatching {withContext(Dispatchers.Default){change()}}.onSuccess {lock.configure(it);s=lock.settings;message="";if(!stay)section=""}.onFailure {s=lock.settings;message=it.message ?: "Проверьте параметры"}
        busy=false
    }}
    fun mail()=SmtpSettings(host.trim(),port.toInt(),smtpSecurity,username.trim(),smtpPassword,"",recipient.trim()).also {require(it.host.isEmpty() || it.isComplete){"Проверьте параметры"}}
    fun save() {when(section) {
        "question"->apply {val q=lock.question(question,answer);lock.settings.copy(question=q.question,answerSalt=q.answerSalt,answerHash=q.answerHash)}
        // A new key is kept only after the authenticator proved it has it (as in Notio): a mistyped key must not become the way back in.
        "totp"->apply {require(totp.isEmpty() || totp==lock.settings.totp || Totp.verify(totp,totpCode.trim(),System.currentTimeMillis())){"Код не подошёл. Проверьте время на устройстве и введите текущий код из приложения-аутентификатора."};totpCode="";lock.settings.copy(totp=totp)}
        "mail"->apply {lock.settings.copy(smtp=mail())}
        "timeout"->apply {
            val seconds=customSeconds.toInt();val total=customMinutes.toInt()*60+seconds
            require(seconds in 0..59 && total in 0..LOCK_MAX_SECONDS){"Введите минуты и секунды: секунды от 0 до 59, не больше 23 ч 59 мин 59 с."}
            lock.settings.copy(timeoutSeconds=total)
        }
        "limits"->apply {
            val free=freeAttempts.toInt();val seconds=delay.toInt();val max=maximum.toInt();require(free in 0..100 && seconds in 1..900 && max in 0..1000){"Проверьте параметры"}
            require(!wipe || (max>0 && (lock.settings.wipeAfterLimit || wipeConfirmation=="УДАЛИТЬ"))) {"Укажите лимит и подтверждение УДАЛИТЬ"}
            lock.settings.copy(freeAttempts=free,delaySeconds=seconds,maxAttempts=max,wipeAfterLimit=wipe)
        }
    }}
    /** Asks for the current secret; a wrong try counts exactly as on the lock screen. */
    @Composable fun Verify(then:()->Unit) {
        Text("Введите текущий код или пароль")
        SecretForm(s.mode,creating=false,pinLength=s.pinLength.takeIf {it>0},enabled=!busy,message=message,onSubmit={typed->scope.launch {
            busy=true
            runCatching {withContext(Dispatchers.Default){lock.unlock(typed)}}.onSuccess {if(it){message="";then()}else message="Неверный пароль, ответ или код"}.onFailure {message=it.message ?: "Ошибка"}
            s=lock.settings;busy=false
        }})
    }
    @Composable fun Methods() {lockMethods.forEach {(id,label)->SecurityRow(lockMethodIcons.getValue(id),label,"",selected=mode==id,arrow=false){mode=id;message=""}}}
    val forms=setOf("question","totp","mail","limits","timeout")
    SettingsPage(onDismissRequest={if(section.isEmpty())onClose()else open("")},title={Text(when(section){"change"->"Сменить способ входа";"timeout"->"Автоблокировка";"question"->"Контрольный вопрос";"totp"->"Аутентификатор TOTP";"mail"->"Почта для кода восстановления";"limits"->"Ограничение попыток";"off"->"Отключить блокировку";else->"Защита и восстановление"})},text={
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            when(section) {
                ""->if(!s.enabled) {
                    Text("Защита выключена",style=MaterialTheme.typography.titleMedium)
                    Text("Настройки защиты остаются только на этом устройстве.",style=MaterialTheme.typography.bodySmall)
                    SettingsGroup("Способ входа")
                    Methods()
                    SecretForm(mode,creating=true,enabled=!busy,message=message,submitLabel="Включить",onSubmit={typed->apply {lock.password(typed,mode=="pattern",mode)}})
                } else {
                    Text("Защита включена",style=MaterialTheme.typography.titleMedium)
                    SecurityRow(lockMethodIcons.getValue(s.mode),"Сменить способ входа",lockMethodName(s.mode)){mode=s.mode;open("change")}
                    if(lock.biometricAvailable())Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainerLow,tonalElevation=1.dp) {
                        Row(Modifier.padding(horizontal=14.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                            Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(9.dp)){RigSymbol("fingerprint",MaterialTheme.colorScheme.onPrimaryContainer)}}
                            Spacer(Modifier.width(14.dp));Text("Разрешить вход по биометрии",Modifier.weight(1f));Switch(s.biometric,{on->apply(stay=true){lock.settings.copy(biometric=on)}})
                        }
                    }
                    SecurityRow("duration","Автоблокировка",lockTimeoutLabel(s.timeoutSeconds,language)){customMinutes=(s.timeoutSeconds.coerceAtLeast(0)/60).toString();customSeconds=(s.timeoutSeconds.coerceAtLeast(0)%60).toString();open("timeout")}
                    SettingsGroup("Восстановление доступа")
                    SecurityRow("help","Контрольный вопрос",if(s.answerHash.isNotEmpty())"Настроено" else "Не настроено"){open("question")}
                    SecurityRow("time","Аутентификатор TOTP",if(s.totp.isNotEmpty())"Настроено" else "Не настроено"){open("totp")}
                    SecurityRow("send","Почта для кода восстановления",if(s.smtp.isComplete)"Настроено" else "Не настроено"){open("mail")}
                    if(s.answerHash.isEmpty() && s.totp.isEmpty() && !s.smtp.isComplete)Text("Без способа восстановления забытый код не вернуть.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                    SettingsGroup("Ограничение попыток")
                    SecurityRow("lock","Ограничение попыток",""){open("limits")}
                    SecurityRow("unlock","Отключить блокировку",""){open("off")}
                }
                "change"->if(!verified)Verify {verified=true} else {
                    SettingsGroup("Способ входа")
                    Methods()
                    SecretForm(mode,creating=true,enabled=!busy,message=message,submitLabel="Сохранить",onSubmit={typed->apply {lock.password(typed,mode=="pattern",mode)}})
                }
                "off"->Verify {lock.configure(lock.settings.copy(enabled=false));s=lock.settings;mode="pin";section=""}
                "timeout"->{
                    Text("Спрашивать снова, если приложение было свёрнуто",style=MaterialTheme.typography.titleMedium)
                    lockTimeouts.forEach {seconds->SecurityRow("duration",lockTimeoutLabel(seconds,language),"",selected=s.timeoutSeconds==seconds,arrow=false){apply(stay=true){lock.settings.copy(timeoutSeconds=seconds)}}}
                    SettingsGroup("Другое время")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(customMinutes,{customMinutes=it.filter(Char::isDigit).take(4)},label={Text("Минуты")},singleLine=true,modifier=Modifier.weight(1f))
                        OutlinedTextField(customSeconds,{customSeconds=it.filter(Char::isDigit).take(2)},label={Text("Секунды")},singleLine=true,modifier=Modifier.weight(1f))
                    }
                    Text("Пока открыт выбор файла или мелодии, приложение не блокируется до пяти минут.",style=MaterialTheme.typography.bodySmall)
                }
                "question"->{
                    SettingsGroup("Готовые вопросы")
                    securityQuestions.forEach {preset->val shown=translate(preset,language);SecurityRow("help",shown,"",selected=question==shown,arrow=false){question=shown}}
                    SettingsGroup("Свой вопрос")
                    OutlinedTextField(question,{question=it},label={Text("Контрольный вопрос")},modifier=Modifier.fillMaxWidth())
                    SecretInput(answer,{answer=it},"Новый ответ")
                }
                "totp"->{
                    Text("Добавьте этот ключ в приложение аутентификатора: FitEasy, 6 цифр, 30 секунд.")
                    if(totp.isNotEmpty())androidx.compose.foundation.text.selection.SelectionContainer {androidx.compose.material3.Text(totp,style=MaterialTheme.typography.titleMedium)}
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedButton(onClick={totp=Totp.newSecret();totpCode=""}){Text("Создать ключ TOTP")};if(totp.isNotEmpty())OutlinedButton(onClick={totp=""}){Text("Удалить")}}
                    // The key as a QR code, as in Notio (D124): the authenticator app reads it with the camera instead of thirty-two typed letters.
                    if(totp.isNotEmpty())QrCode(remember(totp) {lock.totpUri(totp)},translate("QR-код ключа TOTP",language))
                    if(totp.isNotEmpty() && totp!=s.totp) {
                        Text("Отсканируйте QR-код приложением-аутентификатором или введите ключ вручную, затем введите 6-значный код, который оно показывает.",style=MaterialTheme.typography.bodySmall)
                        OutlinedTextField(totpCode,{totpCode=it.filter(Char::isDigit).take(6)},label={Text("6-значный код")},singleLine=true)
                    }
                }
                "mail"->{
                    OutlinedTextField(host,{host=it},label={Text("SMTP сервер")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                        OutlinedTextField(port,{port=it.filter(Char::isDigit).take(5)},label={Text("Порт")},singleLine=true,modifier=Modifier.width(110.dp))
                        SmtpSecurity.entries.forEach {kind->FilterChip(smtpSecurity==kind,{smtpSecurity=kind;port=SmtpSettings.defaultPort(kind).toString()},{Text(kind.name)})}
                    }
                    OutlinedTextField(username,{username=it},label={Text("SMTP логин / отправитель")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    SecretInput(smtpPassword,{smtpPassword=it},"SMTP пароль")
                    OutlinedTextField(recipient,{recipient=it},label={Text("Email для восстановления")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    // Saves what is typed and sends a real code, so a wrong server or password shows now and not when access is lost.
                    androidx.compose.material3.OutlinedButton(enabled=!busy,onClick={scope.launch {
                        busy=true
                        runCatching {lock.configure(lock.settings.copy(smtp=mail()));s=lock.settings;lock.sendRecovery()}.onSuccess {message=mailResultText(it)}.onFailure {message=it.message ?: "Ошибка"}
                        busy=false
                    }}){Text("Проверить отправку")}
                }
                "limits"->{
                    OutlinedTextField(freeAttempts,{freeAttempts=it},label={Text("Попыток без задержки")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(delay,{delay=it},label={Text("Начальная задержка, секунд")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(maximum,{maximum=it},label={Text("Лимит до восстановления (0 — без лимита)")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Text("После свободных попыток задержка удваивается до 15 минут.",style=MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(wipe,{wipe=it});Text("Удалять локальные данные после лимита ошибок")}
                    if(wipe){Text("Удалятся дневник, локальные настройки и защита этого устройства. Внешние бэкапы и данные других устройств сохранятся.");OutlinedTextField(wipeConfirmation,{wipeConfirmation=it},label={Text("Напишите УДАЛИТЬ")})}
                }
            }
            if(section in forms && message.isNotEmpty())Text(message,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={if(section in forms)Button(enabled=!busy,onClick={save()}){Text("Сохранить")}})
}

@Composable private fun SecretInput(value:String,set:(String)->Unit,label:String) {
    OutlinedTextField(value,set,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),visualTransformation=PasswordVisualTransformation())
}

/**
 * On-screen PIN pad, after Notio's: a dot per digit, round keys, backspace and — always — Enter. When the length is known
 * the pad also submits itself at the last digit, like a phone's own lock screen.
 */
@Composable fun PinPad(value:String,onChange:(String)->Unit,expectedLength:Int?=null,maxLength:Int=12,enabled:Boolean=true,onComplete:()->Unit={}) {
    val complete by rememberUpdatedState(onComplete)
    val language=LocalLanguage.current
    LaunchedEffect(value,expectedLength) {if(expectedLength!=null && value.length==expectedLength)complete()}
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.height(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            repeat((expectedLength ?: 4).coerceAtLeast(value.length)) {i->
                Box(Modifier.size(14.dp).then(if(i<value.length)Modifier.background(MaterialTheme.colorScheme.primary,androidx.compose.foundation.shape.CircleShape)else Modifier.border(1.5.dp,MaterialTheme.colorScheme.outline,androidx.compose.foundation.shape.CircleShape)))
            }
        }
        listOf("123","456","789","e0<").forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {row.forEach {key->
            val action=key=='e' || key=='<'
            val active=enabled && if(action)value.isNotEmpty() else value.length<maxLength
            val tint=(if(key=='e')MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha=if(active)1f else .38f)
            HintBox(when(key){'e'->translate("Подтвердить",language);'<'->translate("Стереть",language);else->key.toString()}) {
                Surface(onClick={when(key){'e'->complete();'<'->onChange(value.dropLast(1));else->onChange(value+key)}},enabled=active,modifier=Modifier.size(68.dp),shape=androidx.compose.foundation.shape.CircleShape,
                    color=if(key=='e')MaterialTheme.colorScheme.primary.copy(alpha=if(active)1f else .3f) else if(key=='<')Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Box(contentAlignment=Alignment.Center) {when(key){'e'->RigSymbol("save",tint);'<'->RigSymbol("backspace",tint);else->androidx.compose.material3.Text(key.toString(),style=MaterialTheme.typography.headlineSmall,color=tint)}}
                }
            }
        }}}
    }
}
