package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable private fun NotePaneDialog(embedded:Boolean,onDismissRequest:()->Unit,title:@Composable ()->Unit,text:@Composable ()->Unit,confirmButton:@Composable ()->Unit,dismissButton:@Composable ()->Unit){
    if(embedded)Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){title();text();Row {confirmButton();dismissButton()}}
    else SettingsPage(onDismissRequest=onDismissRequest,title=title,text=text,confirmButton=confirmButton,dismissButton=dismissButton)
}

@Composable fun NotesHub(platform:Platform,snapshot:Snapshot,mutate:((Snapshot)->Snapshot)->Boolean,onDismiss:()->Unit,fullScreen:Boolean=false,focusNote:String?=null,onFocused:()->Unit={}){
    var editing by remember {mutableStateOf<Note?>(null)}
    var reminding by remember {mutableStateOf<Note?>(null)}
    var reminder by remember {mutableStateOf<Reminder?>(null)}
    var offHint by remember {mutableStateOf<Reminder?>(null)}
    // A reminder card or notification asked for its note.
    LaunchedEffect(focusNote){if(focusNote!=null){snapshot.shared.notes.firstOrNull {it.id==focusNote && it.deleted==null}?.let {editing=it};onFocused()}}
    var query by remember {mutableStateOf("")}
    var trash by remember {mutableStateOf(false)}
    var deleting by remember {mutableStateOf<Note?>(null)}
    var kind by remember {mutableStateOf("")}
    var tag by remember {mutableStateOf<String?>(null)}
    val tagCounts=remember(snapshot.shared.notes,snapshot.local.noteTags){noteTagCounts(snapshot)}
    var folder by remember {mutableStateOf<String?>(null)}
    var all by remember {mutableStateOf(true)}
    var folderRoot by remember {mutableStateOf(true)}
    var createFolder by remember {mutableStateOf(false)}
    var sortDialog by remember {mutableStateOf(false)}
    var folderSortDialog by remember {mutableStateOf(false)}
    var folders by remember {mutableStateOf(false)}
    var moving by remember {mutableStateOf<Note?>(null)}
    var stamps by remember {mutableStateOf(false)}
    LaunchedEffect(snapshot.shared.noteFolders) {if(folder!=null && snapshot.shared.noteFolders.none {it.id==folder && it.deleted==null})folder=null}
    val language=LocalLanguage.current
    val display=snapshot.local.appearance.noteDisplay
    fun saveDisplay(value:NoteDisplay){mutate {c->c.copy(local=c.local.copy(appearance=c.local.appearance.copy(noteDisplay=value)))}}
    val sortKey=if(all)"all"else folder ?: "root"
    val noteSort=display.noteSorts[sortKey] ?: NoteSort()
    val notes=remember(snapshot.shared,kind,folder,all,trash,query,noteSort,tag) {sortedNotes(visibleNotes(snapshot.shared,kind,folder,all,trash,query,tag?.takeIf {it in tagCounts}),noteSort)}
    val folderRows=remember(snapshot.shared.noteFolders,snapshot.shared.notes,display.folderSort,query){sortedNoteFolders(snapshot.shared,display.folderSort,query)}
    val folderCounts=remember(snapshot.shared.notes){snapshot.shared.notes.filter {it.deleted==null}.groupingBy {it.folderId}.eachCount()}
    val content:@Composable ()->Unit={Column(if(fullScreen)Modifier.fillMaxWidth()else Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(query,{query=it},label={Text("Поиск заметок")})
        FlowRow {FilterChip(kind.isEmpty(),{kind=""},{Text("Все типы")});listOf("text" to "Текст","drawing" to "Рисунок","list" to "Список").forEach {(id,label)->FilterChip(kind==id,{kind=id},{Text(label)})}}
        if(tagCounts.isNotEmpty())FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(tag==null,{tag=null},{Text("Все теги")});tagCounts.keys.forEach {name->FilterChip(tag==name,{tag=if(tag==name)null else name},{androidx.compose.material3.Text("#$name")})}}
        FlowRow {
            FilterChip(all,{all=true},{Text(noteNavigationText("Все заметки",language))})
            FilterChip(!all,{all=false;folderRoot=true;folder=null},{Text(noteNavigationText("По папкам",language))})
        }
        if(!all){
            Row {
                if(!folderRoot)IconAction("previous","К папкам",onClick={folderRoot=true;folder=null})
                Text(if(folderRoot)translate("Папки",language)else snapshot.shared.noteFolders.firstOrNull {it.id==folder}?.name ?: noteNavigationText("Без папки",language),Modifier.weight(1f))
                IconAction("add","Создать папку",onClick={createFolder=true;folders=true})
                IconAction("folder","Управление папками",onClick={createFolder=false;folders=true})
                if(folderRoot)IconAction("sort","Сортировка папок",onClick={folderSortDialog=true})
            }
            if(folderRoot)LazyColumn(Modifier.heightIn(max=180.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                item {OutlinedCard(onClick={folder=null;folderRoot=false},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp)){FitIcon("notes");Spacer(Modifier.width(8.dp));Text(noteNavigationText("Без папки",language))}}}
                items(folderRows,key={it.id}){f->OutlinedCard(onClick={folder=f.id;folderRoot=false},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Row {FitIcon("folder");Spacer(Modifier.width(8.dp));Text(f.name,Modifier.weight(1f));Text((folderCounts[f.id] ?: 0).toString())};NoteStamps(f.created,f.updated,display.folderCreated,display.folderModified)}}}
            }
        }
        if(all || !folderRoot){
        FlowRow {listOf("text" to "Текст","drawing" to "Рисунок","list" to "Список").forEach {(type,label)->IconAction(type,label,onClick={editing=Note(kind=type,title=automaticName(translate(label,language),snapshot.shared.notes.filter {it.deleted==null}.map {it.title}),created=System.currentTimeMillis(),folderId=if(all)null else folder)})}}
        FlowRow {listOf("cards" to "Карточки","list" to "Компактный список","grid" to "Сетка").forEach {(view,label)->IconAction(view,label,onClick={saveDisplay(display.copy(view=view))},modifier=Modifier.background(if(display.view==view)MaterialTheme.colorScheme.secondaryContainer else Color.Transparent))};IconAction("sort","Сортировка заметок",onClick={sortDialog=true});IconAction("today","Штампы времени",onClick={stamps=true})}
        Row {Text("Корзина заметок",Modifier.weight(1f));Switch(trash,{trash=it})}
        if(notes.isEmpty())Text("Пока нет записей")
        LazyColumn(Modifier.height(320.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(notes.chunked(if(display.view=="grid")2 else 1),key={row->row.first().id}) {row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach {n->Box(Modifier.weight(1f)){NotePreview(n,display,snapshot.shared.noteFolders.firstOrNull {it.id==n.folderId && it.deleted==null}?.name,display.view=="list",onEdit={editing=n},onCopy={mutate {c->c.copy(shared=c.shared.copy(notes=c.shared.notes+copyNote(n).copy(title=n.title+" (${translate("Копировать",language)})"))) }},onDelete={deleting=n},onMove={moving=n},onRestore={mutate {c->c.copy(shared=c.shared.copy(notes=c.shared.notes.map {if(it.id==n.id)it.copy(deleted=null,updated=System.currentTimeMillis())else it}))}},reminders=snapshot.local.reminders.count {it.noteId==n.id},onRemind={reminding=n})}}
                if(display.view=="grid" && row.size==1)Spacer(Modifier.weight(1f))
            }}
        }
        }
    }}
    if(editing==null || !fullScreen){if(fullScreen)content()else SettingsPage(onDismissRequest=onDismiss,title={Text(translate("Notes",language))},text=content,confirmButton={TextButton(onClick=onDismiss){Text(translate("Close",language))}})}
    if(stamps)NoteDisplayDialog(display,::saveDisplay,onDismiss={stamps=false})
    if(sortDialog)NoteSortDialog(noteSort,false,{saveDisplay(display.copy(noteSorts=display.noteSorts+(sortKey to it)))},{sortDialog=false})
    if(folderSortDialog)NoteSortDialog(display.folderSort,true,{saveDisplay(display.copy(folderSort=it))},{folderSortDialog=false})
    if(folders)NoteFoldersDialog(snapshot.shared,display,mutate,onSelect={folder=it;all=false;folderRoot=false},onDismiss={folders=false},createInitially=createFolder)
    moving?.let {note->NoteFoldersDialog(snapshot.shared,display,mutate,onSelect={destination->mutate {c->c.copy(shared=moveNote(c.shared,note.id,destination))}},onDismiss={moving=null})}

    editing?.let {n->NoteEditor(platform,n,display,tagCounts.keys,embedded=fullScreen,onDismiss={editing=null},onSave={value->mutate {c->c.copy(shared=c.shared.copy(notes=c.shared.notes.filterNot {it.id==value.id}+value))}})}
    fun saveReminder(r:Reminder)=mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.filterNot {it.id==r.id}+r))}
    fun dropReminder(r:Reminder)=mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.filterNot {it.id==r.id}))}
    reminding?.let {n->SettingsPage(onDismissRequest={reminding=null},title={androidx.compose.material3.Text(n.title,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},text={Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ReminderCards(platform,snapshot,onEdit={reminder=it},onSave=::saveReminder,onDelete=::dropReminder,filter={it.noteId==n.id})
    }},confirmButton={Button(onClick={val made=Reminder(name="",noteId=n.id).let {it.copy(name=n.title.take(200))};if(snapshot.local.remindersOff)offHint=made else reminder=made}){Text("Добавить напоминание")}})}
    offHint?.let {r->RemindersOffHint(onEnable={if(mutate {c->c.copy(local=c.local.copy(remindersOff=false))}){offHint=null;reminder=r}},onSkip={offHint=null;reminder=r},onDismiss={offHint=null})}
    reminder?.let {r->ReminderEditor(r,onDismiss={reminder=null},onSave=::saveReminder,onDelete={dropReminder(r)},platform=platform,notes=snapshot.shared.notes.filter {it.deleted==null},isNew=snapshot.local.reminders.none {it.id==r.id})}
    deleting?.let {n->AlertDialog(onDismissRequest={deleting=null},title={Text("В корзину?")},confirmButton={TextButton(onClick={if(mutate {c->c.copy(shared=c.shared.copy(notes=c.shared.notes.map {if(it.id==n.id)it.copy(deleted=System.currentTimeMillis(),updated=System.currentTimeMillis())else it}))})deleting=null}){Text("В корзину")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}

@Composable private fun NoteEditor(platform:Platform,original:Note,display:NoteDisplay,knownTags:Collection<String>,embedded:Boolean=false,onDismiss:()->Unit,onSave:(Note)->Boolean){
    var draft by remember {mutableStateOf(original)}
    var discard by remember {mutableStateOf(false)}
    var clear by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var color by remember {mutableLongStateOf(0xFF202020)}
    var width by remember {mutableFloatStateOf(0.006f)}
    var current by remember {mutableStateOf<List<InkPoint>>(emptyList())}
    // A drawing opens straight on the whole-screen sheet; the form with title and tags is behind it.
    var board by remember {mutableStateOf(original.kind=="drawing")}
    SideEffect {platform.editingChanged(draft!=original || current.isNotEmpty())}
    DisposableEffect(platform){onDispose {platform.editingChanged(false)}}
    fun close(){if(draft!=original || current.isNotEmpty())discard=true else onDismiss()}
    NotePaneDialog(embedded=embedded,onDismissRequest=::close,title={Text(when(original.kind){"drawing"->"Рисунок";"list"->"Список";else->"Текстовая заметка"})},text={Column(if(embedded)Modifier.fillMaxWidth()else Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(draft.title,{draft=draft.copy(title=it)},label={Text("Название")})
        NoteStamps(draft.created,draft.updated,display.created,display.modified)
        NoteTagPicker(draft.tags,knownTags){draft=draft.copy(tags=it)}
        when(draft.kind){
            "text"->OutlinedTextField(draft.text,{draft=draft.copy(text=it)},label={Text("Текст заметки")},minLines=8)
            "list"->{draft.items.forEachIndexed {index,item->key(item.id){
                Row {Checkbox(item.checked,{v->draft=draft.copy(items=draft.items.map {if(it.id==item.id)it.copy(checked=v)else it})});OutlinedTextField(item.text,{v->draft=draft.copy(items=draft.items.map {if(it.id==item.id)it.copy(text=v)else it})},modifier=Modifier.weight(1f),label={Text("Пункт")})}
                OutlinedTextField(item.comment,{v->draft=draft.copy(items=draft.items.map {if(it.id==item.id)it.copy(comment=v)else it})},label={Text("Комментарий")})
                FlowRow {if(index>0)TextButton(onClick={draft=draft.copy(items=draft.items.toMutableList().also {java.util.Collections.swap(it,index,index-1)})}){Text("↑")};if(index<draft.items.lastIndex)TextButton(onClick={draft=draft.copy(items=draft.items.toMutableList().also {java.util.Collections.swap(it,index,index+1)})}){Text("↓")};TextButton(onClick={draft=draft.copy(items=draft.items.filterNot {it.id==item.id})}){Text("Удалить пункт")}}
            }};TextButton(onClick={draft=draft.copy(items=draft.items+NoteItem())}){Text("Добавить пункт")}}
            "drawing"->{
                IconAction("drawing","Открыть полотно",onClick={board=true})
                ColorChoice(color,{color=it})
                Text("Толщина пера");Slider(width,{width=it},valueRange=0.002f..0.025f)
                val strokes by rememberUpdatedState(draft.strokes)
                Canvas(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White).pointerInput(color,width){
                    fun point(position:Offset)=InkPoint((position.x/size.width).coerceIn(0f,1f),(position.y/size.height).coerceIn(0f,1f))
                    detectDragGestures(onDragStart={current=listOf(point(it))},onDrag={change,_->change.consume();if(current.size<10000)current=current+point(change.position)},onDragEnd={
                        if(current.isNotEmpty() && strokes.size<2000 && strokes.sumOf {it.points.size}+current.size<=100000)draft=draft.copy(strokes=strokes+InkStroke(color=color,width=width,points=current))
                        else error="Достигнут предел рисунка"
                        current=emptyList()
                    },onDragCancel={current=emptyList()})
                }){
                    fun ink(points:List<InkPoint>,c:Long,w:Float){if(points.isEmpty())return;val radius=w*size.width/2;if(points.size==1)drawCircle(Color(c),radius,Offset(points[0].x*size.width,points[0].y*size.height))else {val path=Path();points.forEachIndexed {i,p->if(i==0)path.moveTo(p.x*size.width,p.y*size.height)else path.lineTo(p.x*size.width,p.y*size.height)};drawPath(path,Color(c),style=Stroke(w*size.width,cap=StrokeCap.Round))}}
                    draft.strokes.forEach {ink(it.points,it.color,it.width)};ink(current,color,width)
                }
                FlowRow {TextButton(onClick={draft=draft.copy(strokes=draft.strokes.dropLast(1))},enabled=draft.strokes.isNotEmpty()){Text("Отменить штрих")};TextButton(onClick={clear=true}){Text("Очистить рисунок")}}
                Text("Рисуйте пальцем, стилусом или мышью. Полотно квадратное на всех устройствах.")
            }
        }
        if(error.isNotEmpty())Text(error)
    }},confirmButton={TextButton(onClick={runCatching {val value=draft.copy(title=draft.title.trim(),updated=System.currentTimeMillis());validateNote(value);if(onSave(value))onDismiss()}.onFailure {error="Проверьте название и размер заметки"}}){Text("Сохранить")}},dismissButton={TextButton(onClick=::close){Text("Закрыть")}})
    if(board)DrawingBoard(platform,draft.strokes,{draft=draft.copy(strokes=it)},{board=false})
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Очистить рисунок?")},confirmButton={TextButton(onClick={draft=draft.copy(strokes=emptyList());clear=false}){Text("Очистить")}},dismissButton={TextButton(onClick={clear=false}){Text("Отмена")}})
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Закрыть без сохранения?")},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть без сохранения")}},dismissButton={TextButton(onClick={discard=false}){Text("Продолжить редактирование")}})
}
