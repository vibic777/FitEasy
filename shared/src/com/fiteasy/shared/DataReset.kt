package com.fiteasy.shared

import com.fiteasy.shared.sync.syncSections

val resetCategories = linkedMapOf("nutrition" to "Питание", "water" to "Вода", "activity" to "Активность", "progress" to "Прогресс", "catalogues" to "Каталоги", "recipes" to "Рецепты", "profile" to "Профиль", "goals" to "Цели", "schedules" to "Расписания", "notes" to "Заметки", "reminders" to "Напоминания")

/** Physical local removal: no deletion messages to peers; backups and secrets are untouched. */
fun resetData(current: Snapshot, sections: Set<String>, all: Boolean = false): Snapshot {
    require(sections.isNotEmpty() && sections.all {it in resetCategories})
    require(syncSections.all {it in resetCategories})
    val selected = if (all) resetCategories.keys else sections
    val d = current.shared
    val keptRecipes = if ("recipes" in selected) emptyList() else d.recipes
    val dependencies = keptRecipes.flatMap {it.ingredients}.map {it.foodId}.toSet()
    val products = if ("catalogues" in selected) d.products.filter {it.id in dependencies} else d.products
    val dishes = if ("catalogues" in selected) d.dishes.filter {it.id in dependencies} else d.dishes
    val shared = d.copy(
        entries = d.entries.filterNot {when(it.type) {"food" -> "nutrition"; "drink" -> "water"; "steps", "activity" -> "activity"; else -> "progress"} in selected},
        products = products, dishes = dishes, recipes = keptRecipes,
        exerciseAssets=if("activity" in selected) emptyList() else d.exerciseAssets,
        routines = if ("activity" in selected) emptyList() else d.routines,
        profile = if ("profile" in selected) Profile() else d.profile,
        goals = if ("goals" in selected) Goals() else d.goals,
        goalHistory = if ("goals" in selected) emptyList() else d.goalHistory,
        estimates = if ("goals" in selected) emptyList() else d.estimates,
        meals = if ("nutrition" in selected) SharedData().meals else d.meals,
        favorites = if ("catalogues" in selected) emptySet() else d.favorites,
        schedules = if ("schedules" in selected) emptyList() else d.schedules,
        noteFolders=if ("notes" in selected) emptyList() else d.noteFolders,
        notes = if ("notes" in selected) emptyList() else d.notes)
    val reminders = if ("reminders" in selected) emptyList() else current.local.reminders.map {r ->
        if ("schedules" in selected && r.scheduleId != null) r.copy(enabled=false, scheduleId=null, rowId=null) else r
    }
    val surviving = shared.entries.map {"entry:${it.id}"} + shared.notes.map {"note:${it.id}"} + shared.schedules.map {"schedule:${it.id}"} + reminders.map {"reminder:${it.id}"}
    return current.copy(shared=shared, local=current.local.copy(reminders=reminders,
        legalNoticeVersion=if(all || selected.containsAll(resetCategories.keys))0 else current.local.legalNoticeVersion,
        phoneCounter=if ("activity" in selected) null else current.local.phoneCounter,
        demo=current.local.demo.filterKeys {it in surviving}))
}
