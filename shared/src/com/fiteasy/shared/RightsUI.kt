package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The name the operating system shows for the app (D128): «ФитИзи» where the system speaks Russian, FitEasy everywhere
 * else. Android takes it from its resources (values-ru); Windows from here, for the title of the window and the tray.
 * Inside the app the name stays FitEasy, as in the documents and on the packages.
 */
fun appSystemName(systemLanguage:String)=if(systemLanguage=="ru")"ФитИзи" else "FitEasy"

/** One right the system gives the app (D127). [granted] is null where the system does not tell the app. */
data class AppRight(val id:String,val granted:Boolean?)

/** Name of a right and the function that needs it, by id; Russian source texts. */
val rightTexts:Map<String,Pair<String,String>> = linkedMapOf(
    "notifications" to ("Уведомления" to "Чтобы напоминание появилось на экране и в шторке."),
    "exact" to ("Точные напоминания" to "Чтобы напоминание пришло в свою минуту, а не с задержкой."),
    "fullscreen" to ("Экран будильника" to "Чтобы напоминание открывало свой экран поверх блокировки; нужно только при включённом будильнике поверх экрана блокировки."),
    "overlay" to ("Поверх других окон" to "Чтобы экран будильника открывался, когда телефон разблокирован и открыто другое приложение."),
    "windows-notifications" to ("Уведомления Windows" to "Показ напоминаний в области уведомлений."),
    "windows-network" to ("Сеть для синхронизации" to "Брандмауэр Windows спрашивает разрешение, когда приложение впервые ждёт другое устройство."))

/** What a right shows next to its name. */
fun rightStateText(granted:Boolean?)=when(granted){true->"Разрешено";false->"Не разрешено";null->"Состояние показывает система"}
/** The one action a right offers in its state: give it, take it back, or look in the system where the state is not told. */
fun rightAction(granted:Boolean?)=when(granted){true->"Отозвать";false->"Разрешить";null->"Открыть в системе"}

/**
 * Settings → Rights (D127): every right the system gives the app in one place — what it is for, whether it is given, the
 * way to give it and to take it back, and all of them back at once. The same rights are still asked for where they are
 * needed (Reminders); this page repeats them so nothing has to be looked for.
 */
@Composable fun RightsPage(platform:Platform) {
    val foreground by platform.foreground.collectAsState(initial=true)
    var check by remember {mutableIntStateOf(0)}
    var rights by remember {mutableStateOf(emptyList<AppRight>())}
    var note by remember {mutableStateOf("")}
    var confirmAll by remember {mutableStateOf(false)}
    // Read again whenever the app returns to the screen: the person may have just changed a right in the system.
    LaunchedEffect(platform,foreground,check) {if(foreground)withContext(Dispatchers.IO){runCatching {platform.rights()}}.onSuccess {rights=it}.onFailure {AppJournal.record("rights.read.failed",JournalLevel.ERROR,it)}}
    LaunchedEffect(platform,foreground) {if(foreground)platform.reminderChanged.collect {check++}}
    fun act(event:String,action:()->Unit) {runCatching(action).onFailure {AppJournal.record(event,JournalLevel.ERROR,it);note="Не удалось открыть системные настройки"};check++}
    val android=platform.name=="Android"
    Hint(if(android)"Здесь собраны все права, которые система выдаёт FitEasy. Приложение работает и без них: право нужно только той функции, которая названа рядом. Любое право можно отозвать."
        else "Windows не выдаёт FitEasy отдельных прав: приложение работает с правами вашей учётной записи. Ниже — два места в параметрах Windows, которые на него влияют.")
    rights.forEach {right->
        val (title,purpose)=rightTexts[right.id] ?: return@forEach
        SettingBlock(title) {
            Hint(purpose)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(rightStateText(right.granted),Modifier.weight(1f))
                TextButton(onClick={
                    note=""
                    if(right.granted==true)act("rights.revoke.failed") {note=if(platform.revokeRight(right.id))"Право будет отозвано, когда приложение закроется." else "Открыты настройки системы: выключите право там."}
                    else act("rights.grant.failed") {platform.grantRight(right.id)}
                }) {Text(rightAction(right.granted))}
            }
        }
    }
    if(note.isNotEmpty())Text(note,style=MaterialTheme.typography.bodySmall)
    if(android) {
        // Named in words, not by an icon: it is the one action here that takes everything away, and a square alone does not say so.
        if(rights.any {it.granted==true})WideButton("Отозвать все права") {confirmAll=true}
        SettingBlock("Права, выдаваемые при установке") {
            Hint("Сеть для синхронизации по Wi-Fi, вибрация, восстановление будильников после перезагрузки, биометрия и служба мелодии напоминания выдаются при установке и отдельно не отзываются. Доступа к контактам, местоположению, камере, микрофону и файлам у приложения нет.")
        }
    }
    if(confirmAll)AlertDialog(onDismissRequest={confirmAll=false},title={Text("Отозвать все права?")},
        text={Text("Напоминания перестанут появляться и приходить вовремя. Данные не удаляются. Уведомления отзываются при закрытии приложения, остальные права выключаются на открывшейся странице системы.")},
        confirmButton={androidx.compose.material3.TextButton(onClick={confirmAll=false;note="";act("rights.revoke-all.failed") {platform.revokeAllRights();note="Открыты настройки системы: выключите право там."}}) {androidx.compose.material3.Text(translate("Отозвать все права",LocalLanguage.current))}},
        dismissButton={TextButton(onClick={confirmAll=false}) {Text("Отмена")}})
}
