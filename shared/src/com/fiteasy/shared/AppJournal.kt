package com.fiteasy.shared

import java.io.File
import java.io.RandomAccessFile
import java.time.Instant
import java.util.concurrent.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

enum class JournalLevel {DEBUG,INFO,WARN,ERROR,CRITICAL,FATAL}
@Serializable data class JournalOptions(val enabled:Boolean=true,val levels:Set<String> = JournalLevel.entries.filter {it!=JournalLevel.FATAL}.map {it.name}.toSet(),val retentionHours:Int=24,val clearDays:Int=1)
@Serializable private data class JournalConfig(val options:JournalOptions=JournalOptions(),val lastClean:Long=0)
fun validateJournalOptions(o:JournalOptions){require(o.retentionHours in 1..8760 && o.clearDays in setOf(0,1,7,30) && o.levels.all {it in JournalLevel.entries.map {level->level.name}})}
fun acceptsJournal(level:JournalLevel,o:JournalOptions)=level==JournalLevel.FATAL || (o.enabled && level.name in o.levels)
fun journalCleanupDue(last:Long,now:Long,days:Int)=days>0 && now>=last && now-last>=days*86400000L

/** Isolated store also used in tests. Fixed event IDs and message-free exception stacks only. */
class JournalStore(private val directory:File,private val clock:()->Long=System::currentTimeMillis) {
    companion object {const val MAX_BYTES=512*1024L;private val EVENT=Regex("[a-zA-Z0-9._-]+")}
    private val current=File(directory,"app.log")
    private val previous=File(directory,"app.1.log")
    private val configFile=File(directory,"options.json")
    @Volatile private var config=JournalConfig(lastClean=clock())
    init {
        check(directory.isDirectory || directory.mkdirs())
        val loaded=runCatching {require(configFile.isFile && configFile.length()<16384);codec.decodeFromString<JournalConfig>(configFile.readText()).also {validateJournalOptions(it.options);require(it.lastClean>0)}}.getOrNull()
        if(loaded!=null)config=loaded else saveConfig(config)
    }
    fun options()=config.options
    @Synchronized fun setOptions(options:JournalOptions){validateJournalOptions(options);val next=config.copy(options=options);saveConfig(next);config=next}
    private fun saveConfig(value:JournalConfig){val temp=File(directory,"options.tmp");temp.writeText(codec.encodeToString(value));java.nio.file.Files.move(temp.toPath(),configFile.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)}
    private fun tail(file:File):String=if(!file.isFile)""else RandomAccessFile(file,"r").use {f->val start=(f.length()-MAX_BYTES).coerceAtLeast(0);f.seek(start);val bytes=ByteArray((f.length()-start).toInt());f.readFully(bytes);String(bytes,Charsets.UTF_8).let {if(start>0)it.substringAfter('\n',"")else it}}
    @Synchronized fun read()=tail(previous)+tail(current)
    @Synchronized fun clear(){check(!previous.exists() || previous.delete());check(!current.exists() || current.delete());val next=config.copy(lastClean=clock());saveConfig(next);config=next}
    @Synchronized fun append(level:JournalLevel,event:String,error:Throwable?=null) {
        if(!acceptsJournal(level,config.options))return
        val now=clock()
        if(journalCleanupDue(config.lastClean,now,config.options.clearDays)){
            val cutoff=now-config.options.retentionHours*3600000L
            listOf(previous,current).filter {it.isFile}.forEach {file->file.writeText(tail(file).lineSequence().filter {line->runCatching {Instant.parse(line.substringBefore(' ')).toEpochMilli()>=cutoff}.getOrDefault(false)}.joinToString("\n").let {if(it.isEmpty())it else "$it\n"})}
            val next=config.copy(lastClean=now);saveConfig(next);config=next
        }
        val safe=event.takeIf {it.length<=120 && it.matches(EVENT)} ?: "invalid-event"
        val stack=buildString {
            var cause=error
            repeat(4){val e=cause ?: return@repeat;append(" | ").append(e.javaClass.name);e.stackTrace.take(8).forEach {s->append(" ").append(s.className).append('.').append(s.methodName).append(':').append(s.lineNumber)};cause=e.cause}
        }.take(7000).replace('\n',' ').replace('\r',' ')
        val line="${Instant.ofEpochMilli(now)} ${level.name} $safe$stack\n"
        if(current.length()+line.toByteArray().size>MAX_BYTES){check(!previous.exists() || previous.delete());if(current.exists())check(current.renameTo(previous))}
        current.appendText(line)
    }
}

object AppJournal {
    @Volatile private var store:JournalStore?=null
    private val writer=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,ArrayBlockingQueue(256),{r->Thread(r,"FitEasy-journal").apply {isDaemon=true}},ThreadPoolExecutor.DiscardPolicy())
    @Volatile private var crashInstalled=false
    @Synchronized fun install(directory:File,platform:String) {
        if(store==null)store=runCatching {JournalStore(directory)}.getOrNull()
        if(!crashInstalled){
            val previous=Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler {thread,error->
                fatal("crash.uncaught",error)
                if(previous!=null)previous.uncaughtException(thread,error)else kotlin.system.exitProcess(1)
            }
            crashInstalled=true
        }
        record("app.start.$platform.v${BuildInfo.VERSION}.b${BuildInfo.NUMBER}")
    }
    fun record(event:String,level:JournalLevel=JournalLevel.INFO,error:Throwable?=null){val target=store ?: return;if(!acceptsJournal(level,target.options()))return;runCatching {writer.execute {runCatching {target.append(level,event,error)}}}}
    fun fatal(event:String,error:Throwable?=null){runCatching {store?.append(JournalLevel.FATAL,event,error)}}
    fun options()=store?.options() ?: JournalOptions()
    fun setOptions(options:JournalOptions){store?.setOptions(options) ?: error("Journal unavailable")}
    fun read()=store?.read() ?: ""
    fun clear(){store?.clear()}
}
