package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.lock.*
import java.io.File
import javax.sound.sampled.AudioFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import kotlin.test.*

/** 0.4.36 (D122–D124): program files checked at start, MP3 and OGG melodies on Windows, the TOTP key as a QR code. */
class CompletionTest {
    private fun resource(name:String):File = File.createTempFile("fiteasy-tone-","."+name.substringAfterLast('.')).also {target->
        javaClass.classLoader.getResourceAsStream("sound/$name")!!.use {input->target.outputStream().use {input.copyTo(it)}}
    }

    @Test fun theLauncherNamesMissingProgramFilesInTheLanguageOfTheSystem() {
        val folder=java.nio.file.Files.createTempDirectory("fiteasy-launcher-").toFile()
        try {
            val present=File(folder,"present.jar").apply {writeBytes(byteArrayOf(1,2,3))}
            val empty=File(folder,"empty.jar").apply {writeBytes(ByteArray(0))}
            val gone=File(folder,"gone.jar")
            val path=listOf(present,empty,gone,folder).joinToString(File.pathSeparator) {it.absolutePath}+File.pathSeparator+File(folder,"*").path+File.pathSeparator
            // A file that is not there and a file cut to nothing are both named; folders, wildcards and blanks are not files to check.
            assertEquals(listOf("empty.jar","gone.jar"),Launcher.missing(path))
            assertTrue(Launcher.missing(present.absolutePath).isEmpty());assertTrue(Launcher.missing("").isEmpty())
            // The class path the tests themselves run on is whole.
            assertTrue(Launcher.missing(System.getProperty("java.class.path")).isEmpty())
            languageNames.keys.forEach {language->assertTrue(Launcher.message(language).contains("FitEasy") && Launcher.message(language).length>40,language)}
            assertEquals(languageNames.keys.size,languageNames.keys.map {Launcher.message(it)}.toSet().size,"Ten different texts")
            assertEquals(Launcher.message("en"),Launcher.message("pt"));assertEquals(3,Launcher.MISSING_FILES)
            assertTrue(Regex("[А-Яа-яЁё]").containsMatchIn(Launcher.message("ru")) && !Regex("[А-Яа-яЁё]").containsMatchIn(Launcher.message("de")))
        }finally {folder.deleteRecursively()}
    }

    @Test fun mp3AndOggMelodiesAreDecodedAndCutToTheTimeTheyRing() {
        listOf("tone.mp3","tone.ogg").forEach {name->
            val file=resource(name)
            try {
                val (format,samples)=DesktopSound.decode(file,60_000)
                assertEquals(AudioFormat.Encoding.PCM_SIGNED,format.encoding,name);assertEquals(16,format.sampleSizeInBits,name);assertEquals(1,format.channels,name);assertEquals(22050f,format.sampleRate,name)
                val seconds=samples.size/format.frameSize/format.frameRate
                assertTrue(seconds in 1.7f..2.4f,"$name: two seconds of sound, got $seconds");assertEquals(0,samples.size%format.frameSize,name)
                // It is a tone and not silence: samples swing both ways.
                val values=(0 until samples.size/2).map {i->((samples[2*i+1].toInt() shl 8) or (samples[2*i].toInt() and 0xFF)).toShort().toInt()}
                assertTrue(values.max()>1000 && values.min()< -1000,"$name: ${values.min()}..${values.max()}")
                // A long song is never unpacked whole: half a second asked, at most a second and a half taken.
                val short=DesktopSound.decode(file,500).second.size/format.frameSize/format.frameRate
                assertTrue(short in 1.2f..1.55f,"$name: $short")
                // The player takes it: with a sound device it rings, without one it falls back to the beep.
                val device=runCatching {javax.sound.sampled.AudioSystem.getClip().close()}.isSuccess
                DesktopSound.play(file.absolutePath,"alarm",5,0,"melody-$name",300)
                if(device)assertEquals("melody-$name",DesktopSound.ringing.value)
                DesktopSound.stop();assertNull(DesktopSound.ringing.value)
            }finally {file.delete()}
        }
        // What is not sound is refused, as before.
        val text=File.createTempFile("fiteasy-not-sound-",".mp3").apply {writeText("not a melody at all, only letters ".repeat(40))}
        try {assertFails {DesktopSound.decode(text,1000)}}finally {text.delete()}
    }

