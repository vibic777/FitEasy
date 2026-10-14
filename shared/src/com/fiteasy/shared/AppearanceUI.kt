package com.fiteasy.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DialogBackground(val platform:Platform,val appearance:Appearance,val section:String)
private val LocalDialogBackground=staticCompositionLocalOf<DialogBackground?> {null}
@Composable fun SectionDialogBackdrop(platform:Platform,a:Appearance,section:String,content:@Composable ()->Unit) {
    CompositionLocalProvider(LocalDialogBackground provides DialogBackground(platform,a,section),content=content)
}
/** The background of the section a form belongs to, behind the form of a full-screen page too. */
@Composable fun DialogBackdrop(content:@Composable ()->Unit) {
    val background=LocalDialogBackground.current
    if(background==null)content() else SectionBackdrop(background.platform,background.appearance,background.section,Modifier.fillMaxSize()){content()}
}
@Composable fun AlertDialog(onDismissRequest:()->Unit,confirmButton:@Composable ()->Unit,modifier:Modifier=Modifier,dismissButton:(@Composable ()->Unit)?=null,title:(@Composable ()->Unit)?=null,text:(@Composable ()->Unit)?=null) {
    val background=LocalDialogBackground.current
    androidx.compose.material3.AlertDialog(onDismissRequest=onDismissRequest,confirmButton=confirmButton,modifier=modifier,dismissButton=dismissButton,title=title,text=if(text==null)null else ({
        if(background==null)text()else SectionBackdrop(background.platform,background.appearance,background.section,Modifier.fillMaxWidth().padding(4.dp)){text()}
    }))
}

@Composable fun ColorChoice(color:Long,onChange:(Long)->Unit) {
    var hex by remember(color){mutableStateOf("%06X".format(color and 0xFFFFFF))}
    var r by remember(color){mutableStateOf(((color shr 16) and 255).toString())}
    var g by remember(color){mutableStateOf(((color shr 8) and 255).toString())}
    var b by remember(color){mutableStateOf((color and 255).toString())}
    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {(vividPalettes+fitPalettes).forEach {c->Button(onClick={onChange(c)},colors=ButtonDefaults.buttonColors(containerColor=Color(c),contentColor=if(Color(c).luminance()>.179f)Color.Black else Color.White),contentPadding=PaddingValues(8.dp)){Text(if(color==c)"✓"else "●")}}}
    OutlinedTextField(hex,{v->if(v.length<=6 && v.all {it in "0123456789abcdefABCDEF"}){hex=v;if(v.length==6)onChange(0xFF000000L or v.toLong(16))}},label={Text("RGB · RRGGBB")},singleLine=true)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf("R" to r,"G" to g,"B" to b).forEachIndexed {i,(label,value)->OutlinedTextField(value,{v->if(v.length<=3 && v.all(Char::isDigit)){when(i){0->r=v;1->g=v;else->b=v};rgbColor(r,g,b)?.let(onChange)}},modifier=Modifier.weight(1f),label={Text("$label · 0–255")},singleLine=true,isError=value.toIntOrNull()?.let {it in 0..255}!=true)}
    }
}

@Composable fun SectionBackdrop(platform:Platform,a:Appearance,section:String,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    val setting=a.sectionBackground(section)
    val encoded=a.backgroundBytes(section)
    val bitmap by produceState<ImageBitmap?>(null,platform,encoded) {
        value=null
        value=withContext(Dispatchers.Default){encoded?.let {runCatching {platform.decodeBackground(java.util.Base64.getDecoder().decode(it))}.getOrNull()}}
    }
    Box(modifier.background(setting.color?.let(::Color) ?: MaterialTheme.colorScheme.background)) {
        bitmap?.let {Image(it,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,alpha=setting.alpha)}
        val base=setting.color?.let(::Color) ?: MaterialTheme.colorScheme.background
        val readable=if(base.luminance()>.179f)Color.Black else Color.White
        CompositionLocalProvider(LocalContentColor provides readable){content()}
    }
}

@Composable fun BackgroundEditor(platform:Platform,a:Appearance,onSave:(Appearance)->Unit,onDismiss:()->Unit) {
    var selected by remember {mutableStateOf(appearanceSections.keys.toSet())}
    var color by remember {mutableLongStateOf(a.sectionBackground("today").color ?: 0xFFF7F7F7L)}
    var alpha by remember {mutableFloatStateOf(.15f)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    fun apply(change:(SectionBackground)->SectionBackground,image:String?=null){runCatching {onSave(applyBackground(platform.load().local.appearance,selected,change,image))}.onFailure {error=translate("Не удалось сохранить оформление",a.language)}}
    SettingsPage(onDismissRequest={if(!busy)onDismiss()},title={Text("Фоны разделов")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row {Checkbox(selected.size==appearanceSections.size,{selected=if(it)appearanceSections.keys.toSet()else emptySet()});Text("Все разделы")}
        FlowRow {appearanceSections.forEach {(id,label)->Row {Checkbox(id in selected,{selected=if(it)selected+id else selected-id});Text(label)}}}
        Text("Цвет фона");ColorChoice(color,{color=it})
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(enabled=selected.isNotEmpty() && !busy,onClick={apply({it.copy(color=color)})}){Text("Применить цвет")}
        OutlinedButton(enabled=selected.isNotEmpty() && !busy,onClick={apply({it.copy(color=null)})}){Text("Системный фон")}
        }
        Text("Прозрачность фона");Slider(alpha,{alpha=it},valueRange=0f..0.35f)
        OutlinedButton(enabled=selected.isNotEmpty() && !busy,onClick={apply({it.copy(alpha=alpha)})}){Text("Применить прозрачность")}
        OutlinedButton(enabled=selected.isNotEmpty() && !busy,onClick={
            val targets=selected
            platform.importFile {bytes->busy=true;platform.launchFileTask {
                try {
                    val encoded=withContext(Dispatchers.Default){java.util.Base64.getEncoder().encodeToString(platform.prepareBackground(bytes))}
                    onSave(applyBackground(platform.load().local.appearance,targets,{it.copy(alpha=alpha)},encoded))
                }catch(t:Throwable){error=translate("Не удалось загрузить фон",a.language)}finally{busy=false}
            }}
        }){Text("Выбрать фон")}
        OutlinedButton(enabled=selected.isNotEmpty() && !busy,onClick={apply({it.copy(image=null)})}){Text("Удалить фон")}
        if(busy)CircularProgressIndicator()
        if(error.isNotEmpty())Text(error)
    }},confirmButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Закрыть")}})
}
