package com.fiteasy.desktop

import com.fiteasy.shared.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.*

class ExerciseVisualTest {
    @Test fun avatarViewsFollowHeadAndBody(){
        val sheet=java.awt.image.BufferedImage(720,480,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics=sheet.createGraphics()
        try {listOf(true,false).forEachIndexed {row,female->
            val views=listOf(rigBase("standing"),rigBase("plank"),rigBase("standing").copy(yaw=180f))
            val results=views.mapIndexed {col,pose->
                val bitmap=ImageBitmap(240,240)
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(240f,240f)){
                    drawRect(Color.White);drawRig(RigClip(frames=listOf(RigFrame(time=0f,pose=pose))),pose,0f,female,Color(0xFF168B70))
                }
                val values=IntArray(240*240);bitmap.readPixels(values)
                val frame=java.awt.image.BufferedImage(240,240,java.awt.image.BufferedImage.TYPE_INT_ARGB)
                frame.setRGB(0,0,240,240,values,0,240);graphics.drawImage(frame,col*240,row*240,null)
                values.toList()
            }
            assertNotEquals(results[0],results[1]);assertNotEquals(results[0],results[2])
            val face=(35..53).flatMap {y->(110..130).map {x->y*240+x}}
            assertNotEquals(face.map {results[0][it]},face.map {results[2][it]})
            if(female){
                fun width(y:Int):Int {val pixels=results[0];val white=Color.White.toArgb();var left=120;var right=120;while(left>0 && pixels[y*240+left-1]!=white)left--;while(right<239 && pixels[y*240+right+1]!=white)right++;return right-left+1}
                assertTrue(width(110)<width(84),"Waist must be narrower than fitted chest")
                assertTrue(width(110)<width(139),"Waist must be narrower than skirt/hips")
            }
        }}finally {graphics.dispose()}
        val file=java.io.File("build/review/avatar-preview.png");file.parentFile.mkdirs()
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",file))
    }
    @Test fun clothingSkinAndHairAreSeparate(){
        listOf(true,false).forEach {female->
            val colors=pixels("",0f,female).toSet()
            assertTrue(Color(0xFFE5B493).toArgb() in colors)
            // She is blonde in the accent colour; he is dark-haired in blue.
            assertTrue(Color(if(female)0xFFE9C46A else 0xFF3B2A22).toArgb() in colors)
            assertTrue(Color(if(female)0xFF168B70 else 0xFF2F64B8).toArgb() in colors)
            assertTrue(Color(if(female)0xFF2E9B57 else 0xFF2F7FD6).toArgb() in pixels("",0f,female,640).toSet(),"Eye colour")
        }
    }
    private fun pixels(technique:String,phase:Float,female:Boolean=true,side:Int=160):IntArray {
        val bitmap=ImageBitmap(side,side)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(side.toFloat(),side.toFloat())){
            drawRect(Color.White);drawExerciseFigure(technique,phase,female,Color(0xFF168B70),Color.Gray)
        }
        return IntArray(side*side).also {bitmap.readPixels(it)}
    }
    @Test fun movingAndStaticPoses(){
        allExerciseTechniques.keys.forEach {key->
            val first=pixels(key,0f)
            assertTrue(first.count {it!=Color.White.toArgb()}>400,key)
            assertNotEquals(first.toList(),pixels(key,0f,false).toList(),key)
            if(exerciseAnimates(key,true,true,true))assertNotEquals(first.toList(),pixels(key,.25f).toList(),key)
            else assertEquals(first.toList(),pixels(key,.25f).toList(),key)
        }
    }
    @Test fun clocksStopWhenPausedDisabledOrInvisible(){
        listOf(false,true).forEach {running->listOf(false,true).forEach {enabled->listOf(false,true).forEach {visible->
            assertEquals(running && enabled && visible,exerciseAnimates("curl",running,enabled,visible))
        }}}
        assertFalse(exerciseAnimates("unknown",true,true,true));assertFalse(exerciseAnimates("plank",true,true,true))
    }
    @Test fun autoNamesFillTheFirstGap(){
        assertEquals("Папка 2",automaticName("Папка",listOf("Папка 1","Папка 3","Папка 4")))
        assertEquals("Папка 4",automaticName("Папка",listOf("Папка 1","Папка 2","Папка 3")))
        assertEquals("Папка 2",automaticName("Папка",listOf(" папка 1 ","Папка 01")))
        assertEquals("Text 1",automaticName("Text",emptyList()))
    }
    @Test fun tenLanguagesAndAvatarKeys(){
        assertEquals(languageNames.keys,reminderVisualRows.keys)
        reminderVisualRows.forEach {(language,row)->assertEquals(reminderVisualKeys.size,row.split('|').size,language);reminderVisualKeys.forEach {assertFalse(translate(it,language).isBlank())}}
        val e=Exercise(name="Example",technique="walk")
        assertTrue(exerciseAvatarKey(e).matches(Regex("[a-f0-9]{64}")))
        assertEquals(exerciseAvatarKey(e),exerciseAvatarKey(e.copy(seconds=60)))
        assertNotEquals(exerciseAvatarKey(e),exerciseAvatarKey(e.copy(technique="curl")))
    }
    @Test fun renderReviewSheet(){
        val names=allExerciseTechniques.keys.toList()
        val sheet=java.awt.image.BufferedImage(640,names.size*180,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g=sheet.createGraphics()
        try {g.color=java.awt.Color.WHITE;g.fillRect(0,0,sheet.width,sheet.height);names.forEachIndexed {index,name->
            g.color=java.awt.Color.BLACK;g.drawString(name.ifBlank {"neutral"},8,index*180+15)
            listOf(0f,.25f,.75f).forEachIndexed {col,phase->val frame=java.awt.image.BufferedImage(160,160,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,160,160,pixels(name,phase),0,160);g.drawImage(frame,col*160,index*180+20,null)}
            val male=java.awt.image.BufferedImage(160,160,java.awt.image.BufferedImage.TYPE_INT_ARGB);male.setRGB(0,0,160,160,pixels(name,0f,false),0,160);g.drawImage(male,480,index*180+20,null)
        }}finally {g.dispose()}
        val file=java.io.File("build/review/exercise-figures.png");file.parentFile.mkdirs();assertTrue(javax.imageio.ImageIO.write(sheet,"png",file))
        val preview=java.awt.image.BufferedImage(640,360,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val pg=preview.createGraphics()
        try {pg.color=java.awt.Color.WHITE;pg.fillRect(0,0,640,360)
            listOf("" to true,"" to false,"curl" to true,"pullup" to false).forEachIndexed {col,(key,female)->
                val image=java.awt.image.BufferedImage(160,160,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,160,160,pixels(key,.25f,female),0,160)
                pg.drawImage(image,col*160,0,null)
                image.setRGB(0,0,160,160,pixels(key,.75f,female),0,160);pg.drawImage(image,col*160,180,null)
            }
        }finally{pg.dispose()}
        assertTrue(javax.imageio.ImageIO.write(preview,"png",java.io.File("build/review/exercise-preview.png")))
    }
}
