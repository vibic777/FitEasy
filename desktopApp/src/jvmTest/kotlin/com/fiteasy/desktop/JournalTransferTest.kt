package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.lock.*
import java.nio.file.Files
import kotlin.test.*

class JournalTransferTest {
    @Test fun detectorCountsDoNotResurrectDeletedRecords(){val first=recordDetectedSteps(Snapshot(),"2026-10-02","test",3);val row=first.shared.entries.single();val deleted=first.copy(shared=first.shared.copy(entries=listOf(row.copy(deleted=1))));val result=recordDetectedSteps(deleted,"2026-10-02","test",2);assertEquals(2,result.shared.entries.size);assertEquals(1,result.shared.entries.count {it.deleted!=null});assertEquals(2.0,result.shared.entries.single {it.deleted==null}.amount)}
    @Test fun transferTranslationsHaveEveryKey(){assertEquals(10,transferRows.size);transferRows.forEach {(language,row)->assertEquals(transferKeys.size,row.split('|').size,language)};assertEquals(10,syncLayoutRows.size)}
    @Test fun cleanupPersistsAcrossRestartAndFatalIgnoresDisabled() {
        val root=Files.createTempDirectory("fiteasy-journal-test").toFile()
        var now=1_000_000L
        try {
            val first=JournalStore(root){now};first.append(JournalLevel.INFO,"test.start",IllegalStateException("private-password-health"))
            assertFalse(first.read().contains("private-password-health"))
            now+=2*86400000L
            val restarted=JournalStore(root){now};restarted.append(JournalLevel.INFO,"test.new")
            assertFalse(restarted.read().contains("test.start"))
            restarted.setOptions(JournalOptions(enabled=false));restarted.append(JournalLevel.INFO,"test.hidden");restarted.append(JournalLevel.FATAL,"test.crash")
            assertFalse(restarted.read().contains("test.hidden"));assertTrue(restarted.read().contains("test.crash"))
            restarted.clear();assertEquals("",restarted.read())
        }finally{root.deleteRecursively()}
    }
    @Test fun neverCleanupAndRotationRemainBounded() {
        val root=Files.createTempDirectory("fiteasy-journal-test").toFile()
        try {val store=JournalStore(root);store.setOptions(JournalOptions(clearDays=0));repeat(10000){store.append(JournalLevel.INFO,"test."+"x".repeat(110))};assertTrue(store.read().length<=JournalStore.MAX_BYTES*2);assertFalse(journalCleanupDue(0,Long.MAX_VALUE,0));assertFails {validateJournalOptions(JournalOptions(retentionHours=0))}}finally{root.deleteRecursively()}
    }
    @Test fun rawDatabaseRoundtripRekeyAndInvalidFileDoNotChangeData() {
        val root=Files.createTempDirectory("fiteasy-db-test").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.path)
        try {
            val platform=DesktopPlatform();val state=Snapshot(shared=SharedData(entries=listOf(Entry(type="drink",name="test",amount=250.0,unit="ml"))))
            platform.save(state)
            val plain=platform.exportDatabase("");assertTrue(String(plain.take(15).toByteArray()).startsWith("SQLite format"));assertEquals(state,platform.readDatabase(plain,""))
            val encrypted=platform.exportDatabase("test-password-123");assertEquals(state,platform.readDatabase(encrypted,"test-password-123"));assertFails {platform.readDatabase(encrypted,"wrong")};assertEquals(state,platform.load())
            platform.changeDatabasePassword("database-password-123");assertTrue(platform.encryptionEnabled());assertEquals("database-password-123",platform.databasePassword());DesktopPlatform().let {other->try {assertEquals(state,other.load())}finally {other.close()}}
            platform.setEncryption(false);DesktopPlatform().let {other->try {assertEquals(state,other.load())}finally {other.close()}};assertFails {platform.readDatabase(ByteArray(500),"")}
        }finally{if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun attachmentMimeAndHeaderInjection() {
        val client=SmtpClient();val settings=SmtpSettings(host="localhost",from="sender@example.com",to="recipient@example.com")
        val mime=client.message(settings,"subject","body",MailAttachment("FitEasy.db",byteArrayOf(1,2,3)))
        assertTrue(mime.contains("multipart/mixed"));assertTrue(mime.contains("filename=\"FitEasy.db\""));assertTrue(mime.contains("AQID"));assertFalse(mime.contains(settings.password.takeIf {it.isNotEmpty()} ?: "no-password-here"))
        assertFails {client.message(settings,"subject","body",MailAttachment("bad\r\nHeader",byteArrayOf(1)))}
    }
}
