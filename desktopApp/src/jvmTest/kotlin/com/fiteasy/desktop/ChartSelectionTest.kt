package com.fiteasy.desktop

import com.fiteasy.shared.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.nio.file.Files
import kotlin.test.*

class ChartSelectionTest {
    @Test fun everyKindPersistsForSingleAndAllWithoutChangingColors() {
        val root=Files.createTempDirectory("fiteasy-chart-test").toFile();val old=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.path)
        try {
            val platform=DesktopPlatform()
            try {
                chartKinds.keys.forEach {kind->
                    val a=platform.load().local.appearance
                    val next=a.withChartStyle("Вода",a.chart("Вода").copy(kind=kind,color=0xFF2266DDL))
                    platform.mutateSnapshot {it.copy(local=it.local.copy(appearance=next))}
                    // The write goes to the disk on its own thread (D125); another reader of the file waits for it.
                    assertTrue(platform.flushSaves())
                    val reopened=DesktopPlatform();try {assertEquals(kind,reopened.load().local.appearance.chart("Вода").kind);assertEquals(0xFF2266DDL,reopened.load().local.appearance.chart("Вода").color)}finally{reopened.close()}
                    val colors=chartMetrics.keys.associateWith {next.chart(it).color}
                    val all=next.withChartKindForAll(kind)
                    platform.mutateSnapshot {it.copy(local=it.local.copy(appearance=all))}
                    chartMetrics.keys.forEach {assertEquals(kind,platform.load().local.appearance.chart(it).kind);assertEquals(colors[it],all.chart(it).color)}
                }
            }finally{platform.close()}
        }finally{if(old==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",old);root.deleteRecursively()}
    }
    @Test fun productionRendererDrawsFourDistinctKinds() {
        val frames=chartKinds.keys.map {kind->
            val bitmap=ImageBitmap(240,140)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(240f,140f)) {
                drawRect(Color.White)
                drawProgressSeries(listOf(2.0,8.0,null,4.0,7.0),ChartStyle(0xFF2266DDL,kind),0.0,10.0,Color.LightGray)
            }
            IntArray(240*140).also {bitmap.readPixels(it)}.toList()
        }
        assertEquals(4,frames.distinct().size)
        assertFails {Appearance().withChartKindForAll("invalid")}
    }
}
