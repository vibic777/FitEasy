package com.fiteasy.app

import android.content.Context
import com.fiteasy.shared.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/** Shared transaction boundary for UI and foreground sensor service. */
object AndroidDatabase {
    val lock=Any()
    private val cache=CommittedState<Snapshot>()
    private var cachePath:String?=null
    // One connection per process (D080): SQLCipher derives the key once, not on every step batch or save.
    private var held:SQLiteDatabase?=null
    private var heldFor:List<Any?>?=null
    // The connection has its own lock (D125): the thread that writes holds it, never [lock], so a save does not wait for the disk.
    private val databaseLock=Any()
    private fun release(){synchronized(databaseLock){runCatching {held?.close()};held=null;heldFor=null}}
    @Volatile private var app:Context?=null
    val saveFailed=MutableSharedFlow<Unit>(extraBufferCapacity=1)
    // Saves are written off the main thread (D125); a failed write puts back what the disk holds and says so.
    private val writer=SaveQueue<Snapshot>("FitEasy-save",{value,persisted->
        connected(app ?: error("No context")) {db->
            db.beginTransaction()
            try {SqlStateStore.write(androidStateSql(db),value,persisted);db.setTransactionSuccessful()}
            finally {db.endTransaction()}
        }
    }) {value,_,error->
        AppJournal.record("android.database.save.failed",JournalLevel.ERROR,error)
        synchronized(lock){if(cache.value===value)cache.invalidate()}
        saveFailed.tryEmit(Unit);changed.tryEmit(Unit)
    }
    /** Waits until everything saved is on the disk: before the app leaves the screen and before the file is touched. */
    fun flush()=writer.flush()
    /** Also closes the held connection: call before renaming, replacing or deleting the database file. */
    fun invalidate()=synchronized(lock){writer.flush();cache.invalidate();release();writer.known(null)}
    private fun selectPath(context:Context){val path=context.getDatabasePath("fiteasy.db").absolutePath;if(path!=cachePath){invalidate();cachePath=path}}
    private fun <T> connected(context:Context,action:(SQLiteDatabase)->T):T = synchronized(databaseLock) {
        val prefs=context.getSharedPreferences("local-security",Context.MODE_PRIVATE)
        val file=context.getDatabasePath("fiteasy.db")
        // The sealed key identifies the connection, so reuse needs no Keystore round trip.
        val identity=listOf(file.absolutePath,prefs.getBoolean("encrypted",false),prefs.getString("databaseKey",null))
        if(heldFor!=identity || held?.isOpen!=true || !file.exists()){release();held=open(file,key(context));heldFor=identity}
        try {action(held!!)}catch(t:Throwable){release();throw t}
    }
    val changed=MutableSharedFlow<Unit>(extraBufferCapacity=16)
    fun recover(context:Context)=synchronized(lock){
        val prefs=context.getSharedPreferences("local-security",Context.MODE_PRIVATE)
        val file=context.getDatabasePath("fiteasy.db");file.parentFile?.mkdirs()
        val safety=File(file.parentFile,"fiteasy-conversion-safety.db")
        if(prefs.getBoolean("conversionPending",false)){
            invalidate()
            if(safety.exists()){check(!file.exists() || file.delete());check(safety.renameTo(file))}
            check(prefs.edit().putBoolean("encrypted",prefs.getBoolean("oldEncrypted",false)).putString("databaseKey",prefs.getString("oldDatabaseKey",null)).remove("conversionPending").remove("oldEncrypted").remove("oldDatabaseKey").commit())
        }else if(safety.exists()){
            invalidate()
            open(file,key(context)).use {db->db.rawQuery("PRAGMA integrity_check",null).use {check(it.moveToFirst() && it.getString(0)=="ok")}}
            check(safety.delete())
        }
    }
    fun key(context:Context):String {
        val prefs=context.getSharedPreferences("local-security",Context.MODE_PRIVATE)
        return if(prefs.getBoolean("encrypted",false))AndroidVault.unseal(prefs.getString("databaseKey",null) ?: error("Database key unavailable"))else ""
    }
    fun open(file:File,password:String):SQLiteDatabase {
        file.parentFile?.mkdirs()
        val db=SQLiteDatabase.openDatabase(file.absolutePath,password,null,SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,null,null)
        try {
            SqlStateStore.migrate(androidStateSql(db))
            return db
        }catch(t:Throwable){db.close();throw t}
    }
    private fun read(db:SQLiteDatabase)=SqlStateStore.read(androidStateSql(db))
    fun load(context:Context,recoverPending:Boolean=true):Snapshot=synchronized(lock){
        selectPath(context)
        if(!recoverPending)return@synchronized open(context.getDatabasePath("fiteasy.db"),key(context)).use(::read)
        app=context.applicationContext
        recover(context);cache.read {writer.flush();connected(context,::read).also(writer::known)}
    }
    fun save(context:Context,snapshot:Snapshot)=mutate(context){snapshot}
    fun mutate(context:Context,transform:(Snapshot)->Snapshot):Snapshot=synchronized(lock){
        val current=load(context)
        val next=transform(current)
        // Checked here, so a wrong value is refused at once; written by the save thread.
        val written=cache.commit(next) {value->validate(value.shared);validateLocal(value.local);writer.submit(value)}
        if(written)changed.tryEmit(Unit)
        next
    }
}

fun androidStateSql(db:SQLiteDatabase)=object:StateSql {
    override fun execute(sql:String,args:List<String?>){if(args.isEmpty())db.execSQL(sql)else db.execSQL(sql,args.toTypedArray())}
    override fun query(sql:String,args:List<String?>):List<List<String?>> =db.rawQuery(sql,args.map {requireNotNull(it)}.toTypedArray()).use {cursor->buildList {while(cursor.moveToNext())add((0 until cursor.columnCount).map {if(cursor.isNull(it))null else cursor.getString(it)})}}
    override fun transaction(action:()->Unit){db.beginTransaction();try {action();db.setTransactionSuccessful()}finally {db.endTransaction()}}
}
