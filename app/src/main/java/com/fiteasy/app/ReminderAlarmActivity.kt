package com.fiteasy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.AppJournal
import com.fiteasy.shared.JournalLevel
import com.fiteasy.shared.alarmScreenMillis
import com.fiteasy.shared.translate

/**
 * Reminder shown like an alarm clock over the lock screen (D086). It shows only the reminder's own name and comment and
 * three actions; the diary stays behind the lock screen and the app lock.
 */
class ReminderAlarmActivity:ComponentActivity() {
    // One screen for all reminders: a second one that goes off while it is open replaces what is shown, so the buttons
    // always answer the reminder on the screen (D115).
    private var shown by mutableStateOf<android.content.Intent?>(null)
    // The screen is not held longer than needed (D121, rule 10): the page closes itself, the notification keeps the reminder.
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val close=Runnable {AppJournal.record("reminder.alarm.screen.closed");finish()}
    private fun closeLater(intent:android.content.Intent) {handler.removeCallbacks(close);handler.postDelayed(close,alarmScreenMillis(intent.getIntExtra("count",1)))}
    override fun onDestroy() {handler.removeCallbacks(close);super.onDestroy()}
    override fun onNewIntent(intent:android.content.Intent) {super.onNewIntent(intent);if(intent.getStringExtra("id")!=null){setIntent(intent);shown=intent;closeLater(intent)}}
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true);setTurnScreenOn(true)
        if(intent.getStringExtra("id")==null){finish();return}
        shown=intent;closeLater(intent)
        setContent {
        val intent=shown ?: return@setContent
        val id=intent.getStringExtra("id") ?: return@setContent
        val name=intent.getStringExtra("name").orEmpty();val comment=intent.getStringExtra("comment").orEmpty()
        val snooze=intent.getIntExtra("snooze",10);val confirm=intent.getBooleanExtra("confirm",false);val language=intent.getStringExtra("language") ?: "ru"
        fun act(action:String){
            val app=applicationContext
            // Keystore work stays off the main thread; the screen closes at once.
            if(id!="test")Thread {runCatching {AndroidReminders.control(app,id,action)}.onFailure {AppJournal.record("android.reminder.alarm.failed",JournalLevel.ERROR,it)}}.start()
            else androidx.core.app.NotificationManagerCompat.from(app).cancel("test",0)
            finish()
        }
        MaterialTheme(colorScheme=if(androidx.compose.foundation.isSystemInDarkTheme())darkColorScheme()else lightColorScheme()) {Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(32.dp),verticalArrangement=Arrangement.spacedBy(20.dp,Alignment.CenterVertically),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("FitEasy",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                Text(name,style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
                if(comment.isNotBlank())Text(comment,style=MaterialTheme.typography.bodyLarge,textAlign=TextAlign.Center)
                if(confirm)Button(onClick={act("ack")},modifier=Modifier.fillMaxWidth().height(56.dp)){Text(translate("Подтвердить",language))}
                Button(onClick={act("snooze")},modifier=Modifier.fillMaxWidth().height(56.dp)){Text("${translate("Отложить",language)} $snooze")}
                OutlinedButton(onClick={act("interrupt")},modifier=Modifier.fillMaxWidth().height(56.dp)){Text(translate("Прервать",language))}
            }
        }}
        }
    }
}