    /** D125: a save returns at once, one thread writes, states that queue up are joined, a failure is told and nothing is assumed after it. */
    @Test fun savesAreWrittenByTheirOwnThreadJoinedAndAFailureIsReported() {
        val written=java.util.Collections.synchronizedList(mutableListOf<Pair<Int,Int?>>())
        val failures=java.util.Collections.synchronizedList(mutableListOf<Triple<Int,Int?,String>>())
        val entered=java.util.concurrent.CountDownLatch(1);val gate=java.util.concurrent.CountDownLatch(1)
        val threads=java.util.Collections.synchronizedSet(mutableSetOf<Thread>())
        val queue=SaveQueue<Int>("FitEasy-save-test",{value,persisted->
            threads+=Thread.currentThread()
            if(value==1){entered.countDown();assertTrue(gate.await(10,java.util.concurrent.TimeUnit.SECONDS))}
            if(value==13)error("disk")
            written+=value to persisted
        }) {value,persisted,error->failures+=Triple(value,persisted,error.message.orEmpty())}
        assertTrue(queue.flush(),"Nothing handed over: nothing to wait for")
        queue.known(0)
        val started=System.nanoTime();queue.submit(1);val taken=(System.nanoTime()-started)/1_000_000
        assertTrue(taken<1000,"Handing over does not wait for the disk: $taken ms")
        assertTrue(entered.await(10,java.util.concurrent.TimeUnit.SECONDS))
        // Two states arrive while the first is being written: only the last of them goes to the disk, against the first.
        queue.submit(2);queue.submit(3);assertTrue(written.isEmpty());gate.countDown()
        assertTrue(queue.flush());assertEquals(listOf<Pair<Int,Int?>>(1 to 0,3 to 1),written.toList())
        assertTrue(Thread.currentThread() !in threads && threads.size==1)
        // The disk refuses: flush says so, the callback gets what the disk held before.
        queue.submit(13);assertFalse(queue.flush())
        // The callback comes after the queue is free again, so a caller that waits in flush is never held by it.
        val until=System.currentTimeMillis()+5000;while(failures.isEmpty() && System.currentTimeMillis()<until)Thread.sleep(5)
        assertEquals(listOf(Triple<Int,Int?,String>(13,3,"disk")),failures.toList())
        // After a failure nothing is assumed about the file; a good write clears the trouble.
        queue.submit(4);assertTrue(queue.flush());assertEquals<Pair<Int,Int?>>(4 to null,written.last())
        queue.submit(5);assertTrue(queue.flush());assertEquals<Pair<Int,Int?>>(5 to 4,written.last())
        // A replaced file is known to hold what was written into it.
        queue.known(9);queue.submit(6);assertTrue(queue.flush());assertEquals<Pair<Int,Int?>>(6 to 9,written.last())
    }

