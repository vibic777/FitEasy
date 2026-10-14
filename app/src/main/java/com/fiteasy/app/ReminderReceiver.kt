package com.fiteasy.app
import android.content.*
class ReminderReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val pending=goAsync()
  Thread {try {AndroidReminders.handle(context,intent)}catch(e:Exception){com.fiteasy.shared.AppJournal.record("android.reminder.failed",com.fiteasy.shared.JournalLevel.ERROR,e);context.getSharedPreferences("scheduled-reminders",Context.MODE_PRIVATE).edit().putString("error",e.javaClass.simpleName).apply()}finally {pending.finish()}}.start()
 }
}
