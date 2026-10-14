package com.fiteasy.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val settingGroups=linkedMapOf(
    "personal" to listOf("Профиль", "Личные данные", "Цели", "Приёмы пищи"),
    "appearance" to listOf("Оформление","Animations","Section backgrounds","Accent / RGB","Спокойная","Цветочная","Воздушная","Геометрическая","Разноцветная","Штампы времени", "Системная", "Светлая", "Тёмная", "Свой цвет", "Размер текста", "Выбрать фон", "Прозрачность фона", "Сворачивать в трей при закрытии", "Оформление графиков"),
    "general" to listOf("Язык", "Единицы") + languageNames.values,
    "reminders" to listOf("Напоминания", "Расписания", "Календарь", "Уведомления и батарея", "Сводка в шторке", "Будильник поверх экрана блокировки", "Режим экономии батареи"),
    "permissions" to listOf("Права", "Уведомления", "Точные напоминания", "Экран будильника", "Поверх других окон", "Разрешить", "Отозвать", "Отозвать все права"),
    "tags" to listOf("Теги", "Создать тег", "Переименовать тег", "Удалить тег"),
    "security" to listOf("Безопасность и приватность", "Блокировка приложения", "Уведомления-напоминания", "Заблокировать", "Защита", "Пароль", "Контрольный вопрос", "TOTP", "Email"),
    "trash" to listOf("Корзина", "Открыть корзину", "Удалить навсегда", "Очистить корзину"),
    "sync" to listOf("Синхронизация", "Ждать устройство", "Найти устройство", "Соединения", "Слияние", "Замещение", "LAN", "Wi-Fi"),
    "data" to listOf("Резервная копия", "Экспорт всех данных", "Импорт всех данных", "Пароль резервной копии", "Шифрование базы данных", "Данные", "Бэкап", "Восстановить", "PDF / CSV отчёт", "Корзина", "Создать демонстрацию", "Удалить демонстрацию", "Удалить демо принудительно", "Обнулить базу", "Обнулить категории"),
    "help" to listOf("Справка и о приложении","Справка","О приложении","Журнал приложения","App journal", "О программе"))

val settingIcons=mapOf("personal" to "profile","appearance" to "palette","general" to "language","reminders" to "bell","permissions" to "key","tags" to "list","security" to "lock","trash" to "delete","sync" to "loop","data" to "database","help" to "help")
/** Title of the settings page shown in the top bar. */
fun settingsTitle(section:String)=settingGroups[section]?.first() ?: "Профиль и настройки"
@Composable fun SettingsMenu(selected:String,onSelect:(String)->Unit) {
    val language=LocalLanguage.current
    var query by remember {mutableStateOf("")}
    if(selected.isEmpty()) {
        OutlinedTextField(query,{query=it},label={Text("Поиск настроек")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val results=settingGroups.filter {(_,words)->query.isBlank() || words.any {translate(it,language).contains(query.trim(),ignoreCase=true) || it.contains(query.trim(),ignoreCase=true)}}
        if(results.isEmpty())Text("Ничего не найдено")
        // One row per page: its own icon in a tinted circle, the title and what is inside. No arrow: the row itself is the way in.
        results.forEach {(id,words)->Surface(Modifier.fillMaxWidth().clickable {onSelect(id)},shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainerLow,tonalElevation=1.dp) {
            val hints=if(query.isBlank())words.drop(1).take(3)else words.filter {translate(it,language).contains(query.trim(),ignoreCase=true) || it.contains(query.trim(),ignoreCase=true)}.take(3)
            Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(9.dp)){RigSymbol(settingIcons[id] ?: "settings",MaterialTheme.colorScheme.onPrimaryContainer)}}
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {Text(words.first(),style=MaterialTheme.typography.titleMedium);androidx.compose.material3.Text(hints.joinToString(" · "){translate(it,language)},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)}
            }
        }}
    }
}

@Composable fun ResetDialog(onDismiss:()->Unit,onReset:(Set<String>,Boolean)->Boolean) {
    val language=LocalLanguage.current
    var selected by remember {mutableStateOf(emptySet<String>())}
    var all by remember {mutableStateOf(false)}
    var confirmation by remember {mutableIntStateOf(0)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(confirmation==0)"Обнуление данных"else if(confirmation==1)"Обнулить базу?"else "Последнее подтверждение")},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Удаление без корзины на этом устройстве. Внешние резервные копии, оформление и защита сохранятся. После синхронизации данные могут вернуться.")
            if(confirmation==0) {
                Row {Checkbox(all,{all=it;selected=if(it)resetCategories.keys else emptySet()});Text("Все данные")}
                resetCategories.forEach {(id,label)->Row {Checkbox(id in selected,{checked->selected=if(checked)selected+id else selected-id;all=selected==resetCategories.keys});Text(label)}}
                Text("Каталоги: продукты оставленных рецептов сохраняются. Расписания: связанные напоминания выключаются. Активность: шагомер останавливается.")
            } else {Text(if(all)"Все записи, профиль, цели и напоминания будут удалены."else selected.joinToString {translate(resetCategories.getValue(it),language)})}
        }
    },confirmButton={TextButton(enabled=selected.isNotEmpty(),onClick={if(confirmation==0 || (all && confirmation==1))confirmation++ else if(onReset(selected,all))onDismiss()}){Text(if(confirmation==0)"Продолжить"else if(all && confirmation==1)"Да, обнулить"else "Удалить окончательно")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
