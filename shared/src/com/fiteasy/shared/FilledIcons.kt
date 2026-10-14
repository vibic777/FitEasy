package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlin.math.*

val sectionColors=mapOf("today" to 0xFFB76C00L,"food" to 0xFF228847L,"water" to 0xFF087CC8L,"activity" to 0xFFD84A39L,"progress" to 0xFF864AC8L,"notes" to 0xFFB14F87L)
fun iconColor(name:String,dark:Boolean,accent:Long)=readableAccent(Color(sectionColors[name] ?: accent),dark)

/** Original filled symbols without badges or trailing decoration. */
@Composable fun FitIcon(name:String,modifier:Modifier=Modifier) {
    val a=LocalAppearance.current
    val dark=MaterialTheme.colorScheme.surface.luminance()<.179f
    val color=remember(name,dark,a.accent){iconColor(name,dark,a.accent)}
    Canvas(modifier.size(24.dp)){scale(size.width/24,size.height/24,pivot=Offset.Zero){drawFilledSymbol(name,color,if(a.style=="rainbow")3f else if(a.style=="geometry")1f else 2f)}}
}

fun DrawScope.drawFilledSymbol(name:String,color:Color,round:Float=2f,phase:Float=0f,animated:Boolean=false) {
    val white=if(color.luminance()>.5f)Color(0xFF202020)else Color.White
    fun polygon(vararg xy:Float){val p=Path().apply {moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()};drawPath(p,color)}
    when(name){
        "folder"->{drawRoundRect(color,Offset(2f,7f),Size(20f,15f),CornerRadius(round));drawRoundRect(color,Offset(2f,3f),Size(9f,8f),CornerRadius(round));drawLine(white.copy(alpha=.5f),Offset(5f,10f),Offset(19f,10f),1f)}
        "sort"->{listOf(5f,11f,17f).forEachIndexed {i,y->drawRoundRect(color,Offset(2f,y),Size(13f-i*3,3f),CornerRadius(1f))};drawLine(color,Offset(19f,3f),Offset(19f,20f),2f);polygon(15f,16f,19f,22f,23f,16f)}
        "grid"->{for(x in listOf(2f,13f))for(y in listOf(2f,13f))drawRoundRect(color,Offset(x,y),Size(9f,9f),CornerRadius(round))}
        "cards"->{drawRoundRect(color,Offset(2f,2f),Size(20f,20f),CornerRadius(round));drawLine(white,Offset(5f,8f),Offset(19f,8f),2f);drawLine(white,Offset(5f,15f),Offset(15f,15f),2f)}
        "water"->{val y=if(animated)((phase*3)%1)*5f-2f else 0f
            val p=Path().apply {moveTo(12f,2f+y);cubicTo(9f,7f+y,5f,11f+y,5f,15f+y);cubicTo(5f,23f+y,19f,23f+y,19f,15f+y);cubicTo(19f,11f+y,15f,7f+y,12f,2f+y);close()};drawPath(p,color);drawOval(white.copy(alpha=.75f),Offset(8f,12f+y),Size(2f,5f))
            if(round>=3f){drawCircle(white,1f,Offset(10f,15f+y));drawCircle(white,1f,Offset(14f,15f+y));drawArc(white,0f,180f,false,Offset(10f,16f+y),Size(4f,2f),style=Stroke(.8f))}
            if(animated)drawOval(color.copy(alpha=.3f),Offset(6f,23f),Size(12f,1f))}
        "food"->{drawCircle(color,10f,Offset(12f,12f));drawCircle(white,8f,Offset(12f,12f))
            val remaining=if(animated)8-floor((phase%1)*8).toInt()else 8
            repeat(remaining){i->drawArc(Color(0xFFE98825),i*45f,38f,true,Offset(6.5f,6.5f),Size(11f,11f))}
            if(!animated)drawCircle(Color(0xFFE98825),2.2f,Offset(12f,12f))}
        "activity"->{
            // Easy jog (D084): shaped torso, bent arms, a flight phase and a ponytail that follows the bounce.
            val pose=joggingPose(if(animated)phase*5.2f else .06f)
            val farInk=Color(color.red*.55f+.45f,color.green*.55f+.45f,color.blue*.55f+.45f).let {if(color.luminance()>.5f)Color(color.red*.6f,color.green*.6f,color.blue*.6f)else it}
            fun segment(ink:Color,a:Offset,b:Offset,wa:Float,wb:Float){
                val d=(b-a).let {it/it.getDistance().coerceAtLeast(.01f)};val n=Offset(-d.y,d.x)
                drawPath(Path().apply {val p=a+n*wa;val q=b+n*wb;val r=b-n*wb;val t=a-n*wa;moveTo(p.x,p.y);lineTo(q.x,q.y);lineTo(r.x,r.y);lineTo(t.x,t.y);close()},ink)
                drawCircle(ink,wa,a);drawCircle(ink,wb,b)
            }
            fun leg(p:JoggingLimb,ink:Color){segment(ink,p.root,p.joint,1.3f,.95f);segment(ink,p.joint,p.end,.95f,.65f);segment(ink,p.end,p.toe,.7f,.5f)}
            fun arm(p:JoggingLimb,ink:Color){segment(ink,p.root,p.joint,.85f,.7f);segment(ink,p.joint,p.end,.7f,.55f)}
            leg(pose.farLeg,farInk);arm(pose.farArm,farInk)
            segment(color,pose.shoulder,pose.hip,1.75f,1.35f)
            segment(color,pose.shoulder,pose.head,.65f,.65f)
            // Long loose hair: two tapering locks streaming behind the head, lifting and falling with the stride.
            val h=pose.head;val tip=pose.hair;val sway=(tip.y-h.y)*.5f
            drawPath(Path().apply {moveTo(h.x-.6f,h.y-1.85f);cubicTo(h.x-3.2f,h.y-2.5f+sway,tip.x+2.6f,tip.y-1.5f,tip.x,tip.y)
                cubicTo(tip.x+2.4f,tip.y+.4f,h.x-3.4f,h.y+.2f+sway,h.x-1.75f,h.y+.75f);close()},color)
            drawPath(Path().apply {moveTo(h.x-1.5f,h.y+.2f);cubicTo(h.x-3f,h.y+.9f+sway,tip.x+3.2f,tip.y+1.3f,tip.x+1.3f,tip.y+2.3f)
                cubicTo(tip.x+3.4f,tip.y+2.2f,h.x-2.6f,h.y+2.3f,h.x-1.1f,h.y+1.55f);close()},color)
            drawCircle(color,1.9f,pose.head)
            leg(pose.nearLeg,color);arm(pose.nearArm,color)
        }
        "today"->{drawCircle(color,11f,Offset(12f,12f));drawCircle(white,8.3f,Offset(12f,12f));val t=if(animated)phase*2*PI else -PI/2
            drawLine(color,Offset(12f,12f),Offset(12f+6f*cos(t).toFloat(),12f+6f*sin(t).toFloat()),2f,cap=StrokeCap.Round);drawLine(color,Offset(12f,12f),Offset(16f,14f),2.5f,cap=StrokeCap.Round);drawCircle(color,1.4f,Offset(12f,12f))}
        "progress"->{val build=if(animated)((phase%1)*1.4f).coerceAtMost(1f)else 1f
            drawRoundRect(color,Offset(3f,13f+8*(1-build)),Size(4f,8*build+.5f),CornerRadius(round));drawRoundRect(color,Offset(10f,8f+13*(1-build)),Size(4f,13*build+.5f),CornerRadius(round));drawRoundRect(color,Offset(17f,2f+19*(1-build)),Size(4f,19*build+.5f),CornerRadius(round))}
        "add"->{drawRoundRect(color,Offset(10f,3f),Size(4f,18f),CornerRadius(round));drawRoundRect(color,Offset(3f,10f),Size(18f,4f),CornerRadius(round))}
        "previous","next"->{if(name=="previous")polygon(15f,2f,4f,12f,15f,22f,19f,18f,12f,12f,19f,6f)else polygon(9f,2f,20f,12f,9f,22f,5f,18f,12f,12f,5f,6f)}
        "edit","drawing"->{polygon(3f,17f,17f,3f,22f,8f,8f,22f,2f,23f);drawLine(white,Offset(15f,5f),Offset(20f,10f),1.5f)}
        "copy"->{drawRoundRect(color,Offset(2f,2f),Size(15f,16f),CornerRadius(round));drawRoundRect(white,Offset(5f,5f),Size(17f,18f),CornerRadius(round));drawRoundRect(color,Offset(7f,7f),Size(15f,16f),CornerRadius(round))}
        "delete"->{drawRoundRect(color,Offset(5f,7f),Size(14f,15f),CornerRadius(round));drawRoundRect(color,Offset(3f,3f),Size(18f,3f),CornerRadius(1f));drawRect(color,Offset(9f,1f),Size(6f,2f));drawLine(white,Offset(10f,10f),Offset(10f,19f),1.5f);drawLine(white,Offset(14f,10f),Offset(14f,19f),1.5f)}
        "notes"->{
            drawRoundRect(color,Offset(2f,2f),Size(17f,21f),CornerRadius(round))
            drawRoundRect(white,Offset(5f,4f),Size(12f,17f),CornerRadius(1f))
            for(y in listOf(6f,11f,16f))drawLine(white,Offset(1f,y),Offset(4f,y),1.4f,StrokeCap.Round)
            val writing=if(animated)((phase*2)%1)*3 else 3f
            listOf(8f,12f,16f).forEachIndexed {i,y->val length=(writing-i).coerceIn(0f,1f)*7f;if(length>0)drawLine(color,Offset(7f,y),Offset(7f+length,y),1.2f,StrokeCap.Round)}
            val row=if(animated)floor(writing).toInt().coerceIn(0,2)else 1
            val x=if(animated)7f+(writing-row)*7 else 14f;val y=8f+row*4f
            withTransform({translate(x,y);rotate(-35f,Offset.Zero)}){
                drawRoundRect(color,Offset(0f,-8f),Size(3f,9f),CornerRadius(.6f))
                polygon(0f,1f,3f,1f,1.5f,3f);drawLine(white,Offset(1.5f,-6f),Offset(1.5f,-1f),.7f)
            }
        }
        "text","list","calendar"->{drawRoundRect(color,Offset(3f,2f),Size(18f,21f),CornerRadius(round));for(y in listOf(8f,13f,18f)){if(name=="list")drawCircle(white,1f,Offset(7f,y));drawLine(white,Offset(if(name=="list")10f else 7f,y),Offset(17f,y),1.8f)}}
        else->{drawCircle(color,4.5f,Offset(12f,6f));drawRoundRect(color,Offset(3f,12f),Size(18f,11f),CornerRadius(if(round>=3)8f else 4f))}
    }
}
