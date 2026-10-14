package com.fiteasy.shared

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val dayHints=mapOf("en" to listOf("Previous day","Next day"),"be" to listOf("Папярэдні дзень","Наступны дзень"),"fr" to listOf("Jour précédent","Jour suivant"),"de" to listOf("Vorheriger Tag","Nächster Tag"),"es" to listOf("Día anterior","Día siguiente"),"it" to listOf("Giorno precedente","Giorno successivo"),"zh" to listOf("前一天","后一天"),"ja" to listOf("前の日","次の日"),"ko" to listOf("이전 날","다음 날"))

/** Shared accessible action: translated hover/long-press hint and screen-reader label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun IconAction(icon:String,label:String,onClick:()->Unit,modifier:Modifier=Modifier,selected:Boolean?=null) {
    val language=LocalLanguage.current

    val description=remember(label,language) {when(label){"Предыдущий день"->dayHints[language]?.get(0)?:label;"Следующий день"->dayHints[language]?.get(1)?:label;else->noteNavigationText(label,language)}}
    HintBox(description) {
        IconButton(onClick=onClick,modifier=modifier.then(if(selected==true)Modifier.background(MaterialTheme.colorScheme.secondaryContainer,RoundedCornerShape(12.dp))else Modifier).semantics {contentDescription=description;selected?.let {this.selected=it}}) {FitIcon(icon)}
    }
}
