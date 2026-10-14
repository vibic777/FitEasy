package com.fiteasy.shared

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText

/** Torso cross-section along chest→pelvis: half-width seen from the front, and front/back offsets seen from the side. */
private class RigStation(val t:Float,val w:Float,val front:Float,val back:Float)
/** Adult athlete proportions (D081). Limb radii: root, muscle belly, joint, lower belly, end. Illustration only, never a body standard. */
private class RigBuild(val female:Boolean) {
    val arm=if(female)floatArrayOf(2f,1.9f,1.45f,1.6f,1f)else floatArrayOf(3.1f,3.1f,2f,2.3f,1.4f)
    val leg=if(female)floatArrayOf(4f,3.7f,2.3f,2.5f,1.3f)else floatArrayOf(4.3f,4.1f,2.7f,3f,1.6f)
    val shoulder=if(female).86f else 1.04f
    val neck=if(female)2.5f else 3.6f
    val thighCloth=if(female).34f else .5f
    /** Seat volume behind the hip joint, visible as the figure turns side-on: radius across, along the thigh, and how far back. */
    val seat=if(female)floatArrayOf(4f,5f,2.1f)else floatArrayOf(3.9f,4.4f,1.2f)
    val torso=if(female)listOf(
        RigStation(-.2f,1.4f,1.3f,1.5f),RigStation(-.09f,4.6f,1.9f,2.2f),RigStation(.02f,7.9f,2.7f,2.9f),RigStation(.14f,6.6f,4.8f,3.7f),
        RigStation(.26f,6.3f,6.5f,3.8f),RigStation(.4f,5.2f,4.3f,3.3f),RigStation(.62f,4.1f,3.3f,2.3f),RigStation(.82f,6f,3.5f,3.4f),
        RigStation(1f,7.5f,3.6f,4.9f),RigStation(1.12f,7.2f,3.2f,4.4f))
    else listOf(
        RigStation(-.2f,2f,1.8f,2f),RigStation(-.11f,6.4f,2.4f,3f),RigStation(.01f,9.8f,3.2f,3.6f),RigStation(.14f,8.6f,5.2f,4.4f),
        RigStation(.28f,8f,5.7f,4.4f),RigStation(.5f,6.5f,4.2f,3.8f),RigStation(.74f,5.6f,3.4f,3f),RigStation(.9f,5.9f,3.4f,3.9f),
        RigStation(1f,6.2f,3.6f,5.3f),RigStation(1.12f,6f,3.2f,5.1f))
}
private fun rigShade(c:Color,amount:Float=.09f)=Color(c.red*(1-amount),c.green*(1-amount),c.blue*(1-amount),c.alpha)
private fun rigSmooth(points:List<Offset>)=Path().apply {
    val n=points.size;fun mid(i:Int)=(points[i%n]+points[(i+1)%n])/2f
    val start=mid(n-1);moveTo(start.x,start.y)
    for(i in 0 until n){val p=points[i];val q=mid(i);quadraticTo(p.x,p.y,q.x,q.y)}
    close()
}
/** Tapered muscle segment whose outline passes through the belly radius; returns its outline for clothing. */
private fun DrawScope.rigLimb(color:Color,a:Offset,b:Offset,ra:Float,rm:Float,rb:Float,at:Float=.38f):Path {
    val axis=b-a;val l=axis.getDistance().coerceAtLeast(.01f);val d=axis/l;val n=Offset(-d.y,d.x);val m=a+axis*at
    fun side(sign:Float,path:Path,reverse:Boolean){
        val s=a+n*(ra*sign);val e=b+n*(rb*sign);val through=m+n*(rm*sign);val control=through*2f-(s+e)/2f
        if(reverse){path.lineTo(e.x,e.y);path.quadraticTo(control.x,control.y,s.x,s.y)}else {path.moveTo(s.x,s.y);path.quadraticTo(control.x,control.y,e.x,e.y)}
    }
    val path=Path().apply {side(1f,this,false);side(-1f,this,true);close()}
    drawPath(path,color);drawCircle(color,ra,a);drawCircle(color,rb,b)
    return path
}

