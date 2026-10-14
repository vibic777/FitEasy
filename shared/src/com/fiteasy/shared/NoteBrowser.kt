package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable fun NoteStamps(created:Long?,modified:Long,showCreated:Boolean,showModified:Boolean) {
    val language=LocalLanguage.current
    val formatter=remember(language){DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(language)).withZone(ZoneId.systemDefault())}
    if(showCreated && created!=null)Text("${translate("Создано",language)}: ${formatter.format(Instant.ofEpochMilli(created))}",style=MaterialTheme.typography.bodySmall)
    if(showModified)Text("${translate("Изменено",language)}: ${formatter.format(Instant.ofEpochMilli(modified))}",style=MaterialTheme.typography.bodySmall)
}

@Composable fun NoteDisplayDialog(display:NoteDisplay,onSave:(NoteDisplay)->Unit,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("Штампы времени")},text={Column {
        listOf("Время создания заметок" to display.created,"Время изменения заметок" to display.modified,"Время создания папок" to display.folderCreated,"Время изменения папок" to display.folderModified).forEachIndexed {index,(label,value)->
            Row {
                Text(label,Modifier.weight(1f))
                Switch(value,{enabled->onSave(when(index){0->display.copy(created=enabled);1->display.copy(modified=enabled);2->display.copy(folderCreated=enabled);else->display.copy(folderModified=enabled)})})
            }
        }
    }},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
}

@Composable fun NotePreview(note:Note,display:NoteDisplay,folderName:String?,compact:Boolean,onEdit:()->Unit,onCopy:()->Unit,onDelete:()->Unit,onMove:()->Unit,onRestore:()->Unit,reminders:Int=0,onRemind:(()->Unit)?=null) {
    Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row {FitIcon(note.kind);Spacer(Modifier.width(6.dp));Text(note.title,style=MaterialTheme.typography.titleMedium)}
        folderName?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
        if(note.tags.isNotEmpty())androidx.compose.material3.Text(note.tags.joinToString("  "){"#$it"},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        if(!compact)when(note.kind) {
            "drawing"->Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                // The sheet is square in the editor: keep it square here, centred, whatever the card or grid cell shape.
                val sheet=noteSheetRect(size.width,size.height)
                drawRect(Color.White,sheet.topLeft,sheet.size)
                note.strokes.take(100).forEach {stroke->val path=Path();stroke.points.take(200).forEachIndexed {i,p->val x=sheet.left+p.x*sheet.width;val y=sheet.top+p.y*sheet.height;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};drawPath(path,Color(stroke.color),style=Stroke(stroke.width*sheet.width,cap=StrokeCap.Round,join=StrokeJoin.Round))}
            }
            "list"->Text(note.items.take(3).joinToString("\n") {(if(it.checked)"☑ "else "☐ ")+it.text})
            else->Text(note.text.take(120))
        }
        NoteStamps(note.created,note.updated,display.created,display.modified)
        FlowRow {
            if(note.deleted==null){IconAction("edit","Открыть",onClick=onEdit);IconAction("notes","Переместить",onClick=onMove);IconAction("copy","Копировать",onClick=onCopy);IconAction("delete","В корзину",onClick=onDelete)
                // Reminders of this note, as in Notio: a plain bell opens them, a bell with a plus means there are none yet.
                if(onRemind!=null)RigAction(if(reminders>0)"bell" else "bell+",textWith("Напоминания заметки: {0}",LocalLanguage.current,reminders),onRemind,selected=reminders>0,translated=false)}
            else IconAction("copy","Восстановить",onClick=onRestore)
        }
    }}
}

@Composable fun NoteFoldersDialog(data:SharedData,display:NoteDisplay,mutate:((Snapshot)->Snapshot)->Boolean,onSelect:(String?)->Unit,onDismiss:()->Unit,createInitially:Boolean=false) {
    val language=LocalLanguage.current
    var editing by remember {mutableStateOf<NoteFolder?>(null)}
    var name by remember {mutableStateOf(if(createInitially)automaticName(translate("Папка",language),data.noteFolders.filter {it.deleted==null}.map {it.name})else "")}
    var editor by remember {mutableStateOf(createInitially)}
    var deleting by remember {mutableStateOf<NoteFolder?>(null)}
    var query by remember {mutableStateOf("")}
    SettingsPage(onDismissRequest=onDismiss,title={Text("Папки заметок")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query,{query=it},label={Text("Поиск папок")},singleLine=true)
        TextButton(onClick={onSelect(null);onDismiss()}){Text("Без папки")}
        IconAction("add","Создать папку",onClick={editing=null;name=automaticName(translate("Папка",language),data.noteFolders.filter {it.deleted==null}.map {it.name});editor=true})
        LazyColumn(Modifier.heightIn(max=300.dp)) {items(sortedNoteFolders(data,display.folderSort,query),key={it.id}) {folder->Column {
            Row {TextButton(onClick={onSelect(folder.id);onDismiss()},modifier=Modifier.weight(1f)){Text(folder.name)};IconAction("edit","Изменить",onClick={editing=folder;name=folder.name;editor=true});IconAction("delete","Удалить папку",onClick={deleting=folder})}
            NoteStamps(folder.created,folder.updated,display.folderCreated,display.folderModified)
        }}}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
    if(editor)AlertDialog(onDismissRequest={editor=false},title={Text("Папка")},text={OutlinedTextField(name,{name=it.take(512)},label={Text("Название")})},confirmButton={TextButton(enabled=name.isNotBlank(),onClick={val original=editing;val folder=original?.copy(name=name.trim(),updated=System.currentTimeMillis()) ?: NoteFolder(name=name.trim());if(mutate {c->c.copy(shared=c.shared.copy(noteFolders=c.shared.noteFolders.filterNot {it.id==folder.id}+folder))})editor=false}){Text("Сохранить")}},dismissButton={TextButton(onClick={editor=false}){Text("Отмена")}})
    deleting?.let {folder->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить папку?")},text={Text("Заметки сохранятся без папки.")},confirmButton={TextButton(onClick={if(mutate {c->c.copy(shared=removeNoteFolder(c.shared,folder.id))})deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}
/** Largest centred square inside a preview area: a drawing keeps the proportions it was drawn with. */
fun noteSheetRect(width:Float,height:Float):androidx.compose.ui.geometry.Rect {
    val side=minOf(width,height).coerceAtLeast(0f)
    return androidx.compose.ui.geometry.Rect((width-side)/2,(height-side)/2,(width+side)/2,(height+side)/2)
}
