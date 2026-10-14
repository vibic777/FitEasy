package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun GoalsEditor(goals:Goals,onDismiss:()->Unit,onSave:(Goals)->Boolean) {
    val units=LocalUnits.current
    var values by remember {mutableStateOf(listOf(displayAmount(goals.kcal,"kcal",units).toString(),displayAmount(goals.waterMl,"ml",units).toString(),goals.steps.toString(),goals.activityMinutes.toString(),goals.weightKg?.let {displayAmount(it,"kg",units).toString()} ?: "",goals.proteinG?.toString() ?: "",goals.fatG?.toString() ?: "",goals.carbsG?.toString() ?: ""))}
    var disabled by remember {mutableStateOf(goals.disabled)}
    var pause by remember {mutableStateOf(goals.pausedUntil ?: "")}
    var allDrinks by remember {mutableStateOf(goals.allDrinks)}
    var automatic by remember {mutableStateOf(goals.autoCalculate)}
    var autoApply by remember {mutableStateOf(goals.autoApply)}
    var error by remember {mutableStateOf("")}
    val ids=listOf("energy","water","steps","activity","weight","protein","fat","carbs")
    val labels=listOf("Энергия, ${unitFactor("kcal",units).second}","Вода, ${unitFactor("ml",units).second}","Шаги","Активность, минуты","Целевой вес, ${unitFactor("kg",units).second} (необязательно)","Белки, г (необязательно)","Жиры, г (необязательно)","Углеводы, г (необязательно)")
    SettingsPage(onDismissRequest=onDismiss,title={Text("Цели")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        labels.forEachIndexed {index,label->
            Row {Text(label,Modifier.weight(1f));Switch(ids[index] !in disabled,{active->disabled=if(active)disabled-ids[index] else disabled+ids[index]})}
            OutlinedTextField(values[index],{text->values=values.toMutableList().apply {set(index,text)}},enabled=ids[index] !in disabled,label={Text(label)})
        }
        OutlinedTextField(pause,{pause=it},label={Text("Пауза до YYYY-MM-DD, пусто — без паузы")})
        Text("Дневник доступен во время паузы. Предыдущие дни сохраняют прежние цели.")
        Row {Text("Все напитки",Modifier.weight(1f));Switch(allDrinks,{allDrinks=it})}
        Row {Text("Автоматический пересчёт энергии",Modifier.weight(1f));Switch(automatic,{automatic=it})}
        Row {Text("Применять без подтверждения",Modifier.weight(1f));Switch(autoApply,{autoApply=it})}
        if(error.isNotEmpty())Text(error)
    }},confirmButton={TextButton(onClick={runCatching {
        fun number(index:Int)=values[index].trim().replace(',','.').toDouble().also {require(it.isFinite() && it>0)}
        fun optional(index:Int)=if(values[index].isBlank())null else number(index)
        fun whole(index:Int)=values[index].trim().toInt().also {require(it>=0)}
        val until=pause.trim().takeIf {it.isNotEmpty()}?.also {java.time.LocalDate.parse(it)}
        val next=goals.copy(kcal=canonicalAmount(number(0),"kcal",units),waterMl=canonicalAmount(number(1),"ml",units),steps=whole(2),activityMinutes=whole(3),weightKg=optional(4)?.let {canonicalAmount(it,"kg",units)},proteinG=optional(5),fatG=optional(6),carbsG=optional(7),disabled=disabled,pausedUntil=until,allDrinks=allDrinks,autoCalculate=automatic,autoApply=autoApply)
        if(onSave(next))onDismiss()
    }.onFailure {error="Проверьте значения и дату паузы"}}){Text("Сохранить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
