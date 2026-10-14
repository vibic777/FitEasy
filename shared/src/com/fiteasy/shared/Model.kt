package com.fiteasy.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }
fun newId() = UUID.randomUUID().toString()

@Serializable data class Nutrients(val kcal: Double? = null, val protein: Double? = null, val fat: Double? = null, val carbs: Double? = null, val fiber: Double? = null) {
    fun scaled(factor: Double) = Nutrients(kcal?.times(factor), protein?.times(factor), fat?.times(factor), carbs?.times(factor), fiber?.times(factor))
}
@Serializable data class Food(val id: String, val name: String, val kind: String, val category: String = "", val cuisine: String = "", val nutrients: Nutrients = Nutrients(), val source: String = "user", val barcode: String = "", val aliases: String = "", val basis: String = "g", val updated: Long = System.currentTimeMillis(), val deleted: Long? = null)
@Serializable data class Entry(val id: String = newId(), val date: String = LocalDate.now().toString(), val type: String, val name: String, val amount: Double, val unit: String, val nutrients: Nutrients = Nutrients(), val foodId: String? = null, val meal: String = "", val plainWater: Boolean = false, val source: String = "manual", val updated: Long = System.currentTimeMillis(), val deleted: Long? = null, val workout:WorkoutResult?=null,val distanceKm:Double?=null)
@Serializable data class Ingredient(val foodId: String, val grams: Double)
@Serializable data class Recipe(val id: String = newId(), val name: String, val ingredients: List<Ingredient>, val steps: String, val yieldGrams: Double, val portions: Int, val updated: Long = System.currentTimeMillis())
@Serializable data class Exercise(val name:String,val instructions:String="",val seconds:Int=30,val repetitions:Int=0,val restSeconds:Int=15,val sets:Int=1,val setRestSeconds:Int=15,val loadKg:Double?=null,val catalogueId:String?=null,val technique:String="",val rig:RigClip?=null)
@Serializable data class WorkoutResult(val routine:Routine,val completed:List<Int>,val skipped:List<Int>,val elapsedSeconds:Int,val finished:Boolean,val sets:List<WorkoutSetResult> = emptyList())
@Serializable data class Routine(val id: String = newId(), val name: String, val instructions: String, val minutes: Int, val updated: Long = System.currentTimeMillis(),val exercises:List<Exercise> = emptyList())
@Serializable data class Profile(val age: Int? = null, val heightCm: Double? = null, val female: Boolean = true, val activityFactor: Double = 1.2)
@Serializable data class Goals(val kcal: Double = 1800.0, val waterMl: Double = 2000.0, val allDrinks: Boolean = false, val steps: Int = 6000, val activityMinutes: Int = 20, val autoCalculate: Boolean = false, val autoApply: Boolean = false, val weightKg: Double? = null,val proteinG:Double?=null,val fatG:Double?=null,val carbsG:Double?=null,val disabled:Set<String> = emptySet(),val pausedUntil:String?=null)
@Serializable data class GoalRevision(val date: String, val goals: Goals)
@Serializable data class EnergyEstimate(val id:String=newId(),val date:String=LocalDate.now().toString(),val age:Int,val heightCm:Double,val weightKg:Double,val female:Boolean,val activityFactor:Double,val restingKcal:Double,val dailyKcal:Double,val applied:Boolean=false)
@Serializable data class Reminder(val id: String = newId(), val name: String, val time: String = "09:00", val weekdays: List<Int> = (1..7).toList(), val monthDays: List<Int> = emptyList(), val everyDays: Int = 1, val enabled: Boolean = true,val times:List<String> = emptyList(),val startDate:String=LocalDate.now().toString(),val endDate:String?=null,val quietStart:String?=null,val quietEnd:String?=null,val snoozeMinutes:Int=10,val comment:String="",val scheduleId:String?=null,val rowId:String?=null,val sound:Boolean=true,val soundKind:String="notification",val vibration:Boolean=true,val popup:Boolean=true,val requiresConfirmation:Boolean=false,val repeatUntilConfirmed:Boolean=false,val repeatCount:Int=0,val repeatMinutes:Int=5,
    /** Own melody (D097): an address the device understands, null = the system tone of soundKind. Device-local like the reminder itself. */
    val soundUri:String?=null,val volumePercent:Int=100,val rampSeconds:Int=0,
    /** The note this reminder belongs to (D103), as in Notio; null for a reminder on its own. The note itself is shared data, the link stays here. */
    val noteId:String?=null,
    /** When the person last changed it; 0 for one made before 0.4.29. A merge takes the reminder changed later (D113). */
    val updated:Long=0)
