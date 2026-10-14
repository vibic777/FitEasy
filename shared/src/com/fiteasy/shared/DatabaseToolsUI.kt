package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.lock.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

@Composable fun DatabaseToolsDialog(platform:Platform,onUpdated:()->Unit,onClose:()->Unit) {
    val scope=rememberCoroutineScope()
    val clipboard=LocalClipboardManager.current
    var password by remember {mutableStateOf("")}
    var confirmation by remember {mutableStateOf("")}
    var shownKey by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var mailKind by remember {mutableStateOf<String?>(null)}
    var smtp by remember {mutableStateOf<SmtpSettings?>(null)}
    fun action(event:String,work:suspend ()->Unit){if(busy)return;busy=true;scope.launch {runCatching {work()}.onSuccess {message="Готово";AppJournal.record(event+".success")}.onFailure {message="Операция не выполнена";AppJournal.record(event+".failed",JournalLevel.ERROR,it)};busy=false}}
    fun bytes(database:Boolean):ByteArray=if(database)platform.exportDatabase(password)else BackupCrypto.encode(codec.encodeToString(Backup(data=platform.load().shared)).toByteArray(),password)
    SettingsPage(onDismissRequest={if(!busy)onClose()},title={Text("База данных и перенос")},text={Column(Modifier.heightIn(max=540.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Пустой пароль — файл без шифрования")
        OutlinedTextField(password,{password=it.take(256)},label={Text("Пароль файла / новый пароль базы")},visualTransformation=PasswordVisualTransformation(),enabled=!busy)
        TextButton(enabled=!busy,onClick={action("database.password.show"){shownKey=withContext(Dispatchers.IO){platform.databasePassword()}}}){Text("Показать пароль базы")}
        shownKey?.let {key->androidx.compose.foundation.text.selection.SelectionContainer {androidx.compose.material3.Text(key)};TextButton(onClick={clipboard.setText(AnnotatedString(key));shownKey=null}){Text("Копировать и скрыть")};TextButton(onClick={shownKey=null}){Text("Скрыть")}}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=!busy && password.length>=8,onClick={action("database.rekey"){withContext(Dispatchers.IO){platform.changeDatabasePassword(password)};shownKey=null;password="";onUpdated()}}){Text("Сменить пароль базы")}
        OutlinedButton(enabled=!busy,onClick={action("database.export"){val result=withContext(Dispatchers.IO){bytes(true)};platform.exportFile("FitEasy-${BuildInfo.VERSION}.db","application/octet-stream",result)}}){Text("Экспорт базы")}
        }
        Text("Импорт базы заменит дневник и локальные настройки; защита устройства сохраняется")
        OutlinedTextField(confirmation,{confirmation=it},label={Text("Напишите ЗАМЕНИТЬ")},enabled=!busy)
        OutlinedButton(enabled=!busy && confirmation=="ЗАМЕНИТЬ",onClick={platform.importFile {data->action("database.import"){val imported=withContext(Dispatchers.IO){platform.readDatabase(data,password)};if(platform.name=="Android")platform.trackSteps(false);withContext(Dispatchers.IO){platform.save(imported.copy(local=imported.local.copy(phoneCounter=null,demo=emptyMap(),legalNoticeVersion=0)))};confirmation="";shownKey=null;onUpdated()}}}){Text("Импорт базы")}
        listOf(false,true).forEach {database->OutlinedButton(enabled=!busy,onClick={action("mail.prepare"){smtp=withContext(Dispatchers.IO){DeviceLock(platform).settings.smtp};require(smtp?.isComplete==true);mailKind=if(database)"db"else "fiteasy"}}){Text(if(database)"Отправить базу по почте"else "Отправить бэкап по почте")}}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message)
    }},confirmButton={TextButton(enabled=!busy,onClick=onClose){Text("Закрыть")}})
    mailKind?.let {kind->AlertDialog(onDismissRequest={mailKind=null},title={Text("Отправить файл?")},text={Column {androidx.compose.material3.Text(smtp?.to.orEmpty());Text(if(password.isEmpty())"Пустой пароль — файл без шифрования"else "Файл защищён паролем")}},confirmButton={TextButton(onClick={val settings=smtp ?: return@TextButton;mailKind=null;action("mail.attachment"){val content=withContext(Dispatchers.IO){bytes(kind=="db")};require(SmtpClient().sendAttachment(settings,"FitEasy","FitEasy ${BuildInfo.VERSION}",MailAttachment("FitEasy-${BuildInfo.VERSION}.$kind",content))==MailResult.Sent)}}){Text("Отправить")}},dismissButton={TextButton(onClick={mailKind=null}){Text("Отмена")}})}
}
