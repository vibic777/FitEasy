package com.fiteasy.shared

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/** One easy-jog stride in the 24-unit icon box. Screen right is forward; each foot is on the ground for a third of its cycle. */
data class JoggingLimb(val root:Offset,val joint:Offset,val end:Offset,val toe:Offset)
data class JoggingPose(val hip:Offset,val shoulder:Offset,val head:Offset,val nearLeg:JoggingLimb,val farLeg:JoggingLimb,val nearArm:JoggingLimb,val farArm:JoggingLimb,val hair:Offset)
const val JOG_CONTACT=.34f
fun joggingPose(phase:Float):JoggingPose {
    val t=((phase%1f)+1f)%1f
    // The body rises twice per stride, once after each push-off.
    val bob=-.55f*sin(PI.toFloat()*2*(t*2-.2f)).coerceAtLeast(-.4f)
    val hip=Offset(10.8f,12.2f+bob);val shoulder=hip+Offset(1.3f,-5.2f);val head=shoulder+Offset(.8f,-2.9f)
    fun leg(time:Float):JoggingLimb {
        val s=((time%1f)+1f)%1f;val ground=22f
        val ankle=if(s<JOG_CONTACT){val u=s/JOG_CONTACT;Offset(hip.x+2.4f-5f*u,ground-.7f)}
            else {val u=(s-JOG_CONTACT)/(1-JOG_CONTACT);val swing=.5f-.5f*cos(PI.toFloat()*u)
                // Heel kicks up behind, then the knee drives the foot forward and down.
                Offset(hip.x-2.6f+5f*swing,ground-.7f-5f*sin(PI.toFloat()*u).pow(.8f)*(1-.3f*u))}
        val delta=ankle-hip;val distance=delta.getDistance().coerceIn(.5f,9.99f);val mid=hip+delta*.5f
        val height=sqrt((5f*5f-distance*distance*.25f).coerceAtLeast(0f))
        val knee=mid+Offset(delta.y,-delta.x)/delta.getDistance().coerceAtLeast(.01f)*height
        val shin=(ankle-knee).let {it/it.getDistance().coerceAtLeast(.01f)}
        // Flat on the ground in contact; in the air the toe hangs along the shin's forward normal.
        val toe=if(s<JOG_CONTACT)ankle+Offset(1.9f,.25f)else ankle+Offset(shin.y,-shin.x)*1.6f+shin*1.1f
        return JoggingLimb(hip,knee,ankle,toe)
    }
    fun arm(time:Float):JoggingLimb {
        // Elbows stay bent near a right angle and swing opposite to the leg of the same side.
        val swing=-.75f*cos(PI.toFloat()*2*time);val elbow=shoulder+Offset(sin(swing),cos(swing))*2.9f
        val fore=swing+1.45f;val hand=elbow+Offset(sin(fore),cos(fore))*2.6f
        return JoggingLimb(shoulder,elbow,hand,hand)
    }
    // Tip of long loose hair streaming behind, lifting and falling with each step.
    val hair=head+Offset(-6.6f,.3f+1.1f*sin(PI.toFloat()*2*(t*2+.15f)))
    return JoggingPose(hip,shoulder,head,leg(t),leg(t+.5f),arm(t),arm(t+.5f),hair)
}