@Serializable data class SharedData(val version: Int = 1, val entries: List<Entry> = emptyList(), val products: List<Food> = emptyList(), val dishes: List<Food> = emptyList(), val recipes: List<Recipe> = emptyList(), val routines: List<Routine> = emptyList(), val profile: Profile = Profile(), val goals: Goals = Goals(), val goalHistory: List<GoalRevision> = emptyList(), val meals: List<String> = listOf("Завтрак", "Обед", "Ужин", "Перекусы"), val favorites: Set<String> = emptySet(),val estimates:List<EnergyEstimate> = emptyList(),val schedules:List<DaySchedule> = emptyList(),val notes:List<Note> = emptyList(),val noteFolders:List<NoteFolder> = emptyList(),val exerciseAssets:List<ExerciseAsset> = emptyList())
@Serializable data class Appearance(val language: String = "ru", val theme: String = "system", val accent: Long = 0xFF1B7F5C, val textScale: Float = 1f, val hiddenCards: Set<String> = emptySet(), val cardOrder: List<String> = listOf("nutrition", "water", "steps", "activity", "goals"), val volumeLiters: Boolean = true, val weightPounds: Boolean = false,val hiddenMetrics:Set<String> = emptySet(),val units:DisplayUnits=DisplayUnits(),val background:String?=null,val backgroundAlpha:Float=0.15f,val charts:Map<String,ChartStyle> = emptyMap(),val noteDisplay:NoteDisplay=NoteDisplay(),val style:String="calm",val animations:Boolean=false,val sectionBackgrounds:Map<String,SectionBackground> = emptyMap(),val backgroundImages:Map<String,String> = emptyMap())
@Serializable data class PhoneCounter(val date:String,val count:Long,val elapsed:Long,val boot:Int)
@Serializable data class LocalData(val appearance: Appearance = Appearance(), val reminders: List<Reminder> = emptyList(), val trashDays: Int = 30,val stepSource:String="auto",val phoneCounter:PhoneCounter?=null,val demo:Map<String,String> = emptyMap(),val legalNoticeVersion:Int=0,val noteTags:List<String> = emptyList(),
    /** Days the trash keeps things before removing them for good (D107); 0 = until the person empties it. Replaces the unused trashDays. */
    val trashRetentionDays:Int=0,
    /** Every reminder of this device is silent while this is on (D108); their own settings are kept for when it is turned off. */
    val remindersOff:Boolean=false)
@Serializable data class Snapshot(val shared: SharedData = SharedData(), val local: LocalData = LocalData())
@Serializable data class Backup(val format: String = "fiteasy", val version: Int = 5, val data: SharedData, val appearance: Appearance? = null)

