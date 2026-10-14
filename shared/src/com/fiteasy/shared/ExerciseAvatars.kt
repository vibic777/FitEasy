package com.fiteasy.shared

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.security.MessageDigest
private val avatarChanges=kotlinx.coroutines.flow.MutableSharedFlow<Platform>(extraBufferCapacity=4)

fun exerciseAvatarKey(e:Exercise):String=MessageDigest.getInstance("SHA-256").digest((e.catalogueId ?: "${e.name}\u0000${e.technique}").toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
/** Local appearance only; never included in shared exercise or LAN records. */
fun readExerciseAvatars(p:Platform):Map<String,Boolean> = synchronized(p){p.readSecret("exercise-avatars")?.let {require(it.length<=100000);codec.decodeFromString<Map<String,Boolean>>(it).also {values->require(values.size<=1000 && values.keys.all {key->key.matches(Regex("[a-f0-9]{64}"))})}} ?: emptyMap()}
fun saveExerciseAvatar(p:Platform,key:String,female:Boolean):Map<String,Boolean> = synchronized(p){
    require(key.matches(Regex("[a-f0-9]{64}")))
    val result=readExerciseAvatars(p)+(key to female)
    require(result.size<=1000)
    p.writeSecret("exercise-avatars",codec.encodeToString(result));avatarChanges.tryEmit(p);result
}
@Composable fun rememberExerciseAvatars(p:Platform):Pair<Map<String,Boolean>,(Exercise,Boolean)->Unit>{
    var values by remember(p){mutableStateOf(emptyMap<String,Boolean>())}
    val scope=rememberCoroutineScope()
    LaunchedEffect(p){
        suspend fun refresh(){withContext(Dispatchers.IO){runCatching {readExerciseAvatars(p)}}.onSuccess {values=it}.onFailure {AppJournal.record("exercise.avatar.read.failed",JournalLevel.ERROR,it)}}
        refresh();avatarChanges.collect {if(it===p)refresh()}
    }
    val save:(Exercise,Boolean)->Unit={e,female->scope.launch {withContext(Dispatchers.IO){runCatching {saveExerciseAvatar(p,exerciseAvatarKey(e),female)}}.onSuccess {values=it}.onFailure {AppJournal.record("exercise.avatar.write.failed",JournalLevel.ERROR,it)}}}
    return values to save
}
