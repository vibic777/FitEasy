package com.fiteasy.shared

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val reminderStamp=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private fun stamp(millis:Long)=reminderStamp.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
/** Text with one value put in place of {0}, in the selected language. */
fun textWith(key:String,language:String,value:Any)=translate(key,language).replace("{0}",value.toString())

/** A red light that pulses while a reminder rings; drawn only then (after Notio's reminders page). */
@Composable private fun RingingLight(modifier:Modifier=Modifier) {
    val alpha by rememberInfiniteTransition().animateFloat(1f,.3f,infiniteRepeatable(tween(600),RepeatMode.Reverse))
    Box(modifier.size(14.dp).alpha(alpha).background(Color(0xFFE53935),CircleShape))
}
/** On top of the list while a reminder rings: the light, which one, and the button that ends the sound; the reminder itself stays. */
@Composable private fun RingingBanner(title:String,onStop:()->Unit) {
    val language=LocalLanguage.current
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
            RingingLight()
            androidx.compose.material3.Text(textWith("Звучит сейчас: {0}",language,title),Modifier.weight(1f).padding(start=10.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onErrorContainer)
            RigAction("pause",translate("Заглушить",language),onStop,translated=false)
        }
    }
}
/** What a reminder does, in one line: sound · vibration · pop-up · must be confirmed · repeats · days. */
fun reminderFlags(r:Reminder,language:String):String = listOfNotNull(
    translate("звук",language).takeIf {r.sound},
    translate("вибрация",language).takeIf {r.vibration},
    translate("всплывающее окно",language).takeIf {r.popup},
    translate("нужно подтвердить",language).takeIf {r.requiresConfirmation},
    textWith("повтор каждые {0} мин",language,r.repeatMinutes).takeIf {r.repeatUntilConfirmed},
    textWith("ещё {0} раз",language,r.repeatCount).takeIf {!r.repeatUntilConfirmed && r.repeatCount>0},
    when(reminderRepeatKind(r)) {
        "once"->translate("Один раз",language).lowercase()
        "monthly"->textWith("числа месяца: {0}",language,r.monthDays.joinToString(", "))
        "weekdays"->r.weekdays.joinToString(", ") {weekdayLabel(it,language)}
        else->translate("Каждый день",language).lowercase()
    }
).joinToString(" · ")