interface Platform {
    val navigation:kotlinx.coroutines.flow.Flow<AppNavigation?> get()=kotlinx.coroutines.flow.emptyFlow()
    fun navigationHandled(token:String) {}
    val reminderChanged:kotlinx.coroutines.flow.Flow<Unit> get()=dataChanged
    val reminderOpen:kotlinx.coroutines.flow.Flow<String?> get()=kotlinx.coroutines.flow.emptyFlow()
    fun reminderOpened() {}
    fun reminderReadiness():ReminderReadiness?=null
    fun openReminderAccess(action:String) {}
    /** Applies a changed device-local notification switch at once. */
    fun notificationSettingsChanged() {}
    fun testAlarm() {}
    /** The rights the system gives this app and their state (D127), shown in Settings → Rights; empty where there are none. */
    fun rights():List<AppRight> = emptyList()
    /** Asks the system for the right, or opens the system page where it is given. */
    fun grantRight(id:String) {}
    /** Takes the right back. True: given up by the app itself, in force once the app closes; false: the system page was opened and the person switches it off there. */
    fun revokeRight(id:String):Boolean=false
    /** All rights at once: what the app can give up itself it gives up, and the system page of the app is opened for the rest. */
    fun revokeAllRights() {}
    /** Melody of a reminder (D097). Kinds of picker the device offers: "system" (its own sounds) and "file" (an audio file). */
    fun reminderSoundPickers():List<String> = emptyList()
    fun pickReminderSound(kind:String,current:String?,onResult:(String?)->Unit) {onResult(null)}
    fun reminderSoundTitle(uri:String):String=uri.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
    fun previewReminderSound(uri:String?,kind:String,volumePercent:Int) {}
    fun stopReminderPreview() {}
    /** Id of the reminder whose melody plays now, null when none. */
    val ringing:kotlinx.coroutines.flow.Flow<String?> get()=kotlinx.coroutines.flow.flowOf(null)
    fun stopRinging() {}
    fun stepMode():String="auto"
    fun setStepMode(mode:String) {}
    fun stepDiagnostics():String=""
    fun stepStatus():String="Steps come from Android by sync or backup"
    fun stepsEnabled():Boolean=false
    fun renderMode():String=""
    fun setRenderMode(mode:String) {}
    fun editingChanged(dirty:Boolean) {}
    /** True while a system file, sound or permission dialog opened from the app covers it; the app lock waits for it (D099). */
    fun pickerOpen():Boolean=false
    /** How this device names itself to paired devices until the person gives it a name. */
    fun deviceName():String="FitEasy $name"
    val name: String
    fun closeToTray():Boolean=readSecret("close-to-tray")!="false"
    fun setCloseToTray(enabled:Boolean){writeSecret("close-to-tray",enabled.toString())}
    fun load(): Snapshot
    fun save(snapshot: Snapshot)
    fun mutateSnapshot(transform:(Snapshot)->Snapshot):Snapshot=synchronized(this){transform(load()).also(::save)}
    /**
     * A save is checked and taken at once, and written to the disk by another thread (D125). When that write fails, what is
     * shown returns to what the disk holds ([dataChanged]) and this tells the person so.
     */
    val saveFailed:kotlinx.coroutines.flow.Flow<Unit> get()=kotlinx.coroutines.flow.emptyFlow()
    /** Waits until everything saved is on the disk; false when the last write failed. */
    fun flushSaves():Boolean=true
    fun readCatalogue(): List<Food>
    fun exportFile(name: String, mime: String, bytes: ByteArray)
    fun importFile(onResult: (ByteArray) -> Unit)
    fun launchFileTask(task:suspend ()->Unit)
    fun reminderStates():Map<String,ReminderDelivery>
    fun controlReminder(id:String,action:String)
    fun encryptionEnabled(): Boolean
    fun setEncryption(enabled: Boolean)
    fun databasePassword():String=""
    fun changeDatabasePassword(password:String){error("Unsupported platform")}
    fun exportDatabase(password:String):ByteArray=error("Unsupported platform")
    fun readDatabase(bytes:ByteArray,password:String):Snapshot=error("Unsupported platform")
    fun trackSteps(enabled: Boolean)
    fun readSecret(name: String): String?
    fun writeSecret(name: String, value: String?)
    fun temporaryFile(): java.io.File
    fun reportPdf(report:DiaryReport):ByteArray
    fun prepareBackground(bytes:ByteArray):ByteArray
    fun decodeBackground(bytes:ByteArray):androidx.compose.ui.graphics.ImageBitmap
    fun multicast(enabled: Boolean) {}
    val foreground: kotlinx.coroutines.flow.Flow<Boolean> get()=kotlinx.coroutines.flow.emptyFlow()
    fun wipeLocal()
    fun biometricAvailable():Boolean=false
    fun authenticateBiometric(onResult:(Boolean)->Unit){onResult(false)}
    val dataChanged:kotlinx.coroutines.flow.Flow<Unit> get()=kotlinx.coroutines.flow.emptyFlow()
    fun readHealthSteps(date:String,onResult:(String)->Unit){onResult("Health Connect доступен на Android")}
}

