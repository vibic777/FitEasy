package com.fiteasy.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Icons instead of captions (RULES.md, D089). Every button of the app goes through the three wrappers below: when its
 * caption has an icon, the button shows the icon and the caption becomes the hint — long press on a touch screen, pointer
 * hover on an ordinary one — and the screen-reader label, in the selected language. Captions that are data (a name, a date,
 * a number) stay as text.
 */
internal class ButtonCaption {var text by mutableStateOf("")}
internal val LocalButtonCaption=compositionLocalOf<ButtonCaption?> {null}

/** Russian source caption → icon. A trailing plus draws the "add" badge on the icon. */
val buttonIcons:Map<String,String> = mapOf(
    "+ Активность" to "activity+","+ Вес / замер" to "scale+","+ Другой напиток" to "water+","+ Еда" to "food+","+ Напиток" to "water+","+ Напоминание" to "bell+","+ Рецепт" to "list+",
    "+ Ручная активность" to "activity+","+ Свой комплекс" to "dumbbell+","+ Своё" to "add","+ Шаги вручную" to "steps+",
    "Accent / RGB" to "palette","App journal" to "document","CSV" to "table","Clear journal" to "delete","Copy journal" to "copy","PDF / CSV отчёт" to "report","PDF" to "document","Refresh" to "loop","Retry" to "loop",
    "Section backgrounds" to "image","Step diagnostics" to "info","Ручной адрес" to "edit","База данных и перенос" to "database","Без папки" to "folder","Без сохранения" to "close","Биометрия" to "fingerprint",
    "Отозвать" to "stop","Открыть в системе" to "settings",
    "Бэкап" to "export","В корзину" to "delete","Вернуться" to "previous","Вид для всех графиков" to "progress","Восстановить" to "import","Готово" to "save","Добавить копией на указанную дату" to "copy",
    "Добавить напоминание" to "bell+","Добавить пункт" to "add","Добавить строку" to "add","Единицы" to "units","Ждать подключения" to "time","Забыть устройство" to "delete","Завершить" to "stop",
    "Закрыть без сохранения" to "close","Закрыть" to "close","Изменить" to "edit","Искать устройства" to "search","Копировать и скрыть" to "copy","Копировать" to "copy","Корзина" to "delete","Личные данные" to "profile",
    "Назначить напоминание" to "bell","Настроить карточки" to "cards","Настроить" to "settings","Начать" to "play","О программе" to "info","Остановить" to "stop","Отклонить" to "close","Отключить блокировку" to "unlock",
    "Открыть день" to "calendar","Отмена" to "close","Отменить правку" to "undo","Отменить штрих" to "undo","Отправить код" to "send","Отправить" to "send","Очистить рисунок" to "delete","Очистить" to "delete",
    "Перейти" to "next","Период и точка отсчёта" to "calendar","Повторить" to "loop","Подключиться" to "link","Показать пароль базы" to "eye","Применить прозрачность" to "save","Применить цвет" to "save","Применить" to "save",
    "Приёмы пищи" to "food","Проверить будильник через 10 секунд" to "bell","Проверить снова" to "loop","Продолжить редактирование" to "edit","Пропустить подход" to "skip","Пропустить упражнение" to "skip2",
    "Прочитать шаги Health Connect" to "steps","Разрешить" to "save","Рассчитать ориентир энергии" to "energy","Системный фон" to "undo","Скрыть" to "hide","Сменить пароль базы" to "key","Создать ключ TOTP" to "key",
    "Создать расписание" to "calendar+","Создать" to "add","Сохранить результат" to "save","Сохранить" to "save","Справка" to "help","Удалить пункт" to "delete","Удалить строку" to "delete","Удалить фон" to "delete",
    "Удалить черновик" to "delete","Удалить" to "delete","Управлять расписаниями и напоминаниями" to "calendar","Цели" to "target","Штампы времени" to "time","Экспорт базы" to "export",
    "‹" to "previous","›" to "next","↑" to "up","↓" to "down","Расписания" to "calendar","Календарь" to "calendar","Синхронизация" to "loop","Заблокировать" to "lock","Включить шагомер телефона" to "steps",
    "Выключить шагомер" to "stop","Rig editor" to "joint","Добавить в комплекс" to "add","Включить анимации" to "play","Уведомления" to "bell","Точные напоминания" to "time","Настройки батареи" to "battery",
    "Экран будильника" to "bell","Поверх других окон" to "depth","Продолжить" to "play","Пауза" to "pause","Export" to "export","Import" to "import","Подтвердить" to "save","Отложить" to "time","Прервать" to "stop",
    "Прервать все" to "stop","Остановить учёт" to "stop","Назад" to "previous","Отнести к выбранному дню" to "calendar","Включить" to "play","Выключить" to "stop","Добавить" to "add","Открыть" to "edit",
    "Профиль" to "profile","Данные" to "database","Защита" to "lock","Оформление" to "palette","Язык" to "language","Напоминания" to "bell","Поиск" to "search","Очистить журнал" to "delete","Обновить" to "loop",
    "Удалить демонстрацию" to "delete","Создать демонстрацию" to "add","Удалить демо принудительно" to "delete","Обнулить базу" to "delete","Обнулить категории" to "delete","Тренировка" to "dumbbell","Продукты и блюда" to "search","Записи напитков" to "list","Справочник упражнений" to "list",
    // Choice chips: categories, kinds, modes. Values that are data themselves (units, periods, weekdays, languages, style names) stay text.
    "Все категории" to "grid","Все оборудование" to "inventory","Мобильность" to "stretch","Сила" to "biceps","Ходьба" to "activity","Отдых" to "pause","Без оборудования" to "standing",
    "Стул / опора" to "chair","Гантели" to "dumbbell","Стена" to "wall","Mobility" to "stretch","Strength" to "biceps","Dumbbells" to "dumbbell","Pull-up bar" to "bar","No equipment" to "standing",
    "Все типы" to "grid","Текст" to "text","Рисунок" to "drawing","Список" to "list","Все заметки" to "notes","По папкам" to "folder","Все теги" to "tag",
    "Продукты" to "product","Блюда" to "dish","Избранное" to "star","Недавние" to "time",
    "Женский силуэт" to "woman","Мужской силуэт" to "man","Woman" to "woman","Man" to "man","Skeleton" to "joint",
    "Светлая" to "sun","Тёмная" to "moon","Системная" to "settings","Включить оформление" to "palette","Применить оформление" to "palette",
    "PIN-код" to "keypad","Пароль" to "key","Логин и пароль" to "profile","Графический ключ" to "pattern","Контрольный вопрос" to "help","TOTP" to "time","Email" to "send",
    "Слияние" to "link","Добавить копиями" to "copy","Замещение" to "loop","Получить" to "import","В обе стороны" to "mirror","Оставить мои" to "lock","Принять входящие" to "import","Все данные" to "database",
    "Звук уведомления" to "bell","Звук будильника" to "time","Мелодия звонка" to "phone",
    "Импорт базы" to "import","Отправить базу по почте" to "send","Отправить бэкап по почте" to "send","Отменить передачу" to "stop",
    "Прослушать" to "play","Звуки телефона…" to "phone","Звуки Windows…" to "bell","Мой файл…" to "folder","Обычный звук" to "undo","Заглушить" to "pause","Ждать устройство" to "time","Найти устройство" to "search","Синхронизировать…" to "loop","Использовать адрес" to "link","Режим экономии батареи" to "battery",
    "Daily" to "today","Weekly" to "calendar","Monthly" to "calendar","Never" to "close","Automatic" to "settings")
