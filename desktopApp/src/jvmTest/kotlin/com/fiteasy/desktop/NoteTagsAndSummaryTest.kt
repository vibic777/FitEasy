package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

class NoteTagsAndSummaryTest {
    private fun note(title:String,vararg tags:String,deleted:Long?=null)=Note(title=title,tags=tags.toList(),deleted=deleted,updated=1000)
    @Test fun tagNamesAreCleanedUniqueAndLimited() {
        assertEquals("Work trip",cleanTag("  #Work   trip "))
        assertTrue(validTag("Дом"));assertFalse(validTag(""));assertFalse(validTag("x".repeat(33)));assertFalse(validTag(" padded"))
        assertEquals(listOf("Дом"),withTag(emptyList(),"#дом",listOf("Дом")));assertEquals(listOf("Дом"),withTag(listOf("Дом"),"ДОМ",listOf("Дом")))
        assertNull(withTag(emptyList(),"   ",emptyList()))
        validateNote(note("a","Дом","Work"));assertFails {validateNote(note("a","Дом","дом"))};assertFails {validateNote(note("a","#bad"))}
    }
    @Test fun createRenameDeleteAndCounts() {
        var s=Snapshot(shared=SharedData(notes=listOf(note("one","Дом"),note("two","Дом","Спорт"),note("gone","Спорт",deleted=5))))
        assertEquals(mapOf("Дом" to 2,"Спорт" to 1),noteTagCounts(s))
        s=createTag(s,"#Идеи");assertEquals(0,noteTagCounts(s)["Идеи"]);assertFails {createTag(s,"идеи")};assertFails {createTag(s,"дом")}
        validateLocal(s.local)
        s=renameTag(s,"Спорт","Тренировки",now=2000)
        assertEquals(setOf("Дом","Идеи","Тренировки"),noteTagCounts(s).keys)
        assertEquals(listOf(1000L,2000L,2000L),s.shared.notes.map {it.updated});assertFails {renameTag(s,"Дом","тренировки")}
        s=renameTag(s,"Идеи","Планы");assertEquals(listOf("Планы"),s.local.noteTags)
        s=deleteTag(s,"дом",now=3000)
        assertTrue(s.shared.notes.none {note->note.tags.any {it=="Дом"}});assertEquals(3,s.shared.notes.size);assertEquals(setOf("Планы","Тренировки"),noteTagCounts(s).keys)
        validate(s.shared);validateLocal(s.local)
    }
    @Test fun tagsFilterSearchSurviveStorageAndOldNotesOpen() {
        val data=SharedData(notes=listOf(note("one","Дом"),note("two","Спорт")))
        assertEquals(listOf("one"),visibleNotes(data,"",null,true,false,"","дом").map {it.title})
        assertEquals(listOf("two"),visibleNotes(data,"",null,true,false,"#спорт").map {it.title})
        assertEquals(2,visibleNotes(data,"",null,true,false,"").size)
        assertEquals(data,codec.decodeFromString<SharedData>(codec.encodeToString(data)))
        assertEquals(emptyList(),codec.decodeFromString<Note>("""{"id":"n","title":"old"}""").tags)
    }
    @Test fun statusSummaryCountsTodayNearestAndWaiting() {
        val zone=ZoneId.of("UTC");val now=Instant.parse("2026-10-04T10:00:00Z")
        val morning=Reminder(name="a",time="08:00",startDate="2026-10-01");val evening=Reminder(name="b",time="18:30",startDate="2026-10-01");val off=Reminder(name="c",time="12:00",enabled=false,startDate="2026-10-01")
        assertNull(reminderSummary(listOf(off),emptyMap(),now,zone))
        val summary=reminderSummary(listOf(morning,evening,off),mapOf(morning.id to ReminderDelivery(status="awaiting",nextAt=now.plusSeconds(600).toEpochMilli())),now,zone)!!
        assertEquals(2,summary.enabled);assertEquals(1,summary.today);assertEquals(1,summary.waiting);assertEquals(now.plusSeconds(600),summary.next)
        languageNames.keys.forEach {language->val lines=reminderSummaryLines(summary,language,zone);assertEquals(4,lines.size);assertTrue(lines.all {it.isNotBlank() && "{0}" !in it},language);assertTrue(lines[2].endsWith("04.10 10:10"),lines[2])}
        assertEquals(3,reminderSummaryLines(summary.copy(waiting=0),"ru",zone).size)
    }
    @Test fun scheduleTasksAreCountedAndSwitchesAreDeviceLocal() {
        val zone=ZoneId.of("UTC");val now=Instant.parse("2026-10-04T10:00:00Z")
        val workout=Reminder(name="workout",time="19:00",startDate="2026-10-01",scheduleId="s",rowId="r");val plain=Reminder(name="water",time="12:00",startDate="2026-10-01")
        val summary=reminderSummary(listOf(workout,plain),emptyMap(),now,zone)!!
        assertEquals(2,summary.today);assertEquals(1,summary.tasks)
        languageNames.keys.forEach {language->assertEquals(4,reminderSummaryLines(summary,language,zone).size,language)}
        assertEquals(15_000L,stepNotificationInterval(false));assertEquals(60_000L,stepNotificationInterval(true))
        assertTrue(STEP_BATCH_ECONOMY_MICROS>STEP_BATCH_MICROS && SOFTWARE_IDLE_ECONOMY_MILLIS<SOFTWARE_IDLE_MILLIS)
        val root=java.nio.file.Files.createTempDirectory("fiteasy-switches-").toFile();val previous=System.getProperty("fiteasy.home");System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {
            assertTrue(statusNotificationEnabled(platform));assertFalse(alarmScreenEnabled(platform));assertFalse(economyEnabled(platform))
            platform.writeSecret("status-notification","false");platform.writeSecret("economy","true");platform.notificationSettingsChanged()
            assertFalse(statusNotificationEnabled(platform));assertTrue(economyEnabled(platform))
            assertEquals(SqlStateStore.newInstallation,platform.load(),"Switches never enter the database")
        }finally {platform.close();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun addedStringsExistInEveryLanguage() {
        assertEquals(languageNames.keys-"ru",extraRows2.keys)
        extraRows2.forEach {(language,row)->assertEquals(extraKeys2.size,row.split('|').size,language)}
        extraKeys2.forEach {key->assertEquals(key,translate(key,"ru"));languageNames.keys.filter {it !in setOf("ru","be")}.forEach {assertNotEquals(key,translate(key,it),"$it/$key")}}
    }
}

class InterfaceTranslationTest {
    /** Every text written straight into the interface must exist in all ten languages; a missing language may only fall back to English. */
    @Test fun noScreenTextIsLeftInRussianOrEnglishOnly() {
        val pattern=Regex("""Text\("([^"$]+)"""")
        val texts=java.io.File("../shared/src").walkTopDown().filter {it.extension=="kt"}.flatMap {f->pattern.findAll(f.readText()).map {it.groupValues[1]}}.toSortedSet()
        assertTrue(texts.size>300)
        val cyrillic=Regex("[А-Яа-яЁё]");val englishWord=Regex("[A-Za-z]{4,}");val untranslatable=setOf("FitEasy","RGB · RRGGBB","YYYY-MM")
        // Typed confirmation words stay as they must be typed.
        fun foreign(text:String)=text.replace("ЗАМЕНИТЬ","").replace("УДАЛИТЬ","")
        languageNames.keys.filter {it !in setOf("ru","be")}.forEach {language->
            val left=texts.filter {cyrillic.containsMatchIn(foreign(translate(it,language)))}
            assertTrue(left.isEmpty(),"$language shows Russian: $left")
        }
        val english=texts.filter {it !in untranslatable && !cyrillic.containsMatchIn(it) && englishWord.containsMatchIn(it) && translate(it,"ru")==it}
        assertTrue(english.isEmpty(),"Russian shows English: $english")
        interfaceRows.forEach {(key,row)->assertEquals(9,row.size,key);assertTrue(row.all {it.isNotBlank()},key);assertEquals(key,translate(key,"ru"))}
    }
}

class FoodNameTest {
    private val foods by lazy {listOf("products.json","dishes.json").flatMap {name->codec.decodeFromString<List<Food>>(java.io.File("../shared/resources/$name").readText())}}
    @Test fun wholeNamesAreRussianOrLeftInEnglishNeverMixed() {
        val cyrillic=Regex("[А-Яа-яЁё]");val latin=Regex("[A-Za-z]{3,}")
        val broccoli=Food(id="usda:1",name="Broccoli, cooked, no added fat",kind="dish",source="USDA FoodData Central")
        assertEquals("Брокколи, приготовлено, без добавления жира",foodDisplayName(broccoli,"ru"))
        assertEquals(broccoli.name,foodDisplayName(broccoli,"en"));assertEquals(broccoli.name,foodDisplayName(broccoli,"sv"))
        assertEquals("Broccoli, unknown part",foodDisplayName(broccoli.copy(name="Broccoli, unknown part"),"ru"))
        assertEquals("Broccoli, cooked",foodDisplayName(broccoli.copy(name="Broccoli, cooked",source="user"),"ru"),"A person's own entry is never renamed")
        val covered=foodNameCoverage(foods,"ru")
        java.io.File("build/review/food-names-ru.txt").also {it.parentFile.mkdirs()}.writeText("$covered of ${foods.size}\n"+foods.filter {foodDisplayName(it,"ru")!=it.name}.take(60).joinToString("\n"){it.name+" -> "+foodDisplayName(it,"ru")})
        assertTrue(covered>2000,"Russian names: $covered of ${foods.size}")
    }
}
