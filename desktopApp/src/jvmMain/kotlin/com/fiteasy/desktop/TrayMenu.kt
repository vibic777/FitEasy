package com.fiteasy.desktop

import com.fiteasy.shared.*

val trayActions=listOf("open","today","food","water","activity","progress","notes","profile","settings","calendar","schedules","reminders","active","help","about","exit","silence")
private val trayLabels=mapOf(
"ru" to "Открыть FitEasy|Сегодня|Питание|Вода|Активность|Прогресс|Заметки|Профиль|Настройки|Календарь|Расписания|Управление напоминаниями|Активные напоминания|Справка|О программе|Завершить FitEasy|Заглушить напоминание",
"en" to "Open FitEasy|Today|Nutrition|Water|Activity|Progress|Notes|Profile|Settings|Calendar|Schedules|Manage reminders|Active reminders|Help|About|Exit FitEasy|Silence the reminder",
"be" to "Адкрыць FitEasy|Сёння|Харчаванне|Вада|Актыўнасць|Прагрэс|Нататкі|Профіль|Налады|Каляндар|Расклады|Кіраванне напамінамі|Актыўныя напаміны|Даведка|Аб праграме|Выйсці з FitEasy|Заглушыць напамін",
"fr" to "Ouvrir FitEasy|Aujourd’hui|Nutrition|Eau|Activité|Progrès|Notes|Profil|Paramètres|Calendrier|Programmes|Gérer les rappels|Rappels actifs|Aide|À propos|Quitter FitEasy|Couper le son du rappel",
"de" to "FitEasy öffnen|Heute|Ernährung|Wasser|Aktivität|Fortschritt|Notizen|Profil|Einstellungen|Kalender|Zeitpläne|Erinnerungen verwalten|Aktive Erinnerungen|Hilfe|Über FitEasy|FitEasy beenden|Erinnerung stummschalten",
"es" to "Abrir FitEasy|Hoy|Alimentación|Agua|Actividad|Progreso|Notas|Perfil|Ajustes|Calendario|Horarios|Gestionar recordatorios|Recordatorios activos|Ayuda|Acerca de|Salir de FitEasy|Silenciar el recordatorio",
"it" to "Apri FitEasy|Oggi|Alimentazione|Acqua|Attività|Progressi|Note|Profilo|Impostazioni|Calendario|Programmi|Gestisci promemoria|Promemoria attivi|Guida|Informazioni|Esci da FitEasy|Silenzia il promemoria",
"zh" to "打开 FitEasy|今天|饮食|饮水|活动|进展|笔记|个人资料|设置|日历|日程|管理提醒|当前提醒|帮助|关于|退出 FitEasy|提醒静音",
"ja" to "FitEasy を開く|今日|食事|水分|活動|進捗|ノート|プロフィール|設定|カレンダー|予定|リマインダー管理|現在のリマインダー|ヘルプ|このアプリについて|FitEasy を終了|リマインダーの音を止める",
"ko" to "FitEasy 열기|오늘|식사|물|활동|진행|메모|프로필|설정|달력|일정|알림 관리|현재 알림|도움말|앱 정보|FitEasy 종료|알림 소리 끄기")
fun trayLabel(action:String,language:String):String {
    val index=trayActions.indexOf(action);require(index>=0)
    return trayLabels[language]?.split('|')?.get(index) ?: trayLabels.getValue("en").split('|')[index]
}
fun activeTrayReminders(states:Map<String,ReminderDelivery>)=states.filterValues {it.status in setOf("awaiting","delivered","snoozed")}.keys.sorted()
fun trayDiscardMessage(language:String)=mapOf(
"ru" to "В заметке есть несохранённые изменения. Перейти в другой раздел без сохранения?",
"be" to "У нататцы ёсць незахаваныя змены. Перайсці ў іншы раздзел без захавання?",
"fr" to "La note contient des modifications non enregistrées. Ouvrir une autre section sans les enregistrer ?",
"de" to "Die Notiz enthält ungespeicherte Änderungen. Ohne Speichern zu einem anderen Bereich wechseln?",
"es" to "La nota tiene cambios sin guardar. ¿Abrir otra sección sin guardarlos?",
"it" to "La nota contiene modifiche non salvate. Aprire un'altra sezione senza salvarle?",
"zh" to "笔记有未保存的更改。是否不保存并打开其他部分？",
"ja" to "ノートに未保存の変更があります。保存せずに別の画面を開きますか？",
"ko" to "메모에 저장되지 않은 변경 사항이 있습니다. 저장하지 않고 다른 화면을 열까요?"
)[language] ?: "The note has unsaved changes. Open another section without saving?"
