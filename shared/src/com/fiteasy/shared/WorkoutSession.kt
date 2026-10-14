package com.fiteasy.shared

import kotlinx.serialization.Serializable

@Serializable data class WorkoutSetResult(val exercise:Int,val set:Int,val seconds:Int,val repetitions:Int?=null,val loadKg:Double?=null,val skipped:Boolean=false)
@Serializable data class WorkoutSessionState(
    val routine:Routine,val date:String,val id:String=newId(),
    val exercise:Int=0,val set:Int=0,val stage:String="work",val remaining:Int=routine.exercises.first().seconds,
    val elapsed:Int=0,val workSeconds:Int=0,val awaiting:Boolean=false,val results:List<WorkoutSetResult> = emptyList()
)

fun startWorkout(routine:Routine,date:String):WorkoutSessionState {
    java.time.LocalDate.parse(date)
    val normalized=routine.copy(exercises=routine.exercises.ifEmpty {val total=routine.minutes*60;List((total+3599)/3600){index->Exercise(routine.name,routine.instructions,(total-index*3600).coerceAtMost(3600),0,0)}})
    return WorkoutSessionState(normalized,date)
}
fun validateWorkoutSession(s:WorkoutSessionState){
    validate(SharedData(routines=listOf(s.routine)))
    java.util.UUID.fromString(s.id)
    require(s.stage in setOf("work","rest_set","rest_next","finished") && s.elapsed in 0..86400 && s.workSeconds in 0..86400 && s.remaining in 0..3600)
    java.time.LocalDate.parse(s.date)
    require(s.routine.exercises.isNotEmpty() && s.exercise in s.routine.exercises.indices && s.set in 0 until s.routine.exercises[s.exercise].sets)
    validateWorkoutSets(s.routine,s.results)
    require(s.results.sumOf {it.seconds}+s.workSeconds<=s.elapsed)
}
fun validateWorkoutSets(routine:Routine,results:List<WorkoutSetResult>){
    require(results.size<=2000 && results.map {it.exercise to it.set}.distinct().size==results.size)
    results.forEach {r->require(r.exercise in routine.exercises.indices && r.set in 0 until routine.exercises[r.exercise].sets && r.seconds in 0..86400 && (r.repetitions==null || r.repetitions in 0..1000) && (r.loadKg==null || r.loadKg.isFinite() && r.loadKg in 0.0..500.0))}
}
private fun nextWork(s:WorkoutSessionState):WorkoutSessionState {
    if(s.stage=="rest_set"){val e=s.routine.exercises[s.exercise];return s.copy(set=s.set+1,stage="work",remaining=e.seconds,workSeconds=0,awaiting=false)}
    if(s.exercise==s.routine.exercises.lastIndex)return s.copy(stage="finished",remaining=0,workSeconds=0,awaiting=false)
    val next=s.exercise+1
    return s.copy(exercise=next,set=0,stage="work",remaining=s.routine.exercises[next].seconds,workSeconds=0,awaiting=false)
}
fun confirmWorkoutSet(s:WorkoutSessionState,repetitions:Int?=null,skip:Boolean=false):WorkoutSessionState {
    if(s.stage=="finished")return s
    if(s.stage!="work")return nextWork(s)
    val e=s.routine.exercises[s.exercise]
    require(repetitions==null || repetitions in 0..1000)
    require(skip || e.repetitions==0 || repetitions!=null){"Confirm actual repetitions"}
    val result=WorkoutSetResult(s.exercise,s.set,s.workSeconds,if(skip)null else repetitions,e.loadKg,skip)
    val state=s.copy(results=s.results+result,workSeconds=0,awaiting=false)
    return if(s.set+1<e.sets){val rest=state.copy(stage="rest_set",remaining=e.setRestSeconds);if(e.setRestSeconds==0)nextWork(rest)else rest}
    else {val rest=state.copy(stage="rest_next",remaining=e.restSeconds);if(e.restSeconds==0 || s.exercise==s.routine.exercises.lastIndex)nextWork(rest)else rest}
}
fun tickWorkout(s:WorkoutSessionState,seconds:Int):WorkoutSessionState {
    require(seconds in 0..86400)
    var state=s
    repeat(seconds){
        if(state.stage=="finished" || state.awaiting || state.elapsed>=86400)return state
        state=state.copy(elapsed=state.elapsed+1,workSeconds=state.workSeconds+if(state.stage=="work")1 else 0,remaining=(state.remaining-1).coerceAtLeast(0))
        if(state.remaining==0){
            if(state.stage!="work")state=nextWork(state)
            else {val e=state.routine.exercises[state.exercise];if(e.seconds>0){state=if(e.repetitions>0)state.copy(awaiting=true)else confirmWorkoutSet(state)}}
        }
    }
    return state
}
fun workoutResult(s:WorkoutSessionState):WorkoutResult {
    val completed=s.routine.exercises.indices.filter {i->s.results.count {it.exercise==i && !it.skipped}==s.routine.exercises[i].sets}
    val skipped=s.routine.exercises.indices.filter {i->s.results.any {it.exercise==i && it.skipped}}
    return WorkoutResult(s.routine,completed,skipped,s.elapsed,s.stage=="finished",s.results)
}