/** Shared orthographic painter: depth is recomputed after both yaw rotations, never a fixed left/right order. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRig(c:RigClip,pose:RigPose,seconds:Float,female:Boolean?,color:Color,handles:Boolean=false,selected:String="",labels:Map<String,TextLayoutResult> = emptyMap()){
    val unit=size.width/100f
    val points=rigJoints.associateWith {rigProject(rigWorldPoint(pose,it),pose.yaw,pose.pitch)}
    fun p(q:RigProjection)=Offset(q.x*unit,q.y*size.height/100)
    fun joint(key:String)=p(points.getValue(key))
    val actions=mutableListOf<Triple<Float,String,()->Unit>>()
    fun add(depth:Float,id:String,draw:()->Unit){actions.add(Triple(depth,id,draw))}
    // Facing comes from the body itself (up × across), so it stays right for lying, turned and twisted poses.
    val (fx,fy,fz)=rigFacing(points)
    val facing=fz;val frontal=abs(facing)
    val flat=hypot(fx,fy);val nose=if(flat<.05f)Offset(1f,0f)else Offset(fx/flat,fy/flat)
    val top=joint("chest");val bottom=joint("pelvis");val axis=bottom-top;val axisLength=axis.getDistance().coerceAtLeast(.01f);val normal=Offset(-axis.y/axisLength,axis.x/axisLength)
    val frontPlus=nose.x*normal.x+nose.y*normal.y>=0
    fun pt(t:Float,w:Float)=top+axis*t+normal*(w*unit)
    /** Sideways offset toward the body's front, fading out as the figure turns to face the viewer. */
    fun forward(amount:Float)=(if(frontPlus)amount else -amount)*(1-frontal)
    val torsoDepth=rigDepth(pose,listOf("pelvis","chest"))
    val headCentre=joint("head");val neckPoint=joint("neck");val headDirection=headCentre-neckPoint
    val headAngle=atan2(headDirection.x,-headDirection.y)*180f/PI.toFloat()
    val headRadians=headAngle*PI.toFloat()/180
    // Nose direction in the rotated head frame: +1 is the frame's own right.
    // The head may turn on the neck: rotate the facing about the neck–head axis.
    val headP=points.getValue("head");val neckP=points.getValue("neck")
    var hx=headP.x-neckP.x;var hy=headP.y-neckP.y;var hz=headP.depth-neckP.depth
    val hl=sqrt(hx*hx+hy*hy+hz*hz).coerceAtLeast(.001f);hx/=hl;hy/=hl;hz/=hl
    val turn=pose.headYaw*PI.toFloat()/180;val tc=cos(turn);val ts=sin(turn);val along=(hx*fx+hy*fy+hz*fz)*(1-tc)
    val gx=fx*tc+(hy*fz-hz*fy)*ts+hx*along;val gy=fy*tc+(hz*fx-hx*fz)*ts+hy*along;val gz=fz*tc+(hx*fy-hy*fx)*ts+hz*along
    // The head or the trunk points at the viewer or away: seen from above or from below (D119).
    val headEndOn=abs(hz)>.82f
    val trunkEndOn=points.getValue("chest").let {c->val q=points.getValue("pelvis");val d=c.depth-q.depth;abs(d)/sqrt((c.x-q.x)*(c.x-q.x)+(c.y-q.y)*(c.y-q.y)+d*d).coerceAtLeast(.001f)>.82f}
    val headFacing=gz;val headFrontal=abs(gz);val headFlat=hypot(gx,gy);val headNose=if(headFlat<.05f)Offset(1f,0f)else Offset(gx/headFlat,gy/headFlat)
    val noseSide=if(headNose.x*cos(headRadians)+headNose.y*sin(headRadians)>=0)1f else -1f
    fun tipAngle(key:String):Float? = rigTipAngle(pose,key,points)
    fun inHead(block:DrawScope.()->Unit)=withTransform({rotate(headAngle,headCentre);translate(headCentre.x,headCentre.y);scale(unit,unit,pivot=Offset.Zero)},block)

    if(female==null){
        val bone=Color(0xFFF1EADB);val edge=color
        fun segment(a:Offset,b:Offset,w:Float,knob:Float=1.45f){
            drawLine(edge,a,b,(w+.7f)*unit,StrokeCap.Round);drawCircle(edge,(w*knob/2+.35f)*unit,a);drawCircle(edge,(w*knob/2+.35f)*unit,b)
            drawLine(bone,a,b,w*unit,StrokeCap.Round);drawCircle(bone,w*knob/2*unit,a);drawCircle(bone,w*knob/2*unit,b)
        }
        fun pair(a:Offset,b:Offset,w:Float,gap:Float){
            val d=(b-a).let {it/it.getDistance().coerceAtLeast(.01f)};val n=Offset(-d.y,d.x)*(gap*unit)
            segment(a+n,b+n*.4f,w,1.2f);segment(a-n,b-n*.4f,w*.8f,1.2f)
        }
        rigBones.forEach {(a,b)->
            if(b in setOf("chest","neck","head","ls","rs","lh","rh"))return@forEach
            add(rigDepth(pose,listOf(a,b)),b){when(b){
                "le","re"->segment(joint(a),joint(b),1.5f)
                "lw","rw"->pair(joint(a),joint(b),.85f,.55f)
                "lk","rk"->segment(joint(a),joint(b),1.9f,1.6f)
                else->pair(joint(a),joint(b),1.25f,.75f)
            }}
        }
        add(torsoDepth+.01f,"body"){
            val wide=.5f+.5f*frontal
            fun stroke(path:Path,w:Float){drawPath(path,edge,style=Stroke((w+.6f)*unit,cap=StrokeCap.Round));drawPath(path,bone,style=Stroke(w*unit,cap=StrokeCap.Round))}
            // No part hangs in the air (D119): the neck reaches the spine and each thigh its side of the pelvis, whatever
            // the pose. In an ordinary pose these lie under the wings and the vertebrae.
            listOf("lh","rh").forEach {segment(pt(1.02f,forward(.6f)),joint(it),1f,1.2f)}
            segment(pt(-.12f,forward(-2.8f)),joint("neck"),1f,1.1f)
            // Pelvis: two iliac wings joined at the sacrum, with the hip sockets at the thigh roots.
            listOf(-1f,1f).forEach {sign->
                val wing=Path().apply {
                    val s=pt(.84f,forward(-1.5f));moveTo(s.x,s.y)
                    var a=pt(.78f,sign*3.5f*wide);var b=pt(.82f,sign*7.4f*wide);var e=pt(.93f,sign*6.9f*wide);cubicTo(a.x,a.y,b.x,b.y,e.x,e.y)
                    a=pt(1.03f,sign*6.2f*wide);b=pt(1.07f,sign*4.2f*wide);e=pt(1.13f,sign*2.4f*wide+forward(1f));cubicTo(a.x,a.y,b.x,b.y,e.x,e.y)
                    e=pt(1.07f,forward(1.6f));lineTo(e.x,e.y);close()
                }
                drawPath(wing,bone);drawPath(wing,edge,style=Stroke(.5f*unit))
                drawCircle(edge,.9f*unit,pt(1.05f,sign*2.6f*wide+forward(.6f)))
            }
            listOf("lh","rh").forEach {drawCircle(edge,1.5f*unit,joint(it));drawCircle(bone,1.1f*unit,joint(it))}
            // Twelve rib pairs sweep from the spine round to the sternum.
            if(facing>-.5f)stroke(Path().apply {val a=pt(.03f,forward(4.2f));val b=pt(.47f,forward(4.8f));moveTo(a.x,a.y);lineTo(b.x,b.y)},1.2f)
            val ribs=floatArrayOf(4.6f,5.8f,6.6f,7f,7.1f,6.9f,6.3f,5.4f)
            ribs.forEachIndexed {i,w->val t=.04f+i*.065f;listOf(-1f,1f).forEach {sign->
                stroke(Path().apply {
                    val s=pt(t,forward(-2.8f));val o=pt(t+.05f,sign*w*frontal+forward(1.2f));val e=pt(t+.11f+i*.012f,sign*(if(i<6)1f else 3.2f)*frontal+forward(if(i<6)4.4f else 3.2f))
                    moveTo(s.x,s.y);quadraticTo(o.x,o.y,e.x,e.y)
                },.55f)
            }}
            // Vertebral column with its natural side-view curve.
            (0..16).forEach {i->val t=-.14f+i*.066f;val curve=forward(-2.8f+1.3f*sin((t-.1f)*PI.toFloat()*1.4f));val w=1f+i*.07f
                val a=pt(t,curve-w);val b=pt(t,curve+w);drawLine(edge,a,b,1.5f*unit,StrokeCap.Round);drawLine(bone,a,b,.95f*unit,StrokeCap.Round)}
            listOf("ls","rs").forEach {key->segment(pt(.02f,forward(3.6f)+if(key=="ls")-.9f*frontal else .9f*frontal),joint(key),.8f,1.5f)}
        }
        add(rigDepth(pose,listOf("neck","head")),"head"){segment(joint("neck"),headCentre,1f,1.1f);if(headEndOn){
            // The skull from above or below: its dome, the seam along it and where the face is.
            drawCircle(edge,4.05f*unit,headCentre);drawCircle(bone,3.8f*unit,headCentre)
            drawLine(edge,headCentre-headNose*(3.2f*unit),headCentre+headNose*(2.2f*unit),.3f*unit)
            drawCircle(edge,.75f*unit,headCentre+headNose*(3.9f*unit));drawCircle(bone,.5f*unit,headCentre+headNose*(3.9f*unit))
        }else inHead {
            val socket=edge
            if(headFrontal>.65f){
                val skull=Path().apply {moveTo(-3.7f,-1f);cubicTo(-3.9f,-6.6f,3.9f,-6.6f,3.7f,-1f);cubicTo(3.6f,1.2f,2.9f,1.9f,2.6f,2.6f);lineTo(2.2f,4.6f);quadraticTo(0f,5.6f,-2.2f,4.6f);lineTo(-2.6f,2.6f);cubicTo(-2.9f,1.9f,-3.6f,1.2f,-3.7f,-1f);close()}
                drawPath(skull,bone);drawPath(skull,edge,style=Stroke(.5f))
                if(headFacing>0){
                    listOf(-1f,1f).forEach {sign->drawOval(socket,Offset(sign*1.55f-1.05f,-.9f),Size(2.1f,1.9f))}
                    drawPath(Path().apply {moveTo(0f,.9f);lineTo(.75f,2.3f);lineTo(-.75f,2.3f);close()},socket)
                    drawLine(socket,Offset(-1.9f,3.4f),Offset(1.9f,3.4f),.3f);listOf(-1.2f,-.4f,.4f,1.2f).forEach {drawLine(socket,Offset(it,2.9f),Offset(it,3.9f),.25f)}
                }else drawLine(edge,Offset(0f,-5.4f),Offset(0f,-.5f),.3f)
            }else withTransform({scale(noseSide,1f,pivot=Offset.Zero)}){
                val skull=Path().apply {moveTo(-3.9f,-.6f);cubicTo(-4.3f,-6.6f,3.6f,-6.8f,3.6f,-1.6f);lineTo(3.3f,.2f);lineTo(3.9f,1.6f);lineTo(3.3f,2f);lineTo(3.4f,3.2f);lineTo(3.1f,4.6f);quadraticTo(2.2f,5.4f,1f,4.9f);lineTo(-.6f,2.6f);quadraticTo(-3.6f,2.6f,-3.9f,-.6f);close()}
                drawPath(skull,bone);drawPath(skull,edge,style=Stroke(.5f))
                drawOval(socket,Offset(1.2f,-1.1f),Size(1.7f,1.9f));drawPath(Path().apply {moveTo(3.3f,.6f);lineTo(3.7f,1.7f);lineTo(2.9f,1.8f);close()},socket)
                drawLine(socket,Offset(1.2f,3.3f),Offset(3.3f,3.2f),.3f);listOf(1.8f,2.4f,3f).forEach {drawLine(socket,Offset(it,2.8f),Offset(it,3.8f),.25f)}
                drawLine(edge,Offset(-.6f,2.6f),Offset(1f,2.4f),.3f)
            }
        }}
        listOf("lw","rw","la","ra").forEach {key->add(rigDepth(pose,listOf(key)),key+"tip"){
            val q=joint(key);val angle=tipAngle(key)
            fun DrawScope.piece(a:Offset,b:Offset,w:Float){drawLine(edge,a,b,w+.42f,StrokeCap.Round);drawLine(bone,a,b,w,StrokeCap.Round)}
            fun DrawScope.knob(at:Offset,r:Float){drawCircle(edge,r+.2f,at);drawCircle(bone,r,at)}
            if(key.endsWith("w")){
                // Carpals, four metacarpals, phalanges and thumb; a grip curls the fingers round the bar in the palm.
                val grip=pose.grips[key] ?: "open"
                withTransform({rotate((angle ?: (PI.toFloat()/2))*180f/PI.toFloat(),q);translate(q.x,q.y);scale(unit,unit,pivot=Offset.Zero)}){
                    listOf(Offset(.3f,-.5f),Offset(.3f,.5f),Offset(1f,-.75f),Offset(1f,0f),Offset(1f,.75f)).forEach {knob(it,.4f)}
                    (0..3).forEach {i->
                        val y=(-1.2f+i*.8f)*(if(grip=="neutral").45f else 1f);val knuckle=Offset(2.9f-abs(i-1.5f)*.15f,y*1.15f)
                        piece(Offset(1.3f,y*.8f),knuckle,.5f)
                        when(grip){
                            "open"->{val mid=Offset(knuckle.x+1.1f,y*1.3f);piece(knuckle,mid,.42f);piece(mid,Offset(mid.x+.9f-abs(i-1.5f)*.2f,y*1.4f),.36f)}
                            "pronated"->{val end=knuckle+Offset(.9f,0f);piece(knuckle,end,.42f);knob(end,.3f)}
                            "supinated"->{val end=knuckle+Offset(.9f,0f);piece(knuckle,end,.42f);piece(end,end+Offset(-.95f,.22f),.36f)}
                            else->{val end=knuckle+Offset(.8f,0f);piece(knuckle,end,.42f);piece(end,end+Offset(0f,1f),.38f);piece(end+Offset(0f,1f),end+Offset(-.75f,1f),.34f)}
                        }
                    }
                    when(grip){
                        "open"->{piece(Offset(.6f,-1f),Offset(1.7f,-2.2f),.46f);piece(Offset(1.7f,-2.2f),Offset(2.5f,-2.7f),.38f)}
                        "supinated"->{piece(Offset(.6f,-1f),Offset(1.9f,-1.9f),.46f);piece(Offset(1.9f,-1.9f),Offset(3f,-1.3f),.38f)}
                        else->piece(Offset(.6f,-1f),Offset(1.8f,-1.7f),.46f)
                    }
                }
            }else if(angle==null){
                // Seen end-on: tarsals with five toes toward the viewer.
                withTransform({translate(q.x,q.y);scale(unit,unit,pivot=Offset.Zero)}){
                    drawOval(edge,Offset(-1.65f,-.25f),Size(3.3f,2.5f));drawOval(bone,Offset(-1.4f,0f),Size(2.8f,2f))
                    (-2..2).forEach {i->val x=i*.62f;piece(Offset(x,1.9f),Offset(x*1.25f,3.3f),.45f);piece(Offset(x*1.25f,3.3f),Offset(x*1.35f,4.1f-abs(i)*.12f),.36f)}
                }
            }else {
                val shin=joint(if(key=="la")"lk" else "rk")-q;val flip=if(-sin(angle)*shin.x+cos(angle)*shin.y>0)-1f else 1f
                withTransform({rotate(angle*180f/PI.toFloat(),q);translate(q.x,q.y);scale(unit,unit*flip,pivot=Offset.Zero)}){
                    // Heel bone, ankle bone, tarsals, metatarsals and toes that bend at the ball of the foot.
                    piece(Offset(-.1f,.9f),Offset(-1.5f,1.7f),1.3f);piece(Offset(.5f,.8f),Offset(2f,1.2f),1.2f)
                    (0..2).forEach {i->piece(Offset(2.1f,.9f+i*.35f),Offset(4.3f,1.55f+i*.2f),.5f)}
                    withTransform({rotate(-(pose.bends[key] ?: 0f),Offset(4.3f,1.8f))}){(0..1).forEach {i->val y=1.6f+i*.3f;piece(Offset(4.4f,y),Offset(5.3f,y+.1f),.42f);piece(Offset(5.3f,y+.1f),Offset(6.1f,y+.15f),.34f)}}
                    knob(Offset(.1f,.3f),.55f)
                }
            }
        }}
    }else {
        val build=RigBuild(female)
        val skin=Color(0xFFE5B493);val hair=if(female)Color(0xFFE9C46A)else Color(0xFF3B2A22)
        // She wears the accent colour; he wears blue, or graphite when the accent itself is blue.
        val cloth=if(female)color else if(color.blue>color.red+.15f && color.blue>color.green)Color(0xFF3C4654)else Color(0xFF2F64B8)
        val iris=if(female)Color(0xFF2E9B57)else Color(0xFF2F7FD6)
        val skinFar=rigShade(skin);val clothFar=rigShade(cloth,.2f);val bottoms=rigShade(cloth,.3f);val bottomsFar=rigShade(cloth,.38f);val detail=Color(0xFF5A413A);val shoe=Color(0xFF2F3440)
        fun far(depth:Float)=depth<torsoDepth-.5f
        fun root(key:String)=top+(joint(key)-top)*build.shoulder
        listOf("l","r").forEach {side->
            val shoulder=root(side+"s");val elbow=joint(side+"e");val wrist=joint(side+"w");val hip=joint(side+"h");val knee=joint(side+"k");val ankle=joint(side+"a")
            val r=build.arm;val g=build.leg
            add(rigDepth(pose,listOf(side+"s",side+"e")),side+"e"){rigLimb(if(far(rigDepth(pose,listOf(side+"s",side+"e"))))skinFar else skin,shoulder,elbow,r[0]*unit,r[1]*unit,r[2]*unit)}
            add(rigDepth(pose,listOf(side+"e",side+"w")),side+"w"){rigLimb(if(far(rigDepth(pose,listOf(side+"e",side+"w"))))skinFar else skin,elbow,wrist,r[2]*unit,r[3]*unit,r[4]*unit,.3f)}
            add(rigDepth(pose,listOf(side+"h",side+"k")),side+"k"){
                val back=far(rigDepth(pose,listOf(side+"h",side+"k")))
                val outline=rigLimb(if(back)skinFar else skin,hip,knee,g[0]*unit,g[1]*unit,g[2]*unit)
                val hem=hip+(knee-hip)*build.thighCloth
                // A round seat that flows into the thigh, set back from the hip along the body's own back direction.
                val along=(knee-hip).let {it/it.getDistance().coerceAtLeast(.01f)};val across=Offset(-along.y,along.x)
                val backSide=if(nose.x*across.x+nose.y*across.y>=0)-1f else 1f;val turned=1-frontal
                if(turned>.05f){
                    val centre=hip+across*(backSide*build.seat[2]*turned*unit)+along*(1.3f*unit)
                    withTransform({rotate(atan2(along.y,along.x)*180f/PI.toFloat(),centre)}){drawOval(if(back)bottomsFar else bottoms,centre-Offset(build.seat[1]*unit,build.seat[0]*unit),Size(build.seat[1]*2*unit,build.seat[0]*2*unit))}
                }
                clipPath(outline){drawLine(if(back)bottomsFar else bottoms,hip,hem,g[0]*unit*2.6f,StrokeCap.Butt)}
                drawCircle(if(back)bottomsFar else bottoms,g[0]*unit,hip)
            }
            add(rigDepth(pose,listOf(side+"k",side+"a")),side+"a"){rigLimb(if(far(rigDepth(pose,listOf(side+"k",side+"a"))))skinFar else skin,knee,ankle,g[2]*unit,g[3]*unit,g[4]*unit,.36f)}
        }
        add(torsoDepth+.01f,"body"){
            fun blend(side:Float,w:Float)=side+(w-side)*frontal
            val plus=build.torso.map {pt(it.t,blend(if(frontPlus)it.front else it.back,it.w))}
            val minus=build.torso.map {pt(it.t,-blend(if(frontPlus)it.back else it.front,it.w))}
            val outline=rigSmooth(plus+pt(1.2f,0f)+minus.reversed())
            // One whole person (D119): the shoulders, the hips and the neck are joined to the trunk by flesh of their own
            // width, drawn under it. In an ordinary pose the trunk covers them; turned end-on, bent or stretched, they
            // are what keeps an arm on its shoulder and a leg on its hip.
            fun bridge(tint:Color,width:Float,through:List<Offset>){
                val path=Path().apply {moveTo(through[0].x,through[0].y);through.drop(1).forEach {lineTo(it.x,it.y)}}
                drawPath(path,tint,style=Stroke(width,cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
            bridge(skin,build.arm[0]*2*unit,listOf(root("ls"),pt(.04f,0f),root("rs")))
            bridge(bottoms,build.leg[0]*2*unit,listOf(joint("lh"),pt(1f,0f),joint("rh")))
            bridge(skin,build.neck*unit,listOf(pt(-.08f,0f),joint("neck")))
            if(trunkEndOn){
                // Looked at along the spine the trunk is its own cross-section: the shirt across the shoulders, the bottoms across the hips.
                val chestNear=points.getValue("chest").depth>=points.getValue("pelvis").depth
                val hips={bridge(bottoms,(build.leg[0]*2+1.5f)*unit,listOf(joint("lh"),pt(1f,0f),joint("rh")))}
                val chest={bridge(cloth,(build.arm[0]*2+3.2f)*unit,listOf(top+(root("ls")-top)*.72f,top,top+(root("rs")-top)*.72f))}
                if(chestNear){hips();chest()}else {chest();hips()}
                return@add
            }
            drawPath(outline,skin)
            fun band(from:Float,to:Float,tint:Color){val path=Path().apply {val a=pt(from,-40f);val b=pt(from,40f);val d=pt(to,40f);val e=pt(to,-40f);moveTo(a.x,a.y);lineTo(b.x,b.y);lineTo(d.x,d.y);lineTo(e.x,e.y);close()};drawPath(path,tint)}
            clipPath(outline){
                fun across(w:Float)=w*frontal+1.9f*(1-frontal)
                fun yoke(from:Float,narrow:Float,to:Float,wide:Float){val path=Path().apply {val a=pt(from,-across(narrow)+forward(.4f));val b=pt(from,across(narrow)+forward(.4f));val d=pt(to,across(wide)+forward(1f));val e=pt(to,-across(wide)+forward(1f));moveTo(a.x,a.y);lineTo(b.x,b.y);lineTo(d.x,d.y);lineTo(e.x,e.y);close()};drawPath(path,cloth)}
                if(female){
                    band(.17f,.37f,cloth);yoke(-.13f,2.7f,.18f,7.6f);band(.8f,1.4f,bottoms)
                    if(facing>.3f){
                        drawPath(Path().apply {val a=pt(-.14f,-2f*frontal);val b=pt(.3f,0f);val d=pt(-.14f,2f*frontal);moveTo(a.x,a.y);quadraticTo(b.x,b.y,d.x,d.y);close()},skin)
                        listOf(-1f,1f).forEach {sign->drawPath(Path().apply {val a=pt(.3f,sign*.4f);val b=pt(.37f,sign*3.2f*frontal);val d=pt(.28f,sign*5.6f*frontal);moveTo(a.x,a.y);quadraticTo(b.x,b.y,d.x,d.y)},clothFar,style=Stroke(unit*.4f,cap=StrokeCap.Round))}
                        drawLine(skinFar,pt(.46f,0f),pt(.7f,0f),unit*.3f,StrokeCap.Round);drawCircle(skinFar,unit*.3f,pt(.74f,0f))
                    }else if(facing<-.3f){
                        listOf(-1f,1f).forEach {sign->drawPath(Path().apply {val a=pt(-.02f,sign*3.6f*frontal);val b=pt(.26f,sign*1.1f*frontal);val d=pt(.3f,sign*6f*frontal);val e=pt(-.02f,sign*9f);moveTo(a.x,a.y);quadraticTo(b.x,b.y,d.x,d.y);lineTo(e.x,e.y);close()},skin)}
                        drawLine(skinFar,pt(.42f,0f),pt(.76f,0f),unit*.3f,StrokeCap.Round)
                    }
                    drawLine(bottomsFar,pt(.82f,-40f),pt(.82f,40f),unit*.8f)
                }else {
                    band(.21f,1.4f,cloth);yoke(-.11f,4.2f,.22f,9.6f);band(.84f,1.4f,bottoms)
                    if(facing>.3f){
                        drawPath(Path().apply {val a=pt(-.11f,-2.7f*frontal);val b=pt(.2f,0f);val d=pt(-.11f,2.7f*frontal);moveTo(a.x,a.y);quadraticTo(b.x,b.y,d.x,d.y);close()},skin)
                        listOf(-1f,1f).forEach {sign->drawPath(Path().apply {val a=pt(.3f,sign*.5f);val b=pt(.36f,sign*4f*frontal);val d=pt(.27f,sign*6.6f*frontal);moveTo(a.x,a.y);quadraticTo(b.x,b.y,d.x,d.y)},clothFar,style=Stroke(unit*.45f,cap=StrokeCap.Round))}
                    }else if(facing<-.3f)drawLine(clothFar,pt(.1f,0f),pt(.78f,0f),unit*.4f,StrokeCap.Round)
                    drawLine(bottomsFar,pt(.86f,-40f),pt(.86f,40f),unit*.8f)
                }
            }
        }
        val neckBase=headCentre-headDirection/headDirection.getDistance().coerceAtLeast(.01f)*(3.4f*unit)
        if(female && headFacing>-.65f && !headEndOn)add(min(rigDepth(pose,listOf("head")),torsoDepth)-.4f,"hair-back"){
            // Ponytail: tied behind the head and falling with gravity whatever the body angle.
            val tie=if(headFrontal>.65f)Offset(2.4f,-3.6f)else Offset(-3.3f*noseSide,-3.2f)
            val base=headCentre+Offset(tie.x*cos(headRadians)-tie.y*sin(headRadians),tie.x*sin(headRadians)+tie.y*cos(headRadians))*unit
            val away=if(headFrontal>.65f)Offset(1f,0f)else -headNose
            val bend=base+away*(4.2f*unit)+Offset(0f,1.5f*unit);val end=base+away*(2.6f*unit)+Offset(0f,8.5f*unit)
            val tail=Path().apply {moveTo(base.x,base.y);quadraticTo(bend.x,bend.y,end.x,end.y)}
            drawPath(tail,hair,style=Stroke(2.6f*unit,cap=StrokeCap.Round));drawCircle(hair,1.7f*unit,base)
        }
        add(rigDepth(pose,listOf("neck","head")),"head"){
            drawLine(skin,neckBase,joint("neck")+(top-joint("neck"))*.5f,build.neck*unit,StrokeCap.Round)
            if(headEndOn){
                // From above: the hair, the forehead and nose ahead of it, the ears; from below: the face under the chin.
                val r=(if(female)3.5f else 3.9f)*unit;val side=Offset(-headNose.y,headNose.x)
                listOf(-1f,1f).forEach {drawCircle(skinFar,.8f*unit,headCentre+side*(it*r))}
                if(hz>0){
                    drawCircle(skin,r*.96f,headCentre+headNose*(.9f*unit));drawCircle(skin,.75f*unit,headCentre+headNose*(r+.5f*unit))
                    drawCircle(hair,r,headCentre-headNose*(.35f*unit))
                    if(female)drawCircle(hair,1.5f*unit,headCentre-headNose*(r+.7f*unit))
                }else {drawCircle(hair,r,headCentre-headNose*(.6f*unit));drawCircle(skin,r*.95f,headCentre+headNose*(.3f*unit));drawCircle(skinFar,.7f*unit,headCentre+headNose*(r*.55f))}
                return@add
            }
            inHead {
                val w=if(female)3.5f else 3.9f
                if(headFrontal>.65f){
                    val face=Path().apply {
                        moveTo(-w,-1f);cubicTo(-w,-6.6f,w,-6.6f,w,-1f)
                        if(female){cubicTo(w,2.4f,2f,4.7f,0f,5.1f);cubicTo(-2f,4.7f,-w,2.4f,-w,-1f)}
                        else {lineTo(w*.95f,2f);cubicTo(w*.88f,4.3f,1.9f,5.3f,0f,5.3f);cubicTo(-1.9f,5.3f,-w*.88f,4.3f,-w*.95f,2f)}
                        close()
                    }
                    listOf(-1f,1f).forEach {sign->drawCircle(skinFar,.75f,Offset(sign*w,.3f))}
                    drawPath(face,if(headFacing>0)skin else hair)
                    if(headFacing>0){
                        val crown=Path().apply {
                            if(female){moveTo(-w-.3f,.6f);cubicTo(-w-.9f,-8.2f,w+.9f,-8.2f,w+.3f,.6f);quadraticTo(w*.95f,-2.4f,w*.25f,-3.5f);quadraticTo(-w*.55f,-3.1f,-w*.9f,-.9f)}
                            else {moveTo(-w-.1f,-.9f);cubicTo(-w-.4f,-7.8f,w+.4f,-7.8f,w+.1f,-.9f);quadraticTo(w*.8f,-3.3f,0f,-3.5f);quadraticTo(-w*.8f,-3.3f,-w-.1f,-.9f)}
                            close()
                        };drawPath(crown,hair)
                        listOf(-1f,1f).forEach {sign->
                            drawOval(Color.White,Offset(sign*1.45f-.62f,-.2f),Size(1.24f,.86f));drawCircle(iris,.4f,Offset(sign*1.45f,.22f));drawCircle(detail,.17f,Offset(sign*1.45f,.22f))
                            drawLine(if(female)hair else detail,Offset(sign*.8f,-1.05f),Offset(sign*2.2f,-1.25f),if(female).28f else .42f,StrokeCap.Round)
                        }
                        drawLine(skinFar,Offset(.05f,.9f),Offset(.3f,2f),.3f,StrokeCap.Round)
                        if(female)drawOval(Color(0xFFB5545A),Offset(-.85f,2.85f),Size(1.7f,.7f))else drawLine(detail,Offset(-.8f,3.2f),Offset(.8f,3.2f),.32f,StrokeCap.Round)
                    }else {
                        // Seen from behind: hair covers the skull; the neck stays visible below a short cut.
                        if(female){drawPath(Path().apply {moveTo(-1.5f,-4f);cubicTo(-2.2f,2f,-1.6f,9f,0f,12.5f);cubicTo(1.6f,9f,2.2f,2f,1.5f,-4f);close()},rigShade(hair,-.25f))}
                        else drawPath(Path().apply {moveTo(-w,2.2f);quadraticTo(0f,5.6f,w,2.2f);lineTo(w*.5f,5f);lineTo(-w*.5f,5f);close()},skin)
                    }
                }else withTransform({scale(noseSide,1f,pivot=Offset.Zero)}){
                    val head=Path().apply {
                        moveTo(-3.7f,-.5f);cubicTo(-4f,-6.5f,3.4f,-6.7f,3.5f,-1.7f);lineTo(3.55f,-.3f)
                        if(female){lineTo(4.35f,1.2f);lineTo(3.55f,1.6f);lineTo(3.75f,2.4f);lineTo(3.3f,3f);lineTo(3.2f,4.1f);quadraticTo(2.6f,5f,1.5f,4.6f)}
                        else {lineTo(4.5f,1.3f);lineTo(3.6f,1.7f);lineTo(3.7f,2.6f);lineTo(3.4f,3.2f);lineTo(3.5f,4.4f);quadraticTo(2.9f,5.4f,1.5f,5f)}
                        lineTo(-1f,3.2f);quadraticTo(-3.5f,2.6f,-3.7f,-.5f);close()
                    };drawPath(head,skin)
                    val crown=Path().apply {
                        if(female){moveTo(3.5f,-2.2f);cubicTo(3.3f,-7.4f,-4.6f,-7.4f,-4.2f,-.6f);lineTo(-3.5f,2.5f);quadraticTo(-2.2f,1f,-1.4f,-1.3f);quadraticTo(.6f,-3.5f,3.5f,-2.2f)}
                        else {moveTo(3.45f,-2.7f);cubicTo(3.1f,-7.1f,-4.3f,-7f,-4f,-1.2f);lineTo(-3.6f,1.2f);quadraticTo(-2.1f,.2f,-1.6f,-1.7f);quadraticTo(.8f,-3.7f,3.45f,-2.7f)}
                        close()
                    };drawPath(crown,hair)
                    drawCircle(skinFar,.8f,Offset(-.7f,.5f))
                    drawOval(Color.White,Offset(1.75f,-.25f),Size(1.05f,.8f));drawCircle(iris,.36f,Offset(2.4f,.15f));drawCircle(detail,.15f,Offset(2.45f,.15f));drawLine(if(female)hair else detail,Offset(1.7f,-1.1f),Offset(3.1f,-1.2f),if(female).28f else .42f,StrokeCap.Round)
                    drawLine(if(female)Color(0xFFB5545A)else detail,Offset(2.9f,2.75f),Offset(3.65f,2.6f),if(female).55f else .32f,StrokeCap.Round)
                }
            }
        }
        listOf("lw","rw","la","ra").forEach {key->val depth=rigDepth(pose,listOf(key));add(depth,key+"tip"){
            val q=joint(key);val angle=tipAngle(key)
            if(key.endsWith("w")){
                val tint=if(far(depth))skinFar else skin;val grip=pose.grips[key] ?: "open";val width=build.arm[4];val line=rigShade(skin,.22f)
                withTransform({rotate((angle ?: (PI.toFloat()/2))*180f/PI.toFloat(),q);translate(q.x,q.y);scale(unit,unit,pivot=Offset.Zero)}){
                    if(grip=="open"){
                        drawLine(tint,Offset(.7f,-width*.5f),Offset(2.3f,-width*1.55f),width*.75f,StrokeCap.Round)
                        drawRoundRect(tint,Offset(-.4f,-width*.95f),Size(4.6f,width*1.9f),CornerRadius(width*.9f))
                        (-1..1).forEach {i->drawLine(line,Offset(2.9f,i*width*.5f),Offset(4f,i*width*.56f),.16f,StrokeCap.Round)}
                    }else {
                        // A closed hand round the bar: knuckles for an overhand grip, fingers and thumb for an underhand one, edge-on for neutral.
                        val w=if(grip=="neutral")width*.8f else width*1.15f
                        drawRoundRect(tint,Offset(-.2f,-w),Size(3.4f,w*2),CornerRadius(w*.9f))
                        when(grip){
                            "pronated"->(-1..1).forEach {i->drawLine(line,Offset(2.4f,i*w*.52f),Offset(3.05f,i*w*.52f),.2f,StrokeCap.Round)}
                            "supinated"->{(-1..1).forEach {i->drawLine(line,Offset(1.3f,i*w*.52f),Offset(2.7f,i*w*.52f),.2f,StrokeCap.Round)};drawLine(line,Offset(.6f,-w*.85f),Offset(2.5f,-w*.25f),.42f,StrokeCap.Round)}
                            else->drawLine(line,Offset(.9f,0f),Offset(2.9f,0f),.22f,StrokeCap.Round)
                        }
                    }
                }
            }else if(angle==null){
                // Facing or turned away: the shoe is seen end-on.
                drawRoundRect(shoe,q+Offset(-2.3f*unit,-1f*unit),Size(4.6f*unit,4.8f*unit),CornerRadius(2f*unit));drawLine(Color.White,q+Offset(-2f*unit,3.5f*unit),q+Offset(2f*unit,3.5f*unit),unit*.6f,StrokeCap.Round)
                drawLine(cloth,q+Offset(-1f*unit,.6f*unit),q+Offset(1f*unit,.6f*unit),unit*.6f,StrokeCap.Round)
            }else {
                // The sole always faces away from the shin, so mirrored and planted feet stay the right way up.
                val shin=joint(if(key=="la")"lk" else "rk")-q;val flip=if(-sin(angle)*shin.x+cos(angle)*shin.y>0)-1f else 1f
                withTransform({rotate(angle*180f/PI.toFloat(),q);translate(q.x,q.y);scale(unit,unit*flip,pivot=Offset.Zero)}){
                    drawPath(Path().apply {moveTo(-1.6f,-1.3f);lineTo(1.3f,-1.5f);quadraticTo(3.4f,-1f,4.5f,-.2f);lineTo(4.5f,2.1f);lineTo(-1.3f,2.1f);quadraticTo(-2.1f,1f,-1.6f,-1.3f);close()},shoe)
                    drawLine(Color.White,Offset(-1.2f,2f),Offset(4.3f,2f),.6f,StrokeCap.Round);drawLine(cloth,Offset(1.2f,-.3f),Offset(3.2f,.9f),.6f,StrokeCap.Round)
                    // The toe box bends at the ball of the foot.
                    withTransform({rotate(-(pose.bends[key] ?: 0f),Offset(4.3f,2f))}){
                        drawCircle(shoe,1.15f,Offset(4.4f,.95f))
                        drawPath(Path().apply {moveTo(4.2f,-.3f);quadraticTo(5.6f,.2f,6.3f,.9f);quadraticTo(6.6f,2f,5.7f,2.1f);lineTo(4.2f,2.1f);close()},shoe)
                        drawLine(Color.White,Offset(4.3f,2f),Offset(5.8f,2f),.6f,StrokeCap.Round)
                    }
                }
            }
        }}
    }
    c.props.forEach {prop->
        val positioned=rigPropProjection(prop,pose)
        val depth=positioned.depth+when(prop.layer){"back"->-300f;"front"->300f;else->0f}
        add(depth,prop.id){
            val center=p(positioned);val width=if(prop.anchor=="hands") (joint("lw")-joint("rw")).getDistance().coerceAtLeast(unit*4)else prop.width*unit;val h=prop.height*unit
            val rotation=if(prop.anchor=="hands")atan2((joint("rw")-joint("lw")).y,(joint("rw")-joint("lw")).x)*180/PI else prop.angle.toDouble()+prop.spin*seconds
            withTransform({rotate(rotation.toFloat(),center)}){
                val gray=Color(0xFF727A87)
                when(prop.kind){
                    "hoop"->drawOval(gray,center-Offset(width/2,h/2),Size(width,h),style=Stroke(unit*1.8f))
                    "ball"->drawOval(gray,center-Offset(width/2,h/2),Size(width,h))
                    "kettlebell"->{drawRoundRect(gray,center-Offset(width/2,h/2),Size(width,h),CornerRadius(unit*3));drawCircle(gray,width*.23f,center-Offset(0f,h/2),style=Stroke(unit*1.8f))}
                    "rope","stick"->drawLine(gray,center-Offset(width/2,0f),center+Offset(width/2,0f),unit*(if(prop.kind=="rope")1f else 2f),StrokeCap.Round)
                    "step","block","roller"->drawRoundRect(gray,center-Offset(width/2,h/2),Size(width,h),CornerRadius(if(prop.kind=="roller")h/2 else unit))
                    "dumbbell","barbell"->{drawLine(gray,center-Offset(width/2,0f),center+Offset(width/2,0f),unit*1.6f);listOf(-1,1).forEach {sign->drawRoundRect(gray,center+Offset(sign*width/2-unit*1.5f,-h),Size(unit*3,h*2),CornerRadius(unit))}}
                    "band"->drawLine(Color(0xFFC263D4),center-Offset(width/2,0f),center+Offset(width/2,0f),unit*2,StrokeCap.Round)
                    "chair"->{drawLine(gray,center-Offset(width/2,0f),center+Offset(width/2,0f),unit*2);drawLine(gray,center-Offset(width/2,h),center-Offset(width/2,-h),unit*2);drawLine(gray,center+Offset(width/2,0f),center+Offset(width/2,h),unit*2)}
                    "bar","mat","wall","custom"->drawRoundRect(gray.copy(alpha=if(prop.kind=="mat").4f else 1f),center-Offset(width/2,h/2),Size(width,h),CornerRadius(unit))
                    "arrow"->{drawLine(color,center-Offset(width/2,0f),center+Offset(width/2,0f),unit*1.5f);val path=Path().apply {moveTo(center.x+width/2,center.y);lineTo(center.x+width/2-unit*5,center.y-unit*3);lineTo(center.x+width/2-unit*5,center.y+unit*3);close()};drawPath(path,color)}
                    "rotation"->{drawArc(color,30f,280f,false,center-Offset(width/2,width/2),Size(width,width),style=Stroke(unit*1.5f));drawLine(color,center+Offset(width*.42f,width*.2f),center+Offset(width*.25f,width*.3f),unit*3,StrokeCap.Round)}
                }
                if(prop.kind=="bar" && prop.variant=="parallel")drawRoundRect(gray,center-Offset(width/2,h/2+unit*8),Size(width,h),CornerRadius(unit))
                if(prop.kind=="bar" && prop.variant=="angled")listOf(-1,1).forEach {sign->drawLine(gray,center+Offset(sign*width*.25f,0f),center+Offset(sign*width*.5f,unit*6),unit*2,StrokeCap.Round)}
                labels[prop.id]?.let {drawText(it,color=color,topLeft=center+Offset(-it.size.width/2f,h/2+unit))}
            }
        }
    }
    actions.sortedWith(compareBy<Triple<Float,String,()->Unit>> {it.first}.thenBy {it.second}).forEach {it.third()}
    if(handles)points.forEach {(key,q)->drawCircle(if(key==selected)Color(0xFFED843B)else Color.White,unit*2,p(q));drawCircle(color,unit*2,p(q),style=Stroke(unit*.6f))}
    // Square handles turn a hand or a foot.
    if(handles)listOf("lw","rw","la","ra").forEach {key->val t=rigTip(pose,key,points);val at=Offset(t.first*unit,t.second*size.height/100);val box=Size(unit*2.8f,unit*2.8f)
        drawLine(color.copy(alpha=.5f),joint(key),at,unit*.4f)
        drawRect(if("tip:$key"==selected)Color(0xFFED843B)else Color.White,at-Offset(unit*1.4f,unit*1.4f),box);drawRect(color,at-Offset(unit*1.4f,unit*1.4f),box,style=Stroke(unit*.6f))}
    c.props.firstOrNull {"prop:"+it.id==selected}?.let {prop->val q=p(rigPropProjection(prop,pose));drawCircle(Color(0xFFED843B),unit*4,q,style=Stroke(unit));drawLine(Color(0xFFED843B),q-Offset(unit*6,0f),q+Offset(unit*6,0f),unit);drawLine(Color(0xFFED843B),q-Offset(0f,unit*6),q+Offset(0f,unit*6),unit)}
}
