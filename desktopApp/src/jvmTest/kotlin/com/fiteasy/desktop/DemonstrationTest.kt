package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlin.test.*
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class DemonstrationTest {
    @Test fun cleanupPreservesUserRecordsAndEditedSamples() {
        val user=Entry(type="activity",name="Моя прогулка",amount=12.0,unit="min")
        val base=Snapshot(shared=SharedData(entries=listOf(user)))
        val demo=createDemonstration(base,LocalDate.of(2026,10,2))
        validate(demo.shared);validateLocal(demo.local)
        assertTrue(demo.local.reminders.all {!it.enabled})
        assertFailsWith<IllegalArgumentException> {createDemonstration(demo)}
        val edited=demo.shared.notes.first().copy(text="Мой текст",updated=System.currentTimeMillis()+1)
        val changed=demo.copy(shared=demo.shared.copy(notes=demo.shared.notes.map {if(it.id==edited.id)edited else it}))
        val restored=codec.decodeFromString<Snapshot>(codec.encodeToString(changed))
        val clean=removeDemonstration(restored)
        assertEquals(user,clean.shared.entries.single {it.id==user.id})
        assertEquals(edited,clean.shared.notes.single {it.id==edited.id})
        assertTrue(clean.shared.entries.filter {it.id!=user.id}.all {it.deleted!=null})
        assertTrue(clean.shared.notes.filter {it.id!=edited.id}.all {it.deleted!=null})
        assertTrue(clean.local.reminders.isEmpty())
        assertEquals(demo.local.demo,clean.local.demo)
        assertEquals(clean,removeDemonstration(clean))
        assertEquals(base.shared.profile,clean.shared.profile)
        assertEquals(base.shared.goals,clean.shared.goals)
    }
    @Test fun editedReminderProtectsItsSchedule() {
        val demo=createDemonstration(Snapshot())
        val changed=demo.copy(local=demo.local.copy(reminders=demo.local.reminders.mapIndexed {i,r->if(i==0)r.copy(enabled=true,comment="Мой комментарий")else r}))
        val clean=removeDemonstration(changed)
        assertEquals(1,clean.local.reminders.size)
        assertNull(clean.shared.schedules.single().deleted)
        validate(clean.shared);validateLocal(clean.local)
    }
}
