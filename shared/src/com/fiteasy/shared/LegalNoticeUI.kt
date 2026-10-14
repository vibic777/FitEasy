package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable fun LegalNoticeScreen(language:String,onLanguage:(String)->Unit,onContinue:suspend ()->Boolean){
    val notice=legalNotice(language)
    var languages by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    Surface(Modifier.fillMaxSize()){
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            WindowBarRow {Text("FitEasy",style=MaterialTheme.typography.headlineSmall)}
            Box{
                OutlinedButton(enabled=!busy,onClick={languages=true}){Text(languageNames[language] ?: "English")}
                DropdownMenu(languages,{languages=false}){languageNames.forEach {(id,name)->DropdownMenuItem(text={Text(name)},onClick={languages=false;onLanguage(id)})}}
            }
            Text(notice.title,style=MaterialTheme.typography.titleLarge)
            key(language){Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())){Text(notice.body)}}
            if(failed)Text(notice.saveError,color=MaterialTheme.colorScheme.error)
            Button(enabled=!busy,onClick={busy=true;failed=false;scope.launch {
                val success=runCatching {onContinue()}.getOrElse {AppJournal.record("legal.notice.save.failed",JournalLevel.ERROR,it);false}
                failed=!success;busy=false
            }},modifier=Modifier.fillMaxWidth()){Text(notice.acknowledge)}
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
