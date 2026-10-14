package com.fiteasy.shared.sync

import com.fiteasy.shared.*

/** Preserve writes made after the user started a merge/copy transfer. Replacement requires a stable snapshot. */
fun preserveConcurrent(original:SharedData,current:SharedData,received:SharedData):Pair<SharedData,Int> {
    var count=0
    fun <T> records(before:List<T>,now:List<T>,result:List<T>,id:(T)->String):List<T> {
        val a=before.associateBy(id);val b=now.associateBy(id)
        val changed=(a.keys+b.keys).filter {a[it]!=b[it]}.toSet();count+=changed.size
        return result.filterNot {id(it) in changed}+now.filter {id(it) in changed}
    }
    val result=received.copy(
        noteFolders=records(original.noteFolders,current.noteFolders,received.noteFolders){it.id},
        notes=records(original.notes,current.notes,received.notes){it.id},
        schedules=records(original.schedules,current.schedules,received.schedules){it.id},
        entries=records(original.entries,current.entries,received.entries){it.id},
        products=records(original.products,current.products,received.products){it.id},
        dishes=records(original.dishes,current.dishes,received.dishes){it.id},
        recipes=records(original.recipes,current.recipes,received.recipes){it.id},
        routines=records(original.routines,current.routines,received.routines){it.id},
        exerciseAssets=records(original.exerciseAssets,current.exerciseAssets,received.exerciseAssets){it.id},
        estimates=records(original.estimates,current.estimates,received.estimates){it.id},
        profile=if(current.profile!=original.profile)current.profile else received.profile,
        goals=if(current.goals!=original.goals)current.goals else received.goals,
        goalHistory=(received.goalHistory+current.goalHistory.filterNot {it in original.goalHistory}).distinct(),
        meals=if(current.meals!=original.meals)current.meals else received.meals,
        favorites=(received.favorites+(current.favorites-original.favorites))-(original.favorites-current.favorites)
    )
    validate(result);return result to count
}
