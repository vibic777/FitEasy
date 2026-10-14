package com.fiteasy.shared

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/** Screen right is forward. Stance travels backward; recovery travels forward above ground. */
data class WalkingLeg(val hip:Offset,val knee:Offset,val ankle:Offset,val contact:Boolean)
fun walkingLeg(phase:Float):WalkingLeg {
    val t=((phase%1f)+1f)%1f
    val contact=t<.5f
    val u=if(contact)t*2 else (t-.5f)*2
    val x=if(contact)3f-6f*u else -3f+6f*(.5f-.5f*cos(PI.toFloat()*u))
    val lift=if(contact)0f else 2.6f*sin(PI.toFloat()*u)
    val hip=Offset(11.5f,13.5f)
    val ankle=Offset(11.5f+x,22f-lift)
    val delta=ankle-hip
    val distance=delta.getDistance()
    val mid=(hip+ankle)*.5f
    val height=sqrt((4.8f*4.8f-distance*distance*.25f).coerceAtLeast(0f))
    // Forward pole: knees flex toward the nose, never into the back of the leg.
    val knee=mid+Offset(delta.y,-delta.x)*(height/distance)
    return WalkingLeg(hip,knee,ankle,contact)
}
