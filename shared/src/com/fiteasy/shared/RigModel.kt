package com.fiteasy.shared

import kotlinx.serialization.Serializable
import kotlin.math.*

val rigJoints=listOf("pelvis","chest","neck","head","ls","le","lw","rs","re","rw","lh","lk","la","rh","rk","ra")
val rigBones=listOf("pelvis" to "chest","chest" to "neck","neck" to "head","chest" to "ls","ls" to "le","le" to "lw","chest" to "rs","rs" to "re","re" to "rw","pelvis" to "lh","lh" to "lk","lk" to "la","pelvis" to "rh","rh" to "rk","rk" to "ra")
val rigEquipment=setOf("dumbbell","barbell","bar","chair","wall","band","mat","hoop","stick","rope","ball","kettlebell","step","block","roller","custom","arrow","rotation","label")
@Serializable data class RigPoint(val x:Float,val y:Float,val z:Float=0f)
@Serializable data class RigPose(val points:Map<String,RigPoint>,val yaw:Float=0f,val angles:Map<String,Float> = emptyMap(),val grips:Map<String,String> = emptyMap(),val layers:Map<String,String> = emptyMap(),val torsoYaw:Float=0f,val contacts:Map<String,RigPoint> = emptyMap(),val headYaw:Float=0f,val bends:Map<String,Float> = emptyMap(),
    /** The scene tipped toward the viewer, in degrees (D119): 0 looks from the side of the room, 90 looks down from above. */
    val pitch:Float=0f)
