package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.sql.DriverManager
import kotlin.test.*

class LegalNoticeTest {
    @Test fun physicalWindowsWipeRequiresNoticeAgain(){
        val root=java.nio.file.Files.createTempDirectory("fiteasy-legal-test-").toFile()
        val previous=System.getProperty("fiteasy.home");System.setProperty("fiteasy.home",root.path)
        try {
            val platform=DesktopPlatform();platform.save(acknowledgeLegalNotice(Snapshot()))
            DesktopPlatform().let {other->try {assertFalse(needsLegalNotice(other.load().local))}finally {other.close()}}
            platform.wipeLocal();assertTrue(needsLegalNotice(platform.load().local))
        }finally {if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun allTenLanguagesHaveFullNoticeAndSameHelp(){
        assertEquals(languageNames.keys,legalNotices.keys)
        legalNotices.forEach {(language,notice)->
            assertTrue(notice.title.isNotBlank());assertTrue(notice.acknowledge.isNotBlank());assertTrue(notice.saveError.isNotBlank())
            assertEquals(5,notice.body.split("\n\n").size,language)
            assertTrue(notice.body.length>200,language)
            assertEquals(HelpArticle(notice.title,notice.body),helpArticles(language).first())
        }
        assertEquals(legalNotice("en"),legalNotice("unsupported"))
    }
    @Test fun firstRunAcknowledgementAndLegacyData(){
        val initial=Snapshot()
        assertTrue(needsLegalNotice(initial.local))
        val acknowledged=acknowledgeLegalNotice(initial)
        assertFalse(needsLegalNotice(acknowledged.local))
        assertEquals(initial.shared,acknowledged.shared)
        assertEquals(acknowledged,acknowledgeLegalNotice(acknowledged))
        val encoded=codec.encodeToString(acknowledged)
        assertEquals(acknowledged,codec.decodeFromString<Snapshot>(encoded))
        val legacy=codec.encodeToString(initial).replace(",\"legalNoticeVersion\":0","")
        assertTrue(needsLegalNotice(codec.decodeFromString<Snapshot>(legacy).local))
        assertTrue(needsLegalNotice(acknowledged.local.copy(legalNoticeVersion=2)))
        assertFails {validateLocal(initial.local.copy(legalNoticeVersion=-1))}
        assertFalse(codec.encodeToString(Backup(data=acknowledged.shared,appearance=acknowledged.local.appearance)).contains("legalNoticeVersion"))
    }
    @Test fun fullResetRepeatsNoticeButPartialResetDoesNot(){
        val initial=acknowledgeLegalNotice(Snapshot(local=LocalData(appearance=Appearance(language="ja"))))
        assertFalse(needsLegalNotice(resetData(initial,setOf("notes")).local))
        val full=resetData(initial,resetCategories.keys,true)
        assertTrue(needsLegalNotice(full.local));assertEquals("ja",full.local.appearance.language)
        assertTrue(needsLegalNotice(resetData(initial,resetCategories.keys).local))
        assertFalse(needsLegalNotice(acknowledgeLegalNotice(full).local))
    }
    @Test fun acknowledgementPersistsInSqlAndFreshDatabaseRequiresNotice(){
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite::memory:").use {connection->
            val db=jdbcStateSql(connection);SqlStateStore.migrate(db)
            val fresh=SqlStateStore.read(db);assertTrue(needsLegalNotice(fresh.local))
            val acknowledged=acknowledgeLegalNotice(fresh)
            db.transaction {SqlStateStore.write(db,acknowledged,fresh)}
            assertFalse(needsLegalNotice(SqlStateStore.read(db).local))
            val cleared=resetData(acknowledged,resetCategories.keys,true)
            db.transaction {SqlStateStore.write(db,cleared,acknowledged)}
            assertTrue(needsLegalNotice(SqlStateStore.read(db).local))
            assertEquals("2",db.query("PRAGMA user_version").single().single())
        }
    }
}
