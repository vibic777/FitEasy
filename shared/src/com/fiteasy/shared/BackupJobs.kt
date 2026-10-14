package com.fiteasy.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/** Pure work intended for an IO dispatcher; platform file pickers remain on the UI thread. */
fun prepareBackup(data:SharedData,stock:List<Food>,appearance:Appearance?,password:String):ByteArray {
    val materialized=materializeDependencies(data,stock)
    validate(materialized)
    appearance?.let {validateLocal(LocalData(appearance=it))}
    return BackupCrypto.encode(codec.encodeToString(Backup(data=materialized,appearance=appearance)).toByteArray(Charsets.UTF_8),password)
}
fun readBackup(bytes:ByteArray,password:String):Backup {
    val backup=codec.decodeFromString<Backup>(String(BackupCrypto.decode(bytes,password),Charsets.UTF_8))
    require(backup.format=="fiteasy" && backup.version in 1..5)
    validate(backup.data)
    backup.appearance?.let {validateLocal(LocalData(appearance=it))}
    return backup
}
