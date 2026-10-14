package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.sync.syncSections
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class DataResetTest {
    @Test fun newSettingsTranslationsHaveAllKeysForEveryLanguage() {
        assertEquals(languageNames.keys-setOf("ru"),settingsRows.keys)
        settingsRows.forEach {(language,row)->
            assertEquals(settingsKeys.size,row.split('|').size,language)
            settingsKeys.forEach {key->assertFalse(settingsTranslation(key,language).isNullOrBlank(),"$language / $key")}
        }
    }
    @Test fun bothDemoPeriodsProtectUserRecordsAndForceRemovesEdits() {
        for(days in listOf(7,30)) {
            val user=Entry(type="food",name="Демо · Вода",amount=1.0,unit="g")
            val demo=createDemonstration(Snapshot(shared=SharedData(entries=listOf(user))),days=days)
            assertEquals(days*5+1,demo.shared.entries.size)
            assertEquals(days,demo.shared.entries.filter {it.id!=user.id}.map {it.date}.distinct().size)
            val plan=demo.shared.schedules.single()
            val reminder=Reminder(name="Мой сигнал",scheduleId=plan.id,rowId=plan.rows.first().id)
            // Replace original reminder for that row: bindings are unique.
            val changed=demo.copy(shared=demo.shared.copy(notes=demo.shared.notes.map {it.copy(text="Мой текст")}),local=demo.local.copy(reminders=demo.local.reminders.drop(1)+reminder))
            val safe=removeDemonstration(changed)
            val clean=forceRemoveDemonstration(codec.decodeFromString(codec.encodeToString(safe)))
            assertEquals(listOf(user),clean.shared.entries)
            assertTrue(clean.shared.notes.isEmpty());assertTrue(clean.shared.schedules.isEmpty())
            assertEquals(reminder.copy(enabled=false,scheduleId=null,rowId=null),clean.local.reminders.single())
            assertTrue(clean.local.demo.isEmpty());validate(clean.shared);validateLocal(clean.local)
            assertEquals(clean,forceRemoveDemonstration(clean))
        }
        assertFailsWith<IllegalArgumentException> {createDemonstration(Snapshot(),days=1)}
    }
    @Test fun categoriesPreserveOtherDataAndWholeResetPreservesAppearance() {
        val demo=createDemonstration(Snapshot(),days=30)
        val waterOnly=resetData(demo,setOf("water"))
        assertTrue(waterOnly.shared.entries.none {it.type=="drink"})
        assertEquals(demo.shared.entries.filter {it.type!="drink"},waterOnly.shared.entries)
        assertEquals(demo.shared.notes,waterOnly.shared.notes)
        val clean=resetData(demo,resetCategories.keys,all=true)
        assertEquals(SharedData(),clean.shared)
        assertEquals(demo.local.appearance,clean.local.appearance)
        assertTrue(clean.local.reminders.isEmpty());assertTrue(clean.local.demo.isEmpty())
        assertTrue(syncSections.all {it in resetCategories})
        assertFailsWith<IllegalArgumentException> {resetData(demo,emptySet())}
        assertFailsWith<IllegalArgumentException> {resetData(demo,setOf("typo"))}
        validate(clean.shared);validateLocal(clean.local)
    }
    @Test fun catalogueResetPreservesRecipeDependenciesAndPlanResetDetachesReminders() {
        val food=Food(id=newId(),name="Мой продукт",kind="product")
        val other=food.copy(id=newId())
        val recipe=Recipe(name="Мой рецепт",ingredients=listOf(Ingredient(food.id,100.0)),steps="",yieldGrams=100.0,portions=1)
        val base=createDemonstration(Snapshot(shared=SharedData(products=listOf(food,other),recipes=listOf(recipe))))
        val clean=resetData(base,setOf("catalogues","schedules"))
        assertEquals(listOf(food),clean.shared.products)
        assertEquals(listOf(recipe),clean.shared.recipes)
        assertTrue(clean.local.reminders.all {!it.enabled && it.scheduleId==null && it.rowId==null})
        assertTrue(resetData(base,setOf("catalogues","recipes")).shared.products.isEmpty())
        validate(clean.shared);validateLocal(clean.local)
    }
    @Test fun chartSettingsRoundTripAndRejectUnknownKindsOrMetrics() {
        val local=LocalData(appearance=Appearance(charts=chartMetrics.keys.associateWith {ChartStyle(chartMetrics.getValue(it),"area")}))
        assertEquals(local,codec.decodeFromString<LocalData>(codec.encodeToString(local)))
        validateLocal(local)
        assertEquals(8,chartMetrics.values.distinct().size)
        assertFailsWith<IllegalArgumentException> {validateLocal(local.copy(appearance=local.appearance.copy(charts=mapOf("invalid" to ChartStyle()))))}
        assertFailsWith<IllegalArgumentException> {validateLocal(local.copy(appearance=local.appearance.copy(charts=mapOf(chartMetrics.keys.first() to ChartStyle(kind="invalid")))))}
    }
}
