package com.fiteasy.shared

import kotlinx.serialization.Serializable

@Serializable data class NoteItem(val id:String=newId(),val text:String="",val checked:Boolean=false,val comment:String="")
@Serializable data class InkPoint(val x:Float,val y:Float)
@Serializable data class InkStroke(val id:String=newId(),val color:Long=0xFF202020,val width:Float=0.006f,val points:List<InkPoint>)
@Serializable data class Note(val id:String=newId(),val kind:String="text",val title:String="",val text:String="",val items:List<NoteItem> = emptyList(),val strokes:List<InkStroke> = emptyList(),val updated:Long=System.currentTimeMillis(),val deleted:Long?=null,val folderId:String?=null,val created:Long?=null,val tags:List<String> = emptyList())
@Serializable data class NoteFolder(val id:String=newId(),val name:String,val created:Long=System.currentTimeMillis(),val updated:Long=created,val deleted:Long?=null)
@Serializable data class NoteSort(val key:String="updated",val descending:Boolean=true)
@Serializable data class NoteDisplay(val created:Boolean=true,val modified:Boolean=true,val folderCreated:Boolean=false,val folderModified:Boolean=false,val view:String="cards",val noteSorts:Map<String,NoteSort> = emptyMap(),val folderSort:NoteSort=NoteSort("name",false))

fun <T> sortNoteRows(rows:List<T>,sort:NoteSort,id:(T)->String,value:(T)->Comparable<*>?):List<T> {
    val keys=rows.associate {id(it) to value(it)}
    return rows.sortedWith(Comparator {a,b->
        val x=keys[id(a)];val y=keys[id(b)]
        @Suppress("UNCHECKED_CAST")
        val result=when {x==null && y==null->0;x==null->1;y==null->-1;else->{val c=(x as Comparable<Any>).compareTo(y as Any);if(sort.descending)-c else c}}
        if(result==0)id(a).compareTo(id(b))else result
    })
}
fun sortedNotes(rows:List<Note>,sort:NoteSort)=sortNoteRows(rows,sort,{it.id}) {when(sort.key){"name"->it.title.lowercase(java.util.Locale.ROOT);"created"->it.created;"type"->it.kind;else->it.updated}}
fun sortedNoteFolders(data:SharedData,sort:NoteSort,query:String=""):List<NoteFolder> {
    val counts=data.notes.filter {it.deleted==null}.groupingBy {it.folderId}.eachCount()
    return sortNoteRows(data.noteFolders.filter {it.deleted==null && it.name.contains(query,true)},sort,{it.id}) {when(sort.key){"created"->it.created;"updated"->it.updated;"count"->counts[it.id] ?: 0;else->it.name.lowercase(java.util.Locale.ROOT)}}
}
const val NOTE_MAX_TIME=253402300799999L
fun validateNote(n:Note){
    require(n.id.isNotBlank() && n.kind in setOf("text","drawing","list") && n.title.isNotBlank() && n.title.length<=512 && n.text.length<=200000)
    require(n.updated in 0..NOTE_MAX_TIME && (n.created==null || n.created in 0..NOTE_MAX_TIME) && (n.folderId==null || n.folderId.isNotBlank()))
    require(n.tags.size<=NOTE_MAX_TAGS && n.tags.all(::validTag) && n.tags.map {it.lowercase()}.distinct().size==n.tags.size)
    require(n.items.size<=1000 && n.items.map {it.id}.distinct().size==n.items.size)
    n.items.forEach {require(it.id.isNotBlank() && it.text.length<=4096 && it.comment.length<=8192)}
    require(n.strokes.size<=2000 && n.strokes.sumOf {it.points.size}<=100000 && n.strokes.map {it.id}.distinct().size==n.strokes.size)
    n.strokes.forEach {s->require(s.id.isNotBlank() && s.color in 0..0xFFFFFFFF && s.width.isFinite() && s.width in 0.001f..0.1f && s.points.isNotEmpty());s.points.forEach {require(it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f)}}
}
fun copyNote(n:Note)=n.copy(id=newId(),items=n.items.map {it.copy(id=newId())},strokes=n.strokes.map {it.copy(id=newId())},updated=System.currentTimeMillis(),created=System.currentTimeMillis())

fun moveNote(data:SharedData,id:String,folder:String?,now:Long=System.currentTimeMillis()):SharedData {
    require(folder==null || data.noteFolders.any {it.id==folder && it.deleted==null})
    require(data.notes.any {it.id==id})
    return data.copy(notes=data.notes.map {if(it.id==id)it.copy(folderId=folder,updated=now)else it})
}
fun removeNoteFolder(data:SharedData,id:String,now:Long=System.currentTimeMillis()):SharedData {
    require(data.noteFolders.any {it.id==id})
    return data.copy(noteFolders=data.noteFolders.map {if(it.id==id)it.copy(deleted=now,updated=now)else it},notes=data.notes.map {if(it.folderId==id)it.copy(folderId=null,updated=now)else it})
}
fun visibleNotes(data:SharedData,kind:String,folder:String?,all:Boolean,trash:Boolean,query:String,tag:String?=null):List<Note> {
    val folders=data.noteFolders.filter {it.deleted==null}.map {it.id}.toSet()
    return data.notes.filter {(it.deleted!=null)==trash && (kind.isEmpty() || it.kind==kind) && (all || if(folder==null)it.folderId !in folders else it.folderId==folder) && (tag==null || it.tags.any {name->name.equals(tag,true)}) && (it.title+it.text+it.items.joinToString {row->row.text+row.comment}+it.tags.joinToString(" "){name->"#$name"}).contains(query,true)}.sortedByDescending {it.updated}
}