@Serializable data class RigFrame(val id:String=newId(),val time:Float,val pose:RigPose)
@Serializable data class RigProp(val id:String=newId(),val kind:String="dumbbell",val name:String="",val point:RigPoint=RigPoint(50f,60f),val width:Float=16f,val height:Float=4f,val angle:Float=0f,val anchor:String="scene",val layer:String="auto",val spin:Float=0f,val variant:String="straight")
@Serializable data class RigClip(val version:Int=1,val duration:Float=3f,val loop:Boolean=true,val frames:List<RigFrame>,val props:List<RigProp> = emptyList(),val lockLengths:Boolean=true)
@Serializable data class ExerciseAsset(val id:String=newId(),val exercise:Exercise,val updated:Long=System.currentTimeMillis(),val deleted:Long?=null)
fun validateRig(c:RigClip){
    require(c.version==1 && c.duration.isFinite() && c.duration in .5f..30f && c.frames.size in 1..32 && c.props.size<=16)
    require(c.frames.map {it.id}.distinct().size==c.frames.size && c.props.map {it.id}.distinct().size==c.props.size)
    require(c.frames.first().time==0f && c.frames.zipWithNext().all {(a,b)->a.time<b.time})
    fun point(p:RigPoint){require(p.x.isFinite() && p.y.isFinite() && p.z.isFinite() && p.x in 0f..100f && p.y in 0f..100f && p.z in -100f..100f)}
    c.frames.forEach {f->require(f.id.isNotBlank() && f.time.isFinite() && f.time in 0f..c.duration);val p=f.pose;require(p.points.keys==rigJoints.toSet() && p.yaw.isFinite() && p.yaw in -360f..360f && p.torsoYaw.isFinite() && p.torsoYaw in -360f..360f);p.points.values.forEach(::point);require(p.contacts.keys.all {it in setOf("lw","rw","la","ra")});p.contacts.values.forEach(::point);require(p.angles.keys.all {it in setOf("lw","rw","la","ra")} && p.angles.values.all {it.isFinite() && it in -360f..360f});require(p.grips.keys.all {it in setOf("lw","rw")} && p.grips.values.all {it in setOf("open","pronated","supinated","neutral")});require(p.layers.keys.all {it in rigJoints} && p.layers.values.all {it in setOf("auto","back","front")});require(p.pitch.isFinite() && p.pitch in -90f..90f);require(p.headYaw.isFinite() && p.headYaw in -90f..90f && p.bends.keys.all {it in setOf("la","ra")} && p.bends.values.all {it.isFinite() && it in -60f..60f})}
    c.props.forEach {p->require(p.id.isNotBlank() && p.variant in setOf("straight","parallel","angled") && p.kind in rigEquipment && p.name.length<=100 && p.width.isFinite() && p.height.isFinite() && p.width in 1f..100f && p.height in 1f..100f && p.angle.isFinite() && p.angle in -360f..360f && p.spin.isFinite() && p.spin in -360f..360f && p.anchor in (rigJoints+listOf("scene","hands")) && p.layer in setOf("auto","back","front"));point(p.point)}
}
fun rigAngle(a:Float,b:Float,t:Float):Float=a+(((b-a+540f)%360f)-180f)*t
fun rigLength(a:RigPoint,b:RigPoint)=sqrt((a.x-b.x).pow(2)+(a.y-b.y).pow(2)+(a.z-b.z).pow(2))
fun rigConstrain(reference:RigPose,target:RigPose):RigPose {
    val attached=rigAttached(reference,target)
    val result=attached.points.toMutableMap()
    rigBones.forEach {(a,b)->
        if(b in setOf("lh","rh","ls","rs"))return@forEach
        val parent=result.getValue(a);val originalParent=attached.points.getValue(a);val desired=attached.points.getValue(b)
        var delta=RigPoint(desired.x-originalParent.x,desired.y-originalParent.y,desired.z-originalParent.z)
        var length=rigLength(RigPoint(0f,0f),delta)
        val oldA=reference.points.getValue(a);val oldB=reference.points.getValue(b);val fixed=rigLength(oldA,oldB)
        if(length<.001f){delta=RigPoint(oldB.x-oldA.x,oldB.y-oldA.y,oldB.z-oldA.z);length=fixed.coerceAtLeast(.001f)}
        val scale=fixed/length
        result[b]=RigPoint(parent.x+delta.x*scale,parent.y+delta.y*scale,parent.z+delta.z*scale)
    }
    target.contacts.forEach {(end,goal)->
        val (root,middle)=when(end){"lw"->"ls" to "le";"rw"->"rs" to "re";"la"->"lh" to "lk";else->"rh" to "rk"}
        val upper=end in setOf("lw","rw")
        val start=if(upper)rigWorldPoint(target.copy(points=result),root)else result.getValue(root);val l1=rigLength(reference.points.getValue(root),reference.points.getValue(middle));val l2=rigLength(reference.points.getValue(middle),reference.points.getValue(end))
        fun sub(a:RigPoint,b:RigPoint)=RigPoint(a.x-b.x,a.y-b.y,a.z-b.z)
        fun dot(a:RigPoint,b:RigPoint)=a.x*b.x+a.y*b.y+a.z*b.z
        fun scale(a:RigPoint,s:Float)=RigPoint(a.x*s,a.y*s,a.z*s)
        fun plus(a:RigPoint,b:RigPoint)=RigPoint(a.x+b.x,a.y+b.y,a.z+b.z)
        val direction=sub(goal,start);val distance=rigLength(goal,start).coerceAtLeast(.001f);val unit=scale(direction,1/distance)
        val reach=distance.coerceIn(abs(l1-l2)+.001f,(l1+l2).coerceAtLeast(abs(l1-l2)+.002f))
        val x=(l1*l1-l2*l2+reach*reach)/(2*reach);val h=sqrt((l1*l1-x*x).coerceAtLeast(0f))
        val hint=sub(if(upper)rigWorldPoint(target,middle)else target.points.getValue(middle),start);var pole=sub(hint,scale(unit,dot(hint,unit)))
        if(rigLength(pole,RigPoint(0f,0f))<.001f){val axis=if(abs(unit.z)<.9f)RigPoint(0f,0f,1f)else RigPoint(1f,0f);pole=sub(axis,scale(unit,dot(axis,unit)))}
        pole=scale(pole,1/rigLength(pole,RigPoint(0f,0f)).coerceAtLeast(.001f))
        fun local(p:RigPoint):RigPoint {if(!upper)return p;val pivot=result.getValue("pelvis");val a=-target.torsoYaw*PI.toFloat()/180;return p.copy(x=pivot.x+(p.x-pivot.x)*cos(a)+(p.z-pivot.z)*sin(a),z=pivot.z-(p.x-pivot.x)*sin(a)+(p.z-pivot.z)*cos(a))}
        result[middle]=local(plus(start,plus(scale(unit,x),scale(pole,h))))
        result[end]=local(plus(start,scale(unit,reach)))
    }
    return target.copy(points=result)
}
fun rigSample(c:RigClip,seconds:Float):RigPose {
    val t=if(c.loop)seconds.coerceAtLeast(0f)%c.duration else seconds.coerceIn(0f,c.duration)
    val first=c.frames.first();val last=c.frames.last()
    val a=c.frames.lastOrNull {it.time<=t} ?: first
    val b=c.frames.firstOrNull {it.time>t} ?: if(c.loop && last.time<c.duration)first.copy(time=c.duration)else a
    val u=if(b.time==a.time)0f else ((t-a.time)/(b.time-a.time)).coerceIn(0f,1f)
    val sampled=a.pose.copy(pitch=a.pose.pitch+(b.pose.pitch-a.pose.pitch)*u,headYaw=a.pose.headYaw+(b.pose.headYaw-a.pose.headYaw)*u,bends=setOf("la","ra").filter {it in a.pose.bends || it in b.pose.bends}.associateWith {val x=a.pose.bends[it] ?: 0f;x+((b.pose.bends[it] ?: 0f)-x)*u},yaw=rigAngle(a.pose.yaw,b.pose.yaw,u),torsoYaw=rigAngle(a.pose.torsoYaw,b.pose.torsoYaw,u),points=rigJoints.associateWith {key->val x=a.pose.points.getValue(key);val y=b.pose.points.getValue(key);RigPoint(x.x+(y.x-x.x)*u,x.y+(y.y-x.y)*u,x.z+(y.z-x.z)*u)},angles=setOf("lw","rw","la","ra").filter {it in a.pose.angles || it in b.pose.angles}.associateWith {val rest=if(it.endsWith("w"))90f else 0f;rigAngle(a.pose.angles[it] ?: rest,b.pose.angles[it] ?: rest,u)})
    return if(c.lockLengths)rigConstrain(first.pose,sampled)else rigAttached(first.pose,sampled)
}
data class RigProjection(val x:Float,val y:Float,val depth:Float)
/**
 * Where a point of the scene is on the screen. [yaw] turns the scene about the vertical through its middle; [pitch] then
 * tips it toward the viewer about the level line through its middle, so 90 shows it from above, the front of an upright
 * figure pointing down the screen (D119).
 */
