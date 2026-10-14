package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Icon button of the rig editor: the hint appears on hover or long press, in the selected language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RigAction(icon:String,label:String,onClick:()->Unit,enabled:Boolean=true,selected:Boolean=false,translated:Boolean=true) {
    val hint=if(translated)rigText(label,LocalLanguage.current)else label
    HintBox(hint) {
        IconButton(onClick=onClick,enabled=enabled,modifier=Modifier.semantics {contentDescription=hint;this.selected=selected},colors=IconButtonDefaults.iconButtonColors(containerColor=if(selected)MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {RigSymbol(icon)}
    }
}
/** Icon that only explains the control beside it; same hover / long-press hint as a button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RigHint(icon:String,label:String) {
    val hint=rigText(label,LocalLanguage.current)
    HintBox(hint) {
        Box(Modifier.padding(4.dp).semantics {contentDescription=hint}){RigSymbol(icon)}
    }
}
@Composable internal fun RigSymbol(name:String,tint:Color=MaterialTheme.colorScheme.primary){val color=tint;val badge=name.length>1 && name.endsWith("+");val kind=if(badge)name.dropLast(1)else name
    Canvas(Modifier.size(24.dp)){scale(size.width/24,size.height/24,pivot=Offset.Zero){
        fun line(x:Float,y:Float,a:Float,b:Float,w:Float=2f)=drawLine(color,Offset(x,y),Offset(a,b),w,StrokeCap.Round)
        fun rect(x:Float,y:Float,w:Float,h:Float)=drawRoundRect(color,Offset(x,y),Size(w,h),CornerRadius(1f))
        fun frame(x:Float,y:Float,w:Float,h:Float)=drawRoundRect(color,Offset(x,y),Size(w,h),CornerRadius(1.5f),style=Stroke(2f))
        fun head(x:Float,y:Float,r:Float=2.6f)=drawCircle(color,r,Offset(x,y))
        fun turn(mirror:Boolean){withTransform({if(mirror)scale(-1f,1f,pivot=Offset(12f,12f))}){drawArc(color,45f,285f,false,Offset(4f,4f),Size(16f,16f),style=Stroke(3f));line(3f,7f,3f,2f);line(3f,7f,8f,7f)}}
        fun fist(){drawRoundRect(color,Offset(6f,9f),Size(12f,9f),CornerRadius(3f))}
        // "+" badge: the icon shrinks to make room for a plus in the corner.
        if(badge){drawLine(color,Offset(19f,1.5f),Offset(19f,8.5f),2.4f,StrokeCap.Round);drawLine(color,Offset(15.5f,5f),Offset(22.5f,5f),2.4f,StrokeCap.Round)}
        withTransform({if(badge)scale(.78f,.78f,pivot=Offset(0f,24f))}){
        when(kind){
            "star"->drawPath(Path().apply {moveTo(12f,2f);lineTo(15f,9f);lineTo(22.5f,9.6f);lineTo(16.8f,14.5f);lineTo(18.6f,22f);lineTo(12f,17.9f);lineTo(5.4f,22f);lineTo(7.2f,14.5f);lineTo(1.5f,9.6f);lineTo(9f,9f);close()},color)
            "sun"->{head(12f,12f,4.5f);(0..7).forEach {i->val a=i*0.7854f;line(12f+kotlin.math.cos(a)*7.5f,12f+kotlin.math.sin(a)*7.5f,12f+kotlin.math.cos(a)*10.5f,12f+kotlin.math.sin(a)*10.5f,2.2f)}}
            "moon"->drawPath(Path().apply {moveTo(15f,2.5f);cubicTo(4f,3f,1f,16f,10f,20.5f);cubicTo(15f,23f,20.5f,20f,21.5f,15.5f);cubicTo(13f,18f,9f,9.5f,15f,2.5f);close()},color)
            "tag"->{drawPath(Path().apply {moveTo(3f,3f);lineTo(12f,3f);lineTo(21.5f,12.5f);lineTo(12.5f,21.5f);lineTo(3f,12f);close()},color,style=Stroke(2.2f));head(8f,8f,1.8f)}
            "phone"->{frame(7f,2f,10f,20f);line(10.5f,18.5f,13.5f,18.5f,1.8f)}
            "keypad"->{listOf(6f,12f,18f).forEach {x->listOf(5f,11f,17f).forEach {y->head(x,y,1.9f)}};head(12f,22f,1.6f)}
            "pattern"->{listOf(5f,12f,19f).forEach {x->listOf(5f,12f,19f).forEach {y->head(x,y,1.6f)}};line(5f,5f,12f,12f,1.8f);line(12f,12f,19f,12f,1.8f);line(19f,12f,19f,19f,1.8f)}
            "stretch"->{head(15f,4f,2.6f);line(14f,8f,9f,14f,2.4f);line(9f,14f,3f,20f,2.4f);line(9f,14f,16f,19f,2.4f);line(16f,19f,21f,21f,2.4f);line(14f,9f,21f,11f,2.2f)}
            "biceps"->{drawPath(Path().apply {moveTo(3f,19f);lineTo(3f,13f);quadraticTo(4f,8f,9f,6f);lineTo(11f,3f);lineTo(15f,4f);lineTo(14f,9f);quadraticTo(21f,10f,21f,16f);quadraticTo(21f,20f,16f,20.5f);close()},color);drawArc(if(color.luminance()>.5f)Color(0xFF202020)else Color.White,200f,120f,false,Offset(9f,11f),Size(8f,7f),style=Stroke(1.4f))}
            "product"->{drawOval(color,Offset(4f,6f),Size(16f,15f));line(12f,6f,13.5f,2.5f,2.2f);drawPath(Path().apply {moveTo(13f,4.5f);quadraticTo(17f,1.5f,19f,4.5f);quadraticTo(16f,6.5f,13f,4.5f);close()},color)}
            "dish"->{drawArc(color,180f,180f,true,Offset(3f,7f),Size(18f,16f));line(1.5f,18f,22.5f,18f,2.4f);head(12f,5.5f,1.6f)}
            "expand"->{listOf(3f to 3f,21f to 3f,3f to 21f,21f to 21f).forEach {(x,y)->val dx=if(x<12f)6f else -6f;val dy=if(y<12f)6f else -6f;line(x,y,x+dx,y,2.4f);line(x,y,x,y+dy,2.4f)};head(12f,12f,2.6f)}
            "backspace"->{drawPath(Path().apply {moveTo(8f,5f);lineTo(22f,5f);lineTo(22f,19f);lineTo(8f,19f);lineTo(2f,12f);close()},color,style=Stroke(2.2f));line(11.5f,9f,17.5f,15f,2.2f);line(17.5f,9f,11.5f,15f,2.2f)}
            "search"->{drawCircle(color,6.5f,Offset(10f,10f),style=Stroke(2.5f));line(15f,15f,21f,21f,3f)}
            "settings"->{drawCircle(color,3.2f,Offset(12f,12f),style=Stroke(2.2f));drawCircle(color,7.2f,Offset(12f,12f),style=Stroke(2.2f));(0..7).forEach {i->val a=i*0.7854f;line(12f+kotlin.math.cos(a)*7.5f,12f+kotlin.math.sin(a)*7.5f,12f+kotlin.math.cos(a)*10.5f,12f+kotlin.math.sin(a)*10.5f,2.6f)}}
            "help"->{drawCircle(color,9.5f,Offset(12f,12f),style=Stroke(2f));drawArc(color,180f,250f,false,Offset(8.5f,5.5f),Size(7f,7f),style=Stroke(2.2f));line(12f,12.5f,12f,14.5f,2.2f);head(12f,18f,1.4f)}
            "info"->{drawCircle(color,9.5f,Offset(12f,12f),style=Stroke(2f));head(12f,7.5f,1.5f);line(12f,11f,12f,17.5f,2.6f)}
            "bell"->{drawPath(Path().apply {moveTo(5f,17f);quadraticTo(7f,15f,7f,10.5f);cubicTo(7f,3f,17f,3f,17f,10.5f);quadraticTo(17f,15f,19f,17f);close()},color);head(12f,20f,2.2f);line(12f,2f,12f,4f,2f)}
            "key"->{drawCircle(color,4.5f,Offset(8f,8f),style=Stroke(2.5f));line(11.5f,11.5f,21f,21f,2.6f);line(17f,17f,20f,14f,2.4f);line(20f,20f,22.5f,17.5f,2.4f)}
            "send"->drawPath(Path().apply {moveTo(2.5f,3.5f);lineTo(22f,12f);lineTo(2.5f,20.5f);lineTo(6.5f,12f);close()},color)
            "target"->{drawCircle(color,9.5f,Offset(12f,12f),style=Stroke(2f));drawCircle(color,5.5f,Offset(12f,12f),style=Stroke(2f));head(12f,12f,2f)}
            "database"->{drawOval(color,Offset(4f,2.5f),Size(16f,6f));listOf(8.5f,13.5f,18.5f).forEach {drawArc(color,0f,180f,false,Offset(4f,it-3f),Size(16f,6f),style=Stroke(2f))};line(4f,5.5f,4f,18.5f);line(20f,5.5f,20f,18.5f)}
            "eye"->{drawPath(Path().apply {moveTo(1.5f,12f);quadraticTo(12f,1.5f,22.5f,12f);quadraticTo(12f,22.5f,1.5f,12f);close()},color,style=Stroke(2f));head(12f,12f,3.4f)}
            "skip"->{drawPath(Path().apply {moveTo(4f,4f);lineTo(15f,12f);lineTo(4f,20f);close()},color);line(19f,4f,19f,20f,3f)}
            "skip2"->{drawPath(Path().apply {moveTo(2f,4f);lineTo(10.5f,12f);lineTo(2f,20f);close()},color);drawPath(Path().apply {moveTo(10.5f,4f);lineTo(19f,12f);lineTo(10.5f,20f);close()},color);line(21.5f,4f,21.5f,20f,2.6f)}
            "units"->{frame(2f,8f,20f,8f);listOf(6f,10f,14f,18f).forEach {line(it,8f,it,if(it==10f || it==18f)13f else 11.5f,1.6f)}}
            "energy"->drawPath(Path().apply {moveTo(13.5f,1.5f);lineTo(4.5f,13.5f);lineTo(11f,13.5f);lineTo(9.5f,22.5f);lineTo(19.5f,9.5f);lineTo(12.5f,9.5f);close()},color)
            "fingerprint"->{listOf(3.5f,6.5f,9.5f).forEach {r->drawArc(color,180f,180f,false,Offset(12f-r,12f-r),Size(r*2,r*2),style=Stroke(1.8f))};line(8.5f,12f,8.5f,18f,1.8f);line(12f,11f,12f,21f,1.8f);line(15.5f,12f,15.5f,19f,1.8f);line(5.5f,12f,5.5f,15f,1.8f);line(18.5f,12f,18.5f,16f,1.8f)}
            "link"->{drawRoundRect(color,Offset(2.5f,8.5f),Size(11f,7f),CornerRadius(3.5f),style=Stroke(2.3f));drawRoundRect(color,Offset(10.5f,8.5f),Size(11f,7f),CornerRadius(3.5f),style=Stroke(2.3f))}
            "unlock"->{rect(5f,11f,14f,10f);drawArc(color,180f,150f,false,Offset(8f,3f),Size(8f,12f),style=Stroke(2.5f))}
            "up"->{line(12f,20f,12f,5f,3f);line(12f,4f,6f,10f,3f);line(12f,4f,18f,10f,3f)}
            "down"->{line(12f,4f,12f,19f,3f);line(12f,20f,6f,14f,3f);line(12f,20f,18f,14f,3f)}
            "pause"->{rect(5f,4f,5f,16f);rect(14f,4f,5f,16f)}
            "battery"->{frame(2f,7f,17f,10f);rect(20f,10f,2.5f,4f);rect(4.5f,9.5f,8f,5f)}
            "image"->{frame(2.5f,4f,19f,16f);head(8f,9f,1.8f);drawPath(Path().apply {moveTo(4f,18.5f);lineTo(10f,12.5f);lineTo(13.5f,16f);lineTo(16.5f,13f);lineTo(20f,18.5f);close()},color)}
            "table"->{frame(2.5f,4f,19f,16f);line(2.5f,9.5f,21.5f,9.5f,1.6f);line(2.5f,14.5f,21.5f,14.5f,1.6f);line(9f,4f,9f,20f,1.6f);line(15.5f,4f,15.5f,20f,1.6f)}
            "document","report"->{drawPath(Path().apply {moveTo(5f,2.5f);lineTo(14.5f,2.5f);lineTo(19f,7f);lineTo(19f,21.5f);lineTo(5f,21.5f);close()},color,style=Stroke(2f));listOf(10f,13.5f,17f).forEach {line(8f,it,16f,it,1.6f)};if(kind=="report"){line(8f,17f,8f,13f,2.2f);line(12f,17f,12f,10f,2.2f);line(16f,17f,16f,12f,2.2f)}}
            "scale"->{drawRoundRect(color,Offset(3f,5f),Size(18f,15f),CornerRadius(3f),style=Stroke(2.2f));drawArc(color,200f,140f,false,Offset(7f,8f),Size(10f,10f),style=Stroke(2f));line(12f,13f,14.5f,9.5f,2f)}
            "steps"->{drawOval(color,Offset(4f,3f),Size(6f,10f));drawOval(color,Offset(5f,14.5f),Size(4.5f,4f));drawOval(color,Offset(14f,7f),Size(6f,10f));drawOval(color,Offset(14.5f,18.5f),Size(4.5f,4f))}
            "language"->{drawCircle(color,9.5f,Offset(12f,12f),style=Stroke(2f));drawOval(color,Offset(7.5f,2.5f),Size(9f,19f),style=Stroke(1.8f));line(2.5f,12f,21.5f,12f,1.8f)}
            "play"->{drawPath(Path().apply {moveTo(6f,3f);lineTo(21f,12f);lineTo(6f,21f);close()},color)}
            "stop"->rect(4f,4f,16f,16f)
            "undo","rotation"->turn(false)
            "redo"->turn(true)
            "close"->{line(5f,5f,19f,19f,3f);line(19f,5f,5f,19f,3f)}
            "save"->{line(4f,13f,10f,19f,3f);line(10f,19f,20f,6f,3f)}
            "loop"->{drawArc(color,200f,140f,false,Offset(3f,5f),Size(18f,14f),style=Stroke(2.5f));drawArc(color,20f,140f,false,Offset(3f,5f),Size(18f,14f),style=Stroke(2.5f));line(20f,9f,21f,4f);line(20f,9f,15f,8f);line(4f,15f,3f,20f);line(4f,15f,9f,16f)}
            "mirror"->{line(12f,2f,12f,22f,1.5f);drawPath(Path().apply {moveTo(9f,6f);lineTo(2f,12f);lineTo(9f,18f);close()},color);drawPath(Path().apply {moveTo(15f,6f);lineTo(22f,12f);lineTo(15f,18f);close()},color,style=Stroke(2f))}
            "lock"->{rect(5f,11f,14f,10f);drawArc(color,180f,180f,false,Offset(8f,4f),Size(8f,12f),style=Stroke(2.5f))}
            "pin"->{head(12f,7f,5f);line(12f,12f,12f,22f,2.5f)}
            "depth"->{frame(3f,3f,12f,12f);rect(9f,9f,12f,12f)}
            "layer-auto"->{frame(3f,3f,12f,12f);frame(9f,9f,12f,12f)}
            "layer-back"->{rect(3f,3f,12f,12f);frame(9f,9f,12f,12f)}
            "layer-front"->{frame(3f,3f,12f,12f);rect(9f,9f,12f,12f);line(15f,12f,15f,18f,1.5f)}
            "horizontal","width"->{line(3f,12f,21f,12f);line(3f,12f,7f,8f);line(3f,12f,7f,16f);line(21f,12f,17f,8f);line(21f,12f,17f,16f);if(kind=="width"){line(2f,5f,2f,19f);line(22f,5f,22f,19f)}else head(12f,12f,2.2f)}
            "vertical","height"->{line(12f,3f,12f,21f);line(12f,3f,8f,7f);line(12f,3f,16f,7f);line(12f,21f,8f,17f);line(12f,21f,16f,17f);if(kind=="height"){line(5f,2f,19f,2f);line(5f,22f,19f,22f)}else head(12f,12f,2.2f)}
            "angle"->{line(4f,20f,20f,20f,2.5f);line(4f,20f,16f,5f,2.5f);drawArc(color,-52f,52f,false,Offset(-4f,12f),Size(16f,16f),style=Stroke(2f))}
            "tools"->{listOf(6f,12f,18f).forEachIndexed {i,y->line(3f,y,21f,y);head(if(i==1)8f else 16f,y,2.6f)}}
            "move"->{line(12f,3f,12f,21f);line(3f,12f,21f,12f);line(12f,3f,9f,6f);line(12f,3f,15f,6f);line(12f,21f,9f,18f);line(12f,21f,15f,18f);line(3f,12f,6f,9f);line(3f,12f,6f,15f);line(21f,12f,18f,9f);line(21f,12f,18f,15f)}
            "hide"->{drawArc(color,200f,140f,false,Offset(2f,7f),Size(20f,14f),style=Stroke(2f));drawArc(color,20f,140f,false,Offset(2f,3f),Size(20f,14f),style=Stroke(2f));head(12f,12f,2.6f);line(4f,20f,20f,4f,2.5f)}
            "palette"->{drawCircle(color,9f,Offset(12f,12f),style=Stroke(2f));head(8f,9f,1.8f);head(13f,7f,1.8f);head(16.5f,11f,1.8f);head(9f,15f,1.8f)}
            "line-thin"->line(4f,18f,20f,6f,1.4f)
            "line-medium"->line(4f,18f,20f,6f,3.2f)
            "line-thick"->line(5f,17f,19f,7f,6f)
            "toes"->{line(2f,17f,13f,17f,3f);line(13f,17f,21f,9f,3f);drawCircle(color,2.2f,Offset(13f,17f))}
            "headturn"->{head(12f,9f,5f);line(12f,14f,12f,21f,3f);drawArc(color,200f,140f,false,Offset(3f,1f),Size(18f,10f),style=Stroke(2f));line(21f,4f,21f,8f);line(21f,4f,17f,4f)}
            "torso"->{frame(7f,4f,10f,13f);drawArc(color,20f,140f,false,Offset(3f,13f),Size(18f,9f),style=Stroke(2f));line(19f,20f,21f,16f);line(19f,20f,15f,20f)}
            "time"->{drawCircle(color,9f,Offset(12f,12f),style=Stroke(2f));line(12f,12f,12f,6f);line(12f,12f,16f,14f)}
            "duration"->{line(5f,3f,19f,3f);line(5f,21f,19f,21f);drawPath(Path().apply {moveTo(6f,3f);lineTo(18f,3f);lineTo(12f,12f);close()},color);drawPath(Path().apply {moveTo(12f,12f);lineTo(18f,21f);lineTo(6f,21f);close()},color,style=Stroke(2f))}
            "anchor"->{drawCircle(color,2.5f,Offset(12f,4.5f),style=Stroke(2f));line(12f,7f,12f,21f);line(7f,11f,17f,11f);drawArc(color,10f,160f,false,Offset(4f,8f),Size(16f,13f),style=Stroke(2f))}
            "front"->{drawCircle(color,9f,Offset(12f,12f),style=Stroke(2f));head(8.5f,10f,1.5f);head(15.5f,10f,1.5f);line(9f,16f,15f,16f)}
            "side"->{drawArc(color,90f,270f,false,Offset(3f,3f),Size(18f,18f),style=Stroke(2f));line(12f,3f,17f,9f);line(17f,9f,21f,13f);line(21f,13f,17f,15f);line(17f,15f,12f,21f);head(13f,10f,1.5f)}
            "back"->head(12f,12f,9f)
            "side2"->withTransform({scale(-1f,1f,pivot=Offset(12f,12f))}){drawArc(color,90f,270f,false,Offset(3f,3f),Size(18f,18f),style=Stroke(2f));line(12f,3f,17f,9f);line(17f,9f,21f,13f);line(21f,13f,17f,15f);line(17f,15f,12f,21f);head(13f,10f,1.5f)}
            // Seen from above: the shoulders, the head on them and the nose.
            "top"->{drawOval(color,Offset(2f,9f),Size(20f,8f),style=Stroke(2f));head(12f,12.5f,4.2f);line(12f,17f,12f,19.5f,2.2f)}
            "woman"->{head(12f,4.5f,3f);drawPath(Path().apply {moveTo(8f,9f);lineTo(16f,9f);lineTo(13.5f,14f);lineTo(17f,20f);lineTo(7f,20f);lineTo(10.5f,14f);close()},color);line(4f,3f,6f,8f,1.8f)}
            "man"->{head(12f,4.5f,3f);drawPath(Path().apply {moveTo(5f,9f);lineTo(19f,9f);lineTo(15f,17f);lineTo(15f,22f);lineTo(9f,22f);lineTo(9f,17f);close()},color)}
            "joint","Skeleton"->{drawCircle(color,3f,Offset(12f,4f));line(12f,8f,12f,15f);line(5f,12f,19f,12f);line(12f,15f,6f,22f);line(12f,15f,18f,22f)}
            "standing"->{head(12f,3.5f);line(12f,7f,12f,15f);line(8f,9f,16f,9f);line(12f,15f,9f,22f);line(12f,15f,15f,22f)}
            "seated"->{head(8f,4f);line(8f,8f,8f,15f);line(8f,15f,16f,15f);line(16f,15f,16f,22f);line(8f,10f,13f,12f);line(3f,19f,11f,19f,1.5f)}
            "lying"->{head(4f,14f);line(8f,15f,21f,15f);line(2f,20f,22f,20f,1.5f)}
            "plank"->{head(4f,10f);line(7f,12f,21f,17f);line(8f,12f,8f,18f);line(2f,20f,22f,20f,1.5f)}
            "hanging"->{line(3f,3f,21f,3f,3f);head(12f,9f);line(8f,4f,12f,12f);line(16f,4f,12f,12f);line(12f,12f,12f,18f);line(12f,18f,10f,22f);line(12f,18f,14f,22f)}
            "grip-open"->{drawRoundRect(color,Offset(7f,11f),Size(10f,10f),CornerRadius(3f));listOf(8f,11f,14f,17f).forEach {line(it-.5f,11f,it-.5f,3f+abs(it-12.5f)*.6f,2.2f)}}
            "grip-pronated"->{fist();line(12f,2f,12f,7f);line(12f,7f,9f,4f);line(12f,7f,15f,4f);line(3f,21f,21f,21f,1.5f)}
            "grip-supinated"->{fist();line(12f,7f,12f,2f);line(12f,2f,9f,5f);line(12f,2f,15f,5f);line(3f,21f,21f,21f,1.5f)}
            "grip-neutral"->{fist();line(12f,2f,12f,22f,1.5f)}
            "export","import","svg"->{rect(3f,18f,18f,3f);val from=if(kind=="import")3f else 16f;val to=if(kind=="import")16f else 3f;line(12f,from,12f,to,3f);line(12f,to,7f,if(kind=="import")11f else 8f,3f);line(12f,to,17f,if(kind=="import")11f else 8f,3f);if(kind=="svg")drawCircle(color,2f,Offset(21f,3f))}
            "dumbbell","barbell","inventory"->{line(3f,12f,21f,12f,3f);rect(2f,6f,4f,12f);rect(18f,6f,4f,12f);if(kind=="barbell"){rect(7f,8f,2f,8f);rect(15f,8f,2f,8f)}}
            "bar"->{rect(2f,3f,20f,3f);line(3f,3f,3f,21f);line(21f,3f,21f,21f)}
            "chair"->{line(5f,2f,5f,22f,3f);line(5f,13f,20f,13f,3f);line(19f,13f,19f,22f,3f)}
            "wall"->{rect(3f,2f,3f,20f);listOf(4f,10f,16f).forEach {line(8f,it,19f,it);line(12f,it,12f,it+4)}}
            "band","rope","stick"->{line(3f,18f,21f,6f,if(kind=="stick")4f else 2f);if(kind!="stick"){drawCircle(color,3f,Offset(4f,18f),style=Stroke(2f));drawCircle(color,3f,Offset(20f,6f),style=Stroke(2f))}}
            "hoop","ball","roller"->drawOval(color,Offset(3f,3f),Size(18f,18f),style=if(kind=="ball")Fill else Stroke(if(kind=="roller")5f else 2f))
            "mat","step","block","custom"->{rect(3f,if(kind=="mat")17f else 8f,18f,if(kind=="mat")4f else 12f);if(kind=="step")rect(8f,4f,13f,5f)}
            "kettlebell"->{drawCircle(color,7f,Offset(12f,16f));drawCircle(color,5f,Offset(12f,6f),style=Stroke(3f))}
            "arrow"->{line(3f,12f,21f,12f,3f);line(21f,12f,15f,6f,3f);line(21f,12f,15f,18f,3f)}
            "label"->{line(3f,4f,21f,4f,3f);line(12f,4f,12f,21f,3f)}
            else->drawFilledSymbol(kind,color)
        }
        }
    }}
}
private fun abs(v:Float)=kotlin.math.abs(v)
