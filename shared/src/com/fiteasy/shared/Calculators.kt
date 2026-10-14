package com.fiteasy.shared

import java.time.LocalDate

/** Mifflin-St Jeor, DOI 10.1093/ajcn/51.2.241. No automatic calorie deficit. */
fun energyEstimate(data:SharedData,date:String=LocalDate.now().toString()):EnergyEstimate {
    val age=data.profile.age ?: error("Введите возраст в профиле")
    require(age in 19..78) {"Автоматическая формула рассчитана для возраста 19–78 лет. Используйте ручные цели."}
    val height=data.profile.heightCm ?: error("Введите рост в профиле")
    val weight=data.entries.filter {it.type=="measurement" && it.name=="Вес" && it.deleted==null && it.date<=date}.maxWithOrNull(compareBy<Entry>{it.date}.thenBy {it.updated})?.amount ?: error("Добавьте запись веса")
    require(weight in 30.0..300.0 && height in 100.0..250.0) {"Для этих исходных данных используйте ручные цели"}
    val resting=10*weight+6.25*height-5*age+if(data.profile.female)-161 else 5
    require(resting>0)
    return EnergyEstimate(date=date,age=age,heightCm=height,weightKg=weight,female=data.profile.female,activityFactor=data.profile.activityFactor,restingKcal=resting,dailyKcal=resting*data.profile.activityFactor)
}
fun sameInputs(a:EnergyEstimate,b:EnergyEstimate)=a.age==b.age && a.heightCm==b.heightCm && a.weightKg==b.weightKg && a.female==b.female && a.activityFactor==b.activityFactor
fun applyEstimate(data:SharedData,estimate:EnergyEstimate):SharedData {
    val goals=data.goals.copy(kcal=kotlin.math.round(estimate.dailyKcal))
    return data.copy(goals=goals,goalHistory=goalHistory(data,estimate.date,goals),estimates=data.estimates.filterNot {it.id==estimate.id}+estimate.copy(applied=true))
}
fun goalHistory(data:SharedData,date:String,goals:Goals):List<GoalRevision> =
    (if(data.goalHistory.isEmpty())listOf(GoalRevision("0001-01-01",data.goals))else data.goalHistory)+GoalRevision(date,goals)
