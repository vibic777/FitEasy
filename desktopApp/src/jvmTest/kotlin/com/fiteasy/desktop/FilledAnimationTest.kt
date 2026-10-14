package com.fiteasy.desktop

import com.fiteasy.shared.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.*

class FilledAnimationTest {
    @Test fun forwardWalkingCycleAndKnees() {
        val stance=(0..49).map {walkingLeg(it/100f)}
        stance.zipWithNext().forEach {(a,b)->assertTrue(b.ankle.x<a.ankle.x);assertEquals(22f,b.ankle.y)}
        val recovery=(50..99).map {walkingLeg(it/100f)}
        recovery.zipWithNext().forEach {(a,b)->assertTrue(b.ankle.x>a.ankle.x)}
        assertTrue(walkingLeg(.75f).ankle.y<20f)
        repeat(100){i->val p=walkingLeg(i/100f)
            assertTrue(p.knee.x>(p.hip.x+p.ankle.x)/2,"Forward knee at $i")
            assertEquals(4.8f,(p.knee-p.hip).getDistance(),.001f)
            assertEquals(4.8f,(p.ankle-p.knee).getDistance(),.001f)
        }
        assertEquals(walkingLeg(0f),walkingLeg(1f))
    }
    @Test fun easyJogHasFlightForwardKneesAndOpposedArms() {
        val frames=(0 until 100).map {joggingPose(it/100f)}
        frames.forEach {p->listOf(p.nearLeg,p.farLeg).forEach {leg->
            assertEquals(5f,(leg.joint-leg.root).getDistance(),.01f);assertEquals(5f,(leg.end-leg.joint).getDistance(),.01f)
            assertTrue(leg.joint.x>=(leg.root.x+leg.end.x)/2-.01f,"Knee bends forward");assertTrue(leg.end.y<=21.31f,"Foot never sinks below the ground")
        }}
        // Jogging, not walking: at some moment both feet are off the ground; and never both planted.
        assertTrue(frames.any {it.nearLeg.end.y<21.2f && it.farLeg.end.y<21.2f})
        assertTrue(frames.none {it.nearLeg.end.y>21.29f && it.farLeg.end.y>21.29f})
        frames.forEach {p->assertTrue((p.nearArm.joint.x-p.shoulder.x)*(p.farArm.joint.x-p.shoulder.x)<=.01f,"Elbows swing opposite ways");assertTrue(p.head.y<p.shoulder.y && p.shoulder.x>p.hip.x)}
        assertTrue(frames.map {it.hip.y}.let {it.max()-it.min()} in .3f..1.2f)
        val start=joggingPose(0f);val end=joggingPose(1f);assertEquals(start.nearLeg.end.x,end.nearLeg.end.x,.01f);assertEquals(start.hip.y,end.hip.y,.01f)
        val sheet=java.awt.image.BufferedImage(960,384,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
        try {g.color=java.awt.Color.WHITE;g.fillRect(0,0,960,384);(0..9).forEach {i->g.drawImage(render("activity",i/52f,true).getScaledInstance(192,192,java.awt.Image.SCALE_SMOOTH),(i%5)*192,(i/5)*192,null)}}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/jog-cycle.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun exportWalkingCycle() {
        val frames=(0..19).map {render("activity",it/80f,true)}
        val sheet=java.awt.image.BufferedImage(480,384,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g=sheet.createGraphics()
        try{g.color=java.awt.Color.WHITE;g.fillRect(0,0,480,384);frames.forEachIndexed {i,frame->g.drawImage(frame,(i%5)*96,(i/5)*96,null)}}finally{g.dispose()}
        val dir=java.io.File("build/review").apply {mkdirs()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File(dir,"walking-cycle.png")))
        val writer=javax.imageio.ImageIO.getImageWritersByFormatName("gif").next()
        javax.imageio.ImageIO.createImageOutputStream(java.io.File(dir,"walking-forward.gif")).use {out->
            writer.output=out;writer.prepareWriteSequence(null)
            frames.forEachIndexed {i,frame->
                val metadata=writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier.createFromRenderedImage(frame),null)
                val root=javax.imageio.metadata.IIOMetadataNode("javax_imageio_gif_image_1.0")
                root.appendChild(javax.imageio.metadata.IIOMetadataNode("GraphicControlExtension").apply {
                    setAttribute("disposalMethod","none");setAttribute("userInputFlag","FALSE");setAttribute("transparentColorFlag","FALSE");setAttribute("delayTime","5");setAttribute("transparentColorIndex","0")
                })
                if(i==0)root.appendChild(javax.imageio.metadata.IIOMetadataNode("ApplicationExtensions").apply {
                    appendChild(javax.imageio.metadata.IIOMetadataNode("ApplicationExtension").apply {setAttribute("applicationID","NETSCAPE");setAttribute("authenticationCode","2.0");userObject=byteArrayOf(1,0,0)})
                })
                metadata.mergeTree("javax_imageio_gif_image_1.0",root)
                writer.writeToSequence(javax.imageio.IIOImage(frame,null,metadata),null)
            }
            writer.endWriteSequence()
        };writer.dispose()
    }
    private fun render(name:String,phase:Float=0f,animated:Boolean=false,round:Float=2f):java.awt.image.BufferedImage {
        val bitmap=ImageBitmap(96,96)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(96f,96f)) {
            drawRect(Color.White)
            scale(4f,4f,pivot=Offset.Zero){drawFilledSymbol(name,iconColor(name,false,0xFF1B7F5CL),round,phase,animated)}
        }
        val pixels=IntArray(96*96);bitmap.readPixels(pixels)
        return java.awt.image.BufferedImage(96,96,java.awt.image.BufferedImage.TYPE_INT_ARGB).apply {setRGB(0,0,96,96,pixels,0,96)}
    }
    private fun pixels(image:java.awt.image.BufferedImage)=image.getRGB(0,0,96,96,null,0,96).toList()
    @Test fun semanticFilledSymbolsAndAnimationFrames() {
        assertEquals(setOf("today","food","water","activity","progress","notes"),sectionColors.keys)
        assertEquals(6,sectionColors.values.distinct().size)
        sectionColors.keys.forEach {name->
            assertTrue(pixels(render(name)).count {it!=Color.White.toArgb()}>500,name)
            assertNotEquals(pixels(render(name,.1f,true)),pixels(render(name,.2f,true)),name)
            assertEquals(pixels(render(name,.1f,false)),pixels(render(name,.7f,false)),"Static $name")
            listOf(false,true).forEach {dark->assertTrue(colorContrast(iconColor(name,dark,0xFF1B7F5CL),if(dark)Color(0xFF211F26)else Color.White)>=4.5f)}
        }
    }
    @Test fun topAndRightNavigationUseAllSectionsInSameOrder(){
        assertEquals(listOf("today","food","water","activity","progress","notes"),mainSections.map {it.first})
        assertEquals(sectionColors.keys.toList(),mainSections.map {it.first})
        mainSections.forEach {(id,label)->assertEquals(id,appDestinations.getValue(id).screen);languageNames.keys.forEach {assertTrue(noteNavigationText(label,it).isNotBlank())}}
    }
    @Test fun namedStylesRemainVoluntaryAndPortable() {
        assertEquals(7,appearanceStyles.size)
        appearanceStyles.forEach {(id,_)->val appearance=applyStyle(Appearance(),id);validateLocal(LocalData(appearance=appearance));assertEquals(id,appearance.style)}
        assertNotEquals(pixels(render("water",round=3f)),pixels(render("water",round=2f)))
    }
    @Test fun exportReviewSheet() {
        val names=sectionColors.keys.toList()
        val sheet=java.awt.image.BufferedImage(744,570,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics=sheet.createGraphics()
        try {
            graphics.color=java.awt.Color.WHITE;graphics.fillRect(0,0,744,570);graphics.color=java.awt.Color.DARK_GRAY;graphics.font=java.awt.Font("SansSerif",java.awt.Font.PLAIN,14)
            names.forEachIndexed {col,name->graphics.drawString(name,col*124+10,20);listOf(.0f,.2f,.45f,.7f).forEachIndexed {row,phase->graphics.drawImage(render(name,phase,true),col*124+12,row*112+30,null)}}
            graphics.drawString("Soft filled icon",14,512);graphics.drawImage(render("water",round=3f),215,468,null)
        }finally{graphics.dispose()}
        val file=java.io.File("build/review/filled-scenes.png");file.parentFile.mkdirs();assertTrue(javax.imageio.ImageIO.write(sheet,"png",file));println("REVIEW_SHEET ${file.absolutePath}")
    }
    @Test fun windowsRendererPreferenceAndOverrides() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-render-test-").toFile()
        val previous=System.getProperty("skiko.renderApi")
        try {
            assertEquals("AUTO",storedRenderMode(root))
            storeRenderMode(root,"OPENGL");assertEquals("OPENGL",storedRenderMode(root))
            assertFailsWith<IllegalArgumentException>{storeRenderMode(root,"UNKNOWN")}
            java.io.File(root,"renderer-mode.txt").writeText("invalid");assertEquals("AUTO",storedRenderMode(root))
            System.setProperty("skiko.renderApi","DIRECT3D");configureWindowRendering(root);assertEquals("DIRECT3D",System.getProperty("skiko.renderApi"))
            System.clearProperty("skiko.renderApi");configureWindowRendering(root)
            // Automatic: nothing is forced, the drawing library takes Direct3D and falls back by itself.
            if(System.getenv("SKIKO_RENDER_API")==null){assertNull(System.getProperty("skiko.renderApi"));assertEquals(org.jetbrains.skiko.GraphicsApi.DIRECT3D,org.jetbrains.skiko.SkikoProperties.renderApi)}
            storeRenderMode(root,"SOFTWARE_FAST");configureWindowRendering(root)
            if(System.getenv("SKIKO_RENDER_API")==null)assertEquals("SOFTWARE_FAST",System.getProperty("skiko.renderApi"))
        }finally{if(previous==null)System.clearProperty("skiko.renderApi")else System.setProperty("skiko.renderApi",previous);root.deleteRecursively()}
    }
    @Test fun rendererLabelsTenLanguages() {
        languageNames.keys.forEach {language->listOf("Window rendering","Compatibility","Restart FitEasy to apply").forEach {assertNotNull(appearanceTranslation(it,language))}}
    }
}
