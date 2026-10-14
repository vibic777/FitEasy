package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlin.test.*
import kotlin.math.*
import kotlinx.serialization.decodeFromString
import java.time.Instant

class PedometerJournalExportTest {
    private fun trace(seconds:Int,signal:(Double)->Float):Int {
        val detector=SoftwareSteps();return (0..seconds*25).sumOf {i->detector.sample(1_000_000_000L+i*40_000_000L,0f,0f,signal(i/25.0))}
    }
    @Test fun stationaryAndSlowMotionDoNotCount(){assertEquals(0,trace(20){9.81f});assertEquals(0,trace(20){(9.81+2*sin(it*.2*2*PI)).toFloat()})}
    @Test fun rhythmicWalkingCountsAfterConfirmation(){val n=trace(12){(9.81+3*sin(it*1.6*2*PI)).toFloat()};assertTrue(n in 14..21,"$n")}
    @Test fun isolatedPulseAndInvalidSamplesDoNotCount(){assertEquals(0,trace(10){if(it in 2.0..2.2)13f else 9.81f});val d=SoftwareSteps();assertEquals(0,d.sample(1,Float.NaN,0f,9.81f));assertEquals(0,d.sample(-1,0f,0f,9.81f));assertEquals(0,d.sample(2,0f,0f,9.81f));assertEquals(0,d.sample(1,0f,0f,20f))}
    @Test fun backendSwitchPreservesDailyTotalAndTombstones(){
        val first=recordDetectedSteps(Snapshot(),"2026-10-02","test",10)
        val baseline=recordCounter(first,CounterReading(500,1,"2026-10-02",1),"test")
        val next=recordCounter(baseline,CounterReading(505,2,"2026-10-02",1),"test")
        assertEquals(15.0,next.shared.entries.single().amount)
        val row=next.shared.entries.single();val removed=next.copy(shared=next.shared.copy(entries=listOf(row.copy(deleted=1))))
        val fresh=recordCounter(removed,CounterReading(507,3,"2026-10-02",1),"test")
        assertEquals(1,fresh.shared.entries.count {it.deleted!=null});assertEquals(2.0,fresh.shared.entries.single {it.deleted==null}.amount)
    }
    @Test fun textLogAndStructuredJsonExport(){
        val text="2026-10-02T12:00:00Z ERROR steps.test | java.lang.Exception Class.method:10\nнеполная строка\n"
        assertEquals(text,String(journalExport(text,JournalExportType.TXT),Charsets.UTF_8));assertEquals(text,String(journalExport(text,JournalExportType.LOG),Charsets.UTF_8))
        val exported=codec.decodeFromString<ExportedJournal>(String(journalExport(text,JournalExportType.JSON,Instant.parse("2026-10-02T13:00:00Z"))))
        assertEquals("fiteasy-journal",exported.format);assertEquals(1,exported.version);assertEquals(2,exported.entries.size);assertEquals("ERROR",exported.entries[0].level);assertEquals("steps.test",exported.entries[0].event);assertEquals("неполная строка",exported.entries[1].raw)
    }
    @Test fun newLabelsHaveTenLanguages(){assertEquals(10,stepRows.size);stepRows.forEach {(lang,row)->assertEquals(stepKeys.size,row.split('|').size,lang)}}
    @Test fun launcherArtworkHasTransparentCornersAndWhiteHeart(){
        val file=java.io.File("src/jvmMain/resources/icons/fiteasy.png")
        val image=javax.imageio.ImageIO.read(file)
        listOf(0 to 0,0 to image.height-1,image.width-1 to 0,image.width-1 to image.height-1).forEach {(x,y)->assertEquals(0,image.getRGB(x,y) ushr 24)}
        assertTrue((0 until image.height).any {y->(0 until image.width).any {x->image.getRGB(x,y)==-1}})
        val xml=java.io.File("../app/src/main/res/drawable/ic_fiteasy.xml").readText()
        assertTrue(xml.contains("#FFFFFF"));assertFalse(xml.contains("M0,0"))
    }
}
