package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val mainSections=listOf("today" to "Сегодня","food" to "Питание","water" to "Вода","activity" to "Активность","progress" to "Прогресс","notes" to "Заметки")

@Composable fun MainNavigationIcons(selected:String,vertical:Boolean=false,onSelect:(String)->Unit){
    if(vertical)Column(Modifier.width(48.dp).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)){
        mainSections.forEach {(id,label)->IconAction(id,label,{onSelect(id)},selected=selected==id)}
    }else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
        mainSections.forEach {(id,label)->IconAction(id,label,{onSelect(id)},selected=selected==id)}
    }
}
