package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import java.sql.DriverManager
import kotlin.test.*

class SqlStateStoreTest {
    private fun database(test:(StateSql)->Unit){Class.forName("org.sqlite.JDBC");DriverManager.getConnection("jdbc:sqlite::memory:").use {test(jdbcStateSql(it))}}
    private fun snapshot()=Snapshot(shared=SharedData(entries=listOf(Entry(type="drink",name="Water",amount=250.0,unit="ml",deleted=1)),products=listOf(Food("apple","Apple","product")),dishes=listOf(Food("baked","Baked apple","dish")),routines=listOf(Routine(name="Plan",instructions="",minutes=1,exercises=listOf(Exercise("Move"))))),local=LocalData(appearance=Appearance(language="ko"),reminders=listOf(Reminder(name="Drink"))))
    private fun legacy(db:StateSql,snapshot:Snapshot){db.execute("CREATE TABLE app_state(id INTEGER PRIMARY KEY,payload TEXT NOT NULL)");db.execute("INSERT INTO app_state VALUES(1,?)",listOf(codec.encodeToString(snapshot)));db.execute("PRAGMA user_version=1")}
    @Test fun freshSchemaAndAggregateRoundtrip(){database {db->SqlStateStore.migrate(db);assertEquals(SqlStateStore.newInstallation,SqlStateStore.read(db));val s=snapshot();db.transaction {SqlStateStore.write(db,s)};assertEquals(s,SqlStateStore.read(db));assertEquals("2",db.query("PRAGMA user_version").single().single());assertEquals("apple",db.query("SELECT id FROM products").single().single());assertEquals("baked",db.query("SELECT id FROM dishes").single().single())}}
    @Test fun legacyMigrationPreservesSnapshotsAndRemovesOldTable(){database {db->val s=snapshot();legacy(db,s);assertEquals(s,SqlStateStore.read(db));SqlStateStore.migrate(db);assertEquals(s,SqlStateStore.read(db));assertTrue(db.query("SELECT name FROM sqlite_master WHERE name='app_state'").isEmpty());SqlStateStore.migrate(db)}}
    @Test fun interruptedMigrationRollsBack(){database {db->val s=snapshot();legacy(db,s);val failing=object:StateSql by db {override fun execute(sql:String,args:List<String?>){if(sql.startsWith("DROP TABLE"))error("Injected interruption");db.execute(sql,args)}};assertFails {SqlStateStore.migrate(failing)};assertEquals("1",db.query("PRAGMA user_version").single().single());assertEquals(s,SqlStateStore.read(db));assertTrue(db.query("SELECT name FROM sqlite_master WHERE name='state_metadata'").isEmpty());SqlStateStore.migrate(db)}}
    @Test fun futureCorruptAndUnversionedSchemasRejected(){database {db->db.execute("PRAGMA user_version=99");assertFails {SqlStateStore.migrate(db)};assertFails {SqlStateStore.read(db)}};database {db->SqlStateStore.migrate(db);db.execute("DELETE FROM state_metadata");assertFails {SqlStateStore.read(db)}};database {db->db.execute("CREATE TABLE unknown(payload TEXT)");assertFails {SqlStateStore.migrate(db)}}}
    @Test fun onlyChangedAggregateWrittenAndOrderingPreserved(){database {db->SqlStateStore.migrate(db);val a=snapshot();db.transaction {SqlStateStore.write(db,a)};val writes=mutableListOf<String>();val tracking=object:StateSql by db {override fun execute(sql:String,args:List<String?>){writes+=sql;db.execute(sql,args)}};val b=a.copy(shared=a.shared.copy(entries=a.shared.entries.map {it.copy(amount=300.0)}));db.transaction {SqlStateStore.write(tracking,b,a)};assertEquals(1,writes.size);assertTrue(writes.single().startsWith("INSERT OR REPLACE INTO entries"));assertEquals(b,SqlStateStore.read(db));val c=b.copy(shared=b.shared.copy(products=listOf(Food("pear","Pear","product"))+b.shared.products));db.transaction {SqlStateStore.write(db,c,b)};assertEquals(listOf("pear","apple"),SqlStateStore.read(db).shared.products.map {it.id})}}
    @Test fun inconsistentProjectionRejected(){database {db->SqlStateStore.migrate(db);db.transaction {SqlStateStore.write(db,snapshot())};db.execute("UPDATE entries SET event_type='steps'");assertFails {SqlStateStore.read(db)}}}
    @Test fun encryptedLegacyMigrationReopensWithSameKey(){
        Class.forName("org.sqlite.JDBC")
        val file=java.nio.file.Files.createTempFile("fiteasy-migration-test-",".db").toFile();check(file.delete())
        val s=snapshot()
        fun connect()=org.sqlite.mc.SQLiteMCSqlCipherConfig.getV4Defaults().withKey("test-migration-password").build().createConnection("jdbc:sqlite:${file.absolutePath}")
        try {connect().use {connection->val db=jdbcStateSql(connection);legacy(db,s);SqlStateStore.migrate(db)};connect().use {assertEquals(s,SqlStateStore.read(jdbcStateSql(it)))}}finally {check(file.delete())}
    }
}
