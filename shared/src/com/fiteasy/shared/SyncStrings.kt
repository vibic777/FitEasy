package com.fiteasy.shared

private val syncLayoutKeys=listOf("Подключение","Параметры","Ручной адрес")
internal val syncLayoutRows=mapOf(
    "en" to "Connection|Options|Manual address",
    "ru" to "Подключение|Параметры|Ручной адрес",
    "be" to "Падключэнне|Параметры|Адрас уручную",
    "fr" to "Connexion|Options|Adresse manuelle",
    "de" to "Verbindung|Optionen|Manuelle Adresse",
    "es" to "Conexión|Opciones|Dirección manual",
    "it" to "Connessione|Opzioni|Indirizzo manuale",
    "zh" to "连接|选项|手动地址",
    "ja" to "接続|設定|手動アドレス",
    "ko" to "연결|옵션|수동 주소"
)
fun syncLayoutTranslation(text:String,language:String)=syncLayoutKeys.indexOf(text).takeIf {it>=0}?.let {syncLayoutRows[language]?.split('|')?.get(it)}
