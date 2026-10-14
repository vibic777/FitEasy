package com.fiteasy.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

interface StateSql {
    fun execute(sql:String,args:List<String?> = emptyList())
    fun query(sql:String,args:List<String?> = emptyList()):List<List<String?>>
    fun transaction(action:()->Unit)
}

/** Shared aggregate schema. No platform keys, paths or credentials appear here. */
object SqlStateStore {
    const val VERSION=2
    /** One list of the snapshot kept as its own table: where it lives, how a row is named and how a row is written. */
    private class Section<T>(val owner:String,val key:String,val table:String,val items:(Snapshot)->List<T>,val id:((T)->String)?,val encode:(T)->JsonElement) {val positional get()=id==null}
    private inline fun <reified T> section(owner:String,key:String,table:String,noinline items:(Snapshot)->List<T>,noinline id:((T)->String)?)=Section(owner,key,table,items,id){codec.encodeToJsonElement(it)}
    private val sections:List<Section<*>> = listOf(
        section("shared","entries","entries",{it.shared.entries}){it.id},section("shared","products","products",{it.shared.products}){it.id},section("shared","dishes","dishes",{it.shared.dishes}){it.id},
        section("shared","recipes","recipes",{it.shared.recipes}){it.id},section("shared","routines","routines",{it.shared.routines}){it.id},section("shared","schedules","schedules",{it.shared.schedules}){it.id},
        section("shared","notes","notes",{it.shared.notes}){it.id},section("shared","noteFolders","note_folders",{it.shared.noteFolders}){it.id},section("shared","estimates","estimates",{it.shared.estimates}){it.id},
        section<GoalRevision>("shared","goalHistory","goal_history",{it.shared.goalHistory},null),section("local","reminders","local_reminders",{it.local.reminders}){it.id}
    )
    /** The snapshot without the lists that have their own tables: what the metadata row holds. */
    private fun withoutSections(s:Snapshot)=s.copy(shared=s.shared.copy(entries=emptyList(),products=emptyList(),dishes=emptyList(),recipes=emptyList(),routines=emptyList(),schedules=emptyList(),notes=emptyList(),noteFolders=emptyList(),estimates=emptyList(),goalHistory=emptyList()),local=s.local.copy(reminders=emptyList()))
    /** What a new installation starts with: every reminder off until the person turns them on (D113). Existing data are not touched. */
    val newInstallation=Snapshot(local=LocalData(remindersOff=true))
    private fun version(db:StateSql)=db.query("PRAGMA user_version").single().single()!!.toInt()
    fun migrate(db:StateSql){
        val version=version(db)
        require(version in 0..VERSION){"Unsupported database schema"}
        if(version==VERSION)return
        if(version==0)require(db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").isEmpty()){"Unversioned nonempty database"}
        val legacy=if(version==1)read(db)else newInstallation
        validate(legacy.shared);validateLocal(legacy.local)
        db.transaction {
            db.execute("CREATE TABLE state_metadata(id INTEGER PRIMARY KEY CHECK(id=1),payload TEXT NOT NULL)")
            sections.forEach {s->db.execute("CREATE TABLE ${s.table}(id TEXT PRIMARY KEY NOT NULL,position INTEGER NOT NULL CHECK(position>=0),payload TEXT NOT NULL${if(s.table=="entries")",event_date TEXT NOT NULL,event_type TEXT NOT NULL,source TEXT NOT NULL,deleted INTEGER"else ""})")}
            db.execute("CREATE INDEX entries_by_day_type ON entries(event_date,event_type)")
            db.execute("CREATE INDEX entries_by_source ON entries(source,event_date)")
            write(db,legacy)
            db.execute("PRAGMA user_version=$VERSION")
            check(read(db)==legacy){"Migration verification failed"}
            if(version==1)db.execute("DROP TABLE app_state")
        }
    }
    fun read(db:StateSql):Snapshot {
        val version=version(db)
        require(version in 1..VERSION){"Unsupported database schema"}
        if(version==1){val rows=db.query("SELECT payload FROM app_state WHERE id=1");return if(rows.isEmpty())Snapshot()else codec.decodeFromString<Snapshot>(rows.single().single()!!).also {validate(it.shared);validateLocal(it.local)}}
        val meta=codec.parseToJsonElement(db.query("SELECT payload FROM state_metadata WHERE id=1").single().single()!!).jsonObject
        val shared=meta.getValue("shared").jsonObject.toMutableMap()
        val local=meta.getValue("local").jsonObject.toMutableMap()
        sections.forEach {s->
            val rows=db.query("SELECT id,payload,position${if(s.table=="entries")",event_date,event_type,source,deleted"else ""} FROM ${s.table} ORDER BY position,id")
            val values=rows.mapIndexed {index,row->
                require(row[2]?.toInt()==index){"Inconsistent entity order"}
                val value=codec.parseToJsonElement(row[1]!!)
                if(!s.positional)require(value.jsonObject["id"]?.jsonPrimitive?.content==row[0]){"Inconsistent entity ID"}
                if(s.table=="entries"){val obj=value.jsonObject;require(listOf(obj.getValue("date").jsonPrimitive.content,obj.getValue("type").jsonPrimitive.content,obj.getValue("source").jsonPrimitive.content,obj["deleted"]?.jsonPrimitive?.contentOrNull)==row.drop(3)){"Inconsistent diary projection"}}
                value
            }
            (if(s.owner=="shared")shared else local)[s.key]=JsonArray(values)
        }
        return codec.decodeFromString<Snapshot>(JsonObject(mapOf("shared" to JsonObject(shared),"local" to JsonObject(local))).toString()).also {validate(it.shared);validateLocal(it.local)}
    }
    /**
     * Caller owns a transaction. previous is the last committed snapshot, never an optimistic draft.
     * Only what changed is encoded and written (D102): a list that is the same object as before is skipped whole, and in a
     * changed list only the rows whose place or content differ. One new diary record on a large diary is one row.
     */
    fun write(db:StateSql,snapshot:Snapshot,previous:Snapshot?=null){
        validate(snapshot.shared);validateLocal(snapshot.local)
        sections.forEach {writeSection(db,it,snapshot,previous)}
        val stripped=withoutSections(snapshot)
        if(previous!=null && withoutSections(previous)==stripped)return
        val next=codec.encodeToJsonElement(stripped).jsonObject
        val shared=next.getValue("shared").jsonObject.toMutableMap();val local=next.getValue("local").jsonObject.toMutableMap()
        sections.forEach {s->(if(s.owner=="shared")shared else local).remove(s.key)}
        val metadata=JsonObject(mapOf("shared" to JsonObject(shared),"local" to JsonObject(local))).toString()
        val oldMetadata=db.query("SELECT payload FROM state_metadata WHERE id=1").firstOrNull()?.single()
        if(metadata!=oldMetadata)db.execute("INSERT OR REPLACE INTO state_metadata(id,payload) VALUES(1,?)",listOf(metadata))
    }
    private fun <T> writeSection(db:StateSql,s:Section<T>,snapshot:Snapshot,previous:Snapshot?){
        val rows=s.items(snapshot);val old=previous?.let(s.items)
        if(old!=null && old===rows)return
        fun key(i:Int,v:T)=s.id?.invoke(v) ?: i.toString()
        val ids=HashSet<String>(rows.size*2)
        rows.forEachIndexed {i,v->require(ids.add(key(i,v))){"Duplicate aggregate IDs"}}
        val oldRows=old?.let {list->HashMap<String,Pair<Int,T>>(list.size*2).also {map->list.forEachIndexed {i,v->map[key(i,v)]=i to v}}}
        val stored=oldRows?.keys ?: db.query("SELECT id FROM ${s.table}").map {it.single()!!}.toSet()
        stored.forEach {if(it !in ids)db.execute("DELETE FROM ${s.table} WHERE id=?",listOf(it))}
        rows.forEachIndexed {i,v->
            val id=key(i,v);val before=oldRows?.get(id)
            if(before==null || before.first!=i || (before.second!==v && before.second!=v)){
                val json=s.encode(v)
                val args=mutableListOf<String?>(id,i.toString(),json.toString())
                if(s.table=="entries"){val obj=json.jsonObject;args+=listOf(obj.getValue("date").jsonPrimitive.content,obj.getValue("type").jsonPrimitive.content,obj.getValue("source").jsonPrimitive.content,obj["deleted"]?.jsonPrimitive?.contentOrNull)}
                db.execute("INSERT OR REPLACE INTO ${s.table}(id,position,payload${if(s.table=="entries")",event_date,event_type,source,deleted"else ""}) VALUES(${args.joinToString(","){"?"}})",args)
            }
        }
    }
}