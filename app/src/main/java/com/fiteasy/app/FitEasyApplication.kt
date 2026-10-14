package com.fiteasy.app

import android.app.Application
import android.app.ActivityManager
import android.os.Build
import com.fiteasy.shared.*
import java.io.File

class FitEasyApplication:Application() {
    override fun onCreate(){
        super.onCreate()
        AppJournal.install(File(filesDir,"logs"),"android")
        if(Build.VERSION.SDK_INT>=30)Thread({runCatching {
            val prefs=getSharedPreferences("journal-exit",MODE_PRIVATE)
            val last=prefs.getLong("seen",0)
            val exits=getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(packageName,0,5)
            exits.filter {it.timestamp>last}.sortedBy {it.timestamp}.forEach {if(it.reason in setOf(4,5,6))AppJournal.fatal("android.previous-exit.reason.${it.reason}")else AppJournal.record("android.previous-exit.reason.${it.reason}",JournalLevel.WARN)}
            exits.maxOfOrNull {it.timestamp}?.let {prefs.edit().putLong("seen",it).apply()}
        }.onFailure {AppJournal.record("android.exit-history.failed",JournalLevel.ERROR,it)}},"FitEasy-exit-history").start()
    }
}