fun deviceIdentifier(platform:Platform)=platform.readSecret("device-id") ?: newId().also {platform.writeSecret("device-id",it)}
fun stepTotal(entries:List<Entry>,selection:String,deviceId:String):Double {
    val totals=entries.filter {it.type=="steps" && it.deleted==null && !it.source.startsWith("pending:")}.groupBy {it.source}.mapValues {(source,rows)->if(source=="manual")rows.sumOf {it.amount}else rows.maxOf {it.amount}}
    return when(selection){"auto"->totals["phone:$deviceId"] ?: totals.values.maxOrNull() ?: 0.0;"phone"->totals["phone:$deviceId"] ?: 0.0;else->totals[selection] ?: 0.0}
}

enum class ImportMode { MERGE, COPIES, REPLACE }
// Values are immutable, so one that passed is not checked again: a save of one new record does not re-read the whole diary (D102).
@Volatile private var validatedShared:SharedData?=null
@Volatile private var validatedEntries:List<Entry>?=null
@Volatile private var validatedLocal:LocalData?=null
@Volatile private var validatedAppearance:Appearance?=null
fun validate(data: SharedData) {
    if(data===validatedShared)return
    val checkEntries=data.entries!==validatedEntries
    require(data.noteFolders.size<=500 && data.noteFolders.map {it.id}.distinct().size==data.noteFolders.size)
    data.noteFolders.forEach {require(it.id.isNotBlank() && it.name.isNotBlank() && it.name.length<=512 && it.created in 0..NOTE_MAX_TIME && it.updated in 0..NOTE_MAX_TIME)}
    data.notes.forEach(::validateNote)
    require(data.notes.size<=2000 && data.notes.map {it.id}.distinct().size==data.notes.size)
    data.schedules.forEach(::validateSchedule)
    require(data.schedules.size<=1000 && data.schedules.map {it.id}.distinct().size==data.schedules.size)
    require(data.version == 1) { "Unsupported data version" }
    require(data.entries.size <= 200_000) { "Too many entries" }
    if(checkEntries){val ids=HashSet<String>(data.entries.size*2);data.entries.forEach {require(ids.add(it.id)) { "Duplicate entry IDs" }}}
    // An entry that already passed is not read again (D125): a save of one new record checks one record, not the whole diary.
    val fresh=if(!checkEntries)emptyList() else validatedEntries.let {known->
        if(known==null)data.entries else {val passed=java.util.IdentityHashMap<Entry,Boolean>(known.size*2);known.forEach {passed[it]=true};data.entries.filter {it !in passed}}
    }
    fresh.forEach { require(it.amount.isFinite() && it.amount >= 0); LocalDate.parse(it.date); listOf(it.nutrients.kcal, it.nutrients.protein, it.nutrients.fat, it.nutrients.carbs, it.nutrients.fiber).filterNotNull().forEach { n -> require(n.isFinite() && n >= 0) } }
    require(data.products.all { it.kind == "product" } && data.dishes.all { it.kind == "dish" })
    val foods = data.products + data.dishes
    require(foods.size <= 100_000 && foods.map { it.id }.distinct().size == foods.size) { "Duplicate catalogue IDs" }
    fun nutrients(n: Nutrients) { listOf(n.kcal,n.protein,n.fat,n.carbs,n.fiber).filterNotNull().forEach { require(it.isFinite() && it >= 0) { "Invalid nutrient" } } }
    foods.forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.basis in setOf("g","ml")); nutrients(it.nutrients) }
    fresh.forEach { require(it.id.isNotBlank() && it.name.isNotBlank()); require(it.type in setOf("food","drink","steps","activity","measurement")); require(it.unit in setOf("g","ml","steps","min","kg","cm")) }
    fresh.forEach {e->
        require(e.name.length<=4096 && e.id.length<=256 && e.meal.length<=256 && e.source.length<=256)
        require(when(e.type){"food"->e.unit=="g";"drink"->e.unit=="ml";"steps"->e.unit=="steps";"activity"->e.unit=="min";else->e.unit in setOf("kg","cm")})
        if(e.plainWater)require(e.type=="drink" && listOf(e.nutrients.kcal,e.nutrients.protein,e.nutrients.fat,e.nutrients.carbs,e.nutrients.fiber).filterNotNull().all {it==0.0})
        if(e.distanceKm!=null)require(e.type=="activity")
    }
    require(data.recipes.map { it.id }.distinct().size == data.recipes.size)
    data.recipes.forEach { r -> require(r.id.isNotBlank() && r.name.isNotBlank() && r.yieldGrams.isFinite() && r.yieldGrams > 0 && r.portions > 0); r.ingredients.forEach { require(it.foodId.isNotBlank() && it.grams.isFinite() && it.grams > 0) } }
    require(data.routines.map { it.id }.distinct().size == data.routines.size)
    fun routine(r:Routine){require(r.name.isNotBlank() && r.minutes in 1..240 && r.exercises.size<=100);r.exercises.forEach {e->require(e.name.isNotBlank() && e.seconds in 0..3600 && e.repetitions in 0..1000 && e.restSeconds in 0..600 && e.sets in 1..20 && e.setRestSeconds in 0..600 && (e.loadKg==null || e.loadKg.isFinite() && e.loadKg in 0.0..500.0) && (e.catalogueId?.length ?: 0)<=128 && e.technique.length<=128 && (e.seconds>0 || e.repetitions>0));e.rig?.let(::validateRig)}}
    data.routines.forEach(::routine)
    fresh.mapNotNull {it.distanceKm}.forEach {require(it.isFinite() && it>=0 && it<=10000)}
    fresh.mapNotNull {it.workout}.forEach {w->routine(w.routine);require(w.elapsedSeconds in 0..86400);require((w.completed+w.skipped).all {it in w.routine.exercises.indices});require(w.completed.distinct().size==w.completed.size && w.skipped.distinct().size==w.skipped.size && w.completed.intersect(w.skipped.toSet()).isEmpty());validateWorkoutSets(w.routine,w.sets);require(w.sets.sumOf {it.seconds}<=w.elapsedSeconds)}
    require(data.profile.age == null || data.profile.age in 18..120)
    require(data.profile.heightCm == null || (data.profile.heightCm.isFinite() && data.profile.heightCm in 80.0..250.0))
    require(data.profile.activityFactor.isFinite() && data.profile.activityFactor in 1.0..2.5)
    require(data.goals.steps >= 0 && data.goals.activityMinutes >= 0)
    require(data.goals.weightKg == null || (data.goals.weightKg.isFinite() && data.goals.weightKg > 0))
    require(data.estimates.size<=100_000 && data.estimates.map {it.id}.distinct().size==data.estimates.size)
    data.estimates.forEach {e->LocalDate.parse(e.date);require(e.age in 19..78 && e.heightCm.isFinite() && e.weightKg.isFinite() && e.activityFactor.isFinite() && e.restingKcal.isFinite() && e.dailyKcal.isFinite());require(e.restingKcal>0 && e.dailyKcal>0)}
    fun goals(g:Goals){
        require(g.kcal.isFinite() && g.kcal>0 && g.waterMl.isFinite() && g.waterMl>0 && g.steps>=0 && g.activityMinutes>=0)
        listOf(g.weightKg,g.proteinG,g.fatG,g.carbsG).filterNotNull().forEach {require(it.isFinite() && it>0)}
        require(g.disabled.all {it in setOf("energy","water","steps","activity","weight","protein","fat","carbs")})
        g.pausedUntil?.let {LocalDate.parse(it)}
    }
    require(data.exerciseAssets.size<=100 && data.exerciseAssets.map {it.id}.distinct().size==data.exerciseAssets.size)
    data.exerciseAssets.forEach {require(it.id.isNotBlank() && it.updated in 0..NOTE_MAX_TIME && (it.deleted==null || it.deleted in 0..NOTE_MAX_TIME));routine(Routine(name="Asset",instructions="",minutes=1,exercises=listOf(it.exercise)))}
    goals(data.goals);data.goalHistory.forEach {LocalDate.parse(it.date);goals(it.goals)}
    validatedEntries=data.entries;validatedShared=data
}
fun mergeData(local: SharedData, incoming: SharedData, mode: ImportMode): SharedData {
    validate(incoming)
    if (mode == ImportMode.REPLACE) return incoming
    if (mode == ImportMode.COPIES) {
        val folderIds=incoming.noteFolders.associate {it.id to newId()}
        val foods = incoming.products + incoming.dishes
        val ids = foods.associate { it.id to newId() }
        return local.copy(entries = local.entries + incoming.entries.map { it.copy(id = newId(), foodId = ids[it.foodId] ?: it.foodId) }, products = local.products + incoming.products.map { it.copy(id = ids.getValue(it.id)) }, dishes = local.dishes + incoming.dishes.map { it.copy(id = ids.getValue(it.id)) }, recipes = local.recipes + incoming.recipes.map { it.copy(id = ids[it.id] ?: newId(), ingredients = it.ingredients.map { i -> i.copy(foodId = ids[i.foodId] ?: i.foodId) }) }, routines = local.routines + incoming.routines.map { it.copy(id = newId()) },exerciseAssets=local.exerciseAssets+incoming.exerciseAssets.map {it.copy(id=newId())},estimates=local.estimates+incoming.estimates.map {it.copy(id=newId())},schedules=local.schedules+incoming.schedules.map(::copySchedule),notes=local.notes+incoming.notes.map {copyNote(it).copy(folderId=folderIds[it.folderId])},noteFolders=local.noteFolders+incoming.noteFolders.map {it.copy(id=folderIds.getValue(it.id))})
    }
    // Preserve local concurrent variants until explicit conflict review is implemented.
    fun <T> union(a: List<T>, b: List<T>, key: (T) -> String): List<T> = (a + b).distinctBy(key)
    return local.copy(entries = union(local.entries, incoming.entries) { it.id }, products = union(local.products, incoming.products) { it.id }, dishes = union(local.dishes, incoming.dishes) { it.id }, recipes = union(local.recipes, incoming.recipes) { it.id }, routines = union(local.routines, incoming.routines) { it.id }, exerciseAssets = (local.exerciseAssets+incoming.exerciseAssets).groupBy {it.id}.values.map {items->items.maxBy {it.updated}}, favorites = local.favorites + incoming.favorites,estimates=union(local.estimates,incoming.estimates){it.id},schedules=union(local.schedules,incoming.schedules){it.id},notes=union(local.notes,incoming.notes){it.id},noteFolders=union(local.noteFolders,incoming.noteFolders){it.id})
}
/**
 * A built-in record whose source gives an impossible value — a nutrient below zero, as "carbohydrate by difference" can be —
 * keeps that value as unknown. Without this one such record made every catalogue export, backup or transfer that included it fail (D106).
 */
