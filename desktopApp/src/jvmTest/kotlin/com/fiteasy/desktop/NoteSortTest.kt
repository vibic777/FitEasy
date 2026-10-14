package com.fiteasy.desktop
import com.fiteasy.shared.*
import kotlin.test.*

class NoteSortTest {
    @Test fun allAndFolderModesIncludeMovedNotes(){
        val data=SharedData(noteFolders=listOf(NoteFolder(id="f",name="Folder")),notes=listOf(Note(id="a",title="A"),Note(id="b",title="B",folderId="f")))
        assertEquals(setOf("a","b"),visibleNotes(data,"",null,true,false,"").map {it.id}.toSet())
        assertEquals(listOf("b"),visibleNotes(data,"","f",false,false,"").map {it.id})
        assertEquals(listOf("a"),visibleNotes(data,"",null,false,false,"").map {it.id})
    }
    @Test fun unknownCreationLastInBothDirectionsAndStableTies(){
        val notes=listOf(Note(id="z",title="Same",created=null),Note(id="b",title="Same",created=10),Note(id="a",title="Same",created=10))
        listOf(true,false).forEach {assertEquals(listOf("a","b","z"),sortedNotes(notes,NoteSort("created",it)).map {n->n.id})}
        assertEquals(listOf("a","b","z"),sortedNotes(notes,NoteSort("name",false)).map {it.id})
    }
    @Test fun folderCountExcludesTrashAndSettingsStayLocal(){
        val data=SharedData(noteFolders=listOf(NoteFolder(id="a",name="A"),NoteFolder(id="b",name="B")),notes=listOf(Note(title="One",folderId="b"),Note(title="Trash",folderId="a",deleted=1)))
        assertEquals(listOf("b","a"),sortedNoteFolders(data,NoteSort("count",true)).map {it.id})
        val display=NoteDisplay(noteSorts=mapOf("all" to NoteSort("name",false),"b" to NoteSort("created",true)))
        assertEquals(display,codec.decodeFromString<NoteDisplay>(codec.encodeToString(NoteDisplay.serializer(),display)))
        assertEquals(NoteSort(),codec.decodeFromString<NoteDisplay>("{}").noteSorts["all"] ?: NoteSort())
    }
    @Test fun navigationHintsInAllLanguages(){
        languageNames.keys.forEach {language->listOf("Все заметки","По папкам","К папкам","Создать папку","Сортировка папок","По количеству заметок","Сетка").forEach {label->assertTrue(noteNavigationText(label,language).isNotBlank());if(language !in setOf("ru","be"))assertNotEquals(label,noteNavigationText(label,language))}}
    }
    @Test fun drawingPreviewKeepsTheSquareSheetInCardsAndGridCells() {
        listOf(600f to 120f,150f to 120f,90f to 120f,120f to 120f).forEach {(w,h)->
            val sheet=com.fiteasy.shared.noteSheetRect(w,h)
            kotlin.test.assertEquals(sheet.width,sheet.height);kotlin.test.assertEquals(minOf(w,h),sheet.width)
            kotlin.test.assertEquals(w/2,sheet.center.x,.001f);kotlin.test.assertEquals(h/2,sheet.center.y,.001f)
            kotlin.test.assertTrue(sheet.left>=0f && sheet.top>=0f && sheet.right<=w && sheet.bottom<=h)
        }
    }
}
