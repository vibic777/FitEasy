package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun reminderStatus(platform:Platform):Map<String,ReminderDelivery> {
    var states by remember {mutableStateOf(emptyMap<String,ReminderDelivery>())}
    val foreground by platform.foreground.collectAsState(initial=true)
    LaunchedEffect(platform,foreground){if(foreground){
        suspend fun refresh(){withContext(Dispatchers.IO){runCatching {platform.reminderStates()}}.onSuccess {states=it}.onFailure {AppJournal.record("reminder.states.failed",JournalLevel.ERROR,it)}}
        refresh()
        platform.reminderChanged.collect {refresh()}
    }}
    return states
}

@Composable private fun ReminderActions(platform:Platform,r:Reminder,d:ReminderDelivery?,edit:()->Unit,toggle:(Boolean)->Unit) {
    var error by remember {mutableStateOf("")}
    Row {Text(reminderStateLabel(r,d),Modifier.weight(1f));Switch(r.enabled,toggle)}
    if(d!=null)androidx.compose.material3.Text(textWith("Последнее: {0}",LocalLanguage.current,Instant.ofEpochMilli(d.at).atZone(ZoneId.systemDefault()).toLocalDateTime().withNano(0)))
    FlowRow {
        TextButton(onClick=edit){Text("Настроить")}
        if(r.enabled && d?.status in listOf("awaiting","delivered","snoozed")){
            listOf("ack" to "Подтвердить","snooze" to "Отложить","interrupt" to "Прервать").forEach {(action,label)->
                TextButton(onClick={runCatching {platform.controlReminder(r.id,action)}.onFailure {error="Не удалось изменить напоминание"}}){Text(label)}
            }
        }
    }
    if(error.isNotEmpty())Text(error)
}

