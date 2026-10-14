package com.fiteasy.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.fiteasy.shared.*
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class RigEditorReviewTest {
    private fun <T> withPlatform(block:(DesktopPlatform)->T):T {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-rig-editor-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform()
        try {return block(platform)}finally {platform.close();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun editorOpensOnPhoneAndDesktopAndEveryTabRenders() {
        withPlatform {platform->listOf(400 to 860,1000 to 760).forEach {(width,height)->
            var saved:RigClip?=null
            val scene=ImageComposeScene(width,height,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {RigEditor(platform,rigTemplate("curl"),onDismiss={},onSave={saved=it})}}}
            try {
                fun shot(name:String){val image=scene.render();val file=java.io.File("build/review/rig-editor-$width-$name.png");file.parentFile.mkdirs();file.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}
                scene.render().close();shot("joint")
                assertNull(saved)
            }finally {scene.close()}
        }}
    }
    @Test fun everyEditorHintExistsInTenLanguages() {
        val editor=listOf("Close","Undo","Redo","Save","Skeleton","Woman","Man","Front view","Side view","Back view","Stop","Preview","Previous frame","Next frame","Add frame","Delete frame","Loop",
            "Joint","Objects","Pose","Timing","Files","Depth","Horizontal position","Vertical position","Hand / foot angle","Pin contact","open","pronated","supinated","neutral","auto","back","front",
            "Remove object","Copy object","Anchor","bar","Caption","Width","Height","Rotation","Rotation per second","standing","seated","lying","plank","hanging","Mirror left / right","Fixed bone lengths",
            "Figure rotation","Torso rotation","Head turn","Toe bend","Frame time","Duration","Export JSON","Export SVG","Import JSON")+rigEquipment+rigJoints
        assertEquals(10,languageNames.size)
        languageNames.keys.forEach {language->
            assertEquals(rigToolKeys.size,rigToolRows[language]?.split('|')?.size ?: rigToolKeys.size,language)
            editor.forEach {key->val hint=rigText(key,language);assertTrue(hint.isNotBlank(),"$language/$key");if(language in setOf("ru","be","zh","ja","ko"))assertNotEquals(key,hint,"$language/$key is not translated")}
        }
    }
    @Test fun everyIconButtonLabelInTheAppIsTranslated() {
        val pattern=Regex("IconAction\\(\"[^\"]*\",\"([^\"]*)\"")
        val labels=java.io.File("../shared/src").walkTopDown().filter {it.extension=="kt"}.flatMap {file->pattern.findAll(file.readText()).map {it.groupValues[1]}}.toSet()+mainSections.map {it.second}
        assertTrue(labels.size>15)
        fun hint(label:String,language:String)=when(label){"Предыдущий день"->if(language=="en")"Previous day" else "x";"Следующий день"->if(language=="en")"Next day" else "x";else->noteNavigationText(label,language)}
        languageNames.keys.filter {it !in setOf("ru","be")}.forEach {language->labels.forEach {label->val text=hint(label,language);assertTrue(text.isNotBlank() && text!=label,"$language/$label has no translated hint")}}
    }
    @Test fun mirrorSwapsSidesAndKeepsTheSkeletonValid() {
        val pose=rigBase().let {it.copy(points=it.points+("rw" to RigPoint(80f,40f,10f)),angles=mapOf("rw" to 30f),grips=mapOf("rw" to "supinated"),yaw=40f)}
        val mirrored=rigMirror(pose)
        assertEquals(RigPoint(20f,40f,10f),mirrored.points.getValue("lw"));assertEquals("supinated",mirrored.grips["lw"]);assertEquals(150f,mirrored.angles.getValue("lw"),.01f);assertEquals(-40f,mirrored.yaw)
        assertTrue(mirrored.points.getValue("ls").x<mirrored.points.getValue("rs").x)
        validateRig(RigClip(frames=listOf(RigFrame(time=0f,pose=mirrored))))
        assertEquals(pose.points,rigMirror(mirrored).points)
    }
    @Test fun animationSheetsForEveryExampleAndFigure() {
        val side=200;val accent=Color(0xFF168B70);val white=Color.White.toArgb()
        listOf("pushup","squat","raise","curl","pullup","heelraise","toeraise").forEach {key->
            val clip=rigTemplate(key);val steps=8
            val sheet=java.awt.image.BufferedImage(side*steps,side*3,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
            try {listOf<Boolean?>(null,true,false).forEachIndexed {row,female->
                val frames=(0 until steps).map {step->
                    val time=clip.duration*step/steps;val bitmap=ImageBitmap(side,side)
                    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(side.toFloat(),side.toFloat())){drawRect(Color.White);drawRig(clip,rigSample(clip,time),time,female,accent)}
                    val pixels=IntArray(side*side).also {bitmap.readPixels(it)}
                    val frame=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,side,side,pixels,0,side);g.drawImage(frame,step*side,row*side,null)
                    pixels
                }
                assertTrue(frames.map {it.toList()}.distinct().size>=3,"$key moves")
                // No frame may tear the figure apart or drop it: drawn area stays within a third of the first frame.
                val area=frames.map {it.count {pixel->pixel!=white}}
                area.forEach {assertTrue(it>area[0]*2/3 && it<area[0]*3/2,"$key keeps a whole figure: $area")}
            }}finally {g.dispose()}
            assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/rig-motion-$key.png").also {it.parentFile.mkdirs()}))
        }
    }
    @Test fun libraryGivesTheExerciseTheScreen() {
        withPlatform {platform->listOf(false,true).forEach {full->
            val scene=ImageComposeScene(360,720,Density(1f)){CompositionLocalProvider(LocalLanguage provides "ru"){MaterialTheme {androidx.compose.material3.Surface {ExerciseLibraryScreen(platform,onDismiss={},onSelect={},startFull=full)}}}}
            try {scene.render().close();val image=scene.render(50_000_000);java.io.File("build/review/library-${if(full)"full" else "page"}.png").also {it.parentFile.mkdirs()}.writeBytes(requireNotNull(image.encodeToData()).bytes);image.close()}finally {scene.close()}
        }}
        languageNames.keys.forEach {language->listOf("Показать упражнение","Новое упражнение","Описание").forEach {key->assertTrue(translate(key,language).isNotBlank());if(language!="ru")assertNotEquals(key,translate(key,language),"$language/$key")}}
    }
}
