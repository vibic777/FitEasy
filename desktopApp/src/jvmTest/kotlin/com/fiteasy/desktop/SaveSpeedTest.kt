package com.fiteasy.desktop

import com.fiteasy.shared.*
import java.time.LocalDate
import kotlin.test.*

/** How long one small change takes to save on a large diary (D102); the numbers go to build/review/save-speed.txt. */
class SaveSpeedTest {
    /** Writing only changed rows must leave the file equal to the snapshot after every kind of change. */
    @Test fun addingChangingMovingAndDeletingRowsIsReadBackExactly() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-rows-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        var platform=DesktopPlatform()
        fun reopened():Snapshot {val expected=platform.load();platform.close();platform=DesktopPlatform();val stored=platform.load();assertEquals(expected,stored);return stored}
        try {
            val entries=(0 until 40).map {Entry(type="food",name="E$it",amount=it+1.0,unit="g")}
            platform.save(platform.load().let {it.copy(shared=it.shared.copy(entries=entries,notes=listOf(Note(title="a",updated=1),Note(title="b",updated=1)),goalHistory=listOf(GoalRevision("2026-01-01",Goals()),GoalRevision("2026-02-01",Goals(kcal=1900.0)))),local=it.local.copy(reminders=listOf(Reminder(name="r1"),Reminder(name="r2",noteId="n-1",volumePercent=40))))})
            reopened()
            platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries.filterNot {it.name=="E3"}.map {if(it.name=="E7")it.copy(amount=700.0)else it}+Entry(type="drink",name="W",amount=1.0,unit="ml")))}
            val changed=reopened();assertEquals(40,changed.shared.entries.size);assertEquals(700.0,changed.shared.entries.first {it.name=="E7"}.amount);assertTrue(changed.shared.entries.none {it.name=="E3"})
            platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries.reversed(),notes=emptyList(),goalHistory=s.shared.goalHistory.drop(1)),local=s.local.copy(reminders=s.local.reminders.reversed(),trashDays=7))}
            val moved=reopened();assertEquals("W",moved.shared.entries.first().name);assertTrue(moved.shared.notes.isEmpty());assertEquals(1,moved.shared.goalHistory.size);assertEquals("r2",moved.local.reminders.first().name);assertEquals("n-1",moved.local.reminders.first().noteId);assertEquals(7,moved.local.trashDays)
            // Invalid data is still refused although unchanged lists are not checked again.
            assertFails {platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries+s.shared.entries.first()))}}
            assertFails {platform.mutateSnapshot {s->s.copy(local=s.local.copy(reminders=s.local.reminders+Reminder(name="bad",volumePercent=3)))}}
            assertEquals(moved,reopened())
        }finally {runCatching {platform.close()};if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun oneNewEntryOnALargeDiaryIsSavedQuickly() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-speed-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {
            val day=LocalDate.of(2026,1,1)
            val entries=(0 until 20_000).map {i->Entry(date=day.plusDays((i/20).toLong()%365).toString(),type="food",name="Entry $i",amount=100.0+i%50,unit="g",nutrients=Nutrients(kcal=120.0,protein=5.0,fat=3.0,carbs=18.0))}
            val notes=(0 until 300).map {Note(title="Note $it",text="x".repeat(2000),updated=1000)}
            platform.save(platform.load().let {it.copy(shared=it.shared.copy(entries=entries,notes=notes))})
            val times=(0 until 12).map {i->
                val started=System.nanoTime()
                platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries+Entry(type="drink",name="Water $i",amount=250.0,unit="ml",plainWater=true)))}
                (System.nanoTime()-started)/1_000_000.0
            }
            val toggles=(0 until 12).map {i->
                val started=System.nanoTime()
                platform.mutateSnapshot {s->s.copy(local=s.local.copy(appearance=s.local.appearance.copy(animations=i%2==0)))}
                (System.nanoTime()-started)/1_000_000.0
            }
            val median=times.sorted()[times.size/2];val setting=toggles.sorted()[toggles.size/2]
            java.io.File("build/review/save-speed.txt").also {it.parentFile.mkdirs()}.writeText("20000 entries, 300 notes\nnew entry, median ms: %.1f (all: %s)\nsetting switch, median ms: %.1f\n".format(median,times.joinToString {"%.0f".format(it)},setting))
            assertEquals(20_012,platform.load().shared.entries.size)
            // Read back from the file itself: nothing was lost by writing only what changed.
            platform.close()
            val reopened=DesktopPlatform()
            try {val stored=reopened.load();assertEquals(20_012,stored.shared.entries.size);assertEquals(300,stored.shared.notes.size);assertEquals("Water 11",stored.shared.entries.last().name);assertFalse(stored.local.appearance.animations)}finally {reopened.close()}
            assertTrue(median<60,"One new entry took $median ms")
        }finally {runCatching {platform.close()};if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
}