    /** D125 on the real database of Windows: what was saved is read at once, is on the disk after a flush, and survives closing. */
    @Test fun aSavedRecordIsReadAtOnceAndIsOnTheDiskAfterAFlush() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-queue-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {
            repeat(30) {i->platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries+Entry(type="drink",name="W$i",amount=200.0,unit="ml",plainWater=true)))}}
            assertEquals(30,platform.load().shared.entries.size,"Read back at once, before the disk has it")
            assertTrue(platform.flushSaves())
            val other=DesktopPlatform();try {assertEquals((0 until 30).map {"W$it"},other.load().shared.entries.map {it.name})}finally {other.close()}
            // Refused at once, on the thread that asked: nothing wrong is ever queued.
            assertFails {platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries+s.shared.entries.first()))}}
            assertEquals(30,platform.load().shared.entries.size)
            // Encryption replaces the file: everything saved before is in the new one, and saving goes on.
            platform.setEncryption(true)
            platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries.dropLast(1)))}
            platform.close()
            val reopened=DesktopPlatform();try {assertTrue(reopened.encryptionEnabled());assertEquals(29,reopened.load().shared.entries.size)}finally {reopened.close()}
        }finally {runCatching {platform.close()};if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }

    @Test fun theAlarmScreenClosesItselfAndTheLockCountsOnlyOnTheScreen() {
        // D121, rule 10: a minute at the first ring, fifteen seconds at the second unanswered one, five after that.
        assertEquals(listOf(60_000L,60_000L,15_000L,5_000L,5_000L),listOf(0,1,2,3,40).map(::alarmScreenMillis))
        assertEquals(9,DOZE_ALARM_MINUTES)
        val source=File("../shared/src/com/fiteasy/shared/SecurityUI.kt").readText()
        assertTrue("LaunchedEffect(blockedUntil,foreground) {if(foreground)" in source,"The countdown of the lock ticks only while it is looked at")
        val activity=File("../app/src/main/java/com/fiteasy/app/ReminderAlarmActivity.kt").readText()
        assertTrue("alarmScreenMillis(intent.getIntExtra(\"count\",1))" in activity && "FLAG_KEEP_SCREEN_ON" !in activity)
        val manifest=File("../app/src/main/AndroidManifest.xml").readText()
        // Rules 3, 7, 11: no alarm-clock alarms, one short service for the melody, no request to leave battery optimisation.
        assertFalse("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in manifest);assertEquals(1,Regex("<service ").findAll(manifest).count())
        File("../app/src/main/java/com/fiteasy/app").walkTopDown().filter {it.extension=="kt"}.forEach {file->
            val text=file.readText()
            assertFalse("setAlarmClock" in text || "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in text || "newWakeLock" in text,file.name)
        }
    }

    /** D127: Settings → Rights names every right, says what it is for, offers the one action its state allows, in ten languages, and the help finds it. */
    @Test fun theRightsPageNamesEveryRightAndItsActionInEveryLanguage() {
        assertEquals("Права",settingsTitle("permissions"));assertEquals("key",settingIcons["permissions"])
        assertEquals(setOf("notifications","exact","fullscreen","overlay","windows-notifications","windows-network"),rightTexts.keys)
        assertEquals(listOf("Отозвать","Разрешить","Открыть в системе"),listOf(true,false,null).map(::rightAction));assertEquals(3,listOf(true,false,null).map(::rightStateText).toSet().size)
        val shown=rightTexts.values.flatMap {listOf(it.first,it.second)}+listOf(true,false,null).flatMap {listOf(rightAction(it),rightStateText(it))}+rightsRows.keys
        languageNames.keys.filter {it !in setOf("ru","be")}.forEach {language->shown.forEach {assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(translate(it,language)),"$language/$it → ${translate(it,language)}")}}
        rightsRows.forEach {(key,row)->assertEquals(9,row.size,key);assertTrue(row.all {it.isNotBlank()},key)}
        listOf("Разрешить","Отозвать","Открыть в системе").forEach {assertNotNull(buttonIcons[it],it)};assertNull(buttonIcons["Отозвать все права"],"Taking everything back is named in words")
        // Windows: two system pages, their state is not told to the app, nothing to take back at once.
        val root=java.nio.file.Files.createTempDirectory("fiteasy-rights-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {assertEquals(listOf(AppRight("windows-notifications",null),AppRight("windows-network",null)),platform.rights());assertFails {platform.grantRight("camera")}}
        finally {platform.close();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
        // Drawn as the phone and as Windows show it; the sheets go to build/review.
        val drawn=DesktopPlatform()
        try {
            val phone=object:Platform by drawn {override val name="Android";override fun rights()=listOf(AppRight("notifications",true),AppRight("exact",false),AppRight("fullscreen",true),AppRight("overlay",false))}
            listOf("phone" to phone,"windows" to drawn).forEach {(file,shown)->listOf("ru","de").forEach {language->
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                val scene=androidx.compose.ui.ImageComposeScene(420,900,androidx.compose.ui.unit.Density(1f)) {androidx.compose.runtime.CompositionLocalProvider(LocalLanguage provides language) {androidx.compose.material3.MaterialTheme {androidx.compose.material3.Surface(androidx.compose.ui.Modifier.fillMaxSize()) {androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(12.dp)) {RightsPage(shown)}}}}}
                try {scene.render();Thread.sleep(150);val image=scene.render(50_000_000);File("build/review/rights-$file-$language.png").also {it.parentFile.mkdirs()}.writeBytes(image.encodeToData()!!.bytes)}finally {scene.close()}
            }}
        }finally {drawn.close()}
        // Android: the four rights, the system page for each, and only notifications given up by the app itself.
        val activity=File("../app/src/main/java/com/fiteasy/app/MainActivity.kt").readText()
        assertTrue("revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)" in activity && "override fun revokeAllRights()" in activity && "setOf(\"notifications\",\"exact\",\"fullscreen\",\"overlay\")" in activity)
        // What the page says about the rights given at installation is what the manifest asks for.
        val manifest=File("../app/src/main/AndroidManifest.xml").readText()
        listOf("CAMERA","RECORD_AUDIO","ACCESS_FINE_LOCATION","ACCESS_COARSE_LOCATION","READ_CONTACTS","READ_EXTERNAL_STORAGE","READ_MEDIA_").forEach {assertFalse(it in manifest,it)}
        languageNames.keys.forEach {language->
            val tree=helpTree(language);val section=tree.first {it.id=="settings"}.sections.first {it.body==rightsHelp(language)}
            assertEquals(translate("Права",language),section.title);assertEquals(3,section.icons.size)
            assertNotNull(findHelp(tree,translate("Отозвать",language)),language)
            // D128: the system shows «ФитИзи» where it speaks Russian and FitEasy everywhere else.
            assertEquals(if(language=="ru")"ФитИзи" else "FitEasy",appSystemName(language))
            if(language !in setOf("ru","be"))assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(rightsHelp(language)),language)
        }
        assertTrue(searchHelp(helpTree("ru"),"отозвать").isNotEmpty());assertEquals("settings",helpCategoryFor("profile","permissions"))
    }
    @Test fun theTotpKeyIsShownAsAQrCodeAnAuthenticatorReads() {
        val secret=Totp.newSecret()
        val uri=Totp.uri("FitEasy","Рабочий ПК",secret)
        assertTrue(uri.startsWith("otpauth://totp/FitEasy:") && "secret=$secret" in uri && "issuer=FitEasy" in uri && "digits=6" in uri && "period=30" in uri && ' ' !in uri)
        val matrix=qrMatrix(uri)
        assertTrue(matrix.size>=21 && matrix.all {it.size==matrix.size},"A square of modules")
        // Read back by the decoder of the same library: exactly the address that was drawn.
        assertEquals(uri,com.google.zxing.qrcode.decoder.Decoder().decode(matrix).text)
        assertNotEquals(matrix.map {it.toList()},qrMatrix(Totp.uri("FitEasy","Рабочий ПК",Totp.newSecret())).map {it.toList()})
        // The key the code carries gives the codes the lock accepts.
        val read=Regex("secret=([A-Z2-7]+)").find(com.google.zxing.qrcode.decoder.Decoder().decode(matrix).text)!!.groupValues[1]
        val now=System.currentTimeMillis();assertTrue(Totp.verify(secret,Totp.code(read,now),now))
        assertEquals(4,QR_MARGIN)
        // D120: the help tells it where the person looks for it, and the search finds it by the words they would type.
        languageNames.keys.forEach {language->
            val tree=helpTree(language)
            assertTrue(tree.first {it.id=="security"}.sections.first().body.contains(helpAddition(0,language)),language);assertTrue(tree.first {it.id=="reminders"}.sections.first().body.endsWith(helpAddition(1,language)),language)
            assertTrue("TOTP" in helpAddition(0,language) && "Android" in helpAddition(1,language) && "MP3" in tree.first {it.id=="reminders"}.sections.first().body,language)
            if(language !in setOf("ru","be"))assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(helpAddition(0,language)+helpAddition(1,language)),language)
        }
        assertTrue(searchHelp(helpTree("ru"),"QR-код").any {helpTree("ru")[it.place.category].id=="security"});assertTrue(searchHelp(helpTree("en"),"MP3").isNotEmpty())
        languageNames.keys.filter {it !in setOf("ru","be")}.forEach {language->
            listOf("QR-код ключа TOTP","Отсканируйте QR-код приложением-аутентификатором или введите ключ вручную, затем введите 6-значный код, который оно показывает.").forEach {
                assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(translate(it,language)),"$language/$it")
            }
        }
    }

    @Test fun aMelodyIsPlayedOnlyFromTheSoundsOfWindowsOrTheCopyOfTheApp() {
        val data=java.nio.file.Files.createTempDirectory("fiteasy-sounds-").toFile()
        try {
            val sounds=File(data,"reminder_sounds").apply {mkdirs()}
            val own=File(sounds,"abcd1234-tone.mp3").apply {writeBytes(ByteArray(8))}
            assertEquals(own.absolutePath,DesktopSound.own(own.absolutePath,data))
            val media=File(System.getenv("WINDIR") ?: "C:\\Windows","Media")
            assertEquals(File(media,"Alarm01.wav").path,DesktopSound.own(File(media,"Alarm01.wav").path,data))
            // What an imported database file could carry: another computer, the web, a file elsewhere, a way out of the folder.
            listOf("\\\\192.0.2.1\\share\\tone.wav","//192.0.2.1/share/tone.wav","https://example.org/tone.mp3",File(data,"tone.mp3").absolutePath,
                File(sounds,"..\\..\\tone.mp3").path,File(sounds,"tone.exe").absolutePath,""," ").forEach {assertNull(DesktopSound.own(it,data),it)}
            assertNull(DesktopSound.own(null,data))
        } finally {data.deleteRecursively()}
    }

    @Test fun theHelpAndTheHintSayThatTheLockKeepsTheScreenOutOfScreenshots() {
        val hint="Пока блокировка включена, снимки и запись экрана приложения запрещены, а в списке недавних приложений его содержимое скрыто."
        languageNames.keys.forEach {language->
            val paragraph=helpAddition(2,language)
            assertTrue(paragraph.length>(if(language in setOf("zh","ja","ko"))40 else 120) && "FitEasy" in paragraph && "Android" in paragraph,language)
            assertTrue(helpTree(language).first {it.id=="security"}.sections.first().body.endsWith(paragraph),language)
            if(language !in setOf("ru","be"))assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(translate(hint,language)),language) else if(language=="be")assertNotEquals(hint,translate(hint,language))
        }
    }
}
