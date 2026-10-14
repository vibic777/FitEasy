package com.fiteasy.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.time.Instant

enum class JournalExportType(val extension:String,val mime:String){JSON("json","application/json"),TXT("txt","text/plain"),LOG("log","text/plain")}
@Serializable data class ExportedJournalLine(val timestamp:String?=null,val level:String?=null,val event:String?=null,val stack:String?=null,val raw:String?=null)
@Serializable data class ExportedJournal(val format:String="fiteasy-journal",val version:Int=1,val appVersion:String=BuildInfo.VERSION,val build:Int=BuildInfo.NUMBER,val generatedAt:String,val entries:List<ExportedJournalLine>)
fun journalExport(text:String,type:JournalExportType,now:Instant=Instant.now()):ByteArray {
    if(type!=JournalExportType.JSON)return text.toByteArray(Charsets.UTF_8)
    val entries=text.lineSequence().filter {it.isNotBlank()}.map {line->
        val fields=line.split(' ',limit=4)
        if(fields.size>=3 && runCatching {Instant.parse(fields[0])}.isSuccess && fields[1] in JournalLevel.entries.map {it.name} && fields[2].matches(Regex("[a-zA-Z0-9._-]+")))ExportedJournalLine(fields[0],fields[1],fields[2],fields.getOrNull(3))else ExportedJournalLine(raw=line)
    }.toList()
    return codec.encodeToString(ExportedJournal(generatedAt=now.toString(),entries=entries)).toByteArray(Charsets.UTF_8)
}
