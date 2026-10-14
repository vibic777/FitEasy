package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable fun DemoCreationDialog(onDismiss:()->Unit,onCreate:(Int)->Boolean) {
    var days by remember {mutableIntStateOf(7)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Создать демонстрацию")},text={Column {
        Text("Добавить примеры, сохранив ваши записи. Напоминания примера выключены.")
        Row {FilterChip(days==7,{days=7},{Text("Неделя")});FilterChip(days==30,{days=30},{Text("Месяц · 30 дней")})}
    }},confirmButton={TextButton(onClick={if(onCreate(days))onDismiss()}){Text("Создать")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}

@Composable fun DemoRemovalDialog(force:Boolean,onDismiss:()->Unit,onRemove:()->Boolean) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(force)"Удалить демо принудительно?"else "Удалить демонстрацию?")},text={Text(if(force)"Все зарегистрированные образцы, включая изменённые и корзину, будут удалены окончательно. Ваши записи сохранятся. Ваши напоминания для демо-расписания будут выключены и отвязаны."else "Изменённые образцы и ваши записи сохранятся. Неизменённые образцы попадут в корзину. Для окончательной очистки используйте принудительное удаление.")},confirmButton={TextButton(onClick={if(onRemove())onDismiss()}){Text("Удалить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
