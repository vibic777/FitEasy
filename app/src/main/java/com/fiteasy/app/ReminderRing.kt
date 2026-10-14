package com.fiteasy.app

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.OpenableColumns
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.fiteasy.shared.AppJournal
import com.fiteasy.shared.JournalLevel
import com.fiteasy.shared.RING_MILLIS
import com.fiteasy.shared.ringGain
import com.fiteasy.shared.ringTickDelay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID

private fun alarmAudio()=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
private fun systemTone(kind:String)=RingtoneManager.getDefaultUri(when(kind){"alarm"->RingtoneManager.TYPE_ALARM;"ringtone"->RingtoneManager.TYPE_RINGTONE;else->RingtoneManager.TYPE_NOTIFICATION}) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

/**
 * The melody of a reminder (D097, after Notio's reminder sounds): one of the phone's own sounds, chosen with the system
 * picker, or an audio file. A chosen file is copied into the app so it plays even when the original is moved; the app
 * plays it itself, so no other process needs access to it.
 */
object ReminderSounds {
    private const val DIRECTORY="reminder_sounds"
    private const val MAX_BYTES=25L*1024*1024
    private val extensions=setOf("mp3","m4a","aac","ogg","oga","wav","flac","opus","amr","3gp")
    private var preview:MediaPlayer?=null
    private val handler=Handler(Looper.getMainLooper())

    /** Copies the chosen file; null when it is not audio, too big or unreadable. */
    fun importFile(context:Context,source:Uri):String? = runCatching {
        val resolver=context.contentResolver
        val type=resolver.getType(source).orEmpty()
        val name=resolver.query(source,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {if(it.moveToFirst())it.getString(0) else null}.orEmpty()
        val extension=name.substringAfterLast('.',"").lowercase().takeIf {it in extensions} ?: when {
            type.contains("mpeg")->"mp3";type.contains("mp4") || type.contains("aac")->"m4a";type.contains("ogg")->"ogg";type.contains("wav")->"wav";type.startsWith("audio/")->"mp3";else->return null
        }
        val safe=name.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N} _.-]"),"_").take(40).ifBlank {"melody"}
        val target=File(File(context.filesDir,DIRECTORY).apply {mkdirs()},"${UUID.randomUUID().toString().take(8)}-$safe.$extension")
        resolver.openInputStream(source)?.use {input->target.outputStream().use {out->
            var total=0L;val buffer=ByteArray(64*1024)
            while(true){val n=input.read(buffer);if(n<0)break;total+=n;if(total>MAX_BYTES){out.close();target.delete();return null};out.write(buffer,0,n)}
        }} ?: return null
        if(target.length()==0L){target.delete();return null}
        Uri.fromFile(target).toString()
    }.onFailure {AppJournal.record("reminder.sound.import.failed",JournalLevel.WARN,it)}.getOrNull()

    /**
     * Where a melody may come from (D130): one of the phone's own sounds or the app's own copy of a file. The address
     * arrives with the data — also from an imported database file — so anything else, a network address above all, is
     * not opened and the system tone plays instead.
     */
    fun own(context:Context,uri:String?):Uri? {
        val parsed=uri?.takeIf {it.isNotBlank()}?.let(Uri::parse) ?: return null
        return when(parsed.scheme) {
            "content"->parsed
            "file"->parsed.takeIf {runCatching {File(it.path!!).canonicalFile.parentFile==File(context.filesDir,DIRECTORY).canonicalFile}.getOrDefault(false)}
            else->null
        }
    }
    fun title(context:Context,uri:String):String {
        val parsed=own(context,uri) ?: return ""
        if(parsed.scheme=="file")return parsed.lastPathSegment.orEmpty().substringAfter('-').substringBeforeLast('.')
        return runCatching {RingtoneManager.getRingtone(context,parsed)?.getTitle(context)}.getOrNull() ?: parsed.lastPathSegment.orEmpty()
    }
    /** Files of melodies no reminder uses any more. */
    fun prune(context:Context,used:Set<String>) {
        File(context.filesDir,DIRECTORY).listFiles().orEmpty().filter {Uri.fromFile(it).toString() !in used && System.currentTimeMillis()-it.lastModified()>3_600_000}.forEach {it.delete()}
    }
    fun player(context:Context,uri:String?,kind:String):MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(alarmAudio())
        runCatching {setDataSource(context,own(context,uri) ?: systemTone(kind))}.getOrElse {reset();setAudioAttributes(alarmAudio());setDataSource(context,systemTone(kind))}
        prepare()
    }
    /** A preview of at most fifteen seconds, at the chosen volume. */
    @Synchronized fun preview(context:Context,uri:String?,kind:String,volumePercent:Int) {
        stopPreview()
        runCatching {
            val gain=ringGain(0,0,volumePercent)
            preview=player(context.applicationContext,uri,kind).apply {setVolume(gain,gain);setOnCompletionListener {stopPreview()};start()}
            handler.postDelayed({stopPreview()},15_000)
        }.onFailure {AppJournal.record("reminder.sound.preview.failed",JournalLevel.WARN,it);stopPreview()}
    }
    @Synchronized fun stopPreview() {
        handler.removeCallbacksAndMessages(null)
        runCatching {preview?.stop()};runCatching {preview?.release()};preview=null
    }
}

/**
 * Plays the melody of a reminder that has its own sound, volume or fade-in (D097, after Notio's ring service): a
 * notification channel can do none of these. A foreground service whose notification is the reminder's own; it plays on
 * the alarm stream, loops for at most a minute, waits while a call or another alarm has the sound, and stops when the
 * person silences it or answers the reminder. When Android refuses the service the reminder is still shown, without sound.
 */
