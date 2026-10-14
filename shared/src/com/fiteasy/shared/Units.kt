package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.serialization.Serializable

@Serializable data class DisplayUnits(val weight:String="kg",val length:String="cm",val volume:String="l",val distance:String="km",val energy:String="kcal")
val LocalUnits=staticCompositionLocalOf {DisplayUnits()}
fun unitFactor(canonical:String,units:DisplayUnits):Pair<Double,String> = when(canonical){
    "kg"->if(units.weight=="lb")1.0/0.45359237 to "lb" else 1.0 to "kg"
    "cm"->if(units.length=="in")1.0/2.54 to "in" else 1.0 to "cm"
    "ml"->when(units.volume){"ml"->1.0 to "ml";"usfloz"->1.0/29.5735295625 to "US fl oz";"impfloz"->1.0/28.4130625 to "Imp fl oz";else->0.001 to "l"}
    "km"->if(units.distance=="mi")1.0/1.609344 to "mi" else 1.0 to "km"
    "kcal"->if(units.energy=="kJ")4.184 to "kJ" else 1.0 to "kcal"
    else->1.0 to canonical
}
fun displayAmount(amount:Double,canonical:String,units:DisplayUnits)=amount*unitFactor(canonical,units).first
fun canonicalAmount(amount:Double,canonical:String,units:DisplayUnits)=amount/unitFactor(canonical,units).first
fun quantity(amount:Double,canonical:String,units:DisplayUnits):String {val (factor,label)=unitFactor(canonical,units);return "${number(amount*factor)} $label"}
fun validateUnits(u:DisplayUnits){require(u.weight in setOf("kg","lb") && u.length in setOf("cm","in") && u.volume in setOf("l","ml","usfloz","impfloz") && u.distance in setOf("km","mi") && u.energy in setOf("kcal","kJ"))}

@Composable fun UnitsEditor(units:DisplayUnits,onDismiss:()->Unit,onSave:(DisplayUnits)->Boolean){
    var value by remember {mutableStateOf(units)}
    SettingsPage(onDismissRequest=onDismiss,title={Text("Единицы")},text={Column {
        Text("Вес");FlowRow {listOf("kg","lb").forEach {FilterChip(value.weight==it,{value=value.copy(weight=it)},{Text(it)})}}
        Text("Длина");FlowRow {listOf("cm","in").forEach {FilterChip(value.length==it,{value=value.copy(length=it)},{Text(it)})}}
        Text("Объём");FlowRow {listOf("l","ml","usfloz","impfloz").forEach {FilterChip(value.volume==it,{value=value.copy(volume=it)},{Text(when(it){"usfloz"->"US fl oz";"impfloz"->"Imp fl oz";else->it})})}}
        Text("Расстояние");FlowRow {listOf("km","mi").forEach {FilterChip(value.distance==it,{value=value.copy(distance=it)},{Text(it)})}}
        Text("Энергия");FlowRow {listOf("kcal","kJ").forEach {FilterChip(value.energy==it,{value=value.copy(energy=it)},{Text(it)})}}
        Text("Данные хранятся в исходных единицах. Переключение меняет отображение и ввод, прошлые записи сохраняются.")
    }},confirmButton={TextButton(onClick={if(onSave(value))onDismiss()}){Text("Сохранить")}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена")}})
}
