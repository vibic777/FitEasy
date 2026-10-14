package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** [onDone] receives the drawn cells when the finger lifts. */
@Composable fun PatternInput(onChange:(String)->Unit,onDone:(String)->Unit={}) {
    var cells by remember {mutableStateOf<List<Int>>(emptyList())}
    val changed by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(240.dp).pointerInput(Unit){
        fun add(position:Offset){
            val step=size.width/3f
            if(position.x<0 || position.y<0 || position.x>=size.width || position.y>=size.height)return
            val col=(position.x/step).toInt();val row=(position.y/step).toInt();val id=row*3+col
            if((position-Offset((col+.5f)*step,(row+.5f)*step)).getDistance()>step*.35f || id in cells)return
            val last=cells.lastOrNull()
            if(last!=null){val dr=abs(last/3-row);val dc=abs(last%3-col);if((dr==2 && dc==0) || (dc==2 && dr==0) || (dr==2 && dc==2)){val middle=((last/3+row)/2)*3+(last%3+col)/2;if(middle !in cells)cells=cells+middle}}
            cells=cells+id;changed(cells.joinToString(","))
        }
        detectDragGestures(onDragStart={cells=emptyList();changed("");add(it)},onDragEnd={done(cells.joinToString(","))},onDrag={change,_->add(change.position);change.consume()})
    }){
        val step=size.width/3f
        fun center(id:Int)=Offset((id%3+.5f)*step,(id/3+.5f)*step)
        cells.zipWithNext().forEach {(a,b)->drawLine(color,center(a),center(b),4.dp.toPx())}
        (0..8).forEach {id->drawCircle(color.copy(alpha=if(id in cells)1f else .35f),8.dp.toPx(),center(id))}
    }
}
