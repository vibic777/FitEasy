package com.fiteasy.shared

import kotlin.math.*

/**
 * The torso's own directions in a pose (D119): across — toward the right shoulder, up — from the pelvis to the chest, and
 * forward. An upright figure gives the scene's own axes; a leaning or lying one carries them with it.
 */
internal fun rigTorsoFrame(points:Map<String,RigPoint>):Triple<RigPoint,RigPoint,RigPoint> {
    val chest=points.getValue("chest");val pelvis=points.getValue("pelvis")
    var ux=chest.x-pelvis.x;var uy=chest.y-pelvis.y;var uz=chest.z-pelvis.z
    val ul=sqrt(ux*ux+uy*uy+uz*uz)
    if(ul<.001f){ux=0f;uy=-1f;uz=0f}else {ux/=ul;uy/=ul;uz/=ul}
    // Across is the scene's left-to-right made square to the spine; for a spine that lies along it, the side that is on top.
    var ax=1f-ux*ux;var ay=-ux*uy;var az=-ux*uz
    if(sqrt(ax*ax+ay*ay+az*az)<.05f){val sy=if(ux>0)1f else -1f;val along=sy*uy;ax=-along*ux;ay=sy-along*uy;az=-along*uz}
    val al=sqrt(ax*ax+ay*ay+az*az).coerceAtLeast(.001f);ax/=al;ay/=al;az/=al
    return Triple(RigPoint(ax,ay,az),RigPoint(ux,uy,uz),RigPoint(uy*az-uz*ay,uz*ax-ux*az,ux*ay-uy*ax))
}
/**
 * Structural attachment points cannot be independently detached in pose editing. The hips sit beside the pelvis. The
 * shoulders sit on the ribcage and turn with it (D119): their place is kept in the torso's own directions, so when the
 * torso leans or lies down the shoulder line leans with it instead of staying level and leaving the body.
 */
