package com.fiteasy.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fiteasy.shared.*
import kotlinx.serialization.decodeFromString
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
class IconButtonsTest {
    @Test fun everyMappedCaptionHasAnIconInEveryLanguage() {
        assertTrue(buttonIcons.size>120)
        languageNames.keys.forEach {language->buttonIcons.forEach {(caption,icon)->
            val shown=translate(caption,language);assertTrue(shown.isNotBlank(),"$language/$caption")
            assertEquals(icon,buttonIcon(caption));assertNotNull(buttonIcon(shown),"$language: '$shown' lost its icon")
        }}
        assertNull(buttonIcon("Анна · 192.168.1.5:4000"),"Data stays text")
    }
    @Test fun captionsBecomeIconsAndDataStaysText() {
        val captions=listOf("+ Еда","+ Напиток","+ Активность","+ Шаги вручную","Сохранить","Отмена","Удалить","Копировать","Изменить","Справка","О программе","Корзина","Бэкап","Восстановить","Цели","Единицы","Расписания","Календарь","Синхронизация","Заблокировать","Начать","Пауза","Завершить","Пропустить подход","Искать устройства","Отправить код","Настроить","Биометрия","База данных и перенос","PDF / CSV отчёт")
        listOf("ru","en").forEach {language->
            val scene=ImageComposeScene(420,420,Density(1f)){CompositionLocalProvider(LocalLanguage provides language){MaterialTheme {Surface(Modifier.fillMaxSize()){FlowRow(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                captions.forEachIndexed {i,caption->when(i%3){0->Button(onClick={}){Text(caption)};1->OutlinedButton(onClick={}){Text(caption)};else->TextButton(onClick={}){Text(caption)}}}
                OutlinedButton(onClick={}){Text("Анна · 192.168.1.5")}
            }}}}}
            try {scene.render().close();val image=scene.render();java.io.File("build/review/icon-buttons-$language.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
        }
        val a=java.io.File("build/review/icon-buttons-ru.png").readBytes();val b=java.io.File("build/review/icon-buttons-en.png").readBytes()
        assertTrue(a.contentEquals(b),"Icons do not depend on the language; only the hint text does")
    }
    @Test fun weekdaysHaveNamesAndRemindersHaveASound() {
        assertEquals("Пн",weekdayLabel(1,"ru").take(2));assertEquals("Mon",weekdayLabel(1,"en"));assertEquals("So",weekdayLabel(7,"de").take(2))
        languageNames.keys.forEach {language->val names=(1..7).map {weekdayLabel(it,language)};assertEquals(7,names.distinct().size,language);assertTrue(names.none {it.any(Char::isDigit)},language)
            reminderSounds.forEach {(_,label)->assertTrue(translate(label,language).isNotBlank())}}
        val reminder=Reminder(name="a",soundKind="alarm");validateReminder(reminder)
        assertFails {validateReminder(reminder.copy(soundKind="song.mp3"))}
        assertEquals("notification",codec.decodeFromString<Reminder>("{\"name\":\"old\"}").soundKind)
    }
    @Test fun choiceChipsFollowTheSameRule() {
        val chips=listOf("Все категории","Мобильность","Сила","Ходьба","Отдых","Все оборудование","Без оборудования","Гантели","Стул / опора","Стена","Все типы","Текст","Рисунок","Список","Все заметки","По папкам","Продукты","Блюда","Избранное","Недавние","Светлая","Тёмная","Системная","PIN-код","Пароль","Логин и пароль","Графический ключ","Слияние","Замещение","Звук уведомления","Звук будильника","Мелодия звонка","Женский силуэт","Мужской силуэт")
        chips.forEach {assertNotNull(buttonIcon(it),it)}
        // Library categories and equipment arrive already translated by the library dictionary.
        languageNames.keys.forEach {language->listOf("Mobility","Strength","Dumbbells","Pull-up bar","No equipment").forEach {assertNotNull(buttonIcon(libraryText(it,language)),"$language/$it")}}
        // Values that are data themselves keep their text.
        listOf("kg","lb","Пн","English","#дом","2/5").forEach {assertNull(buttonIcon(it),it)}
        val scene=ImageComposeScene(420,330,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {Surface(Modifier.fillMaxSize()){FlowRow(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            chips.forEachIndexed {i,caption->FilterChip(i%4==0,{},{Text(caption)})};FilterChip(false,{},{Text("kg")});FilterChip(true,{},{Text("Пн")})
        }}}}}
        try {scene.render().close();val image=scene.render();java.io.File("build/review/icon-chips.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
    }
}