/** Captions arrive either as the Russian source or already translated; both find the icon. */
private val translatedIcons:Map<String,String> by lazy {buttonIcons.flatMap {(caption,icon)->languageNames.keys.flatMap {listOf(translate(caption,it) to icon,libraryText(caption,it) to icon,rigText(caption,it) to icon)}}.toMap()}
fun buttonIcon(caption:String):String? = buttonIcons[caption] ?: translatedIcons[caption]

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Hinted(button:@Composable (Modifier)->Unit) {
    val caption=remember {ButtonCaption()}
    CompositionLocalProvider(LocalButtonCaption provides caption) {
        HintBox(caption.text,enabled=caption.text.isNotEmpty()) {
            button(Modifier.defaultMinSize(minWidth=44.dp,minHeight=44.dp).semantics {if(caption.text.isNotEmpty())contentDescription=caption.text})
        }
    }
}
private val compact=PaddingValues(horizontal=10.dp,vertical=6.dp)
@Composable fun Button(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,colors:ButtonColors=ButtonDefaults.buttonColors(),border:BorderStroke?=null,content:@Composable RowScope.()->Unit) {
    Hinted {hint->androidx.compose.material3.Button(onClick=onClick,modifier=modifier.then(hint),enabled=enabled,colors=colors,border=border,contentPadding=compact,content=content)}
}
@Composable fun OutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit) {
    Hinted {hint->androidx.compose.material3.OutlinedButton(onClick=onClick,modifier=modifier.then(hint),enabled=enabled,contentPadding=compact,content=content)}
}
@Composable fun TextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit) {
    Hinted {hint->androidx.compose.material3.TextButton(onClick=onClick,modifier=modifier.then(hint),enabled=enabled,contentPadding=compact,content=content)}
}
/** A choice chip follows the same rule: an icon with the caption as its hint when the caption has one. */
@Composable fun FilterChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,leadingIcon:(@Composable ()->Unit)?=null) {
    Hinted {hint->androidx.compose.material3.FilterChip(selected=selected,onClick=onClick,label=label,modifier=modifier.then(hint),enabled=enabled,leadingIcon=leadingIcon)}
}
/** Called by the shared Text: inside a button, a caption that has an icon is drawn as that icon. True when it was. */
@Composable internal fun captionAsIcon(text:String,language:String):Boolean {
    val caption=LocalButtonCaption.current ?: return false
    val icon=buttonIcon(text) ?: return false
    val shown=translate(text,language)
    SideEffect {caption.text=shown}
    RigSymbol(icon,LocalContentColor.current)
    return true
}