/** One reminder as a card, after Notio: name and next time, switch, state, what it does, and its actions as icons. */
@Composable private fun ReminderCard(r:Reminder,d:ReminderDelivery?,ringing:Boolean,note:String?,onOpenNote:(()->Unit)?,onStopSound:()->Unit,onEdit:()->Unit,onToggle:(Boolean)->Unit,onControl:(String)->Unit,onDelete:()->Unit) {
    val language=LocalLanguage.current
    val next=remember(r){runCatching {nextReminder(r)}.getOrNull()}
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp).alpha(if(r.enabled)1f else .6f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                if(ringing)RingingLight(Modifier.padding(end=8.dp))
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(r.name,style=MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.Text(next?.let {stamp(it.toEpochMilli())} ?: r.times.ifEmpty {listOf(r.time)}.joinToString(", "),style=MaterialTheme.typography.bodyLarge)
                }
                Switch(r.enabled,onToggle)
            }
            if(note!=null)androidx.compose.material3.Text(textWith("Заметка: {0}",language,note),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(reminderStateLabel(r,d),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
            if(d?.status=="snoozed" && d.nextAt>0)androidx.compose.material3.Text(textWith("Отложено до {0}",language,stamp(d.nextAt)),style=MaterialTheme.typography.labelLarge)
            if(d!=null)androidx.compose.material3.Text(textWith("Последнее: {0}",language,stamp(d.at)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.Text(reminderFlags(r,language),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                if(ringing)RigAction("pause",translate("Заглушить",language),onStopSound,translated=false)
                if(r.enabled && d?.status in waitingStatuses) {
                    RigAction("save",translate("Подтвердить",language),{onControl("ack")},translated=false)
                    RigAction("time",translate("Отложить",language),{onControl("snooze")},translated=false)
                    RigAction("stop",translate("Прервать",language),{onControl("interrupt")},translated=false)
                }
                if(note!=null && onOpenNote!=null)RigAction("document",translate("Открыть заметку",language),onOpenNote,translated=false)
                RigAction("edit",translate("Изменить",language),onEdit,translated=false)
                RigAction("delete",translate("Удалить",language),onDelete,translated=false)
            }
        }
    }
}

/**
 * The reminders of this device as cards (D097, after Notio's reminders page): a hint while exact alarms are not allowed,
 * the reminder that rings now with a button to end its sound, then every reminder. [filter] narrows the list.
 */
@Composable fun ReminderCards(platform:Platform,snapshot:Snapshot,onEdit:(Reminder)->Unit,onSave:(Reminder)->Boolean,onDelete:(Reminder)->Boolean,filter:(Reminder)->Boolean={true},onOpenNote:((String)->Unit)?=null) {
    val language=LocalLanguage.current
    val states=reminderStatus(platform)
    val ringing by platform.ringing.collectAsState(initial=null)
    var deleting by remember {mutableStateOf<Reminder?>(null)}
    var error by remember {mutableStateOf("")}
    val effective=effectiveReminders(snapshot)
    fun raw(r:Reminder)=snapshot.local.reminders.firstOrNull {it.id==r.id} ?: r
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ringing?.let {id->RingingBanner(effective.firstOrNull {it.id==id}?.name ?: translate("Напоминание",language)){platform.stopRinging()}}
        val shown=effective.filter(filter)
        if(shown.isEmpty())Text("Напоминаний пока нет.",style=MaterialTheme.typography.bodyMedium)
        shown.forEach {r->key(r.id) {
            val note=r.noteId?.let {id->snapshot.shared.notes.firstOrNull {it.id==id && it.deleted==null}}
            ReminderCard(r,states[r.id],ringing==r.id,note?.title,note?.let {n->onOpenNote?.let {open->{open(n.id)}}},onStopSound={platform.stopRinging()},onEdit={onEdit(r.copy(comment=raw(r).comment,enabled=raw(r).enabled))},
                onToggle={onSave(raw(r).copy(enabled=it,updated=System.currentTimeMillis()))},onControl={action->runCatching {platform.controlReminder(r.id,action)}.onFailure {error="Не удалось изменить напоминание"}},onDelete={deleting=r})
        }}
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
    }
    deleting?.let {r->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить напоминание?")},text={androidx.compose.material3.Text(r.name)},confirmButton={TextButton(onClick={if(onDelete(r))deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}

/**
 * Shown the moment a reminder is being made while every reminder of this device is off, as after installation (D113):
 * turn them on, or go on without; closing the question makes nothing.
 */
@Composable fun RemindersOffHint(onEnable:()->Unit,onSkip:()->Unit,onDismiss:()->Unit) {
    val language=LocalLanguage.current
    // Both answers are written out: a play triangle in place of "Turn on" would not say what is turned on.
    AlertDialog(onDismissRequest=onDismiss,title={Text("Напоминания выключены")},text={Text("Сейчас все напоминания на этом устройстве выключены: новое напоминание не сработает, пока вы их не включите.")},
        confirmButton={TextButton(onClick=onEnable){androidx.compose.material3.Text(translate("Включить",language))}},dismissButton={TextButton(onClick=onSkip){androidx.compose.material3.Text(translate("Не включать",language))}})
}

private val rampChoices=listOf(0,10,30,60,120)

/** Add or change a reminder: when, how it sounds — melody, volume, fade-in — and how it repeats until answered (after Notio's dialog). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReminderEditor(existing:Reminder?,onDismiss:()->Unit,onSave:(Reminder)->Boolean,onDelete:()->Boolean,platform:Platform?=null,notes:List<Note> = emptyList(),isNew:Boolean=existing==null || existing.name.isEmpty()) {
    val language=LocalLanguage.current
    var noteId by remember {mutableStateOf(existing?.noteId)}
    var noteMenu by remember {mutableStateOf(false)}
    var name by remember {mutableStateOf(existing?.name ?: "")}
    var times by remember {mutableStateOf(existing?.let {it.times.ifEmpty {listOf(it.time)}} ?: listOf("09:00"))}
    var repeat by remember {mutableStateOf(existing?.let(::reminderRepeatKind) ?: "daily")}
    var weekdays by remember {mutableStateOf(existing?.weekdays ?: (1..7).toList())}
    var days by remember {mutableStateOf(existing?.monthDays?.joinToString(",") ?: "")}
    var every by remember {mutableStateOf(existing?.everyDays?.toString() ?: "1")}
    var start by remember {mutableStateOf(existing?.startDate ?: LocalDate.now().toString())}
    var end by remember {mutableStateOf(existing?.endDate ?: "")}
    var quietStart by remember {mutableStateOf(existing?.quietStart ?: "")}
    var quietEnd by remember {mutableStateOf(existing?.quietEnd ?: "")}
    var snooze by remember {mutableStateOf(existing?.snoozeMinutes?.toString() ?: "10")}
    var enabled by remember {mutableStateOf(existing?.enabled ?: true)}
    var comment by remember {mutableStateOf(existing?.comment ?: "")}
    var sound by remember {mutableStateOf(existing?.sound ?: true)}
    var soundKind by remember {mutableStateOf(existing?.soundKind ?: "notification")}
    var soundUri by remember {mutableStateOf(existing?.soundUri)}
    var volume by remember {mutableStateOf((existing?.volumePercent ?: 100).toFloat())}
    var ramp by remember {mutableStateOf(existing?.rampSeconds ?: 0)}
    var vibration by remember {mutableStateOf(existing?.vibration ?: true)}
    var popup by remember {mutableStateOf(existing?.popup ?: true)}
    var confirmation by remember {mutableStateOf(existing?.requiresConfirmation ?: false)}
    var untilConfirmed by remember {mutableStateOf(existing?.repeatUntilConfirmed ?: false)}
    var repeatCount by remember {mutableStateOf(existing?.repeatCount?.toString() ?: "0")}
    var repeatMinutes by remember {mutableStateOf(existing?.repeatMinutes?.toString() ?: "5")}
    var error by remember {mutableStateOf("")}
    var deleting by remember {mutableStateOf(false)}
    var pickTime by remember {mutableStateOf(false)}
    DisposableEffect(platform) {onDispose {platform?.stopReminderPreview()}}
    fun value():Reminder {
        require(times.isNotEmpty())
        val bound=existing?.scheduleId!=null
        val dates=if(repeat!="monthly" || days.isBlank())emptyList()else days.split(',').map {it.trim().toInt()}
        require(bound || repeat!="monthly" || dates.isNotEmpty());require(bound || repeat!="weekdays" || weekdays.isNotEmpty())
        val first=start.trim()
        return (existing ?: Reminder(name="")).copy(name=name.trim(),time=times.first(),times=times,weekdays=if(repeat=="daily" || repeat=="once")(1..7).toList() else weekdays,monthDays=if(bound)existing!!.monthDays else dates,
            everyDays=if(repeat=="once")1 else every.toInt(),enabled=enabled,startDate=first,endDate=if(repeat=="once" && !bound)first else end.trim().takeIf {it.isNotEmpty()},
            quietStart=quietStart.trim().takeIf {it.isNotEmpty()},quietEnd=quietEnd.trim().takeIf {it.isNotEmpty()},snoozeMinutes=snooze.toInt(),comment=comment,sound=sound,soundKind=soundKind,
            soundUri=soundUri.takeIf {sound},volumePercent=if(sound)volume.toInt().coerceIn(10,100) else 100,rampSeconds=if(sound)ramp else 0,vibration=vibration,popup=popup,
            requiresConfirmation=confirmation,repeatUntilConfirmed=confirmation && untilConfirmed,repeatCount=repeatCount.toInt(),repeatMinutes=repeatMinutes.toInt(),noteId=noteId,updated=System.currentTimeMillis()).also(::validateReminder)
    }
    SettingsPage(onDismissRequest=onDismiss,title={Text(if(isNew)"Добавить напоминание" else "Изменить напоминание")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        SettingsGroup("Напоминание")
        // The note the reminder belongs to, as in Notio; choosing one names a reminder that has no name yet.
        if(notes.isNotEmpty() || noteId!=null)Box {
            val chosen=notes.firstOrNull {it.id==noteId}
            androidx.compose.material3.OutlinedButton(onClick={noteMenu=true}){RigSymbol("document");Spacer(Modifier.width(8.dp));androidx.compose.material3.Text(chosen?.title ?: translate("Без заметки",language),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
            DropdownMenu(expanded=noteMenu,onDismissRequest={noteMenu=false}) {
                DropdownMenuItem(text={Text("Без заметки")},onClick={noteId=null;noteMenu=false})
                notes.take(200).forEach {n->DropdownMenuItem(text={androidx.compose.material3.Text(n.title,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},onClick={noteId=n.id;if(name.isBlank())name=n.title;noteMenu=false})}
            }
        }
        OutlinedTextField(name,{name=it},label={Text("Название")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(comment,{comment=it},label={Text("Комментарий напоминания")},modifier=Modifier.fillMaxWidth())
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Включено",Modifier.weight(1f));Switch(enabled,{enabled=it})}
        SettingsGroup("Когда")
        if(existing?.scheduleId!=null)Text("Время и дни наследуются из строки расписания; изменяйте их в графике.")
        if(existing?.scheduleId==null){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.Center) {
                times.forEach {t->InputChip(selected=true,onClick={if(times.size>1)times=times-t},label={androidx.compose.material3.Text(t)})}
                RigAction("time+",translate("Добавить время",language),{pickTime=true},translated=false)
            }
            if(times.size>1)Text("Нажмите на время, чтобы убрать его.",style=MaterialTheme.typography.bodySmall)
            Text("Повторять напоминание",style=MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("once" to "Один раз","daily" to "Каждый день","weekdays" to "По выбранным дням недели","monthly" to "Каждый месяц").forEach {(id,label)->androidx.compose.material3.FilterChip(repeat==id,{if(repeat=="once" && id!="once" && end.trim()==start.trim())end="";repeat=id},{Text(label)})}
            }
            if(repeat=="weekdays")FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {(1..7).forEach {day->FilterChip(day in weekdays,{weekdays=if(day in weekdays)weekdays-day else (weekdays+day).sorted()},{androidx.compose.material3.Text(weekdayLabel(day,language))})}}
            if(repeat=="monthly"){
                OutlinedTextField(days,{days=it},label={Text("Числа месяца 1–31 через запятую")},singleLine=true)
                Text("Месяц, в котором нет такого числа, пропускается.",style=MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(start,{start=it},label={Text(if(repeat=="once")"Дата YYYY-MM-DD" else "Начало YYYY-MM-DD")},singleLine=true)
            if(repeat!="once"){
                OutlinedTextField(end,{end=it},label={Text("Конец YYYY-MM-DD, необязательно")},singleLine=true)
                OutlinedTextField(every,{every=it},label={Text("Каждые N дней от начальной даты")},singleLine=true)
            }
        }
        SettingsGroup("Тихие часы")
        OutlinedTextField(quietStart,{quietStart=it},label={Text("Тихие часы: начало HH:mm")},singleLine=true)
        OutlinedTextField(quietEnd,{quietEnd=it},label={Text("Тихие часы: конец HH:mm")},singleLine=true)
        OutlinedTextField(snooze,{snooze=it},label={Text("Отложить на минут")},singleLine=true)
        SettingsGroup("Сигнал")
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Звук",Modifier.weight(1f));Switch(sound,{sound=it;if(!it)platform?.stopReminderPreview()})}
        if(sound){
            val pickers=platform?.reminderSoundPickers().orEmpty()
            val uri=soundUri
            val melody=remember(uri,soundKind,language){if(uri==null)translate(reminderSounds.first {it.first==soundKind}.second,language) else runCatching {platform?.reminderSoundTitle(uri)}.getOrNull() ?: uri}
            androidx.compose.material3.Text(textWith("Мелодия: {0}",language,melody),style=MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                if(platform!=null){
                    OutlinedButton(onClick={platform.previewReminderSound(soundUri,soundKind,volume.toInt())}){Text("Прослушать")}
                    OutlinedButton(onClick={platform.stopReminderPreview()}){Text("Остановить")}
                }
                if("system" in pickers)OutlinedButton(onClick={platform?.pickReminderSound("system",soundUri){picked->if(picked!=null)soundUri=picked}}){Text(if(platform?.name=="Windows")"Звуки Windows…" else "Звуки телефона…")}
                if("file" in pickers)OutlinedButton(onClick={platform?.pickReminderSound("file",soundUri){picked->if(picked!=null)soundUri=picked else error="Этот файл не удалось использовать как мелодию."}}){Text("Мой файл…")}
                if(soundUri!=null)OutlinedButton(onClick={soundUri=null}){Text("Обычный звук")}
            }
            if(soundUri==null)FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {reminderSounds.forEach {(id,label)->FilterChip(soundKind==id,{soundKind=id},{Text(label)})}}
            androidx.compose.material3.Text(textWith("Громкость: {0} %",language,volume.toInt()),style=MaterialTheme.typography.bodyMedium)
            Slider(value=volume,onValueChange={volume=it},valueRange=10f..100f,steps=8)
            Text("Относительно громкости будильника на устройстве.",style=MaterialTheme.typography.bodySmall)
            Text("Плавно наращивать громкость",style=MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {rampChoices.forEach {seconds->androidx.compose.material3.FilterChip(ramp==seconds,{ramp=seconds},{if(seconds==0)Text("Выкл") else androidx.compose.material3.Text(textWith("{0} с",language,seconds))})}}
        }
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Вибрация (Android)",Modifier.weight(1f));Switch(vibration,{vibration=it})}
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Всплывающее уведомление",Modifier.weight(1f));Switch(popup,{popup=it})}
        SettingsGroup("Подтверждение и повторы")
        Row(verticalAlignment=Alignment.CenterVertically) {Text("Требует подтверждения",Modifier.weight(1f));Switch(confirmation,{confirmation=it;if(!it)untilConfirmed=false})}
        if(confirmation)Row(verticalAlignment=Alignment.CenterVertically) {Text("Повторять до подтверждения",Modifier.weight(1f));Switch(untilConfirmed,{untilConfirmed=it})}
        Text("Повтор, как у будильника: после первого сигнала напоминание звучит ещё несколько раз, пока вы не ответите.",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(!untilConfirmed)OutlinedTextField(repeatCount,{repeatCount=it.filter(Char::isDigit).take(3)},label={Text("Ещё раз (0–100)")},singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(repeatMinutes,{repeatMinutes=it.filter(Char::isDigit).take(4)},label={Text("Каждые, минут")},singleLine=true,modifier=Modifier.weight(1f))
        }
        // Said where the interval is chosen (D121, rule 4): asleep, a phone hands one app its alarms no more often than once in nine minutes.
        if(platform?.name=="Android" && (untilConfirmed || (repeatCount.toIntOrNull() ?: 0)>0) && (repeatMinutes.toIntOrNull() ?: DOZE_ALARM_MINUTES)<DOZE_ALARM_MINUTES)Text("Когда телефон спит, Android показывает повторы не чаще раза в 9 минут; частые повторы расходуют батарею.",style=MaterialTheme.typography.bodySmall)
        val next=runCatching {nextReminder(value())}.getOrNull()
        if(next!=null)androidx.compose.material3.Text(textWith("Следующее: {0}",language,stamp(next.toEpochMilli())))
        else Text("Нет ближайшего срабатывания: проверьте условия или включите напоминание")
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
        if(!isNew)TextButton(onClick={deleting=true}){Text("Удалить")}
    }},confirmButton={TextButton(onClick={runCatching {if(onSave(value()))onDismiss()}.onFailure {error="Проверьте даты, время и диапазоны чисел"}}){Text("Сохранить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
    if(pickTime) {
        val state=rememberTimePickerState(LocalTime.now().hour,0,true)
        AlertDialog(onDismissRequest={pickTime=false},text={TimePicker(state)},
            confirmButton={TextButton(onClick={times=(times+"%02d:%02d".format(state.hour,state.minute)).distinct().sorted();pickTime=false}){Text("Сохранить")}},dismissButton={TextButton(onClick={pickTime=false}){Text("Отмена")}})
    }
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Удалить напоминание?")},confirmButton={TextButton(onClick={if(onDelete())onDismiss()}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=false}){Text("Отмена")}})
}