fun rigProject(p:RigPoint,yaw:Float,pitch:Float=0f):RigProjection {
    val a=yaw*PI.toFloat()/180;val x=50+(p.x-50)*cos(a)+p.z*sin(a);val depth=-(p.x-50)*sin(a)+p.z*cos(a)
    if(pitch==0f)return RigProjection(x,p.y,depth)
    val b=pitch*PI.toFloat()/180;val y=p.y-50
    return RigProjection(x,50+y*cos(b)+depth*sin(b),-y*sin(b)+depth*cos(b))
}
/** What moving by ([dx], [dy]) on the screen is in the scene, for a view turned by [yaw] and tipped by [pitch]: the move stays level with the screen. */
fun rigUnproject(dx:Float,dy:Float,yaw:Float,pitch:Float):RigPoint {
    val b=pitch*PI.toFloat()/180;val y=dy*cos(b);val depth=dy*sin(b)
    val a=yaw*PI.toFloat()/180
    return RigPoint(dx*cos(a)-depth*sin(a),y,dx*sin(a)+depth*cos(a))
}
fun rigWorldPoint(pose:RigPose,key:String):RigPoint {
 val p=pose.points.getValue(key)
 if(key !in setOf("chest","neck","head","ls","le","lw","rs","re","rw"))return p
 val pivot=pose.points.getValue("pelvis");val a=pose.torsoYaw*PI.toFloat()/180
 return p.copy(x=pivot.x+(p.x-pivot.x)*cos(a)+(p.z-pivot.z)*sin(a),z=pivot.z-(p.x-pivot.x)*sin(a)+(p.z-pivot.z)*cos(a))
}
fun rigDepth(pose:RigPose,ids:List<String>):Float {val automatic=ids.map {rigProject(rigWorldPoint(pose,it),pose.yaw,pose.pitch).depth}.average().toFloat();val correction=ids.asReversed().mapNotNull {pose.layers[it]?.takeUnless {it=="auto"}}.firstOrNull();return automatic+when(correction){"back"->-300f;"front"->300f;else->0f}}
fun rigIsMoving(c:RigClip)=c.frames.map {it.pose}.distinct().size>1 || c.props.any {it.spin!=0f}
/** Drag in the projection plane; depth remains authored, other points remain unchanged. */
fun rigDrag(pose:RigPose,joint:String,x:Float,y:Float):RigPose {
    val p=pose.points.getValue(joint)
    val upper=joint in setOf("chest","neck","head","ls","le","lw","rs","re","rw")
    if(pose.pitch!=0f) {
        // A tipped view: the joint follows the pointer in the plane of the screen.
        val q=rigProject(rigWorldPoint(pose,joint),pose.yaw,pose.pitch);val move=rigUnproject(x-q.x,y-q.y,pose.yaw,pose.pitch)
        val t=(if(upper)pose.torsoYaw else 0f)*PI.toFloat()/180
        val next=RigPoint((p.x+move.x*cos(t)-move.z*sin(t)).coerceIn(0f,100f),(p.y+move.y).coerceIn(0f,100f),(p.z+move.x*sin(t)+move.z*cos(t)).coerceIn(-100f,100f))
        return pose.copy(points=pose.points+(joint to next))
    }
    val pivot=if(upper)pose.points.getValue("pelvis")else RigPoint(50f,0f)
    val origin=rigProject(pivot,pose.yaw)
    val a=(pose.yaw+if(upper)pose.torsoYaw else 0f)*PI.toFloat()/180
    // At side view x is depth; avoid division by cos(90).
    val next=if(abs(cos(a))>.5f)p.copy(x=(pivot.x+(x-origin.x-(p.z-pivot.z)*sin(a))/cos(a)).coerceIn(0f,100f),y=y.coerceIn(0f,100f))else p.copy(z=(pivot.z+(x-origin.x-(p.x-pivot.x)*cos(a))/sin(a)).coerceIn(-100f,100f),y=y.coerceIn(0f,100f))
    return pose.copy(points=pose.points+(joint to next))
}
fun rigBase(kind:String="standing"):RigPose {
    val p=linkedMapOf("pelvis" to RigPoint(50f,54f),"chest" to RigPoint(50f,30f),"neck" to RigPoint(50f,26f),"head" to RigPoint(50f,18f),"ls" to RigPoint(42f,31f),"le" to RigPoint(37f,45f),"lw" to RigPoint(35f,59f),"rs" to RigPoint(58f,31f),"re" to RigPoint(63f,45f),"rw" to RigPoint(65f,59f),"lh" to RigPoint(46f,54f),"lk" to RigPoint(44f,72f),"la" to RigPoint(43f,89f),"rh" to RigPoint(54f,54f),"rk" to RigPoint(56f,72f),"ra" to RigPoint(57f,89f))
    fun set(key:String,x:Float,y:Float,z:Float=0f){p[key]=RigPoint(x,y,z)}
    when(kind){
        "seated"->{set("lk",37f,60f,12f);set("rk",63f,60f,12f);set("la",37f,86f,12f);set("ra",63f,86f,12f)}
        "lying"->{p.keys.toList().forEach {k->val q=p.getValue(k);p[k]=RigPoint(q.x,80f,-28+(q.y-18)*.95f)}}
        "plank","pushup"->{
            val chestY=if(kind=="plank")74f else 64f
            fun bodyY(z:Float)=88f+(z-38f)*(88f-chestY)/62f
            set("head",50f,bodyY(-37f),-37f);set("neck",50f,bodyY(-29f),-29f)
            set("chest",50f,chestY,-24f);set("pelvis",50f,bodyY(0f),0f)
            listOf(-1f to "l",1f to "r").forEach {(side,prefix)->
                set(prefix+"s",50f+side*8f,chestY,-24f)
                set(prefix+"h",50f+side*4f,bodyY(0f),0f)
                set(prefix+"k",50f+side*4f,bodyY(19f),19f)
                set(prefix+"a",50f+side*4f,88f,38f)
                set(prefix+"e",50f+side*8f,if(kind=="plank")93f else (chestY+93f)/2,-24f)
                set(prefix+"w",50f+side*8f,93f,if(kind=="plank")-38f else -24f)
            }
        }
        "hanging"->{
            set("head",50f,35f);set("neck",50f,43f);set("chest",50f,47f);set("pelvis",50f,67f)
            listOf(-1f to "l",1f to "r").forEach {(side,prefix)->
                set(prefix+"s",50f+side*8f,47f);set(prefix+"e",50f+side*11.5f,32f);set(prefix+"w",50f+side*15f,17f)
                set(prefix+"h",50f+side*4f,67f);set(prefix+"k",50f+side*5f,80.5f);set(prefix+"a",50f+side*6f,94f)
            }
        }
    }
    return RigPose(p,yaw=if(kind in setOf("lying","plank","pushup"))90f else 0f,
        angles=if(kind in setOf("plank","pushup"))mapOf("lw" to 180f,"rw" to 180f,"la" to 90f,"ra" to 90f)else if(kind=="hanging")mapOf("lw" to -90f,"rw" to -90f)else emptyMap(),
        grips=mapOf("lw" to "pronated","rw" to "pronated"))
}
/** Hip to knee of the standing figure: how far the hips travel between sitting and standing. */
private const val RIG_THIGH=18f
fun rigTemplate(key:String):RigClip {
    val base=rigBase(if(key=="plank")"plank"else if(key=="pullup")"hanging"else "standing").let {when(key){
        "heelraise","toeraise"->it.copy(yaw=90f,angles=mapOf("la" to 0f,"ra" to 0f))
        // Seen a little from the side, so the lifted knee is read as a knee.
        "march"->it.copy(yaw=60f)
        // From the side, standing a thigh's length ahead of the chair: the hips come forward over the feet on the way up.
        "sitstand","kneeext"->it.copy(yaw=90f,points=it.points.mapValues {(_,v)->v.copy(z=v.z+RIG_THIGH)})
        else->it
    }}
    var a=base;var b=base
    // More than two poses in a turn: left and right, there and back (D126).
    var cycle:List<RigPose>?=null
    fun pose(p:RigPose,vararg changes:Pair<String,RigPoint>)=p.copy(points=p.points+changes)
    // Sitting on a chair: everything above the knees comes down and back by the length of the thigh, the feet stay where they stand.
    fun seated(p:RigPose)=p.copy(points=p.points.mapValues {(k,v)->if(k in setOf("la","ra","lk","rk"))v else v.copy(y=v.y+RIG_THIGH,z=v.z-RIG_THIGH)})
    fun pressPose(high:RigPose,progress:Float):RigPose {
        val turn=-.249f*progress;val cosine=cos(turn);val sine=sin(turn)
        return high.copy(points=high.points.mapValues {(k,v)->when(k){
            "lw","rw"->v
            "le","re"->v.copy(y=v.y+(84f-v.y)*progress,z=v.z+14f*progress)
            else->{val y=v.y-88f;val z=v.z-38f;v.copy(y=88f+z*sine+y*cosine,z=38f+z*cosine-y*sine)}
        }})
    }
    val props=when(key){"curl","raise"->listOf(RigProp(anchor="lw"),RigProp(anchor="rw"));"pullup"->listOf(RigProp(kind="bar",point=RigPoint(50f,17f-RIG_PALM),width=60f));"heelraise","toeraise"->listOf(RigProp(kind="mat",point=RigPoint(50f,93f),width=60f,height=1f));"plank","pushup"->listOf(RigProp(kind="mat",point=RigPoint(50f,95.5f),width=90f,height=1f))
        // A chair beside the standing person, its back under the hand.
        "sidelegraise"->listOf(RigProp(kind="chair",point=RigPoint(71f,73f),width=12f,height=14f))
        "sitstand","kneeext"->listOf(RigProp(kind="chair",point=RigPoint(50f,74f,-1f),width=14f,height=15f))
        else->emptyList()}
    when(key){
        "pushup"->{
            a=rigBase("pushup")
            b=pressPose(a,1f)
        }
        // Heels rise over the balls of the feet; the toes stay flat on the floor.
        "heelraise"->{b=base.copy(points=base.points.mapValues {(_,v)->v.copy(y=v.y-2.8f)},angles=mapOf("la" to 40f,"ra" to 40f),bends=mapOf("la" to 40f,"ra" to 40f))}
        "toeraise"->{b=base.copy(angles=mapOf("la" to -24f,"ra" to -24f))}
        "squat"->{b=base.copy(points=base.points.mapValues {(k,v)->when(k){"lk"->v.copy(x=36f,y=72f,z=15f);"rk"->v.copy(x=64f,y=72f,z=15f);"la","ra"->v;else->v.copy(y=v.y+12)}})}
        "raise"->{b=pose(base,"le" to RigPoint(26f,32f),"lw" to RigPoint(10f,32f),"re" to RigPoint(74f,32f),"rw" to RigPoint(90f,32f))}
        "curl"->{b=pose(base,"lw" to RigPoint(39f,31f,8f),"rw" to RigPoint(61f,31f,8f))}
        "pullup"->{b=base.copy(points=base.points.mapValues {(k,v)->when(k){"lw","rw"->v;"le"->RigPoint(28f,36f);"re"->RigPoint(72f,36f);else->v.copy(y=v.y-27f)}})}
        // The gentle seven (D126). Limbs keep their lengths: each point is its joint moved along a direction.
        // Forward first, then up: the arms pass in front of the body, as the description says.
        "armsup"->{
            val forward=pose(base,"le" to RigPoint(42f,31f,14.87f),"lw" to RigPoint(42f,31f,29.01f),"re" to RigPoint(58f,31f,14.87f),"rw" to RigPoint(58f,31f,29.01f))
            val up=pose(base,"le" to RigPoint(36.35f,17.25f),"lw" to RigPoint(30.98f,4.17f),"re" to RigPoint(63.65f,17.25f),"rw" to RigPoint(69.02f,4.17f))
            cycle=listOf(base,forward,up,forward)
        }
        "twist"->{cycle=listOf(base,base.copy(torsoYaw=40f),base,base.copy(torsoYaw=-40f))}
        "march"->{cycle=listOf(base,pose(base,"lk" to RigPoint(45f,58f,17.65f),"la" to RigPoint(45f,75.03f,17.65f)),base,pose(base,"rk" to RigPoint(55f,58f,17.65f),"ra" to RigPoint(55f,75.03f,17.65f)))}
        "sidelegraise"->{b=pose(base,"lk" to RigPoint(36.94f,69.68f),"la" to RigPoint(28.43f,84.43f))}
        "necktilt"->{cycle=listOf(base,pose(base,"head" to RigPoint(53.38f,18.75f)),base,pose(base,"head" to RigPoint(46.62f,18.75f)))}
        "sitstand"->{a=seated(base);b=base}
        "kneeext"->{a=seated(base);b=pose(a,"la" to RigPoint(43f,75f,RIG_THIGH+16.74f))}
    }
    val pinned=when(key){"pullup"->setOf("lw","rw");"pushup"->setOf("lw","rw","la","ra");"squat","sitstand"->setOf("la","ra");else->emptySet()}
    val contacts=pinned.associateWith {a.points.getValue(it)};a=a.copy(contacts=contacts);b=b.copy(contacts=contacts)
    val turn=cycle
    val frames=when {
        turn!=null->(turn+turn.first()).mapIndexed {i,p->RigFrame(time=i*3f/turn.size,pose=p)}
        else->when(key){
        "plank"->listOf(RigFrame(time=0f,pose=a))
        "pushup"->(0..24).map {step->RigFrame(time=step/8f,pose=pressPose(a,((1-cos(step*PI/12))/2).toFloat()))}
        else->listOf(RigFrame(time=0f,pose=a),RigFrame(time=1.5f,pose=b),RigFrame(time=3f,pose=a))
        }
    }
    return RigClip(frames=frames,props=props)
}
