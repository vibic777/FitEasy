package com.fiteasy.shared.sync

import com.fiteasy.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.InetSocketAddress
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64

val syncSections=setOf("nutrition","water","activity","progress","catalogues","recipes","profile","goals","schedules","notes","reminders")
/** [reminders] travel only when the person chose that section (D108); null = not carried, the receiver keeps its own. */
@Serializable data class Transfer(val format:String="fiteasy-lan",val version:Int=5,val sections:Set<String>,val data:SharedData,val reminders:List<Reminder>?=null)
enum class Direction { SEND, RECEIVE, BOTH }
enum class ConflictPolicy { LOCAL, INCOMING }
@Serializable data class TrustedPeer(val deviceId:String,val name:String,val secret:String,val sections:Set<String> = syncSections,val mode:ImportMode=ImportMode.MERGE,val policy:ConflictPolicy=ConflictPolicy.LOCAL)

/** The section a diary record travels with. */
fun entrySection(e:Entry)=when(e.type){"food"->"nutrition";"drink"->"water";"steps","activity"->"activity";else->"progress"}
fun selected(data:SharedData,sections:Set<String>):Transfer {
    require(sections.isNotEmpty() && syncSections.containsAll(sections))
    val entries=data.entries.filter {entrySection(it) in sections}
    val recipes=if("recipes" in sections)data.recipes else emptyList()
    val ids=entries.mapNotNull {it.foodId}.toSet()+recipes.flatMap {it.ingredients.map {i->i.foodId}}
    return Transfer(sections=sections,data=data.copy(entries=entries,noteFolders=if("notes" in sections)data.noteFolders else emptyList(),notes=if("notes" in sections)data.notes else emptyList(),schedules=if("schedules" in sections)data.schedules else emptyList(),
        products=data.products.filter {"catalogues" in sections || it.id in ids},dishes=data.dishes.filter {"catalogues" in sections || it.id in ids},
        exerciseAssets=if("activity" in sections)data.exerciseAssets else emptyList(),recipes=recipes,routines=if("activity" in sections)data.routines else emptyList(),
        profile=if("profile" in sections)data.profile else Profile(),goals=if("goals" in sections)data.goals else Goals(),
        goalHistory=if("goals" in sections)data.goalHistory else emptyList(),estimates=if("goals" in sections)data.estimates else emptyList(),favorites=if("catalogues" in sections)data.favorites else emptySet()))
}
fun applyTransfer(local:SharedData,incoming:Transfer,mode:ImportMode,policy:ConflictPolicy):SharedData {
    require(incoming.format=="fiteasy-lan" && incoming.version==5 && incoming.sections.isNotEmpty() && syncSections.containsAll(incoming.sections))
    require(mode!=ImportMode.REPLACE || incoming.sections==syncSections)
    if(mode==ImportMode.REPLACE)return incoming.data.also(::validate)
    val merged=if(mode==ImportMode.MERGE && policy==ConflictPolicy.INCOMING)mergeData(incoming.data,local,mode)else mergeData(local,incoming.data,mode)
    return merged.copy(profile=if("profile" in incoming.sections && (mode==ImportMode.REPLACE || policy==ConflictPolicy.INCOMING))incoming.data.profile else local.profile,
        goals=if("goals" in incoming.sections && (mode==ImportMode.REPLACE || policy==ConflictPolicy.INCOMING))incoming.data.goals else local.goals,
        goalHistory=if("goals" in incoming.sections)(local.goalHistory+incoming.data.goalHistory).distinct()else local.goalHistory,
        meals=if("nutrition" in incoming.sections)(local.meals+incoming.data.meals).distinct()else local.meals).also(::validate)
}

/** What one import changed: records added, records replaced by a newer version, records left as they were. */
data class SyncCounts(val added:Int=0,val updated:Int=0,val kept:Int=0)

/**
 * Applies a received transfer the way Notio's import does (D105). MERGE: records are matched by id and the one changed
 * later wins, so a correction or a deletion made on either device arrives on the other; what is missing is added. COPIES:
 * everything received is added under new ids. REPLACE: the received sections become what was received, the others stay (D113).
 * The single profile and goals have no time of change: in MERGE they are taken only while this device still has the defaults.
 */
