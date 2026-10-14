package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import androidx.compose.ui.graphics.luminance
import kotlin.test.*

class PerformanceAppearanceTest {
    @Test fun windowsNoOpPersistsAcrossReopenWithoutRewrite() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-synthetic-").toFile()
        val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        try {
            val value=Snapshot(shared=SharedData(entries=listOf(Entry(type="activity",name="Synthetic",amount=5.0,unit="min"))))
            val platform=DesktopPlatform()
            try {
                platform.save(value)
                val file=java.io.File(root,"fiteasy.db")
                java.nio.file.Files.setLastModifiedTime(file.toPath(),java.nio.file.attribute.FileTime.fromMillis(42000))
                platform.save(value)
                assertEquals(42000L,file.lastModified())
                assertSame(value,platform.load())
            }finally{platform.close()}
            val reopened=DesktopPlatform()
            try {assertEquals(value,reopened.load())}finally{reopened.close()}
        }finally {if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun paletteTextContrast() {
        (fitPalettes+vividPalettes).forEach {accent->listOf(false,true).forEach {dark->val colors=fitColorScheme(accent,dark);assertTrue(colorContrast(colors.primary,colors.surface)>=4.5f,"$accent / $dark");assertTrue(colorContrast(colors.primary,colors.onPrimary)>=4.5f)}}
    }
    @Test fun cachePublishesOnlyCommittedState() {
        val cache=CommittedState("old")
        assertFailsWith<IllegalStateException>{cache.commit("new"){error("failed transaction")}}
        assertEquals("old",cache.value)
        var writes=0
        assertFalse(cache.commit("old"){writes++})
        assertTrue(cache.commit("new"){assertEquals("old",cache.value);writes++})
        assertEquals(1,writes);assertEquals("new",cache.value)
        cache.invalidate();assertEquals("disk",cache.read {"disk"});assertEquals("disk",cache.read {error("unnecessary read")})
    }
    @Test fun counterBaselineDuplicatesAndBatches() {
        val baseline=recordCounter(Snapshot(),CounterReading(100,1,"2026-10-02",1),"test",1)
        assertEquals(0.0,baseline.shared.entries.single().amount)
        assertSame(baseline,recordCounter(baseline,CounterReading(100,2,"2026-10-02",1),"test",2))
        val sequential=(101L..150L).fold(baseline){s,n->recordCounter(s,CounterReading(n,n,"2026-10-02",1),"test",10)}
        val batched=recordCounter(baseline,CounterReading(150,150,"2026-10-02",1),"test",10)
        assertEquals(sequential,batched);assertEquals(50.0,batched.shared.entries.single().amount)
    }
    @Test fun counterDayBoundaryAndReboot() {
        val old=recordCounter(Snapshot(),CounterReading(100,100,"2026-10-01",1),"test",1)
        val next=recordCounter(old,CounterReading(110,200,"2026-10-02",1),"test",2)
        assertEquals(10.0,next.shared.entries.single {it.source.startsWith("pending:")}.amount)
        val reboot=recordCounter(next,CounterReading(5,1,"2026-10-02",2),"test",3)
        assertEquals(5.0,reboot.shared.entries.single {it.date=="2026-10-02" && it.source.startsWith("phone:")}.amount)
        validate(reboot.shared);validateLocal(reboot.local)
    }
    @Test fun invalidSensorValuesRejected() {
        listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f).forEach {assertNull(counterReading(it,1,"2026-10-02",1))}
        assertNull(counterReading(1f,-1,"2026-10-02",1));assertNull(counterReading(1f,1,"2026-10-02",-1))
    }
    @Test fun styleTonesAreCleanReadableAndRenderOnASheet() {
        assertEquals(appearanceStyles.keys-"calm",styleTones.keys)
        fun saturation(c:androidx.compose.ui.graphics.Color):Float {val hi=maxOf(c.red,c.green,c.blue);val lo=minOf(c.red,c.green,c.blue);return if(hi==0f)0f else (hi-lo)/hi}
        val sheet=java.awt.image.BufferedImage(140*styleTones.size,200,java.awt.image.BufferedImage.TYPE_INT_RGB);val g=sheet.createGraphics()
        try {styleTones.entries.forEachIndexed {i,(id,tones)->listOf(false,true).forEachIndexed {row,dark->
            val tone=if(dark)tones.second else tones.first;val scheme=fitColorScheme(applyStyle(Appearance(),id).accent,dark)
            val page=androidx.compose.ui.graphics.Color(tone[0]);val card=androidx.compose.ui.graphics.Color(tone[1]);val soft=androidx.compose.ui.graphics.Color(tone[2])
            // A tinted page, not grey fog: clearly light or clearly dark, with visible colour.
            if(dark)assertTrue(page.luminance()<.03f,id)else {assertTrue(page.luminance()>.8f,id);assertTrue(saturation(soft)>.1f,id)}
            listOf(page,card,soft).forEach {surface->assertTrue(colorContrast(scheme.onSurface,surface)>=7f,"$id text");assertTrue(colorContrast(scheme.primary,surface)>=3f,"$id accent on ${if(dark)"dark" else "light"}")}
            fun awt(c:androidx.compose.ui.graphics.Color)=java.awt.Color(c.red,c.green,c.blue)
            g.color=awt(page);g.fillRect(i*140,row*100,140,100);g.color=awt(card);g.fillRoundRect(i*140+10,row*100+24,120,30,16,16);g.color=awt(soft);g.fillRoundRect(i*140+10,row*100+60,120,30,16,16)
            g.color=awt(scheme.onSurface);g.drawString(id,i*140+12,row*100+16);g.drawString("Card text",i*140+18,row*100+44);g.color=awt(scheme.primary);g.fillOval(i*140+100,row*100+66,18,18)
        }}}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/style-tones.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun stepWritesFollowScreenState() {
        assertEquals(2_000L,stepFlushDelay(true,false));assertEquals(2_000L,stepFlushDelay(true,true))
        assertEquals(500L,stepFlushDelay(false,false));assertEquals(30_000L,stepFlushDelay(false,true))
        assertTrue(STEP_BATCH_MICROS>=30_000_000 && SOFTWARE_IDLE_MILLIS in 60_000L..600_000L)
    }
    @Test fun windowsHeldConnectionSurvivesEncryptionChangeAndWipe() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-synthetic-").toFile()
        val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        try {
            val platform=DesktopPlatform()
            try {
                val first=Snapshot(shared=SharedData(entries=listOf(Entry(type="activity",name="Synthetic",amount=5.0,unit="min"))))
                platform.save(first)
                platform.setEncryption(true);assertTrue(platform.encryptionEnabled());assertEquals(first,platform.load())
                val second=first.copy(shared=first.shared.copy(entries=first.shared.entries+Entry(type="drink",name="Synthetic",amount=200.0,unit="ml")))
                platform.save(second);platform.save(second.copy(local=second.local.copy(trashDays=7)))
                platform.setEncryption(false);assertFalse(platform.encryptionEnabled())
                assertEquals(7,platform.load().local.trashDays);assertEquals(2,platform.load().shared.entries.size)
                platform.wipeLocal();assertFalse(java.io.File(root,"fiteasy.db").exists())
                assertEquals(SqlStateStore.newInstallation,platform.load())
            }finally{platform.close()}
            assertTrue(root.deleteRecursively())
        }finally {if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous);root.deleteRecursively()}
    }
    @Test fun themesAndLegacyAppearance() {
        assertEquals(7,appearanceStyles.size)
        appearanceStyles.keys.forEach {val a=applyStyle(Appearance(),it);validateLocal(LocalData(appearance=a));assertEquals(a,codec.decodeFromString<Appearance>(codec.encodeToString(a)))}
        val legacy=codec.decodeFromString<Appearance>("{}");assertEquals("calm",legacy.style);assertFalse(legacy.animations)
    }
    @Test fun rgbBoundsAndChannels() {
        assertEquals(0xFF12ABFFL,rgbColor("18","171","255"));assertNull(rgbColor("256","0","0"));assertNull(rgbColor("-1","0","0"));assertNull(rgbColor("","0","0"))
    }
    @Test fun selectedBackgroundsDeduplicateAndKeepOthers() {
        val image=java.util.Base64.getEncoder().encodeToString(byteArrayOf(1,2,3))
        val a=applyBackground(Appearance(),setOf("profile","settings"),{it.copy(color=0xFFFF1684L)},image)
        assertEquals(1,a.backgroundImages.size);assertEquals(a.sectionBackground("profile"),a.sectionBackground("settings"))
        assertNull(a.sectionBackground("today").color)
        val b=applyBackground(a,setOf("profile"),{it.copy(image=null,color=0xFF375BFFL)})
        assertEquals(1,b.backgroundImages.size);assertEquals(image,b.backgroundBytes("settings"))
        val c=applyBackground(b,setOf("settings"),{it.copy(image=null)})
        assertTrue(c.backgroundImages.isEmpty());assertEquals(0xFF375BFFL,c.sectionBackground("profile").color)
        assertFailsWith<IllegalArgumentException>{applyBackground(c,emptySet(),{it})}
    }
    @Test fun backgroundValidationRejectsDanglingAndOversize() {
        assertFailsWith<IllegalArgumentException>{validateLocal(LocalData(appearance=Appearance(sectionBackgrounds=mapOf("profile" to SectionBackground(image="missing")))))}
        assertFailsWith<IllegalArgumentException>{validateLocal(LocalData(appearance=Appearance(sectionBackgrounds=mapOf("profile" to SectionBackground(alpha=Float.NaN)))))}
        val image=java.util.Base64.getEncoder().encodeToString(ByteArray(1048577))
        assertFailsWith<IllegalArgumentException>{applyBackground(Appearance(),setOf("today"),{it},image)}
    }
    @Test fun tenLanguageAppearanceDictionary() {
        assertEquals(languageNames.keys,appearanceRows.keys)
        appearanceRows.forEach {(language,row)->assertEquals(appearanceKeys.size,row.split('|').size,language);appearanceKeys.forEach {assertNotNull(appearanceTranslation(it,language))}}
    }
    @Test fun syntheticCacheReadBenchmark() {
        val snapshot=Snapshot(shared=SharedData(entries=List(10000){Entry(type="activity",name="Synthetic",amount=1.0,unit="min",date="2026-10-02")}))
        val json=codec.encodeToString(snapshot)
        repeat(3){codec.decodeFromString<Snapshot>(json)}
        val samples=List(9){val start=System.nanoTime();codec.decodeFromString<Snapshot>(json);System.nanoTime()-start}.sorted()
        val cache=CommittedState(snapshot)
        val start=System.nanoTime();repeat(10000){assertSame(snapshot,cache.read {error("disk")})}
        val elapsed=System.nanoTime()-start
        println("SYNTHETIC entries=10000 json_bytes=${json.toByteArray().size} decode_median_ms=${samples[4]/1e6} cache_10000_reads_ms=${elapsed/1e6}")
    }
}
