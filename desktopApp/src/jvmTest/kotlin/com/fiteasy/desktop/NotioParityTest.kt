package com.fiteasy.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.*
import com.fiteasy.shared.lock.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*

/** 0.4.20 (D097–D101): reminder melody, auto-lock, the reworked pages, and texts that used to stay Russian. */
@OptIn(ExperimentalComposeUiApi::class)
class NotioParityTest {
    private val cyrillic=Regex("[А-Яа-яЁё]")
    private val foreign=languageNames.keys.filter {it !in setOf("ru","be")}
    private fun <T> withPlatform(block:(DesktopPlatform)->T):T {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-parity-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {return block(platform)}finally {platform.close();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    private fun snapshot(scene:ImageComposeScene,name:String) {
        try {scene.render().close();val image=scene.render();java.io.File("build/review/$name.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
    }

    @Test fun melodyVolumeAndFadeInAreValidatedAndOldRemindersKeepWorking() {
        val old=codec.decodeFromString<Reminder>("{\"name\":\"old\"}")
        assertNull(old.soundUri);assertEquals(100,old.volumePercent);assertEquals(0,old.rampSeconds);assertFalse(ownMelody(old))
        validateReminder(old.copy(soundUri="content://media/internal/audio/media/12",volumePercent=40,rampSeconds=60))
        assertFails {validateReminder(old.copy(volumePercent=5))};assertFails {validateReminder(old.copy(volumePercent=101))}
        assertFails {validateReminder(old.copy(rampSeconds=301))};assertFails {validateReminder(old.copy(soundUri=""))}
        assertTrue(ownMelody(old.copy(volumePercent=60)));assertTrue(ownMelody(old.copy(rampSeconds=10)));assertTrue(ownMelody(old.copy(soundUri="x")))
        assertFalse(ownMelody(old.copy(soundUri="x",sound=false)),"A silent reminder plays nothing")
        val kept=codec.decodeFromString<Reminder>(codec.encodeToString(old.copy(soundUri="x",volumePercent=70,rampSeconds=30)))
        assertEquals("x",kept.soundUri);assertEquals(70,kept.volumePercent);assertEquals(30,kept.rampSeconds)
    }
    @Test fun aReminderRemembersItsNoteAndMailErrorsAreExplained() {
        val linked=Reminder(name="a",noteId="note-1");validateReminder(linked)
        assertEquals("note-1",codec.decodeFromString<Reminder>(codec.encodeToString(linked)).noteId);assertNull(codec.decodeFromString<Reminder>("{\"name\":\"old\"}").noteId)
        assertFails {validateReminder(linked.copy(noteId=""))}
        val texts=MailError.entries.map {mailResultText(MailResult.Failed(it))};assertEquals(MailError.entries.size,texts.distinct().size)
        foreign.forEach {language->(texts+mailResultText(MailResult.Sent)).forEach {assertFalse(cyrillic.containsMatchIn(translate(it,language)),"$language/$it")}}
        assertEquals("Silence the reminder",trayLabel("silence","en"));languageNames.keys.forEach {assertTrue(trayLabel("silence",it).isNotBlank())}
    }
    @Test fun securityAndBackupPagesAfterNotioRenderAndKeepTheBackupPassword() {
        withPlatform {platform->
            assertEquals(BackupPassword(false,""),backupPassword(platform))
            saveBackupPassword(platform,BackupPassword(true,"secret-1"));assertEquals(BackupPassword(true,"secret-1"),backupPassword(platform))
            // A copy made with the saved password opens with it and asks for it otherwise.
            val bytes=prepareBackup(SharedData(notes=listOf(Note(title="n",updated=1))),emptyList(),null,"secret-1")
            assertTrue(BackupCrypto.isProtected(bytes));assertFalse(BackupCrypto.isProtected(prepareBackup(SharedData(),emptyList(),null,"")))
            assertEquals("n",readBackup(bytes,"secret-1").data.notes.single().title);assertFails {readBackup(bytes,"")};assertFails {readBackup(bytes,"wrong")}
            assertEquals("generic",reminderPrivacy(platform))
            val r=Reminder(name="Вода",comment="стакан")
            assertEquals("Вода" to "стакан",reminderShown(r,"full","ru"));assertEquals("Вода" to "",reminderShown(r,"title","ru"))
            languageNames.keys.forEach {language->val (title,text)=reminderShown(r,"generic",language);assertTrue(title.isNotBlank() && text.isNotBlank() && "Вода" !in title+text,language)}
            listOf("ru","en").forEach {language->
                snapshot(ImageComposeScene(420,560,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){Column(Modifier.padding(12.dp)){SecurityPage(platform,onLockSettings={},onLock={})}}}}},"security-page-$language")
                snapshot(ImageComposeScene(420,1500,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){Column(Modifier.padding(12.dp)){
                    DataPage(platform,emptyList(),demo=false,mutate={true},onAppearance={},onDatabaseTools={},onReport={},onTrash={},onDemoCreate={},onDemoRemove={},onReset={})
                }}}}},"backup-page-$language")
            }
            // Every Notio text taken exists in all ten languages and names FitEasy, not Notio.
            assertTrue(notioKeys.size>300);notioKeys.values.forEach {key->assertEquals(9,notioRows.getValue(key).size,key);assertFalse("Notio" in key+notioRows.getValue(key).joinToString(),key)}
            foreign.forEach {language->listOf("sync_wait","settings_lock","settings_cat_data","import_mode_replace_hint","db_encryption_busy_message").forEach {assertFalse(cyrillic.containsMatchIn(translate(nt(it),language)),"$language/$it")}}
        }
    }
    @Test fun theTrashRestoresPurgesAndKeepsThingsOnlyAsLongAsChosen() {
        val day=86_400_000L;val now=100*day
        val data=SharedData(
            entries=listOf(Entry(id="e1",type="food",name="Soup",amount=1.0,unit="g",deleted=now-10*day),Entry(id="e2",type="drink",name="Tea",amount=1.0,unit="ml")),
            notes=listOf(Note(id="n1",title="Old note",updated=1,deleted=now-40*day),Note(id="n2",title="Kept",updated=1)),
            schedules=listOf(DaySchedule(id="s1",name="Plan",deleted=now-2*day)),products=listOf(Food("p1","Own product","product",deleted=now-day)))
        val items=trashItems(data);assertEquals(listOf("food:p1","schedule:s1","entry:e1","note:n1"),items.map {it.key},"Newest first")
        val restored=items.first {it.key=="note:n1"}.restore(data,now);assertNull(restored.notes.first {it.id=="n1"}.deleted);assertEquals(now,restored.notes.first {it.id=="n1"}.updated)
        val purged=items.first {it.key=="entry:e1"}.purge(data);assertEquals(listOf("e2"),purged.entries.map {it.id});validate(purged)
        assertTrue(trashItems(purgeTrash(data)).isEmpty());assertEquals(listOf("e2"),purgeTrash(data).entries.map {it.id});assertEquals(listOf("n2"),purgeTrash(data).notes.map {it.id})
        // Never by default: a device where nobody chose a period loses nothing by itself.
        assertEquals(0,LocalData().trashRetentionDays);assertEquals(0,codec.decodeFromString<LocalData>("{\"trashDays\":30}").trashRetentionDays)
        val snapshot=Snapshot(shared=data);assertSame(snapshot,purgeExpiredTrash(snapshot,now))
        val month=purgeExpiredTrash(snapshot.copy(local=LocalData(trashRetentionDays=30)),now);assertEquals(listOf("food:p1","schedule:s1","entry:e1"),trashItems(month.shared).map {it.key})
        assertFails {validateLocal(LocalData(trashRetentionDays=5))};trashRetentionOptions.forEach {validateLocal(LocalData(trashRetentionDays=it))}
        withPlatform {platform->
            platform.save(platform.load().copy(shared=data))
            listOf("ru","en").forEach {language->
                snapshot(ImageComposeScene(420,560,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {TrashPage(platform.load(),{true},{})}}},"trash-page-$language")
                snapshot(ImageComposeScene(420,900,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){Column(Modifier.padding(12.dp)){
                    TrashSettings(platform.load(),{true},{});AboutSettings("Windows",{},{},{})
                }}}}},"trash-about-settings-$language")
                snapshot(ImageComposeScene(420,700,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {HelpPage(language,{})}}},"help-page-$language")
            }
        }
        foreign.forEach {language->trashItems(data).forEach {assertFalse(cyrillic.containsMatchIn(translate(it.kind,language)),"$language/${it.kind}")}}
    }
    @Test fun gainClimbsFromAQuietStartToTheChosenVolume() {
        assertEquals(1f,ringGain(0,0,100));assertEquals(.5f,ringGain(0,0,50));assertEquals(.1f,ringGain(0,0,3),"Never below ten percent")
        assertEquals(RING_START_SHARE,ringGain(0,60,100),1e-6f);assertEquals(1f,ringGain(60_000,60,100),1e-6f);assertEquals(1f,ringGain(600_000,60,100),1e-6f)
        val steps=(0..60).map {ringGain(it*1000L,60,80)};assertEquals(steps.sorted(),steps);assertEquals(.8f,steps.last(),1e-6f)
        assertEquals(RING_START_SHARE,ringGain(-5,10,100),1e-6f)
    }
    @Test fun repeatKindFollowsTheStoredRule() {
        val r=Reminder(name="a",startDate="2026-10-05")
        assertEquals("daily",reminderRepeatKind(r));assertEquals("once",reminderRepeatKind(r.copy(endDate="2026-10-05")))
        assertEquals("weekdays",reminderRepeatKind(r.copy(weekdays=listOf(1,3))));assertEquals("monthly",reminderRepeatKind(r.copy(monthDays=listOf(5,20))))
        languageNames.keys.forEach {language->
            val flags=reminderFlags(r.copy(requiresConfirmation=true,repeatUntilConfirmed=true,repeatMinutes=7,weekdays=listOf(1,3)),language)
            assertTrue("7" in flags && flags.split(" · ").size==6,"$language: $flags")
            if(language in foreign)assertFalse(cyrillic.containsMatchIn(flags),"$language: $flags")
        }
    }
    @Test fun autoLockWaitsForTheChosenTime() {
        assertTrue(lockAfterBackground(0,0));assertTrue(lockAfterBackground(0,10_000))
        assertFalse(lockAfterBackground(60,59_999));assertTrue(lockAfterBackground(60,60_000))
        assertFalse(lockAfterBackground(LOCK_NEVER,86_400_000L*30));assertTrue(lockAfterBackground(300,-1),"A clock moved back locks")
        assertTrue(lockTimeouts.all(::validLockTimeout));assertTrue(validLockTimeout(LOCK_MAX_SECONDS));assertFalse(validLockTimeout(LOCK_MAX_SECONDS+1));assertFalse(validLockTimeout(-2))
        assertEquals(0,codec.decodeFromString<LockSettings>("{\"enabled\":true}").timeoutSeconds,"Settings saved before 0.4.20 keep locking at once")
        withPlatform {platform->
            val lock=DeviceLock(platform);lock.configure(lock.password("4821",kind="pin").copy(timeoutSeconds=300))
            assertEquals(300,DeviceLock(platform).settings.timeoutSeconds)
        }
        languageNames.keys.forEach {language->
            val names=(lockTimeouts+listOf(95)).map {lockTimeoutLabel(it,language)};assertEquals(names.size,names.distinct().size,language)
            securityQuestions.forEach {q->val shown=translate(q,language);assertTrue(shown.endsWith("?") || shown.endsWith("？"),"$language/$shown")}
            if(language in foreign)(names+securityQuestions.map {translate(it,language)}).forEach {assertFalse(cyrillic.containsMatchIn(it),"$language/$it")}
        }
    }
    /** D113: reminders are off after installation; an installation that already has data keeps what it had. */
    @Test fun aNewInstallationStartsWithRemindersOffAndAnOldOneIsNotTouched() {
        withPlatform {platform->
            val fresh=platform.load();assertTrue(fresh.local.remindersOff,"Off after installation");assertFalse(alarmScreenEnabled(platform),"The alarm screen is off too")
            platform.save(fresh.copy(local=fresh.local.copy(reminders=listOf(Reminder(name="Вода")))));assertTrue(effectiveReminders(platform.load()).none {it.enabled},"Nothing goes off until the person turns reminders on")
            platform.mutateSnapshot {it.copy(local=it.local.copy(remindersOff=false))};assertTrue(effectiveReminders(platform.load()).single().enabled)
        }
        // Data written before the switch existed have no such field: their reminders keep working after an update.
        assertFalse(codec.decodeFromString<Snapshot>("""{"local":{"reminders":[{"name":"old"}]}}""").local.remindersOff);assertFalse(LocalData().remindersOff)
        // The demonstration still knows its own reminders made before 0.4.29, which had no time of change.
        val demo=createDemonstration(Snapshot());val reminder=demo.local.reminders.first()
        val before=codec.encodeToString(Reminder.serializer(),reminder).also {assertTrue(""","updated":0}""" in it)}.replace(""","updated":0}""","}")
        assertEquals(java.security.MessageDigest.getInstance("SHA-256").digest(before.toByteArray()).joinToString("") {"%02x".format(it.toInt() and 255)},demo.local.demo.getValue("reminder:${reminder.id}"))
        assertTrue(removeDemonstration(demo).local.reminders.isEmpty())
    }
    /** D114, D116: the top panel of the main sections carries the reminders and synchronization icons beside the profile. */
    @Test fun theTopPanelRendersWithTheSyncIcon() {
        withPlatform {platform->
            platform.mutateSnapshot(::acknowledgeLegalNotice)
            val scene=ImageComposeScene(900,420,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {Surface(Modifier.fillMaxSize()){FitEasyContent(platform)}}}}
            try {
                // The content loads off the UI thread: frames are drawn until it is there.
                repeat(40){scene.render(it*100_000_000L).close();Thread.sleep(50)}
                val image=scene.render(4_100_000_000L);java.io.File("build/review/top-panel-ru.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()
            }finally {scene.close()}
        }
    }
    /** D115: less work while a reminder goes off, and timers that survive a sleeping computer. */
    @Test fun aMelodyIsWatchedOnlyWhileItsVolumeClimbsAndSealedTextsAreOpenedOnce() {
        // No fade-in: one wake-up, at the end of the minute. With a fade-in: every quarter of a second until it ends, then once.
        assertEquals(RING_MILLIS,ringTickDelay(0,0));assertEquals(250,ringTickDelay(0,30));assertEquals(250,ringTickDelay(29_900,30))
        assertEquals(RING_MILLIS-30_000,ringTickDelay(30_000,30));assertEquals(1,ringTickDelay(RING_MILLIS+5,0));assertEquals(100,ringTickDelay(RING_MILLIS-100,120))
        var steps=0;var at=0L;while(at<RING_MILLIS){at+=ringTickDelay(at,10);steps++};assertEquals(41,steps,"Forty steps of the fade-in and the end")
        // A sealed text is opened once, however often it is read; a new sealed text is opened again; the oldest are forgotten.
        var opens=0;val texts=OpenedTexts(limit=2)
        repeat(5){assertEquals("plain-a",texts.get("a"){opens++;"plain-$it"})};assertEquals(1,opens)
        texts.put("b","written");assertEquals("written",texts.get("b"){opens++;"x"});assertEquals(1,opens)
        texts.get("c"){opens++;"plain-c"};texts.get("a"){opens++;"again"};assertEquals(3,opens,"The oldest was forgotten and is opened again")
        texts.clear();texts.get("c"){opens++;"plain-c"};assertEquals(4,opens)
        // The summary works out the next time of each reminder once and counts the same as before.
        val zone=java.time.ZoneId.of("UTC");val now=java.time.Instant.parse("2026-10-07T08:00:00Z")
        val list=listOf(Reminder(id="a",name="A",time="09:00",startDate="2026-10-01"),Reminder(id="b",name="B",time="07:00",startDate="2026-10-01",scheduleId="s",rowId="r"),Reminder(id="c",name="C",time="10:00",startDate="2026-10-01",scheduleId="s",rowId="r2"),Reminder(id="off",name="Off",enabled=false))
        val summary=reminderSummary(list,mapOf("a" to ReminderDelivery(status="awaiting")),now,zone)!!
        assertEquals(3,summary.enabled);assertEquals(2,summary.today);assertEquals(1,summary.tasks);assertEquals(1,summary.waiting);assertEquals(java.time.Instant.parse("2026-10-07T09:00:00Z"),summary.next)
    }
    @Test fun remindersAreSetAgainAfterTheComputerSleptAndAMissedOneIsShownOnce() {
        var saved=emptyMap<String,ReminderDelivery>()
        val reminders=DesktopReminders({saved},{saved=it})
        try {
            val start=System.currentTimeMillis()
            assertFalse(reminders.beat(start+60_000),"A minute later is no jump");assertFalse(reminders.beat(start+130_000))
            // Due in a minute or two by the clock; the computer "sleeps" past it.
            val time=java.time.LocalTime.now().plusMinutes(2).withSecond(0).withNano(0)
            val soon=Reminder(id="soon",name="Soon",time=time.toString().take(5),sound=false,popup=false,startDate=java.time.LocalDate.now().minusDays(1).toString())
            val far=Reminder(id="far",name="Far",time=java.time.LocalTime.now().minusHours(2).toString().take(5),sound=false,popup=false,startDate=java.time.LocalDate.now().minusDays(1).toString())
            reminders.sync(listOf(soon,far))
            val dueAt=nextReminder(soon)!!.toEpochMilli()
            assertTrue(reminders.beat(dueAt+10*60_000),"Ten minutes past the time: the clock jumped")
            assertEquals(setOf("soon"),reminders.states().keys,"The one that fell due while asleep is shown once; the other waits for its time")
            assertFalse(reminders.beat(dueAt+11*60_000));assertEquals(1,reminders.states().getValue("soon").count)
            // A clock set back is a jump too; nothing is due, nothing is shown again.
            assertTrue(reminders.beat(dueAt-3600_000));assertEquals(1,reminders.states().getValue("soon").count)
            // Asleep for longer than half an hour past the time: it is not shown late.
            reminders.sync(emptyList());reminders.sync(listOf(soon.copy(id="late")))
            reminders.beat(System.currentTimeMillis());assertTrue(reminders.beat(nextReminder(soon)!!.toEpochMilli()+2*3600_000));assertTrue(reminders.states().isEmpty())
        }finally {reminders.close()}
    }
    /** D117: the help is a tree in every language, every icon in it has a name and a meaning, and a hint finds its place. */
    @Test fun theHelpIsATreeWithIconsInEveryLanguageAndAHintFindsItsPlace() {
        assertTrue(iconMeaningRows.keys==languageNames.keys && iconMeaningRows.values.all {it.size==61 && it.values.all {text->text.isNotBlank()}},"Every drawing is explained in every language")
        val ids=listOf("today","food","water","activity","progress","notes","reminders","sync","security","data","settings","about")
        languageNames.keys.forEach {language->
            val tree=helpTree(language);assertEquals(ids,tree.map {it.id},language)
            tree.forEach {c->
                assertTrue(c.title.isNotBlank() && c.sections.isNotEmpty(),"$language/${c.id}")
                c.sections.forEach {s->
                    assertTrue(s.title.isNotBlank() && s.body.length>10,"$language/${c.id}/${s.title}")
                    assertTrue(s.icons.all {it.label.isNotBlank() && it.about.isNotBlank()},"$language/${c.id}/${s.title}: an icon without a name or a meaning")
                    if(language in foreign)(listOf(c.title,s.title)+s.icons.flatMap {listOf(it.label,it.about)}).forEach {assertFalse(cyrillic.containsMatchIn(it),"$language/${c.id}: $it")}
                }
            }
            assertTrue(tree.sumOf {c->c.sections.sumOf {it.icons.size}}>90,language)
            // Every caption of the help is a caption a button really carries, so its hint leads here.
            tree.flatMap {c->c.sections.flatMap {it.icons}}.filterNot {it.filled}.forEach {icon->assertNotNull(findHelp(tree,icon.label),"$language: ${icon.label}")}
        }
        val tree=helpTree("ru")
        // A hint is found by its text; the category of the place the person is in comes first.
        val delete=findHelp(tree,"Удалить")!!;assertEquals("today",tree[delete.category].id)
        val bell=findHelp(tree,"напоминания","reminders") ?: findHelp(tree,"Напоминания")!!;assertEquals("Напоминания",bell.label)
        val postpone=findHelp(tree,"Отложить","reminders")!!;assertEquals("reminders",tree[postpone.category].id);assertEquals("Отложить",tree[postpone.category].sections[postpone.section].icons.first {it.label==postpone.label}.label)
        assertEquals("sync",tree[findHelp(tree,"Замещение")!!.category].id);assertNull(findHelp(tree,"Такой кнопки нет"))
        assertEquals("Напоминания и уведомления. Плюс на значке: добавить запись этого вида",iconMeaning("bell+","ru"));assertEquals("Search",iconMeaning("search","en"))
        assertEquals("food",helpCategoryFor("food",""));assertEquals("reminders",helpCategoryFor("profile","reminders"));assertEquals("data",helpCategoryFor("profile","trash"));assertEquals("settings",helpCategoryFor("profile","appearance"))
        assertTrue(searchHelp(tree,"  ").isEmpty());assertTrue(searchHelp(tree,"zzzz-нет").isEmpty())
        // The search in context (D118): every result carries the found words inside their sentence, exactly where the marks say.
        val hits=searchHelp(tree,"ЗАМЕЩЕНИЕ");assertTrue(hits.size>=3 && hits.any {tree[it.place.category].id=="sync"})
        hits.forEach {assertEquals("замещение",it.text.substring(it.from,it.to).lowercase(),it.text)}
        assertTrue(hits.any {it.place.label=="Замещение"},"An icon is found by its name");assertTrue(hits.any {it.place.label==null && it.text.startsWith("… ")},"A fragment of a long text starts with the sign of a cut")
        assertTrue(hits.all {it.text.length<260 && '\n' !in it.text});assertTrue(searchHelp(tree,"значок").size<=80)
        // Where the person came from is listed first.
        assertEquals("data",tree[searchHelp(tree,"удалить","data").first().place.category].id);assertEquals("today",tree[searchHelp(tree,"удалить","today").first().place.category].id)
        assertEquals(listOf(0..1,5..6),helpMatches("Ab cdAB","ab"));assertTrue(helpMatches("text","").isEmpty())
        languageNames.keys.forEach {language->val word=translate("Синхронизация",language);assertTrue(searchHelp(helpTree(language),word).isNotEmpty(),"$language: $word")}
        assertEquals(5000L,HINT_MILLIS)
        // The old flat list is still whole: nothing the help said before was dropped.
        assertTrue(helpArticles("ru").size>=20)
    }
    @Test fun theHelpPageAndAHintRender() {
        listOf("ru","en").forEach {language->
            snapshot(ImageComposeScene(900,700,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {HelpPage(language,{})}}},"help-overview-$language")
            snapshot(ImageComposeScene(900,900,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {HelpPage(language,{},focus=translate("Отложить",language),prefer="reminders")}}},"help-from-hint-$language")
            snapshot(ImageComposeScene(420,900,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {HelpPage(language,{},prefer="sync")}}},"help-phone-width-$language")
            // A text that names no icon goes into the search: the found words are shown inside their sentences (D118).
            snapshot(ImageComposeScene(420,700,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {HelpPage(language,{},focus=if(language=="ru")"подтвержд" else "confirm",prefer="sync")}}},"help-search-$language")
            snapshot(ImageComposeScene(320,80,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){Column {HintBubble(translate("Синхронизация",language)){};}}}}},"hint-$language")
        }
    }
    @Test fun newTextsExistInEveryLanguage() {
        assertTrue(interfaceRows2.size>150)
        interfaceRows2.forEach {(key,row)->
            assertEquals(9,row.size,key);assertTrue(row.all {it.isNotBlank()},key);assertEquals(key,translate(key,"ru"))
            listOf("{0}","{1}").forEach {mark->if(mark in key)assertTrue(row.all {mark in it},"$key keeps $mark")}
            foreign.forEach {language->assertFalse(cyrillic.containsMatchIn(translate(key,language).replace("ЗАМЕНИТЬ","").replace("УДАЛИТЬ","")),"$language/$key")}
        }
        assertTrue(interfaceRows.keys.intersect(interfaceRows2.keys).isEmpty(),"One text, one row: "+interfaceRows.keys.intersect(interfaceRows2.keys))
    }
    @Test fun hintsAndErrorMessagesAreNotLeftInRussian() {
        // Messages a screen stores in a variable and shows later, and the reasons given by require.
        val patterns=listOf(Regex("""(?:message|error|note|local|assetError)\s*=\s*(?:if\([^)\n]*\))?\s*"([^"$\n]*[А-Яа-яЁё][^"$\n]*)""""),Regex("""\{\s*"([^"$\n]*[А-Яа-яЁё][^"$\n]*)"\s*\}"""),Regex("""else\s*"([^"$\n]*[А-Яа-яЁё][^"$\n]*)""""))
        val skipped=setOf("Demonstration.kt","CatalogueTransferUI.kt","Help.kt","DeliveryHelp.kt","LegalNotice.kt")
        val texts=java.io.File("../shared/src").walkTopDown().filter {it.extension=="kt" && it.name !in skipped && !it.name.endsWith("Strings.kt") && !it.name.endsWith("Strings2.kt")}
            .flatMap {f->val source=f.readText();patterns.flatMap {p->p.findAll(source).map {it.groupValues[1]}}}.filter {it.length<300}.toSortedSet()
        assertTrue(texts.size>40,"Found ${texts.size}")
        val left=texts.filter {text->cyrillic.containsMatchIn(translate(text,"en").replace("ЗАМЕНИТЬ","").replace("УДАЛИТЬ","")) && libraryText(text,"en")==text && noteNavigationText(text,"en")==text}
        assertTrue(left.isEmpty(),"English shows Russian: $left")
    }
    @Test fun textsBuiltFromWordsAndNumbersAreTranslatedButTypedTextIsNot() {
        assertEquals("Found 12. Showing the first 60.",translate("Найдено 12. Показаны первые 60.","en"))
        assertEquals("1200 kcal · P 40 · F 30 · C 150",translate("1200 ккал · Б 40 · Ж 30 · У 150","en"))
        foreign.forEach {language->listOf("Покой: 1500 ккал; ориентир с активностью: 2000 ккал/день.","12 мин · цель отключена/на паузе","Целевой вес: 70 кг","Повторите через 5 с").forEach {
            assertFalse(cyrillic.containsMatchIn(translate(it,language)),"$language/$it → ${translate(it,language)}")
        }}
        // A name or a note the person typed has words the dictionaries do not know: it stays exactly as typed.
        listOf("Обед у мамы 2","Присед 3 × 10 повторов","Ккал моей бабушки: 100 ккал").forEach {assertEquals(it,translate(it,"en"))}
        assertEquals("Найдено 12. Показаны первые 60.",translate("Найдено 12. Показаны первые 60.","ru"))
    }
    @Test fun helpCoversWhatWasAddedAndNoLongerPromisesAPhonePedometer() {
        languageNames.keys.forEach {language->
            val added=deliveryHelp(language);assertEquals(4,added.size,language)
            added.forEach {assertTrue(it.title.length>3 && it.body.length>60,"$language/${it.title}");if(language in foreign)assertFalse(cyrillic.containsMatchIn(it.title+it.body),"$language/${it.title}")}
            val all=helpArticles(language);assertTrue(all.map {it.title}.containsAll(added.map {it.title}),language)
            assertTrue(all.any {stepsHelp.getValue(language) in it.body},"$language: steps paragraph")
            added.forEach {article->assertEquals(1,all.count {it.title==article.title},"$language: ${article.title}")}
        }
        assertTrue("WAV" in deliveryHelp("ru")[0].body && "пять минут" in deliveryHelp("ru")[1].body && "экономии батареи" in economyNotice)
    }
    @Test fun reminderCardsEditorAndSyncPageRender() {
        withPlatform {platform->
            val reminders=listOf(Reminder(name="Вода",times=listOf("09:00","15:30"),requiresConfirmation=true,repeatUntilConfirmed=true),Reminder(name="Прогулка",enabled=false,weekdays=listOf(1,3,5),volumePercent=60,rampSeconds=30))
            platform.save(platform.load().let {it.copy(local=it.local.copy(reminders=reminders,remindersOff=false))})
            listOf("ru","en").forEach {language->
                snapshot(ImageComposeScene(420,520,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){Column(Modifier.padding(8.dp)){
                    ReminderCards(platform,platform.load(),onEdit={},onSave={true},onDelete={true})
                }}}}},"reminder-cards-$language")
                snapshot(ImageComposeScene(420,1500,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {
                    ReminderEditor(reminders[1],onDismiss={},onSave={true},onDelete={true},platform=platform)
                }}},"reminder-editor-$language")
                // In the Windows window an editing page covers the whole window: no margin, no dimmed edge (D108).
                snapshot(ImageComposeScene(1000,600,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {
                    ReminderEditor(reminders[1],onDismiss={},onSave={true},onDelete={true},platform=platform)
                }}},"reminder-editor-window-$language")
                // Making a reminder while every reminder is off, as after installation: the hint comes first (D113).
                snapshot(ImageComposeScene(420,320,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {RemindersOffHint({},{},{})}}},"reminders-off-hint-$language")
                // One connection asks everything, the other has all three set beforehand (D109).
                com.fiteasy.shared.sync.SyncStore(platform).let {store->store.add("p1","device-1","Phone",ByteArray(32),1);store.add("p2","device-2","Tablet",ByteArray(32),1);store.update("p2"){it.copy(askDirection=false,direction=com.fiteasy.shared.sync.SyncDirection.RECEIVE,askMode=false,mode="REPLACE",askFolders=false,allFolders=true)}}
                snapshot(ImageComposeScene(420,1100,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalDesktopWindow provides true){MaterialTheme {SyncPanel(platform,{},{})}}},"sync-page-$language")
                snapshot(ImageComposeScene(420,900,Density(1f)){CompositionLocalProvider(LocalLanguage provides language,LocalBatteryEconomy provides true){MaterialTheme {Surface(Modifier.fillMaxSize()){ExerciseLibraryScreen(platform,onDismiss={},onOpenEconomy={})}}}},"library-economy-$language")
            }
            assertEquals(listOf("system","file"),platform.reminderSoundPickers());assertEquals("Alarm01",platform.reminderSoundTitle("C:\\Windows\\Media\\Alarm01.wav"))
            assertEquals("Утро",DesktopSound.title(java.io.File(java.io.File("x","reminder_sounds"),"1a2b3c4d-Утро.wav").path))
        }
    }
    @Test fun aMelodyPlaysAtItsVolumeAndStops() {
        val file=java.io.File.createTempFile("fiteasy-tone-",".wav")
        try {
            val rate=8000f;val samples=ByteArray(16000){i->(kotlin.math.sin(i*2*Math.PI*440/rate)*60).toInt().toByte()}
            javax.sound.sampled.AudioSystem.write(javax.sound.sampled.AudioInputStream(samples.inputStream(),javax.sound.sampled.AudioFormat(rate,8,1,true,false),samples.size.toLong()),javax.sound.sampled.AudioFileFormat.Type.WAVE,file)
            // A build machine may have no sound device: then the player falls back to the system beep and reports nothing ringing.
            val device=runCatching {javax.sound.sampled.AudioSystem.getClip().close()}.isSuccess
            DesktopSound.play(file.absolutePath,"alarm",10,0,"r1")
            if(device)assertEquals("r1",DesktopSound.ringing.value)
            DesktopSound.stop();assertNull(DesktopSound.ringing.value)
            DesktopSound.play(java.io.File(file.parentFile,"missing.wav").absolutePath,"alarm",10,0,null,200);DesktopSound.stop()
        }finally {file.delete()}
    }
}