fun applySync(local:SharedData,incoming:Transfer,mode:ImportMode):Pair<SharedData,SyncCounts> {
    require(incoming.format=="fiteasy-lan" && incoming.version==5 && incoming.sections.isNotEmpty() && syncSections.containsAll(incoming.sections))
    val data=incoming.data;validate(data)
    fun total(d:SharedData)=d.entries.size+d.products.size+d.dishes.size+d.recipes.size+d.routines.size+d.schedules.size+d.notes.size+d.noteFolders.size+d.exerciseAssets.size+d.estimates.size
    if(mode==ImportMode.REPLACE)return replaceSections(local,data,incoming.sections) to SyncCounts(added=total(data))
    if(mode==ImportMode.COPIES)return mergeData(local,data,ImportMode.COPIES).also(::validate) to SyncCounts(added=total(data))
    var added=0;var updated=0;var kept=0
    fun <T> newest(mine:List<T>,theirs:List<T>,id:(T)->String,changed:((T)->Long)?):List<T> {
        if(theirs.isEmpty())return mine
        val rows=LinkedHashMap<String,T>(mine.size*2);mine.forEach {rows[id(it)]=it}
        theirs.forEach {row->val old=rows[id(row)];when {
            old==null->{rows[id(row)]=row;added++}
            changed!=null && changed(row)>changed(old)->{rows[id(row)]=row;updated++}
            else->kept++
        }}
        return rows.values.toList()
    }
    val merged=local.copy(
        entries=newest(local.entries,data.entries,{it.id}){it.updated},products=newest(local.products,data.products,{it.id}){it.updated},dishes=newest(local.dishes,data.dishes,{it.id}){it.updated},
        recipes=newest(local.recipes,data.recipes,{it.id}){it.updated},routines=newest(local.routines,data.routines,{it.id}){it.updated},schedules=newest(local.schedules,data.schedules,{it.id}){it.updated},
        notes=newest(local.notes,data.notes,{it.id}){it.updated},noteFolders=newest(local.noteFolders,data.noteFolders,{it.id}){it.updated},exerciseAssets=newest(local.exerciseAssets,data.exerciseAssets,{it.id}){it.updated},
        estimates=newest(local.estimates,data.estimates,{it.id},null),favorites=local.favorites+data.favorites,
        profile=if("profile" in incoming.sections && local.profile==Profile())data.profile else local.profile,
        goals=if("goals" in incoming.sections && local.goals==Goals())data.goals else local.goals,
        goalHistory=if("goals" in incoming.sections)(local.goalHistory+data.goalHistory).distinct()else local.goalHistory,
        meals=if("nutrition" in incoming.sections)(local.meals+data.meals).distinct()else local.meals)
    return merged.also(::validate) to SyncCounts(added,updated,kept)
}

/**
 * Replacement of the chosen sections only (D113): in each of them this device gets exactly what was received, the other
 * sections stay as they are; with every section chosen the shared data become what was received, as before. Products and
 * dishes are kept while a record that stays here still names them, and the ones the received records name are added.
 */
fun replaceSections(local:SharedData,data:SharedData,sections:Set<String>):SharedData {
    if(sections==syncSections)return data
    fun <T> pick(section:String,mine:T,theirs:T)=if(section in sections)theirs else mine
    val arrived=data.entries.filter {entrySection(it) in sections}
    val arrivedIds=arrived.map {it.id}.toSet()
    val entries=local.entries.filter {entrySection(it) !in sections && it.id !in arrivedIds}+arrived
    val recipes=pick("recipes",local.recipes,data.recipes)
    val named=entries.mapNotNull {it.foodId}.toSet()+recipes.flatMap {r->r.ingredients.map {it.foodId}}
    val mineIds=(local.products+local.dishes).map {it.id}.toSet();val theirIds=(data.products+data.dishes).map {it.id}.toSet()
    fun foods(mine:List<Food>,theirs:List<Food>)=if("catalogues" in sections)theirs+mine.filter {it.id !in theirIds && it.id in named} else mine+theirs.filter {it.id !in mineIds}
    return local.copy(entries=entries,products=foods(local.products,data.products),dishes=foods(local.dishes,data.dishes),recipes=recipes,
        routines=pick("activity",local.routines,data.routines),exerciseAssets=pick("activity",local.exerciseAssets,data.exerciseAssets),
        profile=pick("profile",local.profile,data.profile),goals=pick("goals",local.goals,data.goals),goalHistory=pick("goals",local.goalHistory,data.goalHistory),estimates=pick("goals",local.estimates,data.estimates),
        schedules=pick("schedules",local.schedules,data.schedules),notes=pick("notes",local.notes,data.notes),noteFolders=pick("notes",local.noteFolders,data.noteFolders),
        meals=pick("nutrition",local.meals,data.meals),favorites=pick("catalogues",local.favorites,data.favorites)).also(::validate)
}