fun rigAttached(reference:RigPose,target:RigPose):RigPose {
    val points=target.points.toMutableMap()
    listOf(Triple("pelvis","lh",-1),Triple("pelvis","rh",1)).forEach {(root,key,side)->
        val source=reference.points.getValue(key);val origin=reference.points.getValue(root);val parent=points.getValue(root)
        val width=abs(source.x-origin.x).coerceIn(3f,8f)
        points[key]=RigPoint(parent.x+side*width,parent.y+(source.y-origin.y).coerceIn(-2f,2f),parent.z+(source.z-origin.z).coerceIn(-4f,4f))
    }
    val (a0,u0,n0)=rigTorsoFrame(reference.points);val (a,u,n)=rigTorsoFrame(points)
    val origin=reference.points.getValue("chest");val parent=points.getValue("chest")
    listOf("ls" to -1,"rs" to 1).forEach {(key,side)->
        val source=reference.points.getValue(key);val ox=source.x-origin.x;val oy=source.y-origin.y;val oz=source.z-origin.z
        val width=abs(ox*a0.x+oy*a0.y+oz*a0.z).coerceIn(4f,12f)*side
        val along=(ox*u0.x+oy*u0.y+oz*u0.z).coerceIn(-2f,2f);val depth=(ox*n0.x+oy*n0.y+oz*n0.z).coerceIn(-4f,4f)
        points[key]=RigPoint((parent.x+a.x*width+u.x*along+n.x*depth).coerceIn(0f,100f),(parent.y+a.y*width+u.y*along+n.y*depth).coerceIn(0f,100f),(parent.z+a.z*width+u.z*along+n.z*depth).coerceIn(-100f,100f))
    }
    return target.copy(points=points)
}
fun rigPropProjection(prop:RigProp,pose:RigPose):RigProjection {
    if(prop.anchor=="scene")return rigProject(prop.point,pose.yaw,pose.pitch)
    val points=rigProjected(pose)
    // Held objects sit in the palm, not on the wrist joint.
    fun palm(key:String):RigProjection {val w=points.getValue(key);val a=rigTipAngle(pose,key,points) ?: (PI.toFloat()/2);return RigProjection(w.x+cos(a)*RIG_PALM,w.y+sin(a)*RIG_PALM,w.depth)}
    val anchor=when(prop.anchor){"hands"->{val a=palm("lw");val b=palm("rw");RigProjection((a.x+b.x)/2,(a.y+b.y)/2,(a.depth+b.depth)/2)};"lw","rw"->palm(prop.anchor);else->points.getValue(prop.anchor)}
    // The offset from the anchor is a direction, turned and tipped with the scene.
    val offset=rigProject(RigPoint(prop.point.x,prop.point.y-10,prop.point.z),pose.yaw,pose.pitch)
    return RigProjection(anchor.x+offset.x-50,anchor.y+offset.y-50,anchor.depth+offset.depth)
}
data class RigSelection(val joint:String?=null,val propId:String?=null,val tip:String?=null)
fun rigPick(c:RigClip,pose:RigPose,x:Float,y:Float,objects:Boolean,radius:Float=6f,preferredPropId:String?=null):RigSelection? {
    if(!objects){
        // Hand and foot direction handles sit beyond the joints and win when the pointer is on them.
        val points=rigProjected(pose)
        listOf("lw","rw","la","ra").minBy {val t=rigTip(pose,it,points);hypot(t.first-x,t.second-y)}.let {key->val t=rigTip(pose,key,points);if(hypot(t.first-x,t.second-y)<=radius*.55f)return RigSelection(joint=key,tip=key)}
        val key=rigJoints.minBy {val p=rigProject(rigWorldPoint(pose,it),pose.yaw,pose.pitch);hypot(p.x-x,p.y-y)};val p=rigProject(rigWorldPoint(pose,key),pose.yaw,pose.pitch);return RigSelection(joint=key).takeIf {hypot(p.x-x,p.y-y)<=radius}}
    return c.props.sortedWith(compareByDescending<RigProp> {if(it.id==preferredPropId)1 else 0}.thenByDescending {rigPropProjection(it,pose).depth+when(it.layer){"front"->300;"back"->-300;else->0}}.thenByDescending {it.id}).firstOrNull {prop->
        val p=rigPropProjection(prop,pose);val l=rigProject(rigWorldPoint(pose,"lw"),pose.yaw,pose.pitch);val r=rigProject(rigWorldPoint(pose,"rw"),pose.yaw,pose.pitch)
        val angle=if(prop.anchor=="hands")atan2(r.y-l.y,r.x-l.x)else prop.angle*PI.toFloat()/180
        val width=if(prop.anchor=="hands")hypot(r.x-l.x,r.y-l.y).coerceAtLeast(4f)else prop.width
        val a=-angle;val dx=x-p.x;val dy=y-p.y
        abs(dx*cos(a)-dy*sin(a))<=width/2+radius/2 && abs(dx*sin(a)+dy*cos(a))<=prop.height+radius/2
    }?.let {RigSelection(propId=it.id)}
}
/** Translate an object in projected coordinates; maintain its anchor, size and authored depth. */
fun rigDragProp(prop:RigProp,pose:RigPose,dx:Float,dy:Float):RigProp {
    val a=pose.yaw*PI.toFloat()/180;val point=prop.point
    if(pose.pitch!=0f){val move=rigUnproject(dx,dy,pose.yaw,pose.pitch);return prop.copy(point=RigPoint((point.x+move.x).coerceIn(0f,100f),(point.y+move.y).coerceIn(0f,100f),(point.z+move.z).coerceIn(-100f,100f)))}
    val next=if(abs(cos(a))>.5f)point.copy(x=(point.x+dx/cos(a)).coerceIn(0f,100f),y=(point.y+dy).coerceIn(0f,100f))else point.copy(z=(point.z+dx/sin(a)).coerceIn(-100f,100f),y=(point.y+dy).coerceIn(0f,100f))
    return prop.copy(point=next)
}
fun rigEditJoint(reference:RigPose,pose:RigPose,joint:String,point:RigPoint,lockLengths:Boolean=true):RigPose {
    val parent=when(joint){"lh","rh"->"pelvis";"ls","rs"->"chest";else->joint}
    val old=pose.points.getValue(joint)
    val descendants=mutableSetOf(parent)
    rigBones.forEach {(a,b)->if(a in descendants)descendants+=b}
    val group=descendants.map {pose.points.getValue(it)}
    val delta=RigPoint((point.x-old.x).coerceIn(-group.minOf {it.x},100f-group.maxOf {it.x}),(point.y-old.y).coerceIn(-group.minOf {it.y},100f-group.maxOf {it.y}),(point.z-old.z).coerceIn(-100f-group.minOf {it.z},100f-group.maxOf {it.z}))
    val points=pose.points.mapValues {(key,p)->if(key in descendants)p.copy(x=p.x+delta.x,y=p.y+delta.y,z=p.z+delta.z)else p}
    val attached=rigAttached(reference,pose.copy(points=points))
    return if(lockLengths)rigConstrain(reference,attached)else attached
}
/** Mirror image of the whole pose: sides swap names so the shoulders and hips keep their fixed order. */
fun rigMirror(pose:RigPose):RigPose {
    fun other(key:String)=when {key.length==2 && key[0]=='l'->"r"+key[1];key.length==2 && key[0]=='r'->"l"+key[1];else->key}
    fun flip(p:RigPoint)=p.copy(x=100f-p.x)
    return pose.copy(points=rigJoints.associateWith {flip(pose.points.getValue(other(it)))},yaw=-pose.yaw,torsoYaw=-pose.torsoYaw,headYaw=-pose.headYaw,bends=pose.bends.mapKeys {other(it.key)},
        angles=pose.angles.entries.associate {other(it.key) to rigAngle(0f,180f-it.value,1f)},grips=pose.grips.mapKeys {other(it.key)},
        layers=pose.layers.mapKeys {other(it.key)},contacts=pose.contacts.entries.associate {other(it.key) to flip(it.value)})
}
/** Distance from the wrist to the middle of the palm, in scene units. */
const val RIG_PALM=2.4f
fun rigProjected(pose:RigPose)=rigJoints.associateWith {rigProject(rigWorldPoint(pose,it),pose.yaw,pose.pitch)}
/**
 * Unit vector the body faces, in projected space (x, y, depth toward the viewer): torso axis × shoulder line.
 * The shoulder order is fixed, so a horizontal body is prone or supine alike; elbows, hands and knees decide.
 */
