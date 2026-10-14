package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Note tags (D085), rules as in Notio: a name is 1–32 characters, unique ignoring case; a leading # and extra spaces are dropped.
 * Tags travel on the notes themselves; a tag created in Settings and not yet used is kept on this device only.
 */
const val TAG_MAX_LENGTH=32
const val NOTE_MAX_TAGS=32
fun cleanTag(input:String)=input.trim().removePrefix("#").trim().replace(Regex("\\s+")," ")
fun validTag(name:String)=name.isNotEmpty() && name.length<=TAG_MAX_LENGTH && cleanTag(name)==name
private fun same(a:String,b:String)=a.equals(b,ignoreCase=true)
/** Every tag with the number of notes that carry it, trashed notes excluded from the count; unused tags count 0. */
fun noteTagCounts(snapshot:Snapshot):Map<String,Int> {
    val names=(snapshot.shared.notes.flatMap {it.tags}+snapshot.local.noteTags).distinctBy {it.lowercase()}
    return names.sortedBy {it.lowercase()}.associateWith {name->snapshot.shared.notes.count {note->note.deleted==null && note.tags.any {same(it,name)}}}
}
fun createTag(snapshot:Snapshot,input:String):Snapshot {
    val name=cleanTag(input);require(validTag(name)){"Invalid tag"}
    require(noteTagCounts(snapshot).keys.none {same(it,name)}){"Duplicate tag"}
    return snapshot.copy(local=snapshot.local.copy(noteTags=snapshot.local.noteTags+name))
}
fun renameTag(snapshot:Snapshot,old:String,input:String,now:Long=System.currentTimeMillis()):Snapshot {
    val name=cleanTag(input);require(validTag(name)){"Invalid tag"}
    require(noteTagCounts(snapshot).keys.none {!same(it,old) && same(it,name)}){"Duplicate tag"}
    return snapshot.copy(shared=snapshot.shared.copy(notes=snapshot.shared.notes.map {note->if(note.tags.none {same(it,old)})note else note.copy(tags=note.tags.map {if(same(it,old))name else it},updated=now)}),
        local=snapshot.local.copy(noteTags=snapshot.local.noteTags.map {if(same(it,old))name else it}))
}
/** Removes the tag from every note, trashed ones included, and from this device's unused list. */
fun deleteTag(snapshot:Snapshot,name:String,now:Long=System.currentTimeMillis()):Snapshot =
    snapshot.copy(shared=snapshot.shared.copy(notes=snapshot.shared.notes.map {note->if(note.tags.none {same(it,name)})note else note.copy(tags=note.tags.filterNot {same(it,name)},updated=now)}),
        local=snapshot.local.copy(noteTags=snapshot.local.noteTags.filterNot {same(it,name)}))
/** Adds a typed tag to a note, reusing the spelling of a tag that already exists. Null when the text is not a valid tag. */
fun withTag(tags:List<String>,input:String,known:Collection<String>):List<String>? {
    val name=cleanTag(input);if(!validTag(name) || tags.size>=NOTE_MAX_TAGS)return null
    val existing=known.firstOrNull {same(it,name)} ?: name
    return if(tags.any {same(it,existing)})tags else tags+existing
}

/** Tags of one note: tap to put on or take off, type to add a new one. */
@Composable fun NoteTagPicker(tags:List<String>,known:Collection<String>,onChange:(List<String>)->Unit) {
    var typed by remember {mutableStateOf("")}
    val all=(known+tags).distinctBy {it.lowercase()}.sortedBy {it.lowercase()}
    Text("Теги",style=MaterialTheme.typography.titleSmall)
    if(all.isNotEmpty())FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){all.forEach {name->val on=tags.any {it.equals(name,true)}
        FilterChip(on,{onChange(if(on)tags.filterNot {it.equals(name,true)}else tags+name)},{androidx.compose.material3.Text("#$name")})}}
    Row(verticalAlignment=Alignment.CenterVertically){
        OutlinedTextField(typed,{typed=it.take(TAG_MAX_LENGTH+1)},label={Text("Новый тег")},singleLine=true,modifier=Modifier.weight(1f))
        IconAction("add","Добавить тег",onClick={withTag(tags,typed,all)?.let {onChange(it);typed=""}})
    }
}

/** Settings → Tags: create, rename, delete; deleting asks first and takes the tag off every note. */
@Composable fun NoteTagsSettings(snapshot:Snapshot,mutate:((Snapshot)->Snapshot)->Boolean) {
    val counts=remember(snapshot.shared.notes,snapshot.local.noteTags){noteTagCounts(snapshot)}
    var typed by remember {mutableStateOf("")};var error by remember {mutableStateOf("")}
    var renaming by remember {mutableStateOf<String?>(null)};var renamed by remember {mutableStateOf("")};var deleting by remember {mutableStateOf<String?>(null)}
    fun attempt(change:(Snapshot)->Snapshot):Boolean {var failed=false;val saved=mutate {current->runCatching {change(current)}.getOrElse {failed=true;current}};error=if(failed)"Тег уже есть или имя не подходит (1–32 знака)" else "";return saved && !failed}
    Text("Теги помечают заметки и помогают их находить. Тег без заметок хранится только на этом устройстве.",style=MaterialTheme.typography.bodySmall)
    Row(verticalAlignment=Alignment.CenterVertically){
        OutlinedTextField(typed,{typed=it.take(TAG_MAX_LENGTH+1)},label={Text("Новый тег")},singleLine=true,modifier=Modifier.weight(1f))
        IconAction("add","Создать тег",onClick={if(attempt {createTag(it,typed)})typed=""})
    }
    if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
    if(counts.isEmpty())Text("Тегов пока нет")
    counts.forEach {(name,count)->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(horizontal=12.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){androidx.compose.material3.Text("#$name",style=MaterialTheme.typography.titleMedium);androidx.compose.material3.Text(translate("Заметок: {0}",LocalLanguage.current).replace("{0}",count.toString()),style=MaterialTheme.typography.bodySmall)}
        IconAction("edit","Переименовать тег",onClick={renaming=name;renamed=name;error=""})
        IconAction("delete","Удалить тег",onClick={deleting=name})
    }}}
    renaming?.let {old->AlertDialog(onDismissRequest={renaming=null},title={Text("Переименовать тег")},text={OutlinedTextField(renamed,{renamed=it.take(TAG_MAX_LENGTH+1)},label={Text("Название")},singleLine=true)},
        confirmButton={TextButton(onClick={if(attempt {renameTag(it,old,renamed)})renaming=null}){Text("Сохранить")}},dismissButton={TextButton(onClick={renaming=null}){Text("Отмена")}})}
    deleting?.let {name->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить тег?")},text={Text("Тег будет снят со всех заметок. Сами заметки останутся.")},
        confirmButton={TextButton(onClick={attempt {deleteTag(it,name)};deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}
