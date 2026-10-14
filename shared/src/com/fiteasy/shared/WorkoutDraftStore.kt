package com.fiteasy.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.WeakHashMap

/** Epoch prevents a queued draft write from recreating data after save/discard/local wipe. */
object WorkoutDraftStore {
    private val epochs=WeakHashMap<Platform,Long>()
    fun token(platform:Platform):Long=synchronized(platform){synchronized(epochs){epochs[platform] ?: 0L}}
    fun invalidate(platform:Platform)=synchronized(platform){synchronized(epochs){epochs[platform]=(epochs[platform] ?: 0L)+1}}
    fun read(platform:Platform):WorkoutSessionState?=synchronized(platform){platform.readSecret("workout-draft")?.let {text->require(text.length<=1_048_576);codec.decodeFromString<WorkoutSessionState>(text).also(::validateWorkoutSession)}}
    fun recoverable(platform:Platform):WorkoutSessionState?=synchronized(platform){
        val state=read(platform) ?: return@synchronized null
        if(platform.load().shared.entries.any {it.id==state.id}){clear(platform);null}else state
    }
    fun write(platform:Platform,state:WorkoutSessionState,expectedEpoch:Long):Boolean=synchronized(platform){
        if(token(platform)!=expectedEpoch)return@synchronized false
        validateWorkoutSession(state)
        val text=codec.encodeToString(state);require(text.length<=1_048_576)
        platform.writeSecret("workout-draft",text);true
    }
    fun clear(platform:Platform)=synchronized(platform){invalidate(platform);platform.writeSecret("workout-draft",null)}
}