/**
 * Reminders received with a transfer. MERGE adds the ones this device does not have and takes the one changed later
 * ([Reminder.updated], D113); COPIES adds them under new ids; REPLACE makes the list what was received. A schedule row
 * keeps one reminder at most. A melody is a file of this device: a reminder that comes again keeps the one it has here.
 */
fun mergeReminders(mine:List<Reminder>,theirs:List<Reminder>?,mode:ImportMode):List<Reminder> {
    theirs ?: return mine
    theirs.forEach(::validateReminder)
    val own=mine.associateBy {it.id}
    fun here(r:Reminder)=r.copy(soundUri=own[r.id]?.soundUri)
    if(mode==ImportMode.REPLACE)return theirs.distinctBy {it.id}.map(::here)
    fun free(r:Reminder,taken:Collection<Reminder>)=r.scheduleId==null || taken.none {it.id!=r.id && it.scheduleId==r.scheduleId && it.rowId==r.rowId}
    val result=LinkedHashMap<String,Reminder>(mine.size*2);mine.forEach {result[it.id]=it}
    theirs.forEach {r->
        val candidate=if(mode==ImportMode.COPIES)r.copy(id=newId()) else r
        val old=result[candidate.id]
        if(free(candidate,result.values) && (old==null || (mode==ImportMode.MERGE && candidate.updated>old.updated)))result[candidate.id]=here(candidate)
    }
    return result.values.take(500)
}

/** What the sync engine moves for FitEasy: the chosen sections out, a received transfer in, saved in one step. */
class FitSyncData(private val platform:Platform,private val afterImport:()->Unit={}):SyncData {
    override suspend fun folders():List<FolderInfo> = syncSections.map {FolderInfo(it,it)}
    override suspend fun export(out:java.io.OutputStream,folderIds:Set<String>?) {
        val snapshot=platform.load();val sections=folderIds ?: syncSections
        val shared=materializeDependencies(snapshot.shared,platform.readCatalogue())
        // A melody is a file or an address of this device only: the reminder travels, its melody becomes the system tone there.
        val reminders=if("reminders" in sections)snapshot.local.reminders.map {it.copy(soundUri=null)} else null
        out.write(codec.encodeToString(selected(shared,sections).copy(reminders=reminders)).toByteArray())
    }
    override suspend fun import(file:java.io.File,mode:ImportMode):List<Int> {
        val transfer=codec.decodeFromString<Transfer>(file.readText())
        var counts=SyncCounts()
        platform.mutateSnapshot {current->
            val (next,changed)=applySync(current.shared,transfer,mode)
            val before=current.local.reminders.associateBy {it.id}
            val reminders=mergeReminders(current.local.reminders,transfer.reminders,mode)
            counts=if(mode==ImportMode.REPLACE)changed.copy(added=changed.added+(transfer.reminders?.size ?: 0))
                else changed.copy(added=changed.added+reminders.count {it.id !in before},updated=changed.updated+reminders.count {r->before[r.id]?.let {it!=r}==true})
            current.copy(shared=next,local=current.local.copy(reminders=reminders))
        }
        afterImport()
        return listOf(0,0,counts.added,counts.updated,counts.kept,0)
    }
}