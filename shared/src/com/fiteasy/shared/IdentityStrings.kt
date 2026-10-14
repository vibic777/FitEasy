package com.fiteasy.shared

private val identityKeys="О программе|Версия|Сборка|Дата сборки|Контрибьютер|Язык".split('|')
private val identityRows=mapOf(
"en" to "About|Version|Build|Build date|Contributor|Language",
"be" to "Пра праграму|Версія|Зборка|Дата зборкі|Кантрыб’ютар|Мова",
"fr" to "À propos|Version|Compilation|Date de compilation|Contributeur|Langue",
"de" to "Über die App|Version|Build|Build-Datum|Mitwirkender|Sprache",
"es" to "Acerca de|Versión|Compilación|Fecha de compilación|Colaborador|Idioma",
"it" to "Informazioni|Versione|Build|Data della build|Collaboratore|Lingua",
"zh" to "关于|版本|构建号|构建日期|贡献者|语言",
"ja" to "アプリについて|バージョン|ビルド|ビルド日|貢献者|言語",
"ko" to "앱 정보|버전|빌드|빌드 날짜|기여자|언어"
).mapValues {(_,row)->identityKeys.zip(row.split('|')).toMap()}
fun identityTranslation(text:String,language:String)=identityRows[language]?.get(text)
