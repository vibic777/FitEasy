package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun starterRoutines()=emptyList<Routine>()

@Composable fun RoutineEditor(platform:Platform,existing:Routine?=null,onDismiss:()->Unit,onSave:(Routine)->Unit,onEnableAnimations:()->Unit={}) {
    val (avatars,setAvatar)=rememberExerciseAvatars(platform)
    var name by remember {mutableStateOf(existing?.name ?: "")}
    var notes by remember {mutableStateOf(existing?.instructions ?: "")}
    var exercises by remember {mutableStateOf(existing?.exercises ?: emptyList())}
    var exerciseName by remember {mutableStateOf("")}
    var instruction by remember {mutableStateOf("")}
    var technique by remember {mutableStateOf("")}
    var rig by remember {mutableStateOf<RigClip?>(null)}
    var rigEditor by remember {mutableStateOf(false)}
    var selectedCatalogueId by remember {mutableStateOf<String?>(null)}
    var library by remember {mutableStateOf(false)}
    var playing by remember {mutableStateOf<String?>(null)}
    var seconds by remember {mutableStateOf("30")}
    var repetitions by remember {mutableStateOf("0")}
    var rest by remember {mutableStateOf("15")}
    var sets by remember {mutableStateOf("1")}
    var setRest by remember {mutableStateOf("15")}
    var load by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    var editing by remember {mutableStateOf<Int?>(null)}
    SettingsPage(onDismissRequest=onDismiss,title={Text("Свой комплекс")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(name,{name=it},label={Text("Название")})
        OutlinedTextField(notes,{notes=it},label={Text("Описание")})
        OutlinedButton(onClick={library=true}){Text(libraryText("Справочник упражнений",LocalLanguage.current))}
        exercises.forEachIndexed {i,e->Card {Column(Modifier.padding(8.dp)){
            val rowKey=remember(i,e){"$i:${exerciseAvatarKey(e)}"}
            ExerciseSilhouette(e,compact=true,female=avatars[exerciseAvatarKey(e)] ?: true,running=playing==rowKey,onClick={playing=if(playing==rowKey)null else rowKey})
            Text("${i+1}. ${e.name}: ${e.sets} × ${e.seconds} с / ${e.repetitions} повторов; отдых ${e.setRestSeconds} / ${e.restSeconds} с"+(e.loadKg?.let {" · $it кг"} ?: ""))
            Row {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(enabled=editing==null && i>0,onClick={exercises=exercises.toMutableList().apply {val item=removeAt(i);add(i-1,item)}}){Text("↑")}
                TextButton(enabled=editing==null && i<exercises.lastIndex,onClick={exercises=exercises.toMutableList().apply {val item=removeAt(i);add(i+1,item)}}){Text("↓")}
                TextButton(enabled=editing==null,onClick={editing=i;selectedCatalogueId=e.catalogueId;technique=e.technique;rig=e.rig;exerciseName=e.name;instruction=e.instructions;seconds=e.seconds.toString();repetitions=e.repetitions.toString();rest=e.restSeconds.toString();sets=e.sets.toString();setRest=e.setRestSeconds.toString();load=e.loadKg?.toString() ?: "";error=""}){Text("Изменить")}
                }
            }
            Row {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(enabled=editing==null && exercises.size<100,onClick={exercises=exercises.toMutableList().apply {add(i+1,e.copy())}}){Text("Копировать")}
                TextButton(enabled=editing==null,onClick={exercises=exercises.filterIndexed {index,_->index!=i}}){Text("Удалить")}
                }
            }
        }}}
        OutlinedTextField(exerciseName,{exerciseName=it},label={Text("Упражнение")})
        OutlinedTextField(instruction,{instruction=it},label={Text("Инструкция")})
        Text("Схема движения")
        FlowRow {allExerciseTechniques.forEach {(key,label)->FilterChip(selected=technique==key,onClick={technique=key;rig=if(key.isBlank())null else rigTemplate(key)},label={Text(if(key.isBlank())label else exerciseNames(LocalLanguage.current)[exerciseCatalogue.indexOfFirst {it.technique==key}])})}}
        val preview=Exercise(name=exerciseName,technique=technique,catalogueId=selectedCatalogueId,rig=rig)
        TextButton(onClick={rigEditor=true}){Text(rigText("Rig editor",LocalLanguage.current))}
        val avatarTarget=editing?.let {exercises[it]} ?: preview
        val female=avatars[exerciseAvatarKey(avatarTarget)] ?: true
        ExerciseSilhouette(preview,female=female,running=playing=="preview",onClick={playing=if(playing=="preview")null else "preview"})
        Row {FilterChip(selected=female,onClick={setAvatar(avatarTarget,true)},label={Text("Женский силуэт")});FilterChip(selected=!female,onClick={setAvatar(avatarTarget,false)},label={Text("Мужской силуэт")})}
        OutlinedTextField(seconds,{seconds=it},label={Text("Секунды (0 — только повторы)")})
        OutlinedTextField(repetitions,{repetitions=it},label={Text("Повторы")})
        OutlinedTextField(rest,{rest=it},label={Text("Отдых, секунды")})
        OutlinedTextField(sets,{sets=it},label={Text("Подходы")})
        OutlinedTextField(setRest,{setRest=it},label={Text("Отдых между подходами, секунды")})
        OutlinedTextField(load,{load=it},label={Text("Вес снаряда, кг (необязательно)")})
        TextButton(onClick={runCatching {
            val selectedLoad=load.takeIf {it.isNotBlank()}?.replace(',','.')?.toDouble()
            val e=Exercise(exerciseName.trim(),instruction,seconds.toInt(),repetitions.toInt(),rest.toInt(),sets.toInt(),setRest.toInt(),selectedLoad,catalogueId=selectedCatalogueId,technique=technique,rig=rig)
            validate(SharedData(routines=listOf(Routine(name="Validation",instructions="",minutes=1,exercises=listOf(e)))))
            require(e.name.isNotBlank() && e.seconds in 0..3600 && e.repetitions in 0..1000 && e.restSeconds in 0..600 && (e.seconds>0 || e.repetitions>0) && (editing!=null || exercises.size<100))
            setAvatar(e,female)
            exercises=editing?.let {index->exercises.toMutableList().apply {set(index,e)}} ?: (exercises+e);editing=null;selectedCatalogueId=null;rig=null;playing=null;exerciseName="";instruction="";error=""
        }.onFailure {error="Проверьте название, время, повторы и отдых"}}){Text(if(editing==null)"Добавить упражнение"else "Применить правку")}
        if(editing!=null)TextButton(onClick={editing=null;selectedCatalogueId=null;rig=null;playing=null;exerciseName="";instruction="";error=""}){Text("Отменить правку")}
        if(error.isNotEmpty())Text(error)
    }},confirmButton={TextButton(onClick={runCatching {
        require(name.isNotBlank() && exercises.isNotEmpty() && editing==null)
        val duration=exercises.sumOf {it.seconds*it.sets+it.setRestSeconds*(it.sets-1)+it.restSeconds}.coerceAtLeast(60)
        onSave(Routine(id=existing?.id ?: newId(),name=name.trim(),instructions=notes,minutes=((duration+59)/60).coerceIn(1,240),exercises=exercises))
    }.onFailure {error="Добавьте название и хотя бы одно упражнение"}}){Text("Сохранить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
    if(rigEditor)RigEditor(platform,rig,onDismiss={rigEditor=false},onSave={rig=it;playing=null},onEnableAnimations=onEnableAnimations)
    if(library)ExerciseLibrary(platform,onDismiss={library=false},onEnableAnimations=onEnableAnimations,onSelect={e->selectedCatalogueId=e.catalogueId;technique=e.technique;rig=e.rig;exerciseName=e.name;instruction=e.instructions;seconds=e.seconds.toString();repetitions=e.repetitions.toString();rest=e.restSeconds.toString();sets=e.sets.toString();setRest=e.setRestSeconds.toString();load=e.loadKg?.toString() ?: "";error="";playing=null})
}