@Composable fun ScheduleHub(platform:Platform,snapshot:Snapshot,mutate:((Snapshot)->Snapshot)->Boolean,onDismiss:()->Unit,focusReminder:String?=null,focusEditor:Boolean=true) {
    var editor by remember {mutableStateOf<DaySchedule?>(null)}
    var reminder by remember {mutableStateOf<Reminder?>(null)}
    var deleting by remember {mutableStateOf<DaySchedule?>(null)}
    var selected by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(focusReminder,focusEditor){if(focusReminder!=null){val target=snapshot.local.reminders.firstOrNull {it.id==focusReminder};selected=target?.scheduleId;if(focusEditor)reminder=target}}
    val states=reminderStatus(platform)
    val effective=effectiveReminders(snapshot)
    fun saveReminder(r:Reminder)=mutate {current->current.copy(local=current.local.copy(reminders=current.local.reminders.filterNot {it.id==r.id || (r.scheduleId!=null && it.scheduleId==r.scheduleId && it.rowId==r.rowId)}+r))}
    // A new reminder while every reminder of the device is off: say so first (D113).
    var offHint by remember {mutableStateOf<Reminder?>(null)}
    fun create(r:Reminder){if(snapshot.local.remindersOff)offHint=r else reminder=r}
    SettingsPage(onDismissRequest=onDismiss,title={Text("Расписания")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        ReminderReadinessUI(platform)
        Text("Любые графики: время, строка, комментарий. Напоминания настраиваются на этом устройстве.")
        Button(onClick={editor=DaySchedule(name="",weekdays=listOf(LocalDate.now().dayOfWeek.value))}){Text("Создать расписание")}
        Text("Отдельные напоминания",style=MaterialTheme.typography.titleMedium)
        TextButton(onClick={create(Reminder(name=""))}){Text("Добавить напоминание")}
        ReminderCards(platform,snapshot,onEdit={reminder=it},onSave=::saveReminder,onDelete={r->mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.filterNot {it.id==r.id}))}},filter={it.scheduleId==null})
        snapshot.shared.schedules.filter {it.deleted==null}.forEach {s->
            Card {Column(Modifier.padding(10.dp)){
                TextButton(onClick={selected=if(selected==s.id)null else s.id}){Text("${s.name} · ${s.rows.size}")}
                FlowRow {
                    IconAction("edit","Изменить график",onClick={editor=s})
                    IconAction("copy","Копировать",onClick={mutate {c->c.copy(shared=c.shared.copy(schedules=c.shared.schedules+copySchedule(s).copy(name=s.name+" (копия)")))}})
                    IconAction("delete","Удалить",onClick={deleting=s})
                }
                if(selected==s.id)s.rows.forEach {row->
                    HorizontalDivider()
                    Text("${row.time} · ${row.title}")
                    if(row.comment.isNotBlank())Text(row.comment)
                    val raw=snapshot.local.reminders.firstOrNull {it.scheduleId==s.id && it.rowId==row.id}
                    val r=effective.firstOrNull {it.id==raw?.id}
                    if(r==null)TextButton(onClick={create(Reminder(name="${s.name}: ${row.title}",time=row.time,times=listOf(row.time),scheduleId=s.id,rowId=row.id,weekdays=s.weekdays,monthDays=s.monthDays,everyDays=s.everyDays,startDate=s.startDate,endDate=s.endDate))}){Text("Назначить напоминание")}
                    else ReminderActions(platform,r,states[r.id],edit={reminder=r.copy(comment=raw!!.comment)},toggle={enabled->saveReminder(raw!!.copy(enabled=enabled,updated=System.currentTimeMillis()))})
                }
            }}
        }
    }},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
    editor?.let {s->ScheduleEditor(s,onDismiss={editor=null},onSave={next->
        val result=mutate {c->c.copy(shared=c.shared.copy(schedules=c.shared.schedules.filterNot {it.id==next.id}+next),local=c.local.copy(reminders=c.local.reminders.filterNot {it.scheduleId==next.id && next.rows.none {row->row.id==it.rowId}}))}
        if(result){selected=next.id;editor=null}
        result
    })}
    offHint?.let {r->RemindersOffHint(onEnable={if(mutate {c->c.copy(local=c.local.copy(remindersOff=false))}){offHint=null;reminder=r}},onSkip={offHint=null;reminder=r},onDismiss={offHint=null})}
    reminder?.let {r->ReminderEditor(r,onDismiss={reminder=null},onSave=::saveReminder,onDelete={mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.filterNot {it.id==r.id}))}},platform=platform,notes=snapshot.shared.notes.filter {it.deleted==null})}
    deleting?.let {s->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить расписание?")},text={Text("Связанные напоминания этого устройства будут отменены.")},confirmButton={TextButton(onClick={if(mutate {c->c.copy(shared=c.shared.copy(schedules=c.shared.schedules.map {if(it.id==s.id)it.copy(deleted=System.currentTimeMillis(),updated=System.currentTimeMillis())else it}),local=c.local.copy(reminders=c.local.reminders.filterNot {it.scheduleId==s.id}))})deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}

@Composable private fun ScheduleEditor(original:DaySchedule,onDismiss:()->Unit,onSave:(DaySchedule)->Boolean) {
    var name by remember {mutableStateOf(original.name)}
    var rows by remember {mutableStateOf(original.rows)}
    var weekdays by remember {mutableStateOf(original.weekdays)}
    var monthDays by remember {mutableStateOf(original.monthDays.joinToString(","))}
    var every by remember {mutableStateOf(original.everyDays.toString())}
    var start by remember {mutableStateOf(original.startDate)}
    var end by remember {mutableStateOf(original.endDate ?: "")}
    var enabled by remember {mutableStateOf(original.enabled)}
    var error by remember {mutableStateOf("")}
    fun rowChange(id:String,change:(ScheduleRow)->ScheduleRow){rows=rows.map {if(it.id==id)change(it)else it}}
    SettingsPage(onDismissRequest=onDismiss,title={Text("График расписания")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(name,{name=it},label={Text("Название")})
        Row {Text("Включено",Modifier.weight(1f));Switch(enabled,{enabled=it})}
        Text("Дни недели",style=MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {(1..7).forEach {d->FilterChip(d in weekdays,{weekdays=if(d in weekdays)weekdays-d else (weekdays+d).sorted()},{androidx.compose.material3.Text(weekdayLabel(d,LocalLanguage.current))})}}
        OutlinedTextField(monthDays,{monthDays=it},label={Text("Числа месяца, пусто — любые")})
        OutlinedTextField(every,{every=it},label={Text("Каждые N дней от начала")})
        OutlinedTextField(start,{start=it},label={Text("Начало YYYY-MM-DD")})
        OutlinedTextField(end,{end=it},label={Text("Конец YYYY-MM-DD, необязательно")})
        Text("Один день: начало = конец. Все правила применяются вместе.")
        rows.forEachIndexed {index,row->key(row.id){
            HorizontalDivider()
            OutlinedTextField(row.time,{v->rowChange(row.id){it.copy(time=v)}},label={Text("Время HH:mm")})
            OutlinedTextField(row.title,{v->rowChange(row.id){it.copy(title=v)}},label={Text("Строка графика")})
            OutlinedTextField(row.comment,{v->rowChange(row.id){it.copy(comment=v)}},label={Text("Комментарий")})
            FlowRow {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(index>0)TextButton(onClick={rows=rows.toMutableList().also {java.util.Collections.swap(it,index,index-1)}}){Text("↑")}
                if(index<rows.lastIndex)TextButton(onClick={rows=rows.toMutableList().also {java.util.Collections.swap(it,index,index+1)}}){Text("↓")}
                TextButton(onClick={rows=rows.filterNot {it.id==row.id}}){Text("Удалить строку")}
                }
            }
        }}
        TextButton(onClick={rows=rows+ScheduleRow()}){Text("Добавить строку")}
        Text("Сохраните график, затем назначьте напоминания его строкам.")
        if(error.isNotBlank())Text(error)
    }},confirmButton={TextButton(onClick={runCatching {val value=original.copy(name=name.trim(),rows=rows,weekdays=weekdays,monthDays=if(monthDays.isBlank())emptyList()else monthDays.split(',').map {it.trim().toInt()},everyDays=every.toInt(),startDate=start.trim(),endDate=end.trim().takeIf {it.isNotEmpty()},enabled=enabled,updated=System.currentTimeMillis());validateSchedule(value);if(onSave(value))onDismiss()}.onFailure {error="Проверьте название, строки, даты и время"}}){Text("Сохранить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}

@Composable fun CalendarDialog(platform:Platform,snapshot:Snapshot,date:String,onSelect:(String)->Unit,onSchedules:()->Unit,onDismiss:()->Unit) {
    var selected by remember {mutableStateOf(LocalDate.parse(date))}
    var month by remember {mutableStateOf(YearMonth.from(selected))}
    var types by remember {mutableStateOf(calendarEntryTypes.keys.toSet())}
    var jump by remember {mutableStateOf(false)}
    var jumpMonth by remember {mutableStateOf(month.toString())}
    var jumpError by remember {mutableStateOf(false)}
    val dataIndex=remember(snapshot.shared.entries) {calendarDataIndex(snapshot.shared.entries)}
    val appearance=snapshot.local.appearance
    val language=LocalLanguage.current
    val markerColors=calendarEntryTypes.keys.associateWith {type->appearance.chart(when(type){"food"->"Калории";"drink"->"Вода";"steps"->"Шаги";"activity"->"Активность";else->"Вес"}).color}
    val reminders=effectiveReminders(snapshot)
    val states=reminderStatus(platform)
    val locale=Locale.forLanguageTag(LocalLanguage.current)
    SettingsPage(onDismissRequest=onDismiss,title={Text("Календарь")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row {TextButton(enabled=month>YearMonth.of(1,1),onClick={month=month.minusMonths(1)}){Text("‹")};TextButton(onClick={jumpMonth=month.toString();jumpError=false;jump=true},modifier=Modifier.weight(1f)){Text("${month.month.getDisplayName(TextStyle.FULL,locale)} ${month.year}")};TextButton(enabled=month<YearMonth.of(9999,12),onClick={month=month.plusMonths(1)}){Text("›")}}
        FlowRow {FilterChip(types==calendarEntryTypes.keys,{types=calendarEntryTypes.keys.toSet()},{Text("Все данные")});calendarEntryTypes.forEach {(id,label)->FilterChip(id in types,{types=if(id in types)types-id else types+id},{Text(label)},leadingIcon={Canvas(Modifier.size(8.dp)){drawCircle(Color(markerColors.getValue(id)))}})}}
        Row {(1..7).forEach {d->Text(DayOfWeek.of(d).getDisplayName(TextStyle.SHORT,locale),Modifier.weight(1f))}}
        val offset=month.atDay(1).dayOfWeek.value-1
        repeat((offset+month.lengthOfMonth()+6)/7){week->Row {
            repeat(7){day->val n=week*7+day-offset+1
                if(n !in 1..month.lengthOfMonth())Spacer(Modifier.weight(1f))else {
                    val current=month.atDay(n)
                    val plans=snapshot.shared.schedules.any {scheduleOn(it,current)}
                    val assigned=reminders.filter {remindersOn(it,current).isNotEmpty()}
                    val counts=dataIndex[current.toString()].orEmpty()
                    val marks=types.filter {counts.getOrDefault(it,0)>0}
                    val description="$current · "+if(counts.isEmpty())translate("Нет данных",language)else counts.entries.joinToString {translate(calendarEntryTypes.getValue(it.key),language)+": ${it.value}"}
                    TextButton(onClick={selected=current},modifier=Modifier.weight(1f).semantics(mergeDescendants=true){contentDescription=description},contentPadding=PaddingValues(0.dp)){
                        Column {Text(if(current==selected)"[$n]"else "$n")
                            if(marks.isEmpty())Text(if(counts.isEmpty())"—"else "◇")else Canvas(Modifier.width(28.dp).height(8.dp)){marks.forEachIndexed {i,type->drawCircle(Color(markerColors.getValue(type)),2.5.dp.toPx(),Offset((i+.5f)*size.width/marks.size,size.height/2))}}
                            Text((if(plans)"▦"else "")+(if(assigned.any {it.enabled})"●"else if(assigned.isNotEmpty())"○"else ""))}
                    }
                }
            }
        }}
        Text("▦ расписание · ● включённые · ○ выключенные напоминания")
        Text("— нет данных · ◇ есть другие типы · цветные точки — выбранные типы")
        Text("$selected",style=MaterialTheme.typography.titleMedium)
        val counts=dataIndex[selected.toString()].orEmpty()
        if(counts.isEmpty())Text("Нет данных")else {
            if(counts.keys.none {it in types})Text("Нет данных выбранных типов")
            calendarEntryTypes.filterKeys {it in types && counts.getOrDefault(it,0)>0}.forEach {(id,label)->Text("${translate(label,language)}: ${counts.getValue(id)}")}
        }
        snapshot.shared.schedules.filter {scheduleOn(it,selected)}.forEach {s->
            Text(s.name+if(!s.enabled)" (выключено)"else "",style=MaterialTheme.typography.titleMedium)
            s.rows.forEach {row->Text("${row.time} · ${row.title}");if(row.comment.isNotBlank())Text(row.comment)}
        }
        reminders.filter {remindersOn(it,selected).isNotEmpty()}.forEach {r->
            Text("${r.times.ifEmpty {listOf(r.time)}.joinToString()} · ${r.name}")
            Text(reminderStateLabel(r,states[r.id]))
        }
        TextButton(onClick=onSchedules){Text("Управлять расписаниями и напоминаниями")}
        Text("Маркеры показывают назначения; выдача уведомления зависит от разрешений ОС.")
    }},confirmButton={TextButton(onClick={onSelect(selected.toString());onDismiss()}){Text("Открыть день")}},dismissButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
    if(jump)AlertDialog(onDismissRequest={jump=false},title={Text("Перейти к месяцу")},text={Column {
        OutlinedTextField(jumpMonth,{jumpMonth=it;jumpError=false},label={Text("YYYY-MM")},singleLine=true)
        if(jumpError)Text("Проверьте даты и предел 100 лет")
    }},confirmButton={TextButton(onClick={runCatching {YearMonth.parse(jumpMonth).also {require(it.year in 1..9999)}}.onSuccess {month=it;jump=false}.onFailure {jumpError=true}}){Text("Перейти")}},dismissButton={TextButton(onClick={jump=false}){Text("Отмена")}})
}