fun rigFacing(points:Map<String,RigProjection>):FloatArray {
    val chest=points.getValue("chest");val pelvis=points.getValue("pelvis");val ls=points.getValue("ls");val rs=points.getValue("rs")
    val ux=chest.x-pelvis.x;val uy=chest.y-pelvis.y;val uz=chest.depth-pelvis.depth
    val ax=rs.x-ls.x;val ay=rs.y-ls.y;val az=rs.depth-ls.depth
    var fx=uy*az-uz*ay;var fy=uz*ax-ux*az;var fz=ux*ay-uy*ax
    val fl=sqrt(fx*fx+fy*fy+fz*fz);if(fl<.001f)return floatArrayOf(0f,0f,1f)
    fx/=fl;fy/=fl;fz/=fl
    if(abs(uy)<.6f*sqrt(ux*ux+uy*uy+uz*uz)){
        var reach=0f
        listOf("l","r").forEach {side->
            val s=points.getValue(side+"s");val h=points.getValue(side+"h")
            listOf(side+"e",side+"w").forEach {key->val q=points.getValue(key);reach+=(q.x-s.x)*fx+(q.y-s.y)*fy+(q.depth-s.depth)*fz}
            val k=points.getValue(side+"k");reach+=2f*((k.x-h.x)*fx+(k.y-h.y)*fy+(k.depth-h.depth)*fz)
        }
        if(reach< -8f){fx=-fx;fy=-fy;fz=-fz}
    }
    return floatArrayOf(fx,fy,fz)
}
/**
 * Screen direction of a hand or foot in radians. Authored angles win; otherwise a hand continues its forearm and a foot
 * points the way the body faces. Null: the foot is seen end-on (body facing the viewer or turned away).
 */
fun rigTipAngle(pose:RigPose,key:String,points:Map<String,RigProjection> = rigProjected(pose)):Float? {
    pose.angles[key]?.let {return it*PI.toFloat()/180}
    if(key.endsWith("w")){val e=points.getValue(key.take(1)+"e");val w=points.getValue(key);val dx=w.x-e.x;val dy=w.y-e.y;return if(hypot(dx,dy)<1.5f)PI.toFloat()/2 else atan2(dy,dx)}
    val f=rigFacing(points);return if(abs(f[2])>.65f)null else if(f[0]>=0)0f else PI.toFloat()
}
/** Where the direction handle of a hand or foot is drawn and picked, in scene units. */
fun rigTip(pose:RigPose,key:String,points:Map<String,RigProjection> = rigProjected(pose)):Pair<Float,Float> {
    val q=points.getValue(key);val angle=rigTipAngle(pose,key,points)
    val reach=if(key.endsWith("w"))6.2f else if(angle==null)5.2f else 7.6f;val a=angle ?: (PI.toFloat()/2)
    return q.x+cos(a)*reach to q.y+sin(a)*reach
}
/** Turn a hand or foot toward a point of the scene. */
fun rigAim(pose:RigPose,key:String,x:Float,y:Float):RigPose {
    val q=rigProject(rigWorldPoint(pose,key),pose.yaw,pose.pitch);if(hypot(x-q.x,y-q.y)<.5f)return pose
    return pose.copy(angles=pose.angles+(key to atan2(y-q.y,x-q.x)*180f/PI.toFloat()))
}
