package com.fiteasy.desktop

import com.fiteasy.shared.AppJournal
import com.fiteasy.shared.JournalLevel
import com.fiteasy.shared.RING_MILLIS
import com.fiteasy.shared.ringGain
import com.fiteasy.shared.ringTickDelay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Melody of a reminder on Windows (D097): one of the Windows sounds or an audio file — WAV, AIFF, AU, and since D123
 * MP3 (the javax.sound plug-in) and OGG Vorbis ([OggVorbis]) — with the reminder's own volume and fade-in. The
 * melody plays once, for at most a minute.
 */
object DesktopSound {
    private const val MAX_BYTES=25L*1024*1024
    private val extensions=arrayOf("mp3","ogg","oga","wav","aiff","aif","au")
    private var clip:Clip?=null
    private var timer:java.util.Timer?=null
    /** Id of the reminder whose melody plays now. */
    val ringing=MutableStateFlow<String?>(null)
    /** Told when a melody starts or ends, so the tray menu can offer to silence it. */
    @Volatile var onChange:(()->Unit)?=null
    private fun media()=File(System.getenv("WINDIR") ?: "C:\\Windows","Media")
    private fun systemSound(kind:String):File? = (when(kind){"alarm"->listOf("Alarm01.wav","Windows Notify Calendar.wav");"ringtone"->listOf("Ring01.wav","Windows Ringin.wav");else->listOf("Windows Notify System Generic.wav","Windows Notify.wav")}+"notify.wav").map {File(media(),it)}.firstOrNull {it.isFile}

    fun title(path:String):String = File(path).nameWithoutExtension.let {name->if(File(path).parentFile?.name=="reminder_sounds")name.substringAfter('-') else name}

    /** Opens the file dialog in the Windows sounds folder or in the person's files; null when nothing usable was chosen. */
    fun pick(system:Boolean,data:File,current:String?):Pair<Boolean,String?> {
        val chooser=JFileChooser(if(system)media() else current?.let {File(it).parentFile}?.takeIf {it.isDirectory && it.name!="reminder_sounds"}).apply {fileFilter=FileNameExtensionFilter("MP3, OGG, WAV, AIFF, AU",*extensions);isAcceptAllFileFilterUsed=false}
        if(chooser.showOpenDialog(null)!=JFileChooser.APPROVE_OPTION)return false to null
        val source=chooser.selectedFile
        return true to runCatching {
            require(source.isFile && source.length() in 1..MAX_BYTES && source.extension.lowercase() in extensions)
            require(decode(source,1000).second.isNotEmpty())
            if(source.canonicalFile.parentFile==media().canonicalFile)source.absolutePath
            else File(File(data,"reminder_sounds").apply {mkdirs()},"${UUID.randomUUID().toString().take(8)}-${source.nameWithoutExtension.replace(Regex("[^\\p{L}\\p{N} _.-]"),"_").take(40)}.${source.extension.lowercase()}").also {source.copyTo(it,true)}.absolutePath
        }.onFailure {AppJournal.record("reminder.sound.import.failed",JournalLevel.WARN,it)}.getOrNull()
    }
    /**
     * Where a melody may come from (D130): the Windows sounds folder or the app's own copy in the data folder. The path
     * arrives with the data — also from an imported database file — so any other path is not touched at all: a network
     * path would make Windows sign in to the computer it names. The system sound plays instead.
     */
    fun own(path:String?,data:File):String? {
        if(path.isNullOrBlank() || path.startsWith("\\\\") || path.startsWith("//"))return null
        return runCatching {
            val file=File(path);val parent=file.canonicalFile.parentFile
            path.takeIf {file.extension.lowercase() in extensions && (parent==File(data,"reminder_sounds").canonicalFile || parent==media().canonicalFile)}
        }.getOrNull()
    }
    /** Copied melodies no reminder uses any more. */
    fun prune(data:File,used:Set<String>) {
        File(data,"reminder_sounds").listFiles().orEmpty().filter {it.absolutePath !in used && System.currentTimeMillis()-it.lastModified()>3_600_000}.forEach {it.delete()}
    }
    /**
     * The melody as plain samples, at most [limitMillis] of it and a second more (D123). MP3 and OGG are decoded here; a
     * song of many minutes is never unpacked whole, because a melody rings for a minute at most.
     */
    internal fun decode(file:File,limitMillis:Long):Pair<AudioFormat,ByteArray> = if(OggVorbis.isOgg(file))OggVorbis.decode(file,limitMillis+1000) else AudioSystem.getAudioInputStream(file).use {source->
        val from=source.format
        val plain=from.encoding==AudioFormat.Encoding.PCM_SIGNED || from.encoding==AudioFormat.Encoding.PCM_UNSIGNED
        val samples=if(plain)source else AudioSystem.getAudioInputStream(AudioFormat(AudioFormat.Encoding.PCM_SIGNED,from.sampleRate,16,from.channels,from.channels*2,from.sampleRate,false),source)
        samples.use {
            val format=it.format
            val frames=(format.frameRate.toDouble()*(limitMillis+1000)/1000).toLong()
            val bytes=it.readNBytes((frames*format.frameSize).coerceIn(0,256L*1024*1024).toInt())
            format to bytes.copyOf(bytes.size-bytes.size%format.frameSize)
        }
    }
    private fun gain(control:FloatControl?,value:Float) {
        control ?: return
        val decibels=(20.0*kotlin.math.log10(value.coerceIn(0.0001f,1f).toDouble())).toFloat()
        control.value=decibels.coerceIn(control.minimum,control.maximum)
    }
    @Synchronized fun play(path:String?,kind:String,volumePercent:Int,rampSeconds:Int,reminderId:String?,limitMillis:Long=RING_MILLIS) {
        stop()
        val file=path?.let(::File)?.takeIf {it.isFile} ?: systemSound(kind)
        if(file==null){java.awt.Toolkit.getDefaultToolkit().beep();return}
        runCatching {
            val line=AudioSystem.getClip()
            val (format,samples)=decode(file,limitMillis);require(samples.isNotEmpty());line.open(format,samples,0,samples.size)
            val control=runCatching {line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl}.getOrNull()
            gain(control,ringGain(0,rampSeconds,volumePercent))
            val started=System.currentTimeMillis()
            line.addLineListener {event->if(event.type==LineEvent.Type.STOP)synchronized(this){if(clip===line)stop()}}
            clip=line;ringing.value=reminderId;onChange?.invoke()
            // The timer wakes every quarter of a second only while the volume climbs, then once, to end the melody (D115).
            val t=java.util.Timer("FitEasy-melody",true);timer=t
            fun arm(delay:Long) {runCatching {t.schedule(object:java.util.TimerTask(){override fun run(){synchronized(this@DesktopSound){
                if(clip!==line)return
                val elapsed=System.currentTimeMillis()-started
                if(elapsed>=limitMillis){stop();return}
                runCatching {gain(control,ringGain(elapsed,rampSeconds,volumePercent))}
                arm(ringTickDelay(elapsed,rampSeconds,limitMillis))
            }}},delay)}}
            arm(ringTickDelay(0,rampSeconds,limitMillis))
            line.start()
        }.onFailure {AppJournal.record("reminder.sound.play.failed",JournalLevel.WARN,it);stop();java.awt.Toolkit.getDefaultToolkit().beep()}
    }
    @Synchronized fun stop() {
        timer?.cancel();timer=null
        val line=clip;clip=null
        runCatching {line?.stop()};runCatching {line?.close()}
        val was=ringing.value;ringing.value=null;if(was!=null)onChange?.invoke()
    }
}
