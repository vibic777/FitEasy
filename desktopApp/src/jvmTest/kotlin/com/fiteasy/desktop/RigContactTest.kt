package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlin.math.*
import kotlin.test.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.*

class RigContactTest {
    private fun straight(p:RigPose,key:String):Float {
        val a=p.points.getValue("chest");val b=p.points.getValue("la");val q=p.points.getValue(key)
        return abs((b.z-a.z)*(q.y-a.y)-(b.y-a.y)*(q.z-a.z))/hypot(b.z-a.z,b.y-a.y)
    }
    @Test fun plankHasForearmAndToeSupportOnSameFloor(){
        val clip=rigTemplate("plank");val pose=rigSample(clip,0f)
        listOf("le","re","lw","rw").forEach {assertEquals(93f,pose.points.getValue(it).y,.01f)}
        listOf("la","ra").forEach {assertEquals(88f,pose.points.getValue(it).y,.01f);assertEquals(90f,pose.angles.getValue(it))}
        listOf("pelvis","lk","rk").forEach {assertTrue(straight(pose,it)<.05f)}
        assertEquals(95f,clip.props.single().point.y-.5f)
        assertFalse(rigIsMoving(clip))
    }
    @Test fun pushupKeepsHandsAndFeetFixedAndBodyStraight(){
        val clip=rigTemplate("pushup");val first=rigSample(clip,0f)
        for(step in 0..60){val p=rigSample(clip,step/20f)
            listOf("lw","rw","la","ra").forEach {assertTrue(rigLength(first.points.getValue(it),p.points.getValue(it))<.03f,"$step $it")}
            listOf("pelvis","lk","rk").forEach {assertTrue(straight(p,it)<.8f,"$step $it ${straight(p,it)}")}
        }
        val low=rigSample(clip,1.5f)
        assertTrue(low.points.getValue("chest").y>first.points.getValue("chest").y+15f)
        assertTrue(low.points.getValue("le").z>low.points.getValue("ls").z)
    }
    @Test fun pullupLiftsBodyWithFixedGripAndFrontalElbowPlane(){
        val clip=rigTemplate("pullup");val first=rigSample(clip,0f);val top=rigSample(clip,1.5f)
        for(step in 0..60){val pose=rigSample(clip,step/20f)
            listOf("lw","rw").forEach {assertTrue(rigLength(first.points.getValue(it),pose.points.getValue(it))<.03f)}
            listOf("le","re").forEach {assertTrue(abs(pose.points.getValue(it).z)<.05f)}
        }
        assertEquals(27f,first.points.getValue("pelvis").y-top.points.getValue("pelvis").y,.02f)
        assertTrue(top.points.getValue("le").y>top.points.getValue("ls").y)
        assertTrue(top.points.getValue("head").y+5.3f<clip.props.single().point.y)
    }
    @Test fun renderContactReview(){
        val views=listOf("plank" to 0f,"pushup" to 0f,"pushup" to 1.5f,"pullup" to 0f,"pullup" to 1.5f)
        val sheet=java.awt.image.BufferedImage(720,1200,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g=sheet.createGraphics()
        try {views.forEachIndexed {row,(key,time)->listOf(true,false,null).forEachIndexed {col,female->
            val c=rigTemplate(key);val bitmap=ImageBitmap(240,240)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(240f,240f)){
                drawRect(Color.White);drawRig(c,rigSample(c,time),time,female,Color(0xFF168B70))
            }
            val pixels=IntArray(240*240);bitmap.readPixels(pixels)
            val frame=java.awt.image.BufferedImage(240,240,java.awt.image.BufferedImage.TYPE_INT_ARGB)
            frame.setRGB(0,0,240,240,pixels,0,240);g.drawImage(frame,col*240,row*240,null)
        }}}finally {g.dispose()}
        val file=java.io.File("build/review/contact-poses.png");file.parentFile.mkdirs();assertTrue(javax.imageio.ImageIO.write(sheet,"png",file))
    }
}