fun cleanFood(food:Food):Food {
    val n=food.nutrients
    fun known(v:Double?)=v?.takeIf {it.isFinite() && it>=0}
    return if(listOf(n.kcal,n.protein,n.fat,n.carbs,n.fiber).all {it==null || (it.isFinite() && it>=0)})food else food.copy(nutrients=Nutrients(known(n.kcal),known(n.protein),known(n.fat),known(n.carbs),known(n.fiber)))
}
fun materializeDependencies(data:SharedData,stock:List<Food>):SharedData {
    val known=(data.products+data.dishes).map {it.id}.toSet()
    val referenced=data.entries.mapNotNull {it.foodId}.toSet()+data.recipes.flatMap {r->r.ingredients.map {it.foodId}}
    val required=stock.filter {it.id in referenced && it.id !in known}.distinctBy {it.id}.map(::cleanFood)
    return data.copy(products=data.products+required.filter {it.kind=="product"},dishes=data.dishes+required.filter {it.kind=="dish"})
}
fun recipeNutrients(recipe: Recipe, foods: List<Food>): Nutrients {
    require(recipe.yieldGrams > 0 && recipe.yieldGrams.isFinite() && recipe.portions > 0)
    val parts = recipe.ingredients.map { i -> require(i.grams > 0 && i.grams.isFinite()); foods.first { it.id == i.foodId }.nutrients.scaled(i.grams / 100) }
    fun sum(field: (Nutrients) -> Double?): Double? = if (parts.isEmpty() || parts.any { field(it) == null }) null else parts.sumOf { field(it)!! }
    return Nutrients(sum { it.kcal }, sum { it.protein }, sum { it.fat }, sum { it.carbs }, sum { it.fiber })
}
fun csv(data: SharedData, from: String, to: String): String {
    fun cell(s: String) = "\"" + s.replace("\"", "\"\"").let { if (it.startsWith("=") || it.startsWith("+") || it.startsWith("-") || it.startsWith("@")) "'" + it else it } + "\""
    return "date,type,name,amount,unit,kcal,protein,fat,carbs\r\n" + data.entries.filter { it.deleted == null && it.date >= from && it.date <= to }.joinToString("\r\n") { e -> listOf(e.date, e.type, e.name, e.amount.toString(), e.unit, e.nutrients.kcal?.toString() ?: "", e.nutrients.protein?.toString() ?: "", e.nutrients.fat?.toString() ?: "", e.nutrients.carbs?.toString() ?: "").joinToString(",") { cell(it) } }
}

