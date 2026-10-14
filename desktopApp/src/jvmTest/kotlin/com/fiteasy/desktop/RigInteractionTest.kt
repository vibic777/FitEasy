package com.fiteasy.desktop

import com.fiteasy.shared.*
import androidx.compose.ui.graphics.toArgb
import kotlin.test.*

class RigInteractionTest {
    @Test fun pelvisTranslationStopsAtSceneBounds(){
        val base=rigBase();val moved=rigEditJoint(base,base,"pelvis",RigPoint(100f,100f,100f))
        moved.points.values.forEach {assertTrue(it.x in 0f..100f && it.y in 0f..100f && it.z in -100f..100f)}
        assertEquals(4f,moved.points.getValue("rh").x-moved.points.getValue("pelvis").x,.01f)
    }
    @Test fun connectedSkeletonAndSelectedPropPreview(){
        val pose=rigBase();val prop=RigProp(point=RigPoint(80f,45f));val clip=RigClip(frames=listOf(RigFrame(time=0f,pose=pose)),props=listOf(prop))
        val bitmap=androidx.compose.ui.graphics.ImageBitmap(240,240)
        val color=androidx.compose.ui.graphics.Color(0xFF168B70)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f),androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(bitmap),androidx.compose.ui.geometry.Size(240f,240f)) {
            drawRect(androidx.compose.ui.graphics.Color.White);drawRig(clip,pose,0f,null,color,selected="prop:${prop.id}")
        }
        val pixels=IntArray(240*240);bitmap.readPixels(pixels)
        assertTrue(pixels[(54*2.4f).toInt()*240+(48*2.4f).toInt()] in setOf(color.toArgb(),androidx.compose.ui.graphics.Color(0xFFF1EADB).toArgb()),"Pelvis bone joins the hip")
        assertTrue(androidx.compose.ui.graphics.Color(0xFFED843B).toArgb() in pixels)
        val image=java.awt.image.BufferedImage(240,240,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,240,240,pixels,0,240)
        val output=java.io.File("build/review/rig-interaction.png");output.parentFile.mkdirs();assertTrue(javax.imageio.ImageIO.write(image,"png",output))
    }
    @Test fun attachmentCannotBePulledOffPelvisOrChest(){
        val base=rigBase();val loose=base.copy(points=base.points+("lh" to RigPoint(2f,5f))+("rs" to RigPoint(98f,92f)))
        val attached=rigConstrain(base,loose)
        assertEquals(base.points.getValue("lh"),attached.points.getValue("lh"))
        assertEquals(base.points.getValue("rs"),attached.points.getValue("rs"))
        val moved=rigEditJoint(base,base,"lh",base.points.getValue("lh").copy(x=51f,y=57f))
        assertEquals(55f,moved.points.getValue("pelvis").x,.01f)
        assertEquals(57f,moved.points.getValue("lh").y,.01f)
        assertEquals(4f,moved.points.getValue("pelvis").x-moved.points.getValue("lh").x,.01f)
        rigBones.forEach {(a,b)->assertEquals(rigLength(base.points.getValue(a),base.points.getValue(b)),rigLength(moved.points.getValue(a),moved.points.getValue(b)),.02f)}
    }
    @Test fun pickModesSeparatePropAndJointAndEmptySpace(){
        val pose=rigBase();val prop=RigProp(point=pose.points.getValue("rw"));val clip=RigClip(frames=listOf(RigFrame(time=0f,pose=pose)),props=listOf(prop))
        val q=rigPropProjection(prop,pose)
        assertEquals(prop.id,rigPick(clip,pose,q.x,q.y,true)?.propId)
        assertEquals("rw",rigPick(clip,pose,q.x,q.y,false)?.joint)
        assertNull(rigPick(clip,pose,0f,0f,false));assertNull(rigPick(clip,pose,0f,0f,true))
        val moved=rigDragProp(prop,pose,10f,5f)
        assertEquals(prop.point.x+10,moved.point.x,.01f);assertEquals(prop.point.y+5,moved.point.y,.01f)
        assertEquals(pose,clip.frames.single().pose)
    }
    @Test fun rotatedAndAnchoredPropsDragInProjectionPlane(){
        listOf(0f,90f,-90f,180f).forEach {yaw->listOf("scene","rw","hands").forEach {anchor->
            val pose=rigBase().copy(yaw=yaw,torsoYaw=30f);val prop=RigProp(anchor=anchor)
            val before=rigPropProjection(prop,pose);val moved=rigDragProp(prop,pose,4f,3f);val after=rigPropProjection(moved,pose)
            assertEquals(before.x+4,after.x,.01f);assertEquals(before.y+3,after.y,.01f)
            assertEquals(anchor,moved.anchor);assertEquals(prop.width,moved.width)
        }}
    }
    @Test fun frontmostPropWinsAndPosePlaybackKeepsAttachment(){
        val pose=rigBase();val back=RigProp(layer="back");val front=back.copy(id=newId(),layer="front")
        val clip=RigClip(frames=listOf(RigFrame(time=0f,pose=pose)),props=listOf(back,front))
        assertEquals(front.id,rigPick(clip,pose,50f,60f,true)?.propId)
        assertEquals(back.id,rigPick(clip,pose,50f,60f,true,preferredPropId=back.id)?.propId)
        val loose=pose.copy(points=pose.points+("lh" to RigPoint(1f,1f)))
        val sample=rigSample(clip.copy(lockLengths=false,frames=listOf(RigFrame(time=0f,pose=pose),RigFrame(time=1f,pose=loose))),1f)
        assertEquals(pose.points.getValue("lh"),sample.points.getValue("lh"))
    }
}