class ReminderRingService:Service() {
    private var player:MediaPlayer?=null
    private val handler=Handler(Looper.getMainLooper())
    private var focus:AudioFocusRequest?=null
    private var paused=false
    private var tick:Runnable?=null
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==STOP || intent?.action==STOP_REMOVE) {
            release()
            ServiceCompat.stopForeground(this,if(intent.action==STOP_REMOVE)ServiceCompat.STOP_FOREGROUND_REMOVE else ServiceCompat.STOP_FOREGROUND_DETACH)
            stopSelf();return START_NOT_STICKY
        }
        @Suppress("DEPRECATION") val notification=intent?.getParcelableExtra<Notification>("notification")
        val notificationId=intent?.getIntExtra("notificationId",0) ?: 0
        if(notification==null){stopSelf();return START_NOT_STICKY}
        val foreground=runCatching {ServiceCompat.startForeground(this,notificationId,notification,if(Build.VERSION.SDK_INT>=29)ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)}
        if(foreground.isFailure) {
            AppJournal.record("reminder.ring.foreground.refused",JournalLevel.WARN,foreground.exceptionOrNull())
            runCatching {NotificationManagerCompat.from(this).notify(notificationId,notification)}
            stopSelf();return START_NOT_STICKY
        }
        release()
        val volume=intent.getIntExtra("volume",100);val ramp=intent.getIntExtra("ramp",0)
        val started=runCatching {
            player=ReminderSounds.player(applicationContext,intent.getStringExtra("sound"),intent.getStringExtra("kind") ?: "alarm").apply {
                // The processor stays awake only while the melody plays, so the end of the ring is never slept through (D115).
                runCatching {setWakeMode(applicationContext,android.os.PowerManager.PARTIAL_WAKE_LOCK)}
                isLooping=true;val gain=ringGain(0,ramp,volume);setVolume(gain,gain);start()
            }
        }.onFailure {AppJournal.record("reminder.ring.player.failed",JournalLevel.WARN,it)}.isSuccess
        if(!started){release();ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_DETACH);stopSelf();return START_NOT_STICKY}
        takeFocus()
        ringing.value=intent.getStringExtra("reminderId")
        val startedAt=System.currentTimeMillis()
        // Woken every quarter of a second only while the volume climbs; after that once, to end the ring (D115).
        val step=object:Runnable {override fun run() {
            val elapsed=System.currentTimeMillis()-startedAt;val gain=ringGain(elapsed,ramp,volume)
            if(elapsed>=RING_MILLIS){release();ServiceCompat.stopForeground(this@ReminderRingService,ServiceCompat.STOP_FOREGROUND_DETACH);stopSelf();return}
            if(!paused)runCatching {player?.setVolume(gain,gain)}
            handler.postDelayed(this,ringTickDelay(elapsed,ramp))
        }}
        tick=step;handler.post(step)
        return START_NOT_STICKY
    }
    private fun takeFocus() {
        val audio=getSystemService(AudioManager::class.java) ?: return
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(alarmAudio()).setOnAudioFocusChangeListener({change->when(change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK->{paused=true;runCatching {player?.pause()}}
            // The minute may have run out while a call had the sound: the step decides first whether to go on.
            AudioManager.AUDIOFOCUS_GAIN->if(paused){paused=false;runCatching {player?.start()};tick?.let {handler.removeCallbacks(it);handler.post(it)}}
            AudioManager.AUDIOFOCUS_LOSS->{release();ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_DETACH);stopSelf()}
        }},handler).build()
        focus=request
        // Refused only during a call: the melody plays anyway, a reminder must not be lost.
        runCatching {audio.requestAudioFocus(request)}
    }
    private fun release() {
        handler.removeCallbacksAndMessages(null);tick=null
        runCatching {player?.stop()};runCatching {player?.release()};player=null;paused=false
        focus?.let {request->runCatching {getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)}};focus=null
        ringing.value=null
    }
    override fun onDestroy() {release();super.onDestroy()}

    companion object {
        private const val STOP="com.fiteasy.app.STOP_RINGING"
        private const val STOP_REMOVE="com.fiteasy.app.STOP_RINGING_REMOVE"
        /** Id of the reminder whose melody plays or waits for the sound now. */
        val ringing=MutableStateFlow<String?>(null)
        /** False when the system does not let the service start now. */
        fun start(context:Context,notificationId:Int,notification:Notification,reminderId:String,sound:String?,kind:String,volume:Int,ramp:Int):Boolean = runCatching {
            context.startForegroundService(Intent(context,ReminderRingService::class.java).putExtra("notification",notification).putExtra("notificationId",notificationId).putExtra("reminderId",reminderId).putExtra("sound",sound).putExtra("kind",kind).putExtra("volume",volume).putExtra("ramp",ramp))
            true
        }.onFailure {AppJournal.record("reminder.ring.start.refused",JournalLevel.WARN,it)}.getOrDefault(false)
        /** Ends the melody; the notification stays. Harmless when nothing plays. */
        fun stop(context:Context) {if(ringing.value!=null)runCatching {context.startService(Intent(context,ReminderRingService::class.java).setAction(STOP))}}
        /** Ends the melody and takes its notification away: the reminder was answered. */
        fun stopAndRemove(context:Context) {runCatching {context.startService(Intent(context,ReminderRingService::class.java).setAction(STOP_REMOVE))}}
    }
}
