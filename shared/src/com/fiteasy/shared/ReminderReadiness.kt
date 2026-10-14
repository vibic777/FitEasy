package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReminderReadiness(val notifications:Boolean,val exact:Boolean,val restricted:Boolean,val batteryExempt:Boolean,val fullScreen:Boolean=true,val overlay:Boolean=true)
@Composable fun ReminderReadinessUI(platform:Platform){
    val foreground by platform.foreground.collectAsState(initial=true)
    var refresh by remember {mutableIntStateOf(0)}
    var status by remember {mutableStateOf<ReminderReadiness?>(null)}
    var error by remember {mutableStateOf("")}
    LaunchedEffect(platform,foreground){if(foreground)platform.reminderChanged.collect {refresh++}}
    LaunchedEffect(platform,foreground,refresh){if(foreground)withContext(Dispatchers.IO){runCatching {platform.reminderReadiness()}}.onSuccess {status=it}.onFailure {AppJournal.record("reminder.readiness.failed",JournalLevel.ERROR,it);error="Не удалось проверить разрешения"}}
    if(platform.name=="Android")Card {Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("Готовность напоминаний",style=MaterialTheme.typography.titleMedium)
        status?.let {s->
            Text(if(s.notifications)"Уведомления разрешены"else "Уведомления отключены")
            Text(if(s.exact)"Точные напоминания разрешены"else "Без точного доступа напоминания могут задерживаться")
            Text(if(s.restricted)"Фоновая работа ограничена системой"else "Фоновая работа не ограничена")
            Text(if(s.batteryExempt)"Оптимизация батареи отключена для приложения"else "Применяется системная экономия батареи")
            FlowRow {listOf("notifications" to "Уведомления","exact" to "Точные напоминания","battery" to "Настройки батареи").forEach {(action,label)->TextButton(onClick={runCatching {platform.openReminderAccess(action)}.onFailure {AppJournal.record("reminder.access.open.failed",JournalLevel.ERROR,it);error="Не удалось открыть системные настройки"}}){Text(label)}}}
        }
        Text("Разрешения добровольны. Производитель телефона может дополнительно ограничивать фон.")
        TextButton(onClick={refresh++}){Text("Проверить снова")}
        if(error.isNotBlank())Text(error)
    }}else Text("Для напоминаний Windows приложение должно работать или оставаться в трее.")
}

/** Device-local notification switches (D086); stored with the other device secrets, never synchronised. */
fun statusNotificationEnabled(platform:Platform)=platform.readSecret("status-notification")!="false"
fun alarmScreenEnabled(platform:Platform)=platform.readSecret("alarm-screen")=="true"
fun economyEnabled(platform:Platform)=platform.readSecret("economy")=="true"
/** Shown wherever motion is on screen while the battery saver is on, so the reason is named and not guessed. */
const val economyNotice="Включён режим экономии батареи: из-за него отключены анимации разделов и значков. Упражнение двигается только пока вы его показываете."

/** Settings → Reminders: the shade summary, the alarm screen over the lock screen and the battery saver, then the permission panel. */
@Composable fun NotificationSettings(platform:Platform,onChanged:()->Unit={}) {
    var summary by remember {mutableStateOf(statusNotificationEnabled(platform))}
    var alarm by remember {mutableStateOf(alarmScreenEnabled(platform))}
    var economy by remember {mutableStateOf(economyEnabled(platform))}
    var access by remember {mutableStateOf<ReminderReadiness?>(null)};var check by remember {mutableIntStateOf(0)};var note by remember {mutableStateOf("")}
    val foreground by platform.foreground.collectAsState(initial=true)
    LaunchedEffect(platform,foreground,check){if(foreground)withContext(Dispatchers.IO){runCatching {platform.reminderReadiness()}}.onSuccess {access=it}}
    fun set(name:String,value:Boolean){runCatching {platform.writeSecret(name,value.toString());platform.notificationSettingsChanged()}.onFailure {AppJournal.record("reminder.settings.failed",JournalLevel.ERROR,it)};onChanged()}
    @Composable fun option(title:String,hint:String,value:Boolean,change:(Boolean)->Unit){Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Text(hint,style=MaterialTheme.typography.bodySmall)};Switch(value,change)}}
    Card {Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Уведомления и батарея",style=MaterialTheme.typography.titleMedium)
        option("Сводка в шторке","Тихая строка в шторке: шаги, напоминания, задачи по расписанию и ближайшее время.",summary){summary=it;set("status-notification",it)}
        if(platform.name=="Android"){
            option("Будильник поверх экрана блокировки","Напоминание включает экран и показывает кнопки поверх блокировки, как будильник.",alarm){alarm=it;set("alarm-screen",it)}
            if(alarm)access?.let {a->
                Text(if(a.fullScreen)"Экран будильника поверх блокировки: разрешён" else "Экран будильника поверх блокировки: не разрешён")
                Text(if(a.overlay)"Поверх других окон: разрешено" else "Поверх других окон: не разрешено")
                FlowRow {listOf("fullscreen" to "Экран будильника","overlay" to "Поверх других окон").forEach {(action,label)->TextButton(onClick={runCatching {platform.openReminderAccess(action)}.onFailure {AppJournal.record("reminder.access.open.failed",JournalLevel.ERROR,it)}}){Text(label)}}
                    TextButton(onClick={check++}){Text("Проверить снова")}}
                OutlinedButton(onClick={runCatching {platform.testAlarm()}.onSuccess {note="Заблокируйте телефон: будильник сработает через 10 секунд."}.onFailure {AppJournal.record("reminder.test.failed",JournalLevel.ERROR,it)}}){Text("Проверить будильник через 10 секунд")}
                if(note.isNotEmpty())Text(note,style=MaterialTheme.typography.bodySmall)
            }
        }
        option("Режим экономии батареи","Напоминания могут опаздывать на несколько минут, шаги и уведомление обновляются реже, анимации отключены.",economy){economy=it;set("economy",it)}
    }}
    ReminderReadinessUI(platform)
}
