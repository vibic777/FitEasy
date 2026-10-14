package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

val toolDocks=listOf("bottom","right","top","left")
fun nextToolDock(dock:String)=toolDocks[(toolDocks.indexOf(dock).coerceAtLeast(0)+1)%toolDocks.size]

/**
 * Tool panel that floats over a canvas (D088): one icon moves it round the four edges, another hides it down to a single
 * button, so the drawing area stays whole. Icons wrap instead of scrolling.
 */
@Composable fun BoxScope.ToolDock(dock:String,onDock:(String)->Unit,hidden:Boolean,onHidden:(Boolean)->Unit,content:@Composable ()->Unit) {
    val place=when(dock){"top"->Alignment.TopCenter;"left"->Alignment.CenterStart;"right"->Alignment.CenterEnd;else->Alignment.BottomCenter}
    if(hidden) {
        val corner=when(dock){"top"->Alignment.TopEnd;"left"->Alignment.BottomStart;else->Alignment.BottomEnd}
        Surface(Modifier.align(corner).padding(6.dp),shape=CircleShape,tonalElevation=6.dp,shadowElevation=3.dp) {RigAction("tools","Show panel",{onHidden(false)})}
        return
    }
    Surface(Modifier.align(place).padding(6.dp),shape=MaterialTheme.shapes.medium,tonalElevation=6.dp,shadowElevation=3.dp,color=MaterialTheme.colorScheme.surface.copy(alpha=.93f)) {
        val controls:@Composable ()->Unit={RigAction("move","Move panel",{onDock(nextToolDock(dock))});RigAction("hide","Hide panel",{onHidden(true)})}
        if(dock=="top" || dock=="bottom")FlowRow(Modifier.padding(2.dp),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.Center) {controls();content()}
        else FlowColumn(Modifier.padding(2.dp),verticalArrangement=Arrangement.Center,horizontalArrangement=Arrangement.Center) {controls();content()}
    }
}
/** Small gap between groups of icons; works in both panel directions. */
@Composable fun ToolGap() {Spacer(Modifier.size(10.dp))}

/** Whole-screen sheet for a drawing note: the square sheet uses all the room, tools float in a ToolDock. */
@Composable fun DrawingBoard(platform:Platform,strokes:List<InkStroke>,onChange:(List<InkStroke>)->Unit,onClose:()->Unit) {
    val language=LocalLanguage.current
    var color by remember {mutableLongStateOf(0xFF202020)};var width by remember {mutableFloatStateOf(0.006f)}
    var current by remember {mutableStateOf<List<InkPoint>>(emptyList())}
    var dock by remember {mutableStateOf(platform.readSecret("tool-dock")?.takeIf {it in toolDocks} ?: "bottom")};var hidden by remember {mutableStateOf(false)}
    var palette by remember {mutableStateOf(false)};var clear by remember {mutableStateOf(false)}
    val latest by rememberUpdatedState(strokes)
    fun hint(text:String)=translate(text,language)
    FullPage(onClose) {androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {Column(Modifier.fillMaxSize()) {
        WindowBarRow {IconAction("previous","Назад",onClick=onClose);Text("Рисунок",Modifier.weight(1f).padding(start=8.dp),style=MaterialTheme.typography.titleLarge)}
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val side=minOf(maxWidth,maxHeight)
            Canvas(Modifier.size(side).align(Alignment.Center).background(Color.White).pointerInput(color,width) {
                fun point(position:Offset)=InkPoint((position.x/size.width).coerceIn(0f,1f),(position.y/size.height).coerceIn(0f,1f))
                detectDragGestures(onDragStart={current=listOf(point(it))},onDrag={change,_->change.consume();if(current.size<10000)current=current+point(change.position)},onDragEnd={
                    if(current.isNotEmpty() && latest.size<2000 && latest.sumOf {it.points.size}+current.size<=100000)onChange(latest+InkStroke(color=color,width=width,points=current))
                    current=emptyList()
                },onDragCancel={current=emptyList()})
            }) {
                fun ink(points:List<InkPoint>,c:Long,w:Float){if(points.isEmpty())return;val radius=w*size.width/2
                    if(points.size==1)drawCircle(Color(c),radius,Offset(points[0].x*size.width,points[0].y*size.height))
                    else {val path=Path();points.forEachIndexed {i,p->if(i==0)path.moveTo(p.x*size.width,p.y*size.height)else path.lineTo(p.x*size.width,p.y*size.height)};drawPath(path,Color(c),style=Stroke(w*size.width,cap=StrokeCap.Round,join=StrokeJoin.Round))}}
                strokes.forEach {ink(it.points,it.color,it.width)};ink(current,color,width)
            }
            ToolDock(dock,{dock=it;runCatching {platform.writeSecret("tool-dock",it)}},hidden,{hidden=it}) {
                RigAction("palette",hint("Цвет пера"),{palette=!palette},selected=palette,translated=false)
                listOf(Triple("line-thin",0.003f,"Тонкая линия"),Triple("line-medium",0.006f,"Средняя линия"),Triple("line-thick",0.014f,"Толстая линия")).forEach {(icon,value,label)->RigAction(icon,hint(label),{width=value},selected=width==value,translated=false)}
                ToolGap()
                RigAction("undo",hint("Отменить штрих"),{onChange(strokes.dropLast(1))},enabled=strokes.isNotEmpty(),translated=false)
                RigAction("delete",hint("Очистить рисунок"),{clear=true},enabled=strokes.isNotEmpty(),translated=false)
                RigAction("save",hint("Готово"),onClose,translated=false)
            }
            if(palette)Surface(Modifier.align(if(dock=="bottom")Alignment.TopCenter else Alignment.BottomCenter).padding(8.dp).widthIn(max=420.dp),shape=MaterialTheme.shapes.medium,tonalElevation=6.dp,shadowElevation=3.dp) {
                Column(Modifier.padding(8.dp)) {ColorChoice(color,{color=it})}
            }
        }
    }}}
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Очистить рисунок?")},confirmButton={TextButton(onClick={onChange(emptyList());clear=false}){Text("Очистить")}},dismissButton={TextButton(onClick={clear=false}){Text("Отмена")}})
}