fun goalActive(goals:Goals,id:String,date:String)=id !in goals.disabled && (goals.pausedUntil==null || date>goals.pausedUntil)

fun validateLocal(local:LocalData){
    if(local===validatedLocal)return
    require(local.legalNoticeVersion in 0..100000){"Invalid legal notice version"}
    require(local.trashRetentionDays in trashRetentionOptions){"Invalid trash retention"}
    require(local.noteTags.size<=500 && local.noteTags.all(::validTag) && local.noteTags.map {it.lowercase()}.distinct().size==local.noteTags.size){"Invalid note tags"}
    require(local.demo.size<=500 && local.demo.all {(key,hash)->key.matches(Regex("(entry|note|schedule|reminder):[a-zA-Z0-9-]{1,128}")) && hash.matches(Regex("[a-f0-9]{64}"))})
    val a=local.appearance
    require(a.charts.size<=8 && a.charts.all {(id,style)->id in chartMetrics && style.kind in chartKinds && style.color in 0xFF000000L..0xFFFFFFFFL})
    require(a.noteDisplay.view in setOf("cards","list","grid"))
    require(a.style in appearanceStyles)
    require(a.sectionBackgrounds.size<=appearanceSections.size && a.sectionBackgrounds.keys.all {it in appearanceSections})
    require(a.backgroundImages.size<=appearanceSections.size)
    var imageBytes=0L
    if(a!==validatedAppearance)a.backgroundImages.forEach {(id,image)->
        require(id.matches(Regex("[a-f0-9]{64}")) && image.length<=1400000)
        val bytes=java.util.Base64.getDecoder().decode(image)
        require(bytes.size<=1048576)
        require(java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==id)
        imageBytes+=bytes.size
    }
    require(imageBytes<=8L*1048576)
    a.sectionBackgrounds.values.forEach {b->require(b.color==null || b.color in 0xFF000000L..0xFFFFFFFFL);require(b.alpha.isFinite() && b.alpha in 0f..0.35f);require(b.image==null || b.image=="legacy" || b.image in a.backgroundImages)}
    validateUnits(a.units)
    require(a.backgroundAlpha.isFinite() && a.backgroundAlpha in 0f..0.35f && (a.background?.length ?: 0)<=1400000)
    if(a!==validatedAppearance)a.background?.let {require(java.util.Base64.getDecoder().decode(it).size<=1048576)}
    require(a.language in languageNames.keys && a.theme in setOf("system","light","dark"))
    require(a.textScale.isFinite() && a.textScale in 0.5f..3f && a.accent in 0..0xFFFFFFFFL)
    val cards=setOf("nutrition","water","steps","activity","goals")
    require(a.cardOrder.distinct().size==a.cardOrder.size && a.cardOrder.all {it in cards} && a.hiddenCards.all {it in cards})
    require(local.trashDays in 0..3650 && local.reminders.size<=500 && local.reminders.map {it.id}.distinct().size==local.reminders.size)
    local.reminders.forEach(::validateReminder)
    val bindings=local.reminders.filter {it.scheduleId!=null}.map {it.scheduleId to it.rowId}
    require(bindings.distinct().size==bindings.size)
    require(local.stepSource.length<=128)
    local.phoneCounter?.let {LocalDate.parse(it.date);require(it.count>=0 && it.elapsed>=0 && it.boot>=0)}
    validatedAppearance=a;validatedLocal=local
}
