package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.sync.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class ProgressAndFoldersTest {
    @Test fun oldDemoHashesStillMatchUneditedNotes() {
        val note=Note(title="Legacy",updated=1)
        val fields=codec.parseToJsonElement(codec.encodeToString(note)).jsonObject.filterKeys {it !in setOf("created","folderId")}
        val hash=java.security.MessageDigest.getInstance("SHA-256").digest(JsonObject(fields).toString().toByteArray()).joinToString("") {"%02x".format(it.toInt() and 255)}
        val snapshot=Snapshot(shared=SharedData(notes=listOf(note)),local=LocalData(demo=mapOf("note:${note.id}" to hash)))
        assertNotNull(removeDemonstration(snapshot).shared.notes.single().deleted)
        assertNull(removeDemonstration(snapshot.copy(shared=snapshot.shared.copy(notes=listOf(note.copy(text="Edited"))))).shared.notes.single().deleted)
    }
    @Test fun periodsIncludeLeapDaysAndBothReferenceDirections() {
        val anchor=LocalDate.of(2024,2,29)
        fun range(id:String,forward:Boolean=false,years:Int=3)=progressRange(id,anchor,forward,years,"2024-02-01","2024-02-29",emptyList())
        assertEquals(ProgressRange(anchor,anchor),range("day"))
        assertEquals(7,ChronoUnit.DAYS.between(range("week").start,range("week").end)+1)
        assertEquals(LocalDate.of(2024,3,6),range("week",true).end)
        assertEquals(LocalDate.of(2024,1,30),range("month").start)
        assertEquals(LocalDate.of(2023,3,1),range("year").start)
        assertEquals(LocalDate.of(2027,2,27),range("years",true).end)
        assertEquals(29,ChronoUnit.DAYS.between(range("range").start,range("range").end)+1)
        assertFailsWith<IllegalArgumentException> {ProgressRange(anchor,anchor.minusDays(1))}
        assertFailsWith<IllegalArgumentException> {ProgressRange(anchor,anchor.plusYears(101))}
    }
    @Test fun bucketCoverageIsCompleteAndBoundedForOneHundredYears() {
        listOf(1,7,120,121,366,3653,36525).forEach {days->
            val range=ProgressRange(LocalDate.of(1900,1,1),LocalDate.of(1900,1,1).plusDays(days-1L))
            val buckets=progressBuckets(range)
            assertEquals(range.start,buckets.first().start);assertEquals(range.end,buckets.last().end)
            assertTrue(buckets.size<=122)
            buckets.zipWithNext().forEach {(a,b)->assertEquals(a.end.plusDays(1),b.start)}
        }
    }
    @Test fun aggregatesKeepMissingValuesAndDailyStepSourceRules() {
        val range=ProgressRange(LocalDate.of(2024,1,1),LocalDate.of(2024,12,31))
        val buckets=progressBuckets(range)
        val rows=listOf(
            Entry(date="2024-01-01",type="steps",name="A",amount=100.0,unit="steps"),
            Entry(date="2024-01-02",type="steps",name="B",amount=200.0,unit="steps"),
            Entry(date="2024-01-01",type="measurement",name="Вес",amount=70.0,unit="kg"),
            Entry(date="2024-01-02",type="measurement",name="Вес",amount=69.0,unit="kg"),
            Entry(date="2024-01-02",type="drink",name="Water",amount=500.0,unit="ml",plainWater=true))
        val appearance=Appearance()
        assertEquals(300.0,progressSeries("Шаги",buckets,rows,appearance,"manual","test").first())
        assertEquals(69.0,progressSeries("Вес",buckets,rows,appearance,"manual","test").first())
        assertEquals(.5,progressSeries("Вода",buckets,rows,appearance,"manual","test").first())
        assertNull(progressSeries("Вес",buckets,rows,appearance,"manual","test")[1])
        assertNull(progressSeries("Калории",buckets,rows,appearance,"manual","test").first())
    }
    @Test fun calendarMarkersIgnoreTrashAndCountTypes() {
        val rows=listOf(Entry(date="2024-01-01",type="food",name="Apple",amount=1.0,unit="g"),Entry(date="2024-01-01",type="steps",name="Steps",amount=0.0,unit="steps"),Entry(date="2024-01-02",type="food",name="Trash",amount=1.0,unit="g",deleted=1))
        assertEquals(mapOf("food" to 1,"steps" to 1),calendarDataIndex(rows)["2024-01-01"])
        assertNull(calendarDataIndex(rows)["2024-01-02"])
        assertEquals(ProgressRange(LocalDate.of(2024,1,1),LocalDate.of(2024,1,1)),progressRange("all",LocalDate.now(),false,1,"","",rows))
    }
    @Test fun foldersMoveFilterDeleteAndCopiesPreserveNotes() {
        val folder=NoteFolder(name="Folder",created=1,updated=1)
        val note=Note(title="Title",text="Body",created=2,updated=2)
        val data=SharedData(noteFolders=listOf(folder),notes=listOf(note))
        val moved=moveNote(data,note.id,folder.id,3)
        assertEquals(2,moved.notes.single().created);assertEquals(3,moved.notes.single().updated)
        assertEquals(listOf(moved.notes.single()),visibleNotes(moved,"text",folder.id,false,false,"Body"))
        assertTrue(visibleNotes(moved,"drawing",null,true,false,"").isEmpty())
        assertTrue(visibleNotes(moved,"",null,false,false,"").isEmpty())
        val detached=removeNoteFolder(moved,folder.id,4)
        assertNull(detached.notes.single().folderId);assertNull(detached.notes.single().deleted)
        assertEquals(4,detached.noteFolders.single().deleted)
        val copies=mergeData(SharedData(),moved,ImportMode.COPIES)
        assertNotEquals(folder.id,copies.noteFolders.single().id)
        assertEquals(copies.noteFolders.single().id,copies.notes.single().folderId)
        assertNotEquals(note.id,copies.notes.single().id)
        assertEquals(moved,codec.decodeFromString<SharedData>(codec.encodeToString(moved)))
        assertTrue(resetData(Snapshot(shared=moved),setOf("notes")).shared.noteFolders.isEmpty())
        validate(copies)
    }
    @Test fun protocolAndSelectionKeepFolderContractAndRebaseLocalEdits() {
        val folder=NoteFolder(name="Folder")
        val data=SharedData(noteFolders=listOf(folder),notes=listOf(Note(title="Title",folderId=folder.id)))
        assertEquals(listOf(folder),selected(data,setOf("notes")).data.noteFolders)
        assertTrue(selected(data,setOf("water")).data.noteFolders.isEmpty())
        assertEquals(data,applyTransfer(SharedData(),selected(data,setOf("notes")),ImportMode.MERGE,ConflictPolicy.INCOMING))
        assertEquals(7,SYNC_PROTOCOL);assertEquals(5,Backup(data=data).version)
        assertFailsWith<IllegalArgumentException> {applyTransfer(SharedData(),selected(data,setOf("notes")).copy(version=2),ImportMode.MERGE,ConflictPolicy.LOCAL)}
        val changed=data.copy(noteFolders=listOf(folder.copy(name="Changed")))
        assertEquals(changed.noteFolders,preserveConcurrent(data,changed,data).first.noteFolders)
        val legacy=codec.decodeFromString<Note>("""{"title":"Old","updated":1}""")
        assertNull(legacy.created);assertNull(legacy.folderId)
    }
    @Test fun newTranslationsHaveAllKeys() {
        assertEquals(languageNames.keys-setOf("ru"),periodNoteRows.keys)
        periodNoteRows.forEach {(language,row)->assertEquals(periodNoteKeys.size,row.split('|').size,language);periodNoteKeys.forEach {assertFalse(periodNoteTranslation(it,language).isNullOrBlank())}}
    }
}
