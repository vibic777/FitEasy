package com.fiteasy.shared

data class AppNavigation(val token:String=newId(),val destination:String,val reminderId:String?=null)
data class AppDestination(val screen:String,val settings:String="",val dialog:String?=null)
val appDestinations=linkedMapOf(
    "today" to AppDestination("today"),"food" to AppDestination("food"),
    "water" to AppDestination("water"),"activity" to AppDestination("activity"),
    "progress" to AppDestination("progress"),"notes" to AppDestination("notes"),
    "profile" to AppDestination("profile","personal"),"settings" to AppDestination("profile"),
    "reminders" to AppDestination("profile","reminders","reminder-status"),
    "reminder-status" to AppDestination("profile","reminders","reminder-status"),
    "schedules" to AppDestination("profile","reminders","schedules"),
    "calendar" to AppDestination("profile","reminders","calendar"),
    "help" to AppDestination("profile","help","help"),
    "about" to AppDestination("profile","help","about"))
