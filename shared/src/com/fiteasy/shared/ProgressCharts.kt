package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

val chartMetrics=linkedMapOf("Вес" to 0xFFBA68C8L,"Талия" to 0xFFEC6F91L,"Бёдра" to 0xFF8B78D6L,"Грудь" to 0xFFCA9560L,"Калории" to 0xFFE99A39L,"Вода" to 0xFF399FD0L,"Шаги" to 0xFF54A578L,"Активность" to 0xFFDB7563L)
val chartKinds=linkedMapOf("bars" to "Столбцы","line" to "Линия","area" to "Область","dots" to "Точки")
@Serializable data class ChartStyle(val color:Long=0xFF54A578L,val kind:String="bars")
fun Appearance.chart(metric:String)=charts[metric] ?: ChartStyle(chartMetrics.getValue(metric),if(metric in listOf("Вес","Талия","Бёдра","Грудь"))"line"else "bars")
fun Appearance.withChartStyle(metric:String,style:ChartStyle):Appearance {
    require(metric in chartMetrics && style.kind in chartKinds)
    return copy(charts=charts+(metric to style))
}
fun Appearance.withChartKindForAll(kind:String):Appearance {
    require(kind in chartKinds)
    return copy(charts=chartMetrics.keys.associateWith {chart(it).copy(kind=kind)})
}

@Composable fun ProgressChart(metric:String,values:List<Double?>,dates:List<String>,unit:String,appearance:Appearance,onConfigure:()->Unit) {
    val style=appearance.chart(metric)
    val color=Color(style.color)
    val present=values.filterNotNull()
    val grid=MaterialTheme.colorScheme.outlineVariant
    var selected by remember(metric,dates) {mutableStateOf<Int?>(null)}
    val high=(present.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val low=if(metric in listOf("Вес","Талия","Бёдра","Грудь")) ((present.minOrNull() ?: 0.0)-1).coerceAtLeast(0.0) else 0.0
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.09f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row {Text(metric,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);IconAction("edit","Оформление",onClick=onConfigure)}
            Text(if(present.isEmpty())"Пока нет записей"else "${number(present.last())} $unit · ${translate(chartKinds.getValue(style.kind),LocalLanguage.current)}")
            if(present.isNotEmpty()) {
                Text("${number(low)} — ${number(high)} $unit",style=MaterialTheme.typography.labelSmall)
                Canvas(Modifier.fillMaxWidth().height(140.dp).pointerInput(values) {detectTapGestures {position->selected=(position.x/size.width*values.size).toInt().coerceIn(0,values.lastIndex)}}) {
                    drawProgressSeries(values,style,low,high,grid,selected)
                }
                Row {Text(dates.firstOrNull()?.substringBefore(" — ") ?: "",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall);Text(dates.lastOrNull()?.substringAfter(" — ") ?: "",style=MaterialTheme.typography.labelSmall)}
                selected?.let {i->Text("${dates[i]} · ${number(values[i])} $unit",style=MaterialTheme.typography.bodySmall)}
            }
        }
    }
}

@Composable fun ChartEditor(appearance:Appearance,metric:String?,onSave:(Appearance)->Unit,onDismiss:()->Unit) {
    var target by remember {mutableStateOf(metric ?: chartMetrics.keys.first())}
    val style=appearance.chart(target)
    SettingsPage(onDismissRequest=onDismiss,title={Text("Оформление графиков")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(metric==null)FlowRow {chartMetrics.keys.forEach {id->FilterChip(target==id,{target=id},{Text(id)})}}
        Text(target)
        FlowRow {chartKinds.forEach {(id,label)->FilterChip(style.kind==id,{onSave(appearance.withChartStyle(target,style.copy(kind=id)))},{Text(label)})}}
        Text(chartKinds.getValue(style.kind))
        val grid=MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {drawProgressSeries(listOf(2.0,5.0,3.0,7.0,4.0),style,0.0,8.0,grid)}
        ColorChoice(style.color,{onSave(appearance.withChartStyle(target,style.copy(color=it)))})
        OutlinedButton(onClick={onSave(appearance.withChartKindForAll(style.kind))}){Text("Вид для всех графиков")}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Закрыть")}})
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressSeries(values:List<Double?>,style:ChartStyle,low:Double,high:Double,grid:Color,selected:Int?=null) {
    val color=Color(style.color)
                    val range=(high-low).coerceAtLeast(1.0)
                    val w=size.width/values.size.coerceAtLeast(1)
                    fun point(i:Int,v:Double)=Offset(w*(i+.5f),size.height-((v-low)/range*size.height).toFloat().coerceIn(0f,size.height))
                    repeat(4){n->val y=size.height*n/3f;drawLine(grid,Offset(0f,y),Offset(size.width,y),1.dp.toPx())}
                    selected?.let {i->drawLine(color.copy(alpha=.5f),Offset(w*(i+.5f),0f),Offset(w*(i+.5f),size.height),1.dp.toPx())}
                    var previous:Offset?=null
                    values.forEachIndexed {i,v ->
                        if(v==null){previous=null;return@forEachIndexed}
                        val p=point(i,v)
                        if(style.kind=="bars")drawRoundRect(color,Offset(i*w+w*.15f,p.y),Size(w*.7f,(size.height-p.y).coerceAtLeast(2.dp.toPx())))
                        else {
                            previous?.let {before ->
                                if(style.kind=="area") {val fill=Path().apply {moveTo(before.x,size.height);lineTo(before.x,before.y);lineTo(p.x,p.y);lineTo(p.x,size.height);close()};drawPath(fill,color.copy(alpha=.22f))}
                                if(style.kind!="dots")drawLine(color,before,p,3.dp.toPx())
                            }
                            drawCircle(color,if(style.kind=="dots")5.dp.toPx()else 3.dp.toPx(),p)
                        }
                        previous=p
                    }
}
