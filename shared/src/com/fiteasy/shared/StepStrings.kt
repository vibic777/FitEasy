package com.fiteasy.shared

internal val stepKeys="Step counting mode|Automatic|System counter|System detector|Software pedometer|Software steps: approximate; higher battery use|No sensor events; try another mode|Step diagnostics".split('|')
internal val stepRows=mapOf(
"en" to stepKeys.joinToString("|"),
"ru" to "Режим шагомера|Автоматически|Системный счётчик|Системный детектор|Программный шагомер|Программный подсчёт: приблизительно, расход батареи выше|Нет событий датчика: попробуйте другой режим|Диагностика шагомера",
"be" to "Рэжым крокамера|Аўтаматычна|Сістэмны лічыльнік|Сістэмны дэтэктар|Праграмны крокамер|Праграмны падлік: прыблізна, расход батарэі вышэй|Няма падзей датчыка: паспрабуйце іншы рэжым|Дыягностыка крокамера",
"fr" to "Mode du podomètre|Automatique|Compteur système|Détecteur système|Podomètre logiciel|Comptage logiciel : approximatif, consommation accrue|Aucun événement : essayez un autre mode|Diagnostic du podomètre",
"de" to "Schrittzählmodus|Automatisch|Systemzähler|Systemdetektor|Software-Schrittzähler|Softwarezählung: ungefähr, höherer Akkuverbrauch|Keine Sensordaten: anderen Modus versuchen|Schrittzählerdiagnose",
"es" to "Modo del podómetro|Automático|Contador del sistema|Detector del sistema|Podómetro por software|Conteo por software: aproximado, mayor consumo|Sin eventos: pruebe otro modo|Diagnóstico del podómetro",
"it" to "Modalità contapassi|Automatico|Contatore di sistema|Rilevatore di sistema|Contapassi software|Conteggio software: approssimativo, più consumo|Nessun evento: prova un’altra modalità|Diagnostica contapassi",
"zh" to "计步模式|自动|系统计步器|系统步态检测|软件计步器|软件计步：估算，耗电较高|无传感器事件：请尝试其他模式|计步诊断",
"ja" to "歩数計モード|自動|システム歩数計|システム歩行検出|ソフトウェア歩数計|ソフトウェア計測：推定値、電池消費増加|センサーイベントなし：別のモードを試してください|歩数計の診断",
"ko" to "걸음 수 측정 모드|자동|시스템 계수기|시스템 감지기|소프트웨어 만보기|소프트웨어 측정: 추정치, 배터리 소모 증가|센서 이벤트 없음: 다른 모드를 시도하세요|걸음 수 진단"
)
fun stepTranslation(text:String,language:String)=stepKeys.indexOf(text).takeIf {it>=0}?.let {stepRows[language]?.split('|')?.get(it)}
