package com.fiteasy.desktop

import com.fiteasy.shared.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class RigFigureTest {
    private val accent=Color(0xFF168B70)
    private fun render(clip:RigClip,pose:RigPose,female:Boolean?,side:Int=320):IntArray {
        val bitmap=ImageBitmap(side,side)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(side.toFloat(),side.toFloat())){drawRect(Color.White);drawRig(clip,pose,0f,female,accent)}
        return IntArray(side*side).also {bitmap.readPixels(it)}
    }
    private fun still(pose:RigPose)=RigClip(frames=listOf(RigFrame(time=0f,pose=pose)))
    private fun scenes():List<Pair<RigClip,RigPose>> {
        val standing=rigBase()
        val poses=listOf(standing,standing.copy(yaw=90f),standing.copy(yaw=180f),standing.copy(yaw=40f),rigBase("seated").copy(yaw=70f)).map {still(it) to it}
        val templates=listOf("plank" to 0f,"pushup" to 1.5f,"squat" to 1.5f,"pullup" to 1.5f,"curl" to 1.5f).map {(key,time)->val c=rigTemplate(key);c to rigSample(c,time)}
        return poses+templates
    }
    @Test fun reviewSheetShowsSkeletonAndBothAthletes() {
        val all=scenes();val side=320
        val sheet=java.awt.image.BufferedImage(side*5,side*6,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g=sheet.createGraphics()
        try {listOf<Boolean?>(null,true,false).forEachIndexed {row,female->all.forEachIndexed {index,(clip,pose)->
            val frame=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,side,side,render(clip,pose,female),0,side)
            g.drawImage(frame,index%5*side,(row*2+index/5)*side,null)
        }}}finally {g.dispose()}
        val file=java.io.File("build/review/rig-figures.png");file.parentFile.mkdirs();assertTrue(javax.imageio.ImageIO.write(sheet,"png",file))
    }
    @Test fun everyViewDrawsADistinctFigure() {
        val white=Color.White.toArgb()
        scenes().forEach {(clip,pose)->
            val drawn=listOf<Boolean?>(null,true,false).map {render(clip,pose,it,160)}
            drawn.forEach {assertTrue(it.count {pixel->pixel!=white}>500)}
            assertEquals(3,drawn.map {it.toList()}.distinct().size)
        }
    }
    @Test fun faceFollowsTheBodyNotOnlyTheSceneAngle() {
        // Seated knees point forward: the profile nose must point the same way.
        val pose=rigBase("seated").copy(yaw=90f)
        val pixels=render(still(pose),pose,true,200)
        val skin=Color(0xFFE5B493).toArgb()
        fun skinAt(x:IntRange)=(26..44).sumOf {y->x.count {pixels[y*200+it]==skin}}
        val knee=rigProject(rigWorldPoint(pose,"lk"),pose.yaw).x
        assertTrue(knee>50f)
        assertTrue(skinAt(101..112)>skinAt(88..99),"Face skin must be on the knee side of the head")
    }
    /** Poses that used to tear a limb off (D119): a leaning, bent, lying or raised body, seen from every side and from above. */
    private fun hardPoses():List<Pair<String,RigPose>> {
        val base=rigBase()
        fun bend(pose:RigPose,joint:String,x:Float,y:Float,z:Float=0f)=rigEditJoint(base,pose,joint,RigPoint(x,y,z))
        val lean=bend(base,"chest",67f,37f);val low=bend(base,"chest",73f,50f);val forward=bend(base,"chest",50f,40f,20f)
        val armsUp=bend(bend(bend(bend(lean,"le",80f,22f),"lw",88f,8f),"re",70f,14f),"rw",76f,2f)
        val wide=rigAttached(base.copy(points=base.points+("ls" to RigPoint(38f,31f))+("rs" to RigPoint(62f,31f))),base)
        val lying=rigBase("lying");val plank=rigTemplate("plank").frames.first().pose;val seated=rigBase("seated")
        return listOf("lean" to lean,"lean far" to low,"lean far, back" to low.copy(yaw=180f),"bent forward, side" to forward.copy(yaw=90f),"bent forward, front" to forward,
            "arms up, leaning" to armsUp,"wide shoulders" to wide,"twisted" to lean.copy(torsoYaw=60f),"other side" to base.copy(yaw=-90f),"lying, end-on" to lying.copy(yaw=0f),
            "lying, from above" to lying.copy(pitch=90f),"standing, from above" to base.copy(pitch=90f),"plank, from above" to plank.copy(pitch=90f),"seated, from above" to seated.copy(pitch=90f),
            "half tipped" to lean.copy(pitch=45f,yaw=30f),"leaning, from above" to low.copy(pitch=90f,yaw=-90f))
    }
    /** How many separate pieces the figure is drawn in, after its outline is thickened by [grow] pixels. */
    private fun pieces(pixels:IntArray,side:Int,grow:Int):Int {
        val white=Color.White.toArgb();val ink=BooleanArray(side*side)
        for(y in 0 until side)for(x in 0 until side)if(pixels[y*side+x]!=white)for(dy in -grow..grow)for(dx in -grow..grow){val u=x+dx;val v=y+dy;if(u in 0 until side && v in 0 until side)ink[v*side+u]=true}
        var count=0;val stack=IntArray(side*side)
        for(start in ink.indices)if(ink[start]){
            count++;var top=0;stack[top++]=start;ink[start]=false
            while(top>0){val at=stack[--top];val x=at%side;val y=at/side
                for(dy in -1..1)for(dx in -1..1){val u=x+dx;val v=y+dy;if(u in 0 until side && v in 0 until side && ink[v*side+u]){ink[v*side+u]=false;stack[top++]=v*side+u}}}
        }
        return count
    }
    @Test fun theFigureIsOneWholePersonInEveryPoseAndView() {
        val side=320
        hardPoses().forEach {(name,pose)->
            validateRig(still(pose))
            // The silhouettes are one piece as drawn; the skeleton's small bones of the hand are a hair apart by design.
            listOf(true to 1,false to 1,null to 3).forEach {(female,grow)->assertEquals(1,pieces(render(still(pose),pose,female,side),side,grow),"$name, ${when(female){true->"woman";false->"man";else->"skeleton"}}: a part is torn off")}
        }
        // The shoulders lean with the trunk: the line between them stays square to the spine, at their own width.
        val base=rigBase();val leaning=rigEditJoint(base,base,"chest",RigPoint(73f,50f))
        val spine=leaning.points.getValue("chest").let {c->val p=leaning.points.getValue("pelvis");RigPoint(c.x-p.x,c.y-p.y,c.z-p.z)}
        val across=leaning.points.getValue("rs").let {r->val l=leaning.points.getValue("ls");RigPoint(r.x-l.x,r.y-l.y,r.z-l.z)}
        assertTrue(kotlin.math.abs(spine.x*across.x+spine.y*across.y+spine.z*across.z)<.5f,"Shoulders square to the spine");assertEquals(16f,rigLength(leaning.points.getValue("ls"),leaning.points.getValue("rs")),.2f)
        // An upright body is exactly as it was before: nothing saved earlier moves.
        assertEquals(base,rigAttached(base,base));listOf("plank","pushup","squat","pullup","curl").forEach {key->val clip=rigTemplate(key);assertEquals(clip.frames.first().pose.points,rigAttached(clip.frames.first().pose,clip.frames.first().pose).points,key)}
    }
    @Test fun aPoseIsSeenFromBothSidesAndFromAbove() {
        val pose=rigBase()
        // From above an upright figure is its shoulders and head: wider than tall on the screen, the front of the body down.
        val top=rigProjected(pose.copy(pitch=90f))
        assertTrue(top.getValue("rs").x-top.getValue("ls").x>15f);assertTrue(top.values.maxOf {it.y}-top.values.minOf {it.y}<8f,"Seen along the spine");assertTrue(top.getValue("head").depth>top.getValue("la").depth,"The head is nearest")
        val reach=rigProject(RigPoint(50f,50f,20f),0f,90f);assertEquals(70f,reach.y,.01f)
        // The two sides mirror each other.
        val right=rigProjected(pose.copy(yaw=90f));val left=rigProjected(pose.copy(yaw=-90f));rigJoints.forEach {assertEquals(100f-right.getValue(it).x,left.getValue(it).x,.01f,it)}
        // A view that is not tipped is the same projection as before.
        rigJoints.forEach {key->val p=rigWorldPoint(pose,key);assertEquals(rigProject(p,40f),rigProject(p,40f,0f))}
        // Dragging in the top view moves a joint under the pointer and leaves its height alone.
        val above=pose.copy(pitch=90f);val before=rigProject(rigWorldPoint(above,"rw"),above.yaw,above.pitch)
        val dragged=rigDrag(above,"rw",before.x+6f,before.y+9f);val after=rigProject(rigWorldPoint(dragged,"rw"),dragged.yaw,dragged.pitch)
        assertEquals(before.x+6f,after.x,.01f);assertEquals(before.y+9f,after.y,.01f);assertEquals(pose.points.getValue("rw").y,dragged.points.getValue("rw").y,.01f)
        // The tip is part of the pose: kept in a file, blended between frames, refused when out of range.
        val clip=RigClip(frames=listOf(RigFrame(time=0f,pose=pose),RigFrame(time=2f,pose=pose.copy(pitch=90f))),loop=false,duration=2f);validateRig(clip)
        assertEquals(45f,rigSample(clip,1f).pitch,.01f);assertEquals(90f,codec.decodeFromString<RigClip>(codec.encodeToString(clip)).frames.last().pose.pitch)
        assertEquals(0f,codec.decodeFromString<RigPose>(codec.encodeToString(pose).replace(""","pitch":0.0""","")).pitch,"A scene saved before has no tip")
        assertFails {validateRig(still(pose.copy(pitch=120f)))}
        languageNames.keys.forEach {language->listOf("Other side view","Top view").forEach {assertTrue(rigText(it,language).isNotBlank() && (language=="en" || rigText(it,language)!=it),"$language/$it")}}
    }
    @Test fun hardPosesSheet() {
        val all=hardPoses();val side=260;val columns=8
        val sheet=java.awt.image.BufferedImage(side*columns,side*6,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
        try {listOf<Boolean?>(true,false,null).forEachIndexed {row,female->all.forEachIndexed {index,(_,pose)->
            val frame=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,side,side,render(still(pose),pose,female,side),0,side)
            g.drawImage(frame,index%columns*side,(row*2+index/columns)*side,null)
        }}}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/rig-hard-poses.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun closeUpSheet() {
        val side=640;val pose=rigBase()
        val views=listOf(pose.copy(yaw=90f) to true,pose.copy(yaw=-90f) to true,pose.copy(yaw=125f) to true,pose to false)
        val sheet=java.awt.image.BufferedImage(side*4,side,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
        try {views.forEachIndexed {i,(p,female)->val frame=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,side,side,render(still(p),p,female,side),0,side);g.drawImage(frame,i*side,0,null)}}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet.getSubimage(0,60,side*4,560),"png",java.io.File("build/review/rig-closeup.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun plankAndPushUpFaceTheFloor() {
        listOf("plank","pushup").forEach {key->
            val clip=rigTemplate(key);val pose=rigSample(clip,0f);val pixels=render(clip,pose,true,200)
            val head=rigProject(rigWorldPoint(pose,"head"),pose.yaw);val cx=(head.x*2).toInt();val cy=(head.y*2).toInt()
            val skin=Color(0xFFE5B493).toArgb()
            fun skinIn(rows:IntRange)=rows.sumOf {y->(cx-9..cx+9).count {x->pixels[y*200+x]==skin}}
            assertTrue(skinIn(cy+1..cy+10)>skinIn(cy-10..cy-1),"$key: the face is on the floor side of the head")
        }
    }
    @Test fun handsFeetGripsAndHeadTurnSheet() {
        val side=1600;val cell=256;val white=Color.White.toArgb()
        fun crop(pose:RigPose,female:Boolean?,cx:Float,cy:Float):java.awt.image.BufferedImage {
            val image=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,side,side,render(still(pose),pose,female,side),0,side)
            return image.getSubimage((cx*16-cell/2).toInt().coerceIn(0,side-cell),(cy*16-cell/2).toInt().coerceIn(0,side-cell),cell,cell)
        }
        val base=rigBase();val profile=base.copy(yaw=90f)
        val sheet=java.awt.image.BufferedImage(cell*10,cell*3,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
        try {listOf<Boolean?>(null,true,false).forEachIndexed {row,female->
            val cells=listOf("open","pronated","supinated","neutral").map {grip->crop(base.copy(grips=mapOf("rw" to grip)),female,66f,62f)}+
                listOf(crop(profile,female,52f,90f),crop(profile.copy(angles=mapOf("la" to 40f,"ra" to 40f),bends=mapOf("la" to 40f,"ra" to 40f)),female,52f,90f),crop(base,female,50f,90f))+
                listOf(-70f,0f,70f).map {turn->crop(base.copy(headYaw=turn),female,50f,20f)}
            cells.forEachIndexed {i,image->g.drawImage(image,i*cell,row*cell,null)}
            // Every grip, the bent foot and each head turn must look different from its neighbour.
            val pixels=cells.map {image->IntArray(cell*cell).also {image.getRGB(0,0,cell,cell,it,0,cell)}.toList()}
            assertEquals(4,pixels.take(4).distinct().size,"grips $female");assertNotEquals(pixels[4],pixels[5],"toe bend $female");assertEquals(3,pixels.drop(7).distinct().size,"head turn $female")
            pixels.forEach {assertTrue(it.count {pixel->pixel!=white}>300)}
        }}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/rig-extremities.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun handAndFootHandlesAimAndHeldObjectsSitInThePalm() {
        val pose=rigBase()
        val tip=rigTip(pose,"rw");val picked=rigPick(still(pose),pose,tip.first,tip.second,false)
        assertEquals("rw",picked?.tip);assertEquals("rw",picked?.joint)
        assertNull(rigPick(still(pose),pose,pose.points.getValue("rw").x,pose.points.getValue("rw").y,false)?.tip)
        val wrist=rigProject(rigWorldPoint(pose,"rw"),pose.yaw)
        val aimed=rigAim(pose,"rw",wrist.x+10f,wrist.y);assertEquals(0f,aimed.angles.getValue("rw"),.01f)
        assertEquals(90f,rigAim(pose,"la",pose.points.getValue("la").x,pose.points.getValue("la").y+5f).angles.getValue("la"),.01f)
        // An unset hand continues the forearm; the dumbbell is drawn a palm beyond the wrist along it.
        val held=rigPropProjection(RigProp(anchor="rw",point=RigPoint(50f,60f)),pose)
        val angle=rigTipAngle(pose,"rw")!!
        assertEquals(wrist.x+kotlin.math.cos(angle)*RIG_PALM,held.x,.01f);assertEquals(wrist.y+kotlin.math.sin(angle)*RIG_PALM,held.y,.01f)
        validateRig(still(pose.copy(headYaw=45f,bends=mapOf("la" to 30f))))
        assertFails {validateRig(still(pose.copy(headYaw=120f)))};assertFails {validateRig(still(pose.copy(bends=mapOf("lw" to 10f))))}
        val restored=codec.decodeFromString<RigPose>(codec.encodeToString(pose.copy(headYaw=30f,bends=mapOf("ra" to -20f))));assertEquals(30f,restored.headYaw);assertEquals(-20f,restored.bends["ra"])
        // A scene saved before these fields existed still opens.
        assertEquals(0f,codec.decodeFromString<RigPose>("""{"points":{}}""").headYaw)
    }
    @Test fun footExercisesKeepTheBallOfTheFootOnTheFloor() {
        val clip=rigTemplate("heelraise");validateRig(clip);validateRig(rigTemplate("toeraise"))
        val low=rigSample(clip,0f);val high=rigSample(clip,1.5f)
        assertTrue(high.points.getValue("la").y<low.points.getValue("la").y-2f);assertEquals(40f,high.bends.getValue("la"),.01f)
        fun ball(p:RigPose):Float {val a=rigTipAngle(p,"la")!!;return rigProject(rigWorldPoint(p,"la"),p.yaw).y+kotlin.math.sin(a)*4.3f}
        assertEquals(ball(low),ball(high),.4f)
        assertEquals(setOf("heelraise","toeraise","armsup","twist","necktilt","kneeext"),exerciseCatalogue.filter {it.category=="Mobility"}.map {it.technique}.toSet())
    }
    @Test fun womanHasWaistAndManHasShoulders() {
        val pose=rigBase();val white=Color.White.toArgb()
        fun torsoWidth(pixels:IntArray,y:Int):Int {var left=160;var right=160;while(left>0 && pixels[y*320+left-1]!=white)left--;while(right<319 && pixels[y*320+right+1]!=white)right++;return right-left}
        val woman=render(still(pose.copy(points=pose.points+("lw" to RigPoint(20f,59f))+("rw" to RigPoint(80f,59f))+("le" to RigPoint(30f,45f))+("re" to RigPoint(70f,45f)))),pose.copy(points=pose.points+("lw" to RigPoint(20f,59f))+("rw" to RigPoint(80f,59f))+("le" to RigPoint(30f,45f))+("re" to RigPoint(70f,45f))),true)
        assertTrue(torsoWidth(woman,144)<torsoWidth(woman,170),"Waist narrower than hips")
        val man=render(still(pose),pose,false);val lady=render(still(pose),pose,true)
        assertTrue(torsoWidth(man,104)>torsoWidth(lady,104),"Male shoulders are broader")
    }
}
