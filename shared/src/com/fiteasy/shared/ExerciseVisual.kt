package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val exerciseTechniques=linkedMapOf("" to "No scene", "plank" to "Plank", "pushup" to "Push-up", "squat" to "Squat", "raise" to "Lateral raise", "curl" to "Biceps curl", "pullup" to "Pull-up", "heelraise" to "Heel raise", "toeraise" to "Toe raise",
    "armsup" to "Arms overhead", "twist" to "Torso turn", "march" to "Marching in place", "sidelegraise" to "Side leg raise", "necktilt" to "Neck side tilt", "sitstand" to "Sit to stand", "kneeext" to "Seated knee extension")
val allExerciseTechniques get()=exerciseTechniques
fun exerciseMotion(technique:String,phase:Float)=if(technique in exerciseTechniques && technique !in setOf("","plank"))kotlin.math.sin(phase*6.283185f)else 0f
fun exerciseAnimates(technique:String,running:Boolean,enabled:Boolean,visible:Boolean)=running && enabled && visible && technique in exerciseTechniques && technique !in setOf("","plank")
@Composable fun ExerciseSilhouette(exercise:Exercise,running:Boolean=false,female:Boolean=true,compact:Boolean=false,onClick:(()->Unit)?=null){
 val c=exercise.rig
 if(c==null){Box(Modifier.size(if(compact)80.dp else 180.dp).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier)){Text(rigText("No rig scene",LocalLanguage.current))};return}
 RigCanvas(c,running=running,female=female,compact=compact,onClick=onClick)
}
@Composable fun RigCanvas(c:RigClip,running:Boolean=false,female:Boolean?=null,compact:Boolean=false,pose:RigPose?=null,handles:Boolean=false,selected:String="",modifier:Modifier=Modifier,side:androidx.compose.ui.unit.Dp?=null,onClick:(()->Unit)?=null){
 var visible by remember {mutableStateOf(false)}
 val enabled=running && LocalExerciseMotion.current && visible && rigIsMoving(c)
 val seconds=produceState(0f,c,enabled){if(enabled){val start=System.nanoTime();while(true){value=(System.nanoTime()-start)/1e9.toFloat();if(!c.loop && value>=c.duration){value=c.duration;break};delay(50)}}else value=0f}
 val color=MaterialTheme.colorScheme.primary
 val measurer=androidx.compose.ui.text.rememberTextMeasurer()
 val style=MaterialTheme.typography.labelSmall
 val captions=c.props.filter {it.name.isNotEmpty()}.associate {it.id to it.name}
 val labels=remember(captions,style){captions.mapValues {measurer.measure(it.value,style=style)}}
 Canvas(modifier.size(side ?: if(compact)80.dp else 240.dp).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).onGloballyPositioned {val r=it.boundsInWindow();visible=r.width>0 && r.height>0}){drawRig(c,pose ?: rigSample(c,seconds.value),seconds.value,female,color,handles,selected,labels)}
}
fun DrawScope.drawExerciseFigure(technique:String,phase:Float,female:Boolean,color:Color,chair:Color){val c=rigTemplate(if(technique in exerciseTechniques)technique else "plank");drawRig(c,rigSample(c,phase*c.duration),phase*c.duration,female,color)}
