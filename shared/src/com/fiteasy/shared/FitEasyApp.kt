package com.fiteasy.shared

import androidx.compose.ui.Alignment
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

fun number(n: Double?) = n?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—"
private fun parse(s: String) = s.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

@Composable fun FitEasyContent(platform: Platform,onSecurity:()->Unit={},onSync:()->Unit={},onLock:(()->Unit)?=null,onLanguageChanged:(String)->Unit={}) {
    var initial by remember(platform){mutableStateOf<Pair<Snapshot,String>?>(null)}
    var error by remember(platform){mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    LaunchedEffect(platform,retry){error=false;runCatching {withContext(Dispatchers.IO){platform.load() to deviceIdentifier(platform)}}.onSuccess {initial=it}.onFailure {AppJournal.record("ui.load.failed",JournalLevel.ERROR,it);error=true}}
    val loaded=initial
    if(loaded==null){Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){if(error)Button(onClick={retry++}){Text(translate("Retry",LocalLanguage.current))}else CircularProgressIndicator()};return}
    LoadedFitEasyContent(platform,loaded.first,loaded.second,onSecurity,onSync,onLock,onLanguageChanged)
}

@Composable private fun LoadedFitEasyContent(platform:Platform,initial:Snapshot,ownDeviceId:String,onSecurity:()->Unit,onSync:()->Unit,onLock:(()->Unit)?,onLanguageChanged:(String)->Unit) {
    var snapshot by remember(platform) {mutableStateOf(initial)}
    var readFailed by remember {mutableStateOf(false)}
    val foreground by platform.foreground.collectAsState(initial=true)
    LaunchedEffect(platform,foreground){if(foreground){withContext(Dispatchers.IO){runCatching {platform.load()}}.onSuccess {snapshot=it;readFailed=false}.onFailure {AppJournal.record("ui.refresh.failed",JournalLevel.ERROR,it);readFailed=true};platform.dataChanged.conflate().collect {withContext(Dispatchers.IO){runCatching {platform.load()}}.onSuccess {snapshot=it;readFailed=false}.onFailure {AppJournal.record("ui.refresh.failed",JournalLevel.ERROR,it);readFailed=true}}}}
    var stock by remember(platform){mutableStateOf(emptyList<Food>())}
    var catalogueFailed by remember {mutableStateOf(false)}
    var catalogueRetry by remember {mutableIntStateOf(0)}
    LaunchedEffect(platform,catalogueRetry){withContext(Dispatchers.IO){runCatching {platform.readCatalogue().map(::cleanFood)}}.onSuccess {stock=it;catalogueFailed=false}.onFailure {AppJournal.record("catalogue.load.failed",JournalLevel.ERROR,it);catalogueFailed=true}}
    val foods = remember(snapshot.shared.products, snapshot.shared.dishes,stock) { (snapshot.shared.products + snapshot.shared.dishes + stock).distinctBy {it.id} }
    var screen by remember { mutableStateOf("today") }
    val contentScroll=rememberScrollState()
    var progressPeriod by remember {mutableStateOf("week")}
    var progressForward by remember {mutableStateOf(false)}
    var progressYears by remember {mutableIntStateOf(3)}
    var progressFrom by remember {mutableStateOf(LocalDate.now().minusMonths(1).toString())}
    var progressTo by remember {mutableStateOf(LocalDate.now().toString())}
    var progressPeriodEditor by remember {mutableStateOf(false)}
    var calendarTarget by remember {mutableStateOf("anchor")}
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("product") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var activeWorkout by remember {mutableStateOf<WorkoutSessionState?>(null)}
    var pendingWorkout by remember {mutableStateOf<WorkoutSessionState?>(null)}
    var workoutDraftReady by remember {mutableStateOf(false)}
    var workoutDraftError by remember {mutableStateOf(false)}
    LaunchedEffect(platform){withContext(Dispatchers.IO){runCatching {WorkoutDraftStore.recoverable(platform)}}.onSuccess {pendingWorkout=it;workoutDraftReady=true}.onFailure {AppJournal.record("workout.draft.read.failed",JournalLevel.ERROR,it);workoutDraftError=true}}
    var routineEditor by remember {mutableStateOf(false)}
    var exerciseLibrary by remember {mutableStateOf(false)}
    var editRoutine by remember {mutableStateOf<Routine?>(null)}
    var reminderEditor by remember {mutableStateOf(false)}
    var reminderOffHint by remember {mutableStateOf(false)}
    // The help opened from a hint (D117): the text of the hint, shown over whatever page is open, which stays as it was.
    var helpAt by remember {mutableStateOf<String?>(null)}
    val openHelp=remember {{hint:String->helpAt=hint}}
    var scheduleHub by remember {mutableStateOf(false)}
    var focusedReminder by remember {mutableStateOf<String?>(null)}
    var focusedReminderEditor by remember {mutableStateOf(true)}
    // The trash empties itself only after the days the person chose (D107); checked once when the app opens.
    LaunchedEffect(platform){withContext(Dispatchers.IO){runCatching {platform.mutateSnapshot(::purgeExpiredTrash)}}.onSuccess {if(it.shared!=snapshot.shared)snapshot=it}.onFailure {AppJournal.record("trash.purge.failed",JournalLevel.WARN,it)}}
    var focusNote by remember {mutableStateOf<String?>(null)}
    // A reminder that belongs to a note opens that note (D103); any other opens the list of reminders.
    LaunchedEffect(platform){platform.reminderOpen.collect {id->if(id!=null){
        val note=snapshot.local.reminders.firstOrNull {it.id==id}?.noteId?.takeIf {n->snapshot.shared.notes.any {it.id==n && it.deleted==null}}
        if(note!=null){focusNote=note;screen="notes"}else {focusedReminder=id.takeUnless {it=="status"};scheduleHub=true}
        platform.reminderOpened()
    }}}
    var calendar by remember {mutableStateOf(false)}
    var demoRemoval by remember {mutableStateOf(false)}
    var demoForce by remember {mutableStateOf(false)}
    var demoCreate by remember {mutableStateOf(false)}
    var resetDialog by remember {mutableStateOf(false)}
    var settingsSection by remember {mutableStateOf("")}
    var measurementRecords by remember {mutableStateOf<String?>(null)}
    var measurementRange by remember {mutableStateOf<ProgressRange?>(null)}
    LaunchedEffect(screen,settingsSection) {AppJournal.record("ui.screen.$screen.$settingsSection");contentScroll.scrollTo(0)}
    var chartEditor by remember {mutableStateOf<String?>(null)}
    var journal by remember {mutableStateOf(false)}
    var databaseTools by remember {mutableStateOf(false)}
    var backgroundEditor by remember {mutableStateOf(false)}
    var accentEditor by remember {mutableStateOf(false)}
    var noteStampSettings by remember {mutableStateOf(false)}
    var goalsEditor by remember {mutableStateOf(false)}
    var reportEditor by remember {mutableStateOf(false)}
    var unitsEditor by remember {mutableStateOf(false)}
    var renderMode by remember {mutableStateOf(platform.renderMode())}
    var closeToTray by remember {mutableStateOf(platform.closeToTray())}
    var editingReminder by remember {mutableStateOf<Reminder?>(null)}
    var picked by remember { mutableStateOf<Food?>(null) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var field1 by remember { mutableStateOf("") }; var field2 by remember { mutableStateOf("") }; var field3 by remember { mutableStateOf("") }
    var field4 by remember { mutableStateOf("") }; var field5 by remember { mutableStateOf("") }; var field6 by remember { mutableStateOf("") }
    var option by remember { mutableStateOf(false) }; var option2 by remember { mutableStateOf(false) }
    var recipeLines by remember { mutableStateOf<List<Ingredient>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var lastSaveSucceeded by remember {mutableStateOf(true)}
    var legalView by remember {mutableStateOf(false)}
    // A section shows only what matters day to day; the rest opens from its menu on the whole screen (D092).
    var subPage by remember {mutableStateOf("")}
    LaunchedEffect(screen){subPage=""}
    var economy by remember {mutableStateOf(economyEnabled(platform))}
    val fileUiScope=rememberCoroutineScope()
    var estimate by remember { mutableStateOf<EnergyEstimate?>(null) }; var autoApply by remember { mutableStateOf(false) }; var running by remember { mutableStateOf(false) }; var seconds by remember { mutableStateOf(0) }
    val local = snapshot.local; val data = snapshot.shared
    val currentLanguage=LocalLanguage.current
    fun mutate(transform:(Snapshot)->Snapshot):Boolean {
        return runCatching {
            snapshot=platform.mutateSnapshot(transform);AppJournal.record("ui.save.success")
        }.onFailure {AppJournal.record("ui.save.failed",JournalLevel.ERROR,it);message="Ошибка сохранения: "+(it.message ?: "")}.isSuccess.also {lastSaveSucceeded=it}
    }
    fun update(next:Snapshot):Boolean {
        val base=snapshot
        return mutate {current->
            val shared=com.fiteasy.shared.sync.preserveConcurrent(base.shared,current.shared,next.shared).first
            next.copy(shared=shared,local=next.local.copy(phoneCounter=current.local.phoneCounter,stepSource=if(next.local.stepSource==base.local.stepSource)current.local.stepSource else next.local.stepSource))
        }
    }
    // A save is taken at once and written by another thread (D125); when the disk refuses it, the person is told.
    LaunchedEffect(platform) {platform.saveFailed.collect {message="Последнее изменение не записано на диск. Проверьте свободное место и повторите."}}
    fun change(next: SharedData) = update(snapshot.copy(shared = next))
    fun appearance(next: Appearance) {
        if(!update(snapshot.copy(local = snapshot.local.copy(appearance = next))))return
        if(next.language!=currentLanguage) {
            platform.writeSecret("language",next.language);onLanguageChanged(next.language)
        }
    }
    fun open(id: String, a: String = "", b: String = "", c: String = "") { AppJournal.record("ui.dialog.$id");field1 = a; field2 = if(id=="drink" && b.isNotEmpty())displayAmount(b.toDouble(),"ml",snapshot.local.appearance.units).toString()else b; field3 = c; field4 = ""; field5 = ""; field6 = ""; option = false; option2 = false; editing = null; dialog = id }
    LaunchedEffect(platform) {platform.navigation.collect {request->if(request!=null){
        appDestinations[request.destination]?.let {target->
            dialog=null;scheduleHub=false;calendar=false;reminderEditor=false;exerciseLibrary=false;routineEditor=false
            screen=target.screen;settingsSection=target.settings;query="";contentScroll.scrollTo(0)
            focusedReminderEditor=target.dialog!="reminder-status"
            when(target.dialog){"calendar"->{calendarTarget="anchor";calendar=true};"schedules","reminder-status"->{focusedReminder=request.reminderId;scheduleHub=true};"help","about"->open(target.dialog)}
            AppJournal.record("ui.tray.${request.destination}")
        }
        platform.navigationHandled(request.token)
    }}}
    fun add(entry: Entry):Boolean { val saved=change(data.copy(entries = data.entries.filterNot { it.id == entry.id } + entry));if(saved)dialog=null;return saved }
    fun editEntry(entry:Entry){open("entry",entry.name,displayAmount(entry.amount,entry.unit,snapshot.local.appearance.units).toString(),entry.date);editing=entry}
    val displayGoals = remember(data.goalHistory,data.goals,date){data.goalHistory.filter {it.date<=date}.maxByOrNull {it.date}?.goals ?: data.goals}
    val entries = remember(data.entries,date){data.entries.filter { it.date == date && it.deleted == null }}
    val energy = remember(entries){entries.sumOf { it.nutrients.kcal ?: 0.0 }}
    val water = remember(entries){entries.filter { it.type == "drink" && it.plainWater }.sumOf { it.amount }}
    val drinks = remember(entries){entries.filter { it.type == "drink" }.sumOf { it.amount }}
    val steps = remember(entries,local.stepSource,ownDeviceId){stepTotal(entries,local.stepSource,ownDeviceId)}
    val active = remember(entries){entries.filter { it.type == "activity" }.sumOf { it.amount }}
    val app = local.appearance
    val dark = app.theme == "dark" || (app.theme == "system" && isSystemInDarkTheme())
    val colors = remember(app.accent,dark){fitColorScheme(app.accent,dark)}
    var timerStart by remember {mutableLongStateOf(0L)}
    var timerBase by remember {mutableIntStateOf(0)}
    LaunchedEffect(running){if(running){timerBase=seconds;timerStart=System.nanoTime()}}
    LaunchedEffect(running,foreground){if(running && foreground){while(true){seconds=timerBase+((System.nanoTime()-timerStart)/1_000_000_000L).toInt();delay(1000)}}}
    LaunchedEffect(data.profile,data.entries,data.goals.autoCalculate,data.goals.autoApply,data.goals.disabled,data.goals.pausedUntil) {
        if(data.goals.autoCalculate && goalActive(data.goals,"energy",LocalDate.now().toString()))runCatching {energyEstimate(data)}.onSuccess {e ->
            if(data.estimates.lastOrNull()?.let {sameInputs(it,e)}!=true){
                if(data.goals.autoApply)change(applyEstimate(data,e))
                else {estimate=e;change(data.copy(estimates=data.estimates+e));dialog="calculator"}
            }
        }
    }
    FitTheme(app.copy(animations=app.animations && foreground && !economy),colors,exerciseMotion=app.animations && foreground,economy=economy) {
        if(needsLegalNotice(local)){
            LegalNoticeScreen(app.language,onLanguage={appearance(app.copy(language=it))},onContinue={
                val result=withContext(Dispatchers.IO){runCatching {platform.mutateSnapshot(::acknowledgeLegalNotice)}}
                result.onSuccess {snapshot=it;AppJournal.record("legal.notice.acknowledged.v$LEGAL_NOTICE_VERSION")}
                    .onFailure {AppJournal.record("legal.notice.save.failed",JournalLevel.ERROR,it)}.isSuccess
            })
            return@FitTheme
        }
        CompositionLocalProvider(LocalHelpOpener provides openHelp) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val section=if(screen=="profile" && settingsSection.isNotEmpty())"settings"else screen
                SectionBackdrop(platform,app,section,Modifier.fillMaxSize()) {
                val wide = maxWidth > 700.dp
                val titled=LocalWindowBar.current.height>0.dp
                val edge=if (wide) 24.dp else 12.dp
                // On Windows the header is the window title bar, so it starts at the very top edge.
                Column(Modifier.fillMaxSize().padding(start=edge,end=edge,top=if(titled)0.dp else edge,bottom=edge)) {
                    val settings=screen=="profile" || subPage.isNotEmpty()
                    WindowBarRow {
                        // Settings take the whole screen, as in Notio: a back arrow and the page title, nothing else.
                        if(settings){
                            IconAction("previous","Назад",onClick={if(subPage.isNotEmpty())subPage="" else if(settingsSection.isNotEmpty())settingsSection="" else screen="today"})
                            Text(when(subPage){"catalog"->"Продукты и блюда";"drinks"->"Записи напитков";else->settingsTitle(settingsSection)},Modifier.weight(1f).padding(start=8.dp),style=MaterialTheme.typography.titleLarge)
                        }else {
                        Column(Modifier.weight(1f)) { Text("FitEasy", fontSize = if(titled)22.sp else 28.sp, color = MaterialTheme.colorScheme.primary); Text("Маленькие шаги каждый день", style = MaterialTheme.typography.bodySmall) }
                        // Synchronization is one tap away, beside the profile; the settings menu keeps its own entry (D114).
                        // Reminders are one tap away too, as the alarm clock in Notio's top bar (D116).
                        RigAction("bell",translate("Напоминания",app.language),{screen="profile";settingsSection="reminders"},translated=false)
                        RigAction("loop",translate("Синхронизация",app.language),onSync,translated=false)
                        IconAction("profile","Профиль",onClick = { screen = "profile";settingsSection="" })
                        }
                    }
                    // One navigation at a time: a row on a phone, a rail on a wide window.
                    if(!settings && !wide)MainNavigationIcons(screen,onSelect={screen=it;query=""})
                    if(!settings)Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconAction("previous","Предыдущий день",onClick = { date = LocalDate.parse(date).minusDays(1).toString() })
                        TextButton(onClick = { calendar=true }) { Text(date) }
                        IconAction("next","Следующий день",onClick = { date = LocalDate.parse(date).plusDays(1).toString() })
                    }
                    if(readFailed)Text("Data could not be refreshed")
                    if (message.isNotEmpty()) { Card(Modifier.fillMaxWidth().clickable { message = "" }) { Text(message, Modifier.padding(12.dp)) } }
                    Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    Column(Modifier.weight(1f).verticalScroll(contentScroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if(!settings)SectionAnimation(screen)
                        when (screen) {
                            "notes" -> NotesHub(platform,snapshot,::mutate,onDismiss={screen="today"},fullScreen=true,focusNote=focusNote,onFocused={focusNote=null})
                            "today" -> {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { screen = "food" }) { Text("+ Еда") }
                                    Button(onClick = { open("drink", "Вода", "250", "0") }) { Text("+ Напиток") }
                                    OutlinedButton(onClick = { open("activity", "Прогулка", "15") }) { Text("+ Активность") }
                                    }
                                }
                                app.cardOrder.filterNot { it in app.hiddenCards }.forEach { id -> when(id) {
                                    "nutrition" -> Metric("Питание", if(goalActive(displayGoals,"energy",date))"${quantity(energy,"kcal",app.units)} / ${quantity(displayGoals.kcal,"kcal",app.units)}" else "${quantity(energy,"kcal",app.units)} · цель отключена/на паузе", if(goalActive(displayGoals,"energy",date))energy / displayGoals.kcal else 0.0) { screen = "food" }
                                    "water" -> Metric("Вода и напитки", "${quantity(water,"ml",app.units)} воды · ${quantity(drinks,"ml",app.units)} всего", if(goalActive(displayGoals,"water",date))(if (displayGoals.allDrinks) drinks else water) / displayGoals.waterMl else 0.0) { screen = "water" }
                                    "steps" -> Metric("Шаги", if(goalActive(displayGoals,"steps",date))"${steps.toInt()} / ${displayGoals.steps}" else "${steps.toInt()} · цель отключена/на паузе", if(goalActive(displayGoals,"steps",date))steps / displayGoals.steps.coerceAtLeast(1) else 0.0) { screen = "activity" }
                                    "activity" -> Metric("Активность", if(goalActive(displayGoals,"activity",date))"${active.toInt()} / ${displayGoals.activityMinutes} мин" else "${active.toInt()} мин · цель отключена/на паузе", if(goalActive(displayGoals,"activity",date))active / displayGoals.activityMinutes.coerceAtLeast(1) else 0.0) { screen = "activity" }
                                    "goals" -> Metric("Ваши цели", "Настройте удобный ритм", 0.0) { goalsEditor=true }
                                } }
                                OutlinedButton(onClick = { open("cards") }) { Text("Настроить карточки") }
                                Text("Сегодня в дневнике", style = MaterialTheme.typography.titleMedium)
                                EntryList(entries) { e -> editEntry(e) }
                            }
                            "food" -> {
                                if(catalogueFailed)Button(onClick={catalogueRetry++}){Text("Retry")}
                                Metric("Питание",quantity(energy,"kcal",app.units),if(goalActive(displayGoals,"energy",date))energy / displayGoals.kcal else 0.0) {}
                                if(subPage.isEmpty()){
                                Text("Дневник питания", style = MaterialTheme.typography.headlineSmall)
                                Text("${number(energy)} ккал · Б ${number(entries.sumOf { it.nutrients.protein ?: 0.0 })} · Ж ${number(entries.sumOf { it.nutrients.fat ?: 0.0 })} · У ${number(entries.sumOf { it.nutrients.carbs ?: 0.0 })}")
                                listOf(Triple("protein","Белки",displayGoals.proteinG),Triple("fat","Жиры",displayGoals.fatG),Triple("carbs","Углеводы",displayGoals.carbsG)).forEach {(id,label,target)->
                                    if(target!=null && goalActive(displayGoals,id,date)){
                                        val total=entries.sumOf {when(id){"protein"->it.nutrients.protein;"fat"->it.nutrients.fat;else->it.nutrients.carbs} ?: 0.0}
                                        Metric(label,"${number(total)} / ${number(target)} г",total/target){}
                                    }
                                }
                                EntryList(entries.filter { it.type == "food" || (it.type == "drink" && it.nutrients.kcal != null) }) { e -> editEntry(e) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick={subPage="catalog"}){Text("Продукты и блюда")} }
                                }
                                if(subPage=="catalog"){
                                // Holding a finger or the pointer on the chip tells how many products or dishes the catalogue has now.
                                val foodCounts=remember(foods){foods.filter {it.deleted==null}.groupingBy {it.kind}.eachCount()}
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HintBox("${translate("Продукты",app.language)}: ${foodCounts["product"] ?: 0}"){FilterChip(kind == "product", { kind = "product" }, { Text("Продукты") })}; HintBox("${translate("Блюда",app.language)}: ${foodCounts["dish"] ?: 0}"){FilterChip(kind == "dish", { kind = "dish" }, { Text("Блюда") })} }
                                OutlinedTextField(query, { query = it }, label = { Text("Поиск названия или штрихкода") }, modifier = Modifier.fillMaxWidth())
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(onlyFavorites, { onlyFavorites = !onlyFavorites }, { Text("Избранное") }); FilterChip(recent, { recent = !recent }, { Text("Недавние") }) }
                                OutlinedTextField(category, { category = it }, label = { Text("Категория / кухня") }, modifier = Modifier.fillMaxWidth())
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { open("custom", "", "", ""); field4 = ""; field5 = "" }) { Text("+ Своё") }
                                    OutlinedButton(onClick = { recipeLines = emptyList(); open("recipe", "", "500", "2") }) { Text("+ Рецепт") }
                                    }
                                }
                                val used = remember(data.entries){data.entries.mapNotNull { it.foodId }.toSet()}
                                val found = remember(foods,kind,query,category,onlyFavorites,data.favorites,recent,used,app.language){foods.filter { it.deleted == null && it.kind == kind && (query.isBlank() || it.name.contains(query, true) || foodDisplayName(it,app.language).contains(query, true) || it.aliases.contains(query, true) || it.barcode == query) && (category.isBlank() || it.category.contains(category, true) || it.cuisine.contains(category, true)) && (!onlyFavorites || it.id in data.favorites) && (!recent || it.id in used) }}
                                Text("Найдено ${found.size}. Показаны первые 60.", style = MaterialTheme.typography.bodySmall)
                                found.take(60).forEach { f -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp)) {
                                    Column(Modifier.weight(1f).clickable { picked = f; open("food", "100", data.meals.firstOrNull() ?: "", date) }) { androidx.compose.material3.Text(foodDisplayName(f,app.language)); Text("${number(f.nutrients.kcal)} ккал / 100 ${f.basis} · ${f.source}", style = MaterialTheme.typography.bodySmall) }
                                    TextButton(onClick = { change(data.copy(favorites = if (f.id in data.favorites) data.favorites - f.id else data.favorites + f.id)) }) { Text(if (f.id in data.favorites) "★" else "☆") }
                                } } }
                                }
                            }
                            "water" -> {
                                if(subPage.isEmpty()){
                                Metric("Вода", quantity(water,"ml",app.units), if(goalActive(displayGoals,"water",date))water / displayGoals.waterMl else 0.0) {}
                                Metric("Все напитки", quantity(drinks,"ml",app.units), if(goalActive(displayGoals,"water",date))drinks / displayGoals.waterMl else 0.0) {}
                                Text(if(goalActive(displayGoals,"water",date))"Цель: ${if (displayGoals.allDrinks) "все напитки" else "чистая вода"} · ${quantity(displayGoals.waterMl,"ml",app.units)}" else "Цель отключена/на паузе")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(100, 200, 250, 500).forEach { n -> OutlinedButton(onClick = { add(Entry(date = date, type = "drink", name = "Вода", amount = n.toDouble(), unit = "ml", plainWater = true, nutrients = Nutrients(0.0, 0.0, 0.0, 0.0))) }) { Text("+$n") } } }
                                Button(onClick = { open("drink", "", "250", "") }) { Text("+ Другой напиток") }
                                OutlinedButton(onClick={subPage="drinks"}){Text("Записи напитков")}
                                }
                                if(subPage=="drinks"){
                                Text("Кофе с молоком, латте, соки, газировка — учитываем всё. Калории на 100 мл вводите с упаковки или рецепта.", style = MaterialTheme.typography.bodySmall)
                                EntryList(entries.filter { it.type == "drink" }) { e -> editEntry(e) }
                                }
                            }
                            "activity" -> {
                                Metric("Активность",quantity(active,"min",app.units),if(goalActive(displayGoals,"activity",date))active / displayGoals.activityMinutes.coerceAtLeast(1) else 0.0) {}
                                Metric("Шаги",quantity(steps,"steps",app.units),if(goalActive(displayGoals,"steps",date))steps / displayGoals.steps.coerceAtLeast(1) else 0.0) {}
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick={open("steps","1000")}){Text("+ Шаги вручную")}
                                }
                                data.entries.filter {it.source.startsWith("pending:") && it.deleted==null}.forEach {pending->Card {Column(Modifier.padding(12.dp)){
                                    Text("Шаги на границе суток: ${pending.amount.toInt()} · ${pending.name}")
                                    Text("Точный день неизвестен. Выберите день календаря выше и подтвердите, если можете отнести шаги к нему.")
                                    TextButton(onClick={
                                        mutate {current->
                                        val currentData=current.shared
                                        val live=currentData.entries.firstOrNull {it.id==pending.id && it.deleted==null} ?: error("Интервал уже обработан")
                                        val device=pending.source.removePrefix("pending:")
                                        val id=java.util.UUID.nameUUIDFromBytes("fiteasy:phone:$device:$date".toByteArray()).toString()
                                        val old=currentData.entries.firstOrNull {it.id==id}
                                        val assigned=Entry(id=id,date=date,type="steps",name="Телефон",amount=(old?.amount ?: 0.0)+live.amount,unit="steps",source="phone:$device")
                                        current.copy(shared=currentData.copy(entries=currentData.entries.filterNot {it.id==id}.map {if(it.id==pending.id)it.copy(deleted=System.currentTimeMillis(),updated=System.currentTimeMillis())else it}+assigned))
                                        }
                                    }){Text("Отнести к выбранному дню")}
                                }}}
                                Text("Прогулка: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.titleLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { running = !running }) { Text(if (running) "Пауза" else "Начать") }
                                    OutlinedButton(onClick = { running = false; if (seconds > 0 && add(Entry(date = date, type = "activity", name = "Прогулка", amount = seconds / 60.0, unit = "min"))) seconds = 0 }) { Text("Завершить") }
                                    }
                                }
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { open("activity", "", "10") }) { Text("+ Ручная активность") }
                                OutlinedButton(onClick = { editRoutine=null;routineEditor=true }) { Text("+ Свой комплекс") }
                                OutlinedButton(onClick={exerciseLibrary=true}){Text(libraryText("Справочник упражнений",app.language))}
                                }
                                val routines = remember(data.routines){starterRoutines() + data.routines}
                                routines.forEach {r->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp)) {
                                    Text(r.name);Text("${r.minutes} мин",style=MaterialTheme.typography.bodySmall)
                                    Row {TextButton(enabled=workoutDraftReady && pendingWorkout==null,onClick={activeWorkout=startWorkout(r,date)}){Text("Начать")};if(r.id in data.routines.map {it.id})TextButton(onClick={editRoutine=r;routineEditor=true}){Text("Изменить")}}
                                }}}
                                EntryList(entries.filter { it.type == "activity" }) { e -> editEntry(e) }
                            }
                            "progress" -> {
                                Button(onClick = { open("measurement", "Вес", "", date) }) { Text("+ Вес / замер") }
                                FlowRow {progressPeriods.forEach {(id,label)->FilterChip(progressPeriod==id,{progressPeriod=id;if(id in setOf("years","range"))progressPeriodEditor=true},{Text(label)})}}
                                OutlinedButton(onClick={progressPeriodEditor=true}) {Text("Период и точка отсчёта")}
                                val rangeResult=runCatching {progressRange(progressPeriod,LocalDate.parse(date),progressForward,progressYears,progressFrom,progressTo,data.entries)}
                                val range=rangeResult.getOrElse {ProgressRange(LocalDate.parse(date),LocalDate.parse(date))}
                                if(rangeResult.isFailure)Text("Проверьте даты и предел 100 лет")
                                Text("${range.start} — ${range.end}")
                                val metrics=listOf("Вес","Талия","Бёдра","Грудь","Калории","Вода","Шаги","Активность")
                                Text("Показывать показатели")
                                FlowRow {metrics.forEach {name->FilterChip(name !in app.hiddenMetrics,{appearance(app.copy(hiddenMetrics=if(name in app.hiddenMetrics)app.hiddenMetrics-name else app.hiddenMetrics+name))},{Text(name)})}}
                                OutlinedButton(onClick={chartEditor=""}) {Text(translate("Оформление графиков",app.language))}
                                val buckets=remember(range) {progressBuckets(range)}
                                val dates=buckets.map {if(it.start==it.end)it.start.toString()else "${it.start} — ${it.end}"}
                                Text(if(buckets.any {it.start!=it.end})"Суммы за интервал; замеры — последнее значение"else "Дневные значения")
                                chartMetrics.keys.filter {it !in app.hiddenMetrics}.forEach {name->
                                    val measurement=name in chartMetrics.keys.take(4)
                                    val unit=if(measurement)unitFactor(if(name==chartMetrics.keys.first())"kg"else "cm",app.units).second else when(name){chartMetrics.keys.elementAt(4)->unitFactor("kcal",app.units).second;chartMetrics.keys.elementAt(5)->unitFactor("ml",app.units).second;chartMetrics.keys.elementAt(6)->"steps";else->"min"}
                                    val values=remember(name,buckets,data.entries,app.units,local.stepSource) {progressSeries(name,buckets,data.entries,app,local.stepSource,ownDeviceId)}
                                    ProgressChart(name,values,dates,unit,app) {chartEditor=name}
                                    if(measurement)TextButton(onClick={measurementRecords=name;measurementRange=range}) {Text(translate("Записи",app.language))}
                                }
                                val weight = data.entries.filter { it.type == "measurement" && it.name == "Вес" && it.deleted == null && it.date<=range.end.toString() }.maxByOrNull { it.date }?.amount
                                val height = data.profile.heightCm
                                if(displayGoals.weightKg!=null && goalActive(displayGoals,"weight",date))Text("Целевой вес: ${quantity(displayGoals.weightKg,"kg",app.units)}")
                                if ((data.profile.age ?: 0) >= 20 && weight != null && height != null && height > 0) Text("ИМТ: ${number(weight / (height / 100 * height / 100))} · ориентировочный взрослый показатель")
                                OutlinedButton(onClick={runCatching {energyEstimate(data,date)}.onSuccess {estimate=it;change(data.copy(estimates=data.estimates+it));open("calculator")}.onFailure {AppJournal.record("ui.action.failed",JournalLevel.ERROR,it);message=it.message ?: "Заполните профиль"}}){Text("Рассчитать ориентир энергии")}; data.estimates.takeLast(5).forEach {Text("${it.date}: ${number(it.dailyKcal)} ккал · ${if(it.applied) "применено" else "предложение"}")}
                            }
                            "profile" -> {
                                SettingsMenu(settingsSection) {settingsSection=it}
                                if(settingsSection=="personal") {
                                MenuRow("profile","Личные данные","Возраст, рост, пол и коэффициент активности") { open("profile", data.profile.age?.toString() ?: "", data.profile.heightCm?.let {displayAmount(it,"cm",app.units).toString()} ?: "", data.profile.activityFactor.toString()); option = data.profile.female }
                                MenuRow("target","Цели","Калории, вода, шаги, активность, вес и БЖУ") {goalsEditor=true}
                                MenuRow("dish","Приёмы пищи","Названия приёмов пищи в дневнике") { open("meals", data.meals.joinToString(", ")) }
                                }
                                if(settingsSection=="appearance") {
                                Text("Оформление", style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.Center) { Text("Выбор темы:",Modifier.align(Alignment.CenterVertically)); listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (id, name) -> FilterChip(app.theme == id, { appearance(app.copy(theme = id)) }, { Text(name) }) } }
                                FlowRow {appearanceStyles.entries.forEach {(id,label)->FilterChip(app.style==id,{appearance(applyStyle(app,id))},{Text(translate(label,app.language))},leadingIcon={FitIcon(if(id=="blossom")"food"else if(id=="air")"water"else "today")})}}
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick={accentEditor=true}){Text("Accent / RGB")}
                                    OutlinedButton(onClick={backgroundEditor=true}){Text("Section backgrounds")}
                                    Spacer(Modifier.weight(1f))
                                    Text("Animations");Switch(app.animations,{appearance(app.copy(animations=it))})
                                }
                                var textScaleDraft by remember(app.textScale) {mutableFloatStateOf(app.textScale)}
                                Text("Размер текста"); Slider(textScaleDraft, { textScaleDraft=it },onValueChangeFinished={appearance(app.copy(textScale=textScaleDraft))}, valueRange = .85f..1.5f)
                                if(platform.name=="Windows")Row {Text("Сворачивать в трей при закрытии",Modifier.weight(1f));Switch(closeToTray,{enabled->runCatching {platform.setCloseToTray(enabled)}.onSuccess {closeToTray=enabled}.onFailure {AppJournal.record("ui.action.failed",JournalLevel.ERROR,it);message="Не удалось сохранить настройку трея"}})}
                                if(platform.name=="Windows"){
                                    Text("Window rendering")
                                    FlowRow {listOf("AUTO" to "Automatic","SOFTWARE_FAST" to "Compatibility", "DIRECT3D" to "Direct3D", "OPENGL" to "OpenGL").forEach {(id,label)->FilterChip(renderMode==id,{runCatching {platform.setRenderMode(id)}.onSuccess {renderMode=id;message=translate("Restart FitEasy to apply",app.language)}.onFailure {AppJournal.record("ui.action.failed",JournalLevel.ERROR,it);message=translate("Data could not be refreshed",app.language)}},{Text(label)})}}
                                    Text("Restart FitEasy to apply")
                                }

                                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick={chartEditor=""}) {Text(translate("Оформление графиков",app.language))}
                                OutlinedButton(onClick={noteStampSettings=true}) {Text("Штампы времени")}
                                }
                                }
                                if(settingsSection=="general") {
                                Text("Язык")
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){languageNames.forEach {(id,label)->FilterChip(app.language==id,{appearance(app.copy(language=id))},{Text(label)})}}
                                OutlinedButton(onClick={unitsEditor=true}){Text("Единицы")}
                                }
                                if(settingsSection=="reminders") {
                                // One switch for every reminder of this device; each keeps its own settings (D108).
                                Card {Row(Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {Text("Напоминания",style=MaterialTheme.typography.titleMedium);Text(if(local.remindersOff)"Все напоминания выключены. Их настройки сохранены." else "Выключатель отключает все напоминания сразу.",style=MaterialTheme.typography.bodySmall)}
                                    Switch(!local.remindersOff,{on->mutate {c->c.copy(local=c.local.copy(remindersOff=!on))}})
                                }}
                                NotificationSettings(platform){economy=economyEnabled(platform)}
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {editingReminder=null;if(local.remindersOff)reminderOffHint=true else reminderEditor=true}) { Text("+ Напоминание") }
                                    OutlinedButton(onClick={scheduleHub=true}) {Text(translate("Расписания",app.language))}
                                    OutlinedButton(onClick={calendar=true}) {Text(translate("Календарь",app.language))}
                                }
                                ReminderCards(platform,snapshot,onEdit={editingReminder=it;reminderEditor=true},onSave={r->mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.map {if(it.id==r.id)r else it}))}},onDelete={r->mutate {c->c.copy(local=c.local.copy(reminders=c.local.reminders.filterNot {it.id==r.id}))}},onOpenNote={focusNote=it;screen="notes"})
                                }
                                if(settingsSection=="permissions")RightsPage(platform)
                                if(settingsSection=="tags")NoteTagsSettings(snapshot,::mutate)
                                if(settingsSection=="security")SecurityPage(platform,onLockSettings=onSecurity,onLock=onLock)
                                if(settingsSection=="data")DataPage(platform,stock,demo=local.demo.isNotEmpty(),mutate=::mutate,onAppearance={appearance(it)},onDatabaseTools={databaseTools=true},onReport={reportEditor=true},onTrash={open("trash")},onDemoCreate={demoCreate=true},onDemoRemove={force->demoForce=force;demoRemoval=true},onReset={resetDialog=true},onDatabaseImported={snapshot=platform.load()})
                                if(settingsSection=="help")AboutSettings(platform.name,onHelp={open("help")},onAbout={open("about")},onJournal={journal=true})
                                if(settingsSection=="trash")TrashSettings(snapshot,::mutate,onOpen={open("trash")})
                                if(settingsSection=="sync") {OutlinedButton(onClick=onSync) {Text(translate("Синхронизация",app.language))}}
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    if(wide && !settings)MainNavigationIcons(screen,vertical=true,onSelect={screen=it;query=""})
                    }
                }
            }
            }
            if(demoRemoval)DemoRemovalDialog(demoForce,onDismiss={demoRemoval=false},onRemove={mutate {if(demoForce)forceRemoveDemonstration(it)else removeDemonstration(it)}})
            if(demoCreate)DemoCreationDialog(onDismiss={demoCreate=false},onCreate={days->mutate {createDemonstration(it,days=days)}})
            if(legalView)legalNotice(app.language).let {notice->AlertDialog(onDismissRequest={legalView=false},title={androidx.compose.material3.Text(notice.title)},text={Column(Modifier.verticalScroll(rememberScrollState())){androidx.compose.material3.Text(notice.body)}},confirmButton={TextButton(onClick={legalView=false}){Text("Закрыть")}})}
            if(resetDialog)ResetDialog(onDismiss={resetDialog=false},onReset={sections,all->runCatching {if(all || "activity" in sections)platform.trackSteps(false)}.fold(onSuccess={mutate {resetData(it,sections,all)}},onFailure={message=it.message ?: "Error";false})})
            chartEditor?.let {metric->ChartEditor(app,metric.takeIf {it.isNotEmpty()},onSave=::appearance,onDismiss={chartEditor=null})}
            if(journal)JournalDialog(platform,{journal=false})
            if(databaseTools)DatabaseToolsDialog(platform,{snapshot=platform.load();if(snapshot.local.appearance.language!=currentLanguage){platform.writeSecret("language",snapshot.local.appearance.language);onLanguageChanged(snapshot.local.appearance.language)}},{databaseTools=false})
            if(backgroundEditor)BackgroundEditor(platform,app,::appearance,{backgroundEditor=false})
            if(accentEditor){var draft by remember {mutableLongStateOf(app.accent)};SettingsPage(onDismissRequest={accentEditor=false},title={Text("Accent / RGB")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())){ColorChoice(draft,{draft=it})}},confirmButton={TextButton(onClick={appearance(app.copy(accent=draft));accentEditor=false}){Text("Сохранить")}},dismissButton={TextButton(onClick={accentEditor=false}){Text("Отмена")}})}
            if(noteStampSettings)NoteDisplayDialog(app.noteDisplay,onSave={appearance(app.copy(noteDisplay=it))},onDismiss={noteStampSettings=false})
            measurementRecords?.let {name->SettingsPage(onDismissRequest={measurementRecords=null},title={Text(name)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {EntryList(data.entries.filter {it.type=="measurement" && it.name==name && it.deleted==null && (measurementRange==null || it.date in measurementRange!!.start.toString()..measurementRange!!.end.toString())}.sortedByDescending {it.date}) {entry->measurementRecords=null;editEntry(entry)}}},confirmButton={TextButton(onClick={measurementRecords=null}) {Text(translate("Закрыть",app.language))}})}
            if(routineEditor)RoutineEditor(platform,editRoutine,onDismiss={routineEditor=false},onSave={r->if(change(data.copy(routines=data.routines.filterNot {it.id==r.id}+r)))routineEditor=false},onEnableAnimations={appearance(app.copy(animations=true))})
            if(exerciseLibrary)ExerciseLibrary(platform,onDismiss={exerciseLibrary=false},onEnableAnimations={appearance(app.copy(animations=true))},onOpenAppearance={exerciseLibrary=false;screen="profile";settingsSection="appearance"},onOpenEconomy={exerciseLibrary=false;screen="profile";settingsSection="reminders"},onSelect={e->editRoutine=Routine(name=translate("Свой комплекс",app.language),instructions="",minutes=1,exercises=listOf(e));routineEditor=true})
            if(goalsEditor)GoalsEditor(data.goals,onDismiss={goalsEditor=false},onSave={goals->change(data.copy(goals=goals,goalHistory=goalHistory(data,date,goals)))})
            if(reportEditor)ReportEditor(platform,data,date,onDismiss={reportEditor=false})
            if(unitsEditor)UnitsEditor(app.units,onDismiss={unitsEditor=false},onSave={units->update(snapshot.copy(local=local.copy(appearance=app.copy(units=units))))})
            if(progressPeriodEditor)ProgressPeriodEditor(progressPeriod,date,progressForward,progressYears,progressFrom,progressTo,onCalendar={target->calendarTarget=target;calendar=true},onApply={period,forward,years,from,to->runCatching {progressRange(period,LocalDate.parse(date),forward,years,from,to,data.entries)}.fold(onSuccess={progressPeriod=period;progressForward=forward;progressYears=years;progressFrom=from;progressTo=to;true},onFailure={message=translate("Проверьте даты и предел 100 лет",app.language);false})},onDismiss={progressPeriodEditor=false})
            if(calendar)SectionDialogBackdrop(platform,app,"calendar"){CalendarDialog(platform,snapshot,when(calendarTarget){"from"->progressFrom;"to"->progressTo;else->date},onSelect={when(calendarTarget){"from"->progressFrom=it;"to"->progressTo=it;else->date=it};calendarTarget="anchor"},onSchedules={calendar=false;scheduleHub=true},onDismiss={calendar=false;calendarTarget="anchor"})}
            if(scheduleHub)SectionDialogBackdrop(platform,app,"schedules"){ScheduleHub(platform,snapshot,::mutate,onDismiss={scheduleHub=false;focusedReminder=null;focusedReminderEditor=true},focusReminder=focusedReminder,focusEditor=focusedReminderEditor)}
            if(reminderOffHint)RemindersOffHint(onEnable={if(mutate {c->c.copy(local=c.local.copy(remindersOff=false))}){reminderOffHint=false;reminderEditor=true}},onSkip={reminderOffHint=false;reminderEditor=true},onDismiss={reminderOffHint=false})
            if(reminderEditor)ReminderEditor(editingReminder,onDismiss={reminderEditor=false},onSave={r->update(snapshot.copy(local=local.copy(reminders=local.reminders.filterNot {it.id==r.id}+r)))},onDelete={update(snapshot.copy(local=local.copy(reminders=local.reminders.filterNot {it.id==editingReminder?.id})))},platform=platform,notes=remember(data.notes){data.notes.filter {it.deleted==null}})
            pendingWorkout?.let {draft->AlertDialog(onDismissRequest={},title={Text("Незавершённая тренировка")},text={Column {Text("${draft.routine.name} · ${draft.date}");Text("Восстановление начнётся на паузе; время закрытого приложения не засчитывается.")}},confirmButton={TextButton(onClick={activeWorkout=draft;pendingWorkout=null}){Text("Восстановить")}},dismissButton={TextButton(onClick={WorkoutDraftStore.clear(platform);pendingWorkout=null}){Text("Удалить черновик")}})}
            if(workoutDraftError)AlertDialog(onDismissRequest={},title={Text("Не удалось прочитать черновик тренировки")},text={Text("Записанные результаты дневника сохранены. Можно повторить чтение или удалить только неисправный черновик.")},confirmButton={TextButton(onClick={fileUiScope.launch {withContext(Dispatchers.IO){runCatching {WorkoutDraftStore.recoverable(platform)}}.onSuccess {pendingWorkout=it;workoutDraftReady=true;workoutDraftError=false}.onFailure {AppJournal.record("workout.draft.retry.failed",JournalLevel.ERROR,it)}}}){Text("Повторить")}},dismissButton={TextButton(onClick={WorkoutDraftStore.clear(platform);workoutDraftReady=true;workoutDraftError=false}){Text("Удалить черновик")}})
            activeWorkout?.let {session->WorkoutPlayer(platform,session,onDismiss={activeWorkout=null},onSave={result->
                val entry=Entry(id=session.id,date=session.date,type="activity",name=result.routine.name,amount=result.elapsedSeconds/60.0,unit="min",workout=result)
                val saved=add(entry);if(saved)activeWorkout=null;saved
            })}
            if (dialog != null) {
                val id = dialog!!
                // The help and the trash are whole pages, as in Notio (D107).
                if(id=="help")HelpPage(app.language,onDismiss={dialog=null},prefer=helpCategoryFor(screen,settingsSection))
                else if(id=="trash")TrashPage(snapshot,::mutate,onDismiss={dialog=null})
                else SettingsPage(onDismissRequest = { dialog = null }, title = { Text(when(id) { "food" -> picked?.let {foodDisplayName(it,app.language)} ?: "Еда"; "drink" -> "Напиток"; "recipe" -> "Рецепт"; "routine" -> "Свой комплекс"; "session" -> field1; "profile" -> "Личные данные"; "goals" -> "Цели"; "measurement" -> "Замер"; "trash" -> "Корзина"; "about" -> translate("О программе",app.language); "help" -> "Справка"; "cards" -> "Карточки"; else -> "Изменить" }) }, text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        @Composable fun input(value: String, set: (String)->Unit, label: String) { OutlinedTextField(value, set, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
                        when(id) {
                            "steps" -> input(field1,{field1=it},"Шаги")
                            "color" -> input(field1,{field1=it},"Цвет RRGGBB")
                            "calculator" -> { estimate?.let {e -> Text("Покой: ${number(e.restingKcal)} ккал; ориентир с активностью: ${number(e.dailyKcal)} ккал/день.");Text("Миффлин — Сан Жеор. Коэффициент активности: ${e.activityFactor}. Это оценка поддержания веса, без дефицита. Изменить цель на это значение?") } }
                            "date" -> input(field1, { field1 = it }, "Дата YYYY-MM-DD")
                            "food" -> { input(field1, { field1 = it }, "Количество, г"); input(field2, { field2 = it }, "Приём пищи"); Text("${number(picked?.nutrients?.scaled((parse(field1) ?: 0.0)/100)?.kcal)} ккал") }
                            "custom" -> { input(field1, { field1 = it }, "Название"); input(field2, { field2 = it }, "Ккал / 100 г"); input(field3, { field3 = it }, "Белки"); input(field4, { field4 = it }, "Жиры"); input(field5, { field5 = it }, "Углеводы") }
                            "drink" -> { input(field1, { field1 = it }, "Название напитка"); input(field2, { field2 = it }, "Объём, ${unitFactor("ml",app.units).second}"); input(field3, { field3 = it }, "Ккал / 100 мл, если известно"); input(field4,{field4=it},"Белки / 100 мл"); input(field5,{field5=it},"Жиры / 100 мл"); input(field6,{field6=it},"Углеводы / 100 мл"); Row { Checkbox(option, { option = it }); Text("Чистая вода") } }
                            "activity", "routine" -> { input(field1, { field1 = it }, "Название"); input(field2, { field2 = it }, "Минуты"); if (id == "routine") input(field3, { field3 = it }, "Упражнения и отдых")else input(field3,{field3=it},"Расстояние, ${unitFactor("km",app.units).second} (необязательно)") }
                            "session" -> { Text(field3); Text("${field2} мин"); Text("Подтвердите только реально выполненную активность.") }
                            "measurement" -> { listOf("Вес", "Талия", "Бёдра", "Грудь").forEach { n -> FilterChip(field1 == n, { field1 = n }, { Text(n) }) }; input(field2, { field2 = it }, unitFactor(if(field1=="Вес")"kg" else "cm",app.units).second) }
                            "profile" -> { input(field1, { field1 = it }, "Возраст (18+)"); input(field2, { field2 = it }, "Рост, ${unitFactor("cm",app.units).second}"); input(field3, { field3 = it }, "Коэффициент активности"); Row { Checkbox(option, { option = it }); Text("Женщина") } }
                            "goals" -> { input(field1, { field1 = it }, "Ккал"); input(field2, { field2 = it }, "Жидкость, л"); input(field3, { field3 = it }, "Шаги"); input(field4, { field4 = it }, "Активность, мин"); Row { Checkbox(option, { option = it }); Text("Все напитки вместо чистой воды") }; Row {Checkbox(option2,{option2=it});Text("Автоматически пересчитывать энергию")}; Row {Checkbox(autoApply,{autoApply=it});Text("Применять без подтверждения")}; Text("Автоматические расчёты меняют только энергетическую цель; прошлые дни сохраняются.") }
                            "entry" -> { input(field1, { field1 = it }, "Название"); input(field2, { field2 = it }, "Количество, ${editing?.let {unitFactor(it.unit,app.units).second}}"); input(field3, { field3 = it }, "Дата YYYY-MM-DD"); TextButton(onClick = { val e = editing!!; change(data.copy(entries = data.entries.map { if (it.id == e.id) it.copy(deleted = System.currentTimeMillis(), updated = System.currentTimeMillis()) else it })); dialog = null }) { Text("В корзину") }; TextButton(onClick = { val e = editing!!; LocalDate.parse(field3); add(e.copy(id = newId(), date = field3, updated = System.currentTimeMillis())) }) { Text("Добавить копией на указанную дату") } }
                            "meals" -> input(field1, { field1 = it }, "Названия через запятую")
                            "cards" -> app.cardOrder.forEachIndexed { index, card -> Row { Checkbox(card !in app.hiddenCards, { show -> appearance(app.copy(hiddenCards = if (show) app.hiddenCards - card else app.hiddenCards + card)) }); Text(card, Modifier.weight(1f)); if (index > 0) TextButton(onClick = { val order = app.cardOrder.toMutableList(); java.util.Collections.swap(order,index,index-1); appearance(app.copy(cardOrder=order)) }) { Text("↑") } } }
                            "recipe" -> { input(field1, { field1 = it }, "Название"); input(field2, { field2 = it }, "Готовый вес, г"); input(field3, { field3 = it }, "Число порций"); input(field4, { field4 = it }, "Шаги приготовления"); input(field5, { field5 = it }, "Найти ингредиент"); foods.filter { field5.length > 1 && it.name.contains(field5,true) }.take(5).forEach { f -> TextButton(onClick = { recipeLines = recipeLines + Ingredient(f.id,100.0); field5 = "" }) { Text(f.name) } }; recipeLines.forEachIndexed { index,i -> Text(foods.firstOrNull { it.id==i.foodId }?.name ?: i.foodId); input(i.grams.toString(), { v -> parse(v)?.let { n -> recipeLines = recipeLines.mapIndexed { at,line -> if(at==index) line.copy(grams=n) else line } } }, "Граммы") } }
                            "reminder" -> { input(field1,{field1=it},"Название"); input(field2,{field2=it},"Время HH:mm"); input(field3,{field3=it},"Дни недели 1–7 через запятую"); input(field4,{field4=it},"Числа месяца через запятую"); input(field5,{field5=it},"Каждые N дней"); input(field6,{field6=it},"Тихие часы HH:mm-HH:mm (необязательно)"); Text("Несколько времён можно указать через запятую. Числа 29–31 пропускаются в месяцах без такого дня. Интервал считается от даты создания. На Android без разрешения точных будильников уведомление может задерживаться.") }
                            "csv" -> { input(field1,{field1=it},"С YYYY-MM-DD"); input(field2,{field2=it},"По YYYY-MM-DD") }
                            "trash" -> data.entries.filter { it.deleted != null }.forEach { e -> Row { Text(e.name,Modifier.weight(1f)); TextButton(onClick = { change(data.copy(entries=data.entries.map { if(it.id==e.id) it.copy(deleted=null,updated=System.currentTimeMillis()) else it })) }) {Text("Восстановить")} } }
                            "help" -> { TextButton(onClick={journal=true}){Text("App journal")};Text("FitEasy ${BuildInfo.VERSION} · ${translate("Сборка",app.language)} ${BuildInfo.NUMBER}");input(field1,{field1=it},"Поиск в справке"); helpArticles(app.language).filter {it.title.contains(field1,true) || it.body.contains(field1,true)}.forEach {article -> Text(article.title,style=MaterialTheme.typography.titleMedium);Text(article.body)} }
                            "about" -> {Text("FitEasy",style=MaterialTheme.typography.titleLarge);Text("${translate("Версия",app.language)}: ${BuildInfo.VERSION}");Text("${translate("Сборка",app.language)}: ${BuildInfo.NUMBER}");Text("${translate("Дата сборки",app.language)}: ${BuildInfo.DATE}");Text("${translate("Контрибьютер",app.language)}: ${BuildInfo.CONTRIBUTOR}");Text(platform.name);TextButton(onClick={open("help")}){Text("Справка")};OutlinedButton(onClick={legalView=true}){androidx.compose.material3.Text(legalNotice(app.language).title)}}
                        }
                    }
                }, confirmButton = { TextButton(onClick = {
                    lastSaveSucceeded=true
                    runCatching {
                        when(id) {
                            "steps" -> {val count=field1.toInt();require(count>0);add(Entry(date=date,type="steps",name="Ручные шаги",amount=count.toDouble(),unit="steps"))}
                            "color" -> {require(field1.trim().removePrefix("#").matches(Regex("[0-9a-fA-F]{6}")));appearance(app.copy(accent=0xFF000000 or field1.trim().removePrefix("#").toLong(16)))}
                            "calculator" -> {estimate?.let {change(applyEstimate(data,it))}}
                            "date" -> { LocalDate.parse(field1); date=field1 }
                            "food" -> { val f=picked!!; val grams=parse(field1) ?: error("Введите количество"); require(grams>0); add(Entry(date=date,type="food",name=foodDisplayName(f,app.language),amount=grams,unit="g",nutrients=f.nutrients.scaled(grams/100),foodId=f.id,meal=field2)) }
                            "custom" -> { require(field1.isNotBlank()); val f=Food(newId(),field1,kind,nutrients=Nutrients(parse(field2),parse(field3),parse(field4),parse(field5))); change(if(kind=="product") data.copy(products=data.products+f) else data.copy(dishes=data.dishes+f)) }
                            "drink" -> { require(field1.isNotBlank()); val ml=canonicalAmount(parse(field2) ?: error("Введите объём"),"ml",app.units); require(ml>0); val plain=option || field1.equals("Вода",true); add(Entry(date=date,type="drink",name=field1,amount=ml,unit="ml",plainWater=plain,nutrients=if(plain)Nutrients(0.0,0.0,0.0,0.0,0.0)else Nutrients(parse(field3),parse(field4),parse(field5),parse(field6)).scaled(ml/100))) }
                            "activity", "session" -> { require(field1.isNotBlank()); val min=parse(field2) ?: error("Введите минуты"); require(min>0);val distance=if(id=="activity" && field3.isNotBlank())canonicalAmount(parse(field3) ?: error("Введите расстояние"),"km",app.units)else null; add(Entry(date=date,type="activity",name=field1,amount=min,unit="min",distanceKm=distance)) }
                            "routine" -> { require(field1.isNotBlank()); val min=field2.toInt(); require(min in 1..240); change(data.copy(routines=data.routines+Routine(name=field1,instructions=field3,minutes=min))) }
                            "measurement" -> { val unit=if(field1=="Вес")"kg" else "cm";val n=canonicalAmount(parse(field2) ?: error("Введите значение"),unit,app.units); require(n>0); add(Entry(date=date,type="measurement",name=field1,amount=n,unit=unit)) }
                            "profile" -> { val age=field1.toIntOrNull(); require(age==null || age in 18..120); val height=parse(field2)?.let {canonicalAmount(it,"cm",app.units)}; require(height==null || height in 80.0..250.0); val factor=parse(field3) ?: 1.2; require(factor in 1.0..2.5); change(data.copy(profile=Profile(age,height,option,factor))) }
                            "goals" -> { val goals=data.goals.copy(kcal=parse(field1) ?: error("Введите ккал"),waterMl=(parse(field2) ?: error("Введите литры"))*1000,steps=field3.toInt(),activityMinutes=field4.toInt(),allDrinks=option,autoCalculate=option2,autoApply=autoApply); require(goals.steps>0 && goals.activityMinutes>0); change(data.copy(goals=goals,goalHistory=goalHistory(data,date,goals))) }
                            "entry" -> { val e=editing!!; val n=canonicalAmount(parse(field2) ?: error("Введите количество"),e.unit,app.units); require(n>0); LocalDate.parse(field3); add(e.copy(name=field1,amount=n,date=field3,nutrients=e.nutrients.scaled(if(e.amount>0)n/e.amount else 1.0),updated=System.currentTimeMillis())) }
                            "meals" -> { val meals=field1.split(',').map {it.trim()}.filter {it.isNotEmpty()}.distinct(); require(meals.isNotEmpty()); change(data.copy(meals=meals)) }
                            "recipe" -> { require(field1.isNotBlank() && recipeLines.isNotEmpty()); val recipe=Recipe(name=field1,ingredients=recipeLines,steps=field4,yieldGrams=parse(field2) ?: error("Выход"),portions=field3.toInt()); val total=recipeNutrients(recipe,foods); val dish=Food(recipe.id,recipe.name,"dish",nutrients=total.scaled(100/recipe.yieldGrams),source="user recipe"); change(data.copy(recipes=data.recipes+recipe,dishes=data.dishes+dish)) }
                            "reminder" -> { val times=field2.split(',').map {it.trim()}.filter {it.isNotEmpty()}; require(times.isNotEmpty()); times.forEach {java.time.LocalTime.parse(it)}; val weekdays=field3.split(',').mapNotNull {it.trim().toIntOrNull()}; val days=field4.split(',').mapNotNull {it.trim().toIntOrNull()}; require(weekdays.all {it in 1..7} && days.all {it in 1..31}); val interval=field5.toInt(); require(interval>0); update(snapshot.copy(local=local.copy(reminders=local.reminders+Reminder(name=field1,time=times.first(),times=times,weekdays=weekdays,monthDays=days,everyDays=interval,quietStart=field6.takeIf {it.isNotBlank()}?.substringBefore('-'),quietEnd=field6.takeIf {it.isNotBlank()}?.substringAfter('-'))))) }
                            "csv" -> { LocalDate.parse(field1); LocalDate.parse(field2); require(field1<=field2); platform.exportFile("FitEasy-$date.csv","text/csv",("\uFEFF"+csv(data,field1,field2)).toByteArray()) }
                        }
                    }.onSuccess {if(lastSaveSucceeded)dialog=null}.onFailure {AppJournal.record("ui.action.failed",JournalLevel.ERROR,it);message=it.message ?: "Проверьте значения"}
                }) { Text(if(id=="about" || id=="help" || id=="trash" || id=="cards") "Готово" else "Сохранить") } }, dismissButton = { TextButton(onClick={dialog=null}){Text("Отмена")} })
            }
        }
        helpAt?.let {hint->HelpPage(app.language,onDismiss={helpAt=null},focus=hint,prefer=helpCategoryFor(screen,settingsSection))}
        }
    }
}

@Composable private fun Metric(title: String, value: String, progress: Double, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(value,style=MaterialTheme.typography.headlineSmall); val target=progress.toFloat().coerceIn(0f,1f)
        val displayed by animateFloatAsState(target,tween(if(LocalAppearance.current.animations)220 else 0),label="metric")
        LinearProgressIndicator(progress={displayed},modifier=Modifier.fillMaxWidth()) } }
}
@Composable private fun EntryList(entries: List<Entry>, click: (Entry)->Unit) {
    val units=LocalUnits.current
    if(entries.isEmpty()) Text("Пока нет записей",style=MaterialTheme.typography.bodySmall)
    entries.forEach { e -> Row(Modifier.fillMaxWidth().clickable {click(e)}.padding(vertical=8.dp)) { Column(Modifier.weight(1f)) {Text(e.name); Text("${e.date} · ${e.meal}",style=MaterialTheme.typography.bodySmall)}; Column {Text(quantity(e.amount,e.unit,units));e.distanceKm?.let {Text(quantity(it,"km",units))}; if(e.nutrients.kcal!=null)Text(quantity(e.nutrients.kcal,"kcal",units),style=MaterialTheme.typography.bodySmall)} } }
}
