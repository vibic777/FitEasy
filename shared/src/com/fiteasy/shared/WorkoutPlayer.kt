package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun WorkoutPlayer(platform:Platform,initial:WorkoutSessionState,onDismiss:()->Unit,onSave:(WorkoutResult)->Boolean){
    var state by remember(initial){mutableStateOf(initial)}
    var paused by remember(initial){mutableStateOf(true)}
    var stopping by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    val epoch=remember(platform,initial){WorkoutDraftStore.token(platform)}
    val clock=remember(initial){ActiveSecondsClock()}
    val language=LocalLanguage.current
    val exercise=state.routine.exercises[state.exercise]
    val (avatars,setAvatar)=rememberExerciseAvatars(platform)
    val female=avatars[exerciseAvatarKey(exercise)] ?: true
    var repetitions by remember(state.exercise,state.set){mutableStateOf(exercise.repetitions.toString())}
    LaunchedEffect(platform){platform.foreground.collect {if(!it)paused=true}}
    LaunchedEffect(state){withContext(Dispatchers.IO){runCatching {WorkoutDraftStore.write(platform,state,epoch)}}.onFailure {AppJournal.record("workout.draft.failed",JournalLevel.ERROR,it);error="Черновик не записан. Сохраните результат перед выходом."}}
    LaunchedEffect(paused,state.stage,state.exercise,state.set){
        if(paused || state.stage=="finished" || state.awaiting || state.elapsed>=86400)return@LaunchedEffect
        clock.resume(System.nanoTime())
        try {while(!paused && state.stage!="finished"){
            delay(100)
            state=tickWorkout(state,clock.sample(System.nanoTime()))
            if(state.awaiting || state.elapsed>=86400)paused=true
        }}finally {clock.pause(System.nanoTime())}
    }
    fun discard(){WorkoutDraftStore.clear(platform);onDismiss()}
    fun save(){paused=true;if(onSave(workoutResult(state)))WorkoutDraftStore.clear(platform)}
    fun confirm(skip:Boolean=false){runCatching {val resume=state.awaiting;state=confirmWorkoutSet(state,if(!skip && state.stage=="work" && exercise.repetitions>0)repetitions.toInt()else null,skip);if(resume)paused=false}.onFailure {error="Введите фактические повторы от 0 до 1000"}}
    fun skipExercise(){val index=state.exercise;while(state.exercise==index && state.stage!="finished")state=confirmWorkoutSet(state,skip=true)}
    SettingsPage(onDismissRequest={paused=true;stopping=true},title={Text(state.routine.name)},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(state.stage=="finished")Text("Комплекс завершён")else {
            Text("${state.exercise+1}/${state.routine.exercises.size}: ${exercise.name}",style=MaterialTheme.typography.titleMedium)
            Text(exercise.instructions)
            ExerciseSilhouette(exercise,running=!paused && state.stage=="work" && !state.awaiting,female=female)
            Row {FilterChip(selected=female,onClick={setAvatar(exercise,true)},label={Text("Женский силуэт")});FilterChip(selected=!female,onClick={setAvatar(exercise,false)},label={Text("Мужской силуэт")})}
            Text(workoutText("Подход {0}/{1}",language,state.set+1,exercise.sets)+(exercise.loadKg?.let {" · $it kg"} ?: ""))
            Text(workoutText(when(state.stage){"rest_set"->"Отдых между подходами: {0} с";"rest_next"->"До следующего упражнения: {0} с";else->"Осталось: {0} с"},language,state.remaining))
            if(state.stage=="work" && exercise.repetitions>0)OutlinedTextField(repetitions,{repetitions=it},label={Text("Фактические повторы (подтвердить кнопкой)")})
        }
        Text(workoutText("Фактическое время: {0}",language,"${state.elapsed/60}:${(state.elapsed%60).toString().padStart(2,'0')}"))
        if(state.elapsed>=86400)Text("Достигнут предел 24 часа. Сохраните результат или завершите сеанс.")
        if(state.stage!="finished")FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(enabled=!state.awaiting && state.elapsed<86400,onClick={paused=!paused}){Text(if(paused)"Продолжить"else "Пауза")}
            OutlinedButton(onClick={confirm()}){Text("Готово")}
            OutlinedButton(onClick={confirm(true)}){Text("Пропустить подход")}
            OutlinedButton(onClick=::skipExercise){Text("Пропустить упражнение")}
        }
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Text("Не выполняйте движения через боль. Можно пропустить упражнение или остановиться.")
    }},confirmButton={TextButton(enabled=state.elapsed>0,onClick=::save){Text("Сохранить результат")}},dismissButton={TextButton(onClick={paused=true;stopping=true}){Text("Остановить")}})
    if(stopping)AlertDialog(onDismissRequest={stopping=false},title={Text("Завершить выполнение?")},text={Text("Можно сохранить фактическое время. Закрытие без сохранения удалит черновик этой тренировки.")},confirmButton={TextButton(onClick=::discard){Text("Без сохранения")}},dismissButton={Row {TextButton(enabled=state.elapsed>0,onClick=::save){Text("Сохранить")};TextButton(onClick={stopping=false}){Text("Вернуться")}}})
}
