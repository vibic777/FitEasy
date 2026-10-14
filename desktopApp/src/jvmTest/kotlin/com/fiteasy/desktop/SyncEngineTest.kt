package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.sync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlin.test.*

/** Two FitEasy installations on this computer pair with a code and sync through the engine taken from Notio (D105). */
class SyncEngineTest {
    private class Device(name:String) {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-sync-$name-").toFile()
        val platform:DesktopPlatform
        init {val previous=System.getProperty("fiteasy.home");System.setProperty("fiteasy.home",root.absolutePath);platform=DesktopPlatform();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous)}
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val store=SyncStore(platform).also {it.deviceName=name}
        var imports=0
        val engine=SyncEngine(store,null,FitSyncData(platform){imports++},java.io.File(root,"incoming"),scope)
        /** The person on this device answers whatever is asked: allows it, or refuses while [allow] is off. */
        @Volatile var allow=true
        @Volatile var asked=0
        fun allowEverything()=scope.launch {engine.prompt.filterNotNull().collect {asked++;it.answer.complete(allow)}}
        fun close(){engine.stopListening();scope.cancel();platform.close();root.deleteRecursively()}
    }
    private fun entry(name:String,updated:Long,amount:Double=1.0,id:String=newId())=Entry(id=id,type="food",name=name,amount=amount,unit="g",updated=updated)

    @Test fun pairOnceThenReplaceMergeAndUnpair()=runBlocking {
        val a=Device("A");val b=Device("B")
        try {
            a.platform.save(a.platform.load().let {it.copy(shared=it.shared.copy(entries=listOf(entry("a1",100,id="shared-1"),entry("a2",100)),notes=listOf(Note(title="from A",updated=100))))})
            b.platform.save(b.platform.load().let {it.copy(shared=it.shared.copy(entries=listOf(entry("b-old",50)),profile=Profile(age=40)),local=it.local.copy(reminders=listOf(Reminder(name="stays on B"))))})
            a.engine.startListening();a.allowEverything()
            val port=a.engine.listening.value!!.port
            // Not waiting with a code: pairing is refused; a wrong code is refused too.
            assertTrue(b.engine.pair("127.0.0.1",port,"AAAA-BBBB-CCCC").isFailure)
            a.engine.startPairing();val code=a.engine.pairingCode.value!!
            assertTrue(b.engine.pair("127.0.0.1",port,if(code.first()=='A')"B"+code.drop(1) else "A"+code.drop(1)).isFailure)
            val pairing=b.engine.pair("127.0.0.1",port,a.engine.pairingCode.value!!).getOrThrow()
            assertEquals("A",pairing.peerName);assertEquals(1,a.store.pairings.value.size);assertEquals("B",a.store.pairings.value.single().peerName);assertNull(a.engine.pairingCode.value)
            // The pairing survives a restart of the page: it is read back from the device secret.
            assertEquals(pairing.id,SyncStore(b.platform).pairings.value.single().id)

            // Replace needs one direction: both ways is refused before anything is sent.
            assertTrue(b.engine.sync(pairing.id,SyncDirection.BOTH,ImportMode.REPLACE,null).isFailure)
            assertTrue(b.engine.sync(pairing.id,SyncDirection.BOTH,ImportMode.REPLACE,setOf("notes")).isFailure);assertTrue(b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.REPLACE,emptySet()).isFailure)
            assertEquals(1,b.platform.load().shared.entries.size)

            // B becomes a copy of A: with every section chosen the reminders are a section too (D108).
            val replaced=b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.REPLACE,null).getOrThrow()
            assertNotNull(replaced.received);assertNull(replaced.sent)
            assertEquals(a.platform.load().shared,b.platform.load().shared);assertTrue(b.platform.load().local.reminders.isEmpty());assertEquals(1,b.imports)
            // Reminders alone, as one more section: they arrive without the melody file of the other device; other sections leave them alone.
            a.platform.mutateSnapshot {s->s.copy(local=s.local.copy(reminders=listOf(Reminder(id="r-a",name="from A",soundUri="C:\\melody.wav",volumePercent=40))))}
            b.platform.mutateSnapshot {s->s.copy(local=s.local.copy(reminders=listOf(Reminder(id="r-b",name="own of B"))))}
            b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.MERGE,setOf("reminders")).getOrThrow()
            assertEquals(setOf("r-a","r-b"),b.platform.load().local.reminders.map {it.id}.toSet());assertNull(b.platform.load().local.reminders.first {it.id=="r-a"}.soundUri);assertEquals(40,b.platform.load().local.reminders.first {it.id=="r-a"}.volumePercent)
            b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.MERGE,setOf("notes")).getOrThrow();assertEquals(2,b.platform.load().local.reminders.size)
            // A reminder changed on A after that reaches B by a merge, and B keeps the melody it chose for it itself (D113).
            b.platform.mutateSnapshot {s->s.copy(local=s.local.copy(reminders=s.local.reminders.map {if(it.id=="r-a")it.copy(soundUri="C:\\own-of-b.wav")else it}))}
            a.platform.mutateSnapshot {s->s.copy(local=s.local.copy(reminders=s.local.reminders.map {it.copy(name="renamed on A",time="07:30",updated=500)}))}
            val changed=b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.MERGE,setOf("reminders")).getOrThrow()
            b.platform.load().local.reminders.first {it.id=="r-a"}.let {assertEquals("renamed on A",it.name);assertEquals("07:30",it.time);assertEquals("C:\\own-of-b.wav",it.soundUri)}
            assertEquals(listOf(0,1),changed.received!!.subList(2,4),"Nothing new, one reminder updated")
            assertEquals(listOf(0,0),b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.MERGE,setOf("reminders")).getOrThrow().received!!.subList(2,4),"The same again changes nothing")
            // Replacement of chosen sections only (D113): the notes and reminders of B become those of A, its diary stays.
            b.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(notes=listOf(Note(title="only on B",updated=900)),entries=s.shared.entries+entry("b-keeps",120)))}
            b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.REPLACE,setOf("notes","reminders")).getOrThrow()
            assertEquals(listOf("from A"),b.platform.load().shared.notes.map {it.title});assertTrue(b.platform.load().shared.entries.any {it.name=="b-keeps"},"Another section is left alone")
            assertEquals(listOf("r-a"),b.platform.load().local.reminders.map {it.id});assertEquals(a.platform.load().shared.entries.size+1,b.platform.load().shared.entries.size)
            // The other way: A takes the diary of B and nothing else.
            b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.REPLACE,setOf("nutrition")).getOrThrow()
            assertEquals(b.platform.load().shared.entries.map {it.id}.toSet(),a.platform.load().shared.entries.map {it.id}.toSet());assertEquals(listOf("from A"),a.platform.load().shared.notes.map {it.title});assertEquals(1,a.platform.load().local.reminders.size)
            listOf(a,b).forEach {d->d.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries.filterNot {it.name=="b-keeps"}))}}

            // Merge: the later change of the same record wins on both sides; a new record on each side reaches the other.
            b.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries.map {if(it.id=="shared-1")it.copy(amount=9.0,updated=200)else it}+entry("b-new",200)))}
            a.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(entries=s.shared.entries+entry("a-new",150)))}
            val merged=b.engine.sync(pairing.id,SyncDirection.BOTH,ImportMode.MERGE,null).getOrThrow()
            assertEquals(a.platform.load().shared.entries.map {it.id}.toSet(),b.platform.load().shared.entries.map {it.id}.toSet())
            assertEquals(9.0,a.platform.load().shared.entries.first {it.id=="shared-1"}.amount);assertEquals(4,a.platform.load().shared.entries.size)
            assertEquals(listOf(0,0,1,0,3,0).drop(2).take(1),merged.received!!.drop(2).take(1));assertEquals(1,merged.sent!![3],"A took the newer version of one record")

            // Only the chosen sections travel.
            b.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(notes=s.shared.notes+Note(title="B note",updated=300),entries=s.shared.entries+entry("b-private",300)))}
            b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes")).getOrThrow()
            assertTrue(a.platform.load().shared.notes.any {it.title=="B note"});assertTrue(a.platform.load().shared.entries.none {it.name=="b-private"})
            assertEquals(listOf("notes"),b.store.get(pairing.id)!!.folderIds);assertTrue(b.store.get(pairing.id)!!.lastSyncAt>0)

            // What a connection does can be set beforehand (D109): the choice survives a restart and a sync does not reset it.
            assertTrue(pairing.askDirection && pairing.askMode && pairing.askFolders,"A new connection asks everything")
            b.store.update(pairing.id){it.copy(askDirection=false,direction=SyncDirection.RECEIVE,askMode=false,mode="MERGE",askFolders=false,allFolders=false,folderIds=listOf("notes","reminders"))}
            val set=SyncStore(b.platform).get(pairing.id)!!;assertFalse(set.askDirection || set.askMode || set.askFolders);assertEquals(SyncDirection.RECEIVE,set.direction);assertEquals(listOf("notes","reminders"),set.folderIds)
            b.engine.sync(pairing.id,set.direction,ImportMode.valueOf(set.mode),set.folderIds.toSet()).getOrThrow()
            assertFalse(b.store.get(pairing.id)!!.askDirection);assertEquals(listOf("notes","reminders"),b.store.get(pairing.id)!!.folderIds)
            // Confirmation on the other device (D116): a sync started with a choice still open is always asked there, even
            // when the same three are set for the connection on that device.
            val onA=a.store.pairings.value.single();a.allow=false
            assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes")).isFailure,"A asks and its person refuses")
            a.store.update(onA.id){it.copy(askDirection=false,direction=SyncDirection.RECEIVE,askMode=false,mode="MERGE",askFolders=false,allFolders=false,folderIds=listOf("notes"))}
            val before=a.asked
            assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes")).isFailure);assertEquals(before+1,a.asked,"Set on A only: the question is still asked, because B started with a choice open")
            assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.COPIES,setOf("notes")).isFailure,"Another way is asked");assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,null).isFailure,"Other sections are asked")
            assertTrue(b.engine.sync(pairing.id,SyncDirection.RECEIVE,ImportMode.MERGE,setOf("notes")).isFailure,"The other direction is asked");assertEquals(before+4,a.asked)
            a.store.update(onA.id){it.copy(askMode=true)};assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes")).isFailure,"One open choice: asked again")
            // The owner's rule (D111): all three set on the device that starts — the other device does not ask at all,
            // whatever is set there; started from the dialog with a choice still open, it asks.
            a.store.update(onA.id){it.copy(askDirection=true,askMode=true,askFolders=true)};val quiet=a.asked
            b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes"),preset=true).getOrThrow();assertEquals(quiet,a.asked,"Not asked")
            assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.MERGE,setOf("notes")).isFailure);assertEquals(quiet+1,a.asked,"Asked and refused")
            // A replacement set beforehand goes through without a question too, and still only for all sections in one direction.
            b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.REPLACE,null,preset=true).getOrThrow();assertEquals(b.platform.load().shared,a.platform.load().shared)
            assertTrue(b.engine.sync(pairing.id,SyncDirection.BOTH,ImportMode.REPLACE,null,preset=true).isFailure)
            // The owner's example: from this device to the other, every section but the reminders, replacement, nothing left
            // to choose — the other device takes it without a question, and its reminders stay its own.
            val asked=a.asked;val remindersOfA=a.platform.load().local.reminders
            b.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(notes=s.shared.notes+Note(title="set on B",updated=2000)))}
            b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.REPLACE,syncSections-"reminders",preset=true).getOrThrow()
            assertEquals(asked,a.asked,"Not asked");assertEquals(b.platform.load().shared,a.platform.load().shared);assertEquals(remindersOfA,a.platform.load().local.reminders)
            // The same sync with one choice left open is asked on the other device, and refused there it changes nothing.
            b.platform.mutateSnapshot {s->s.copy(shared=s.shared.copy(notes=s.shared.notes+Note(title="not allowed",updated=3000)))}
            assertTrue(b.engine.sync(pairing.id,SyncDirection.SEND,ImportMode.REPLACE,syncSections-"reminders").isFailure);assertEquals(asked+1,a.asked);assertTrue(a.platform.load().shared.notes.none {it.title=="not allowed"})
            a.allow=true
            // Test connection reports every step; removing the pairing here removes it there too.
            val checks=b.engine.test(pairing.id);assertTrue(checks.first {it.step=="handshake"}.ok && checks.first {it.step=="reply"}.ok,checks.toString())
            assertTrue(b.engine.unpair(pairing.id));assertTrue(a.store.pairings.value.isEmpty() && b.store.pairings.value.isEmpty())
            assertTrue(b.store.log.value.isNotEmpty() && b.store.log.value.none {a.engine.pairingCode.value?.let {c->c in it}==true})
        }finally {a.close();b.close()}
    }
    /** The owner's rule (D116), all eight combinations: only "nothing is chosen at the sync" goes without a question on the other device. */
    @Test fun onlyAConnectionWithAllThreeSetGoesWithoutAQuestion() {
        val base=Pairing("p","device","Peer","secret",1)
        for(direction in listOf(false,true))for(mode in listOf(false,true))for(sections in listOf(false,true)) {
            val pairing=base.copy(askDirection=direction,askMode=mode,askFolders=sections)
            assertEquals(!direction && !mode && !sections,pairing.allSetBeforehand,"ask direction=$direction, way=$mode, sections=$sections")
        }
        assertFalse(base.allSetBeforehand,"A new connection asks everything, so the other device asks too")
    }
    /** Always ready (D112): off by default; on, a computer listens while the app runs, with the page closed and the window hidden. */
    @Test fun alwaysReadyListensWithoutThePageOnlyWhenTurnedOn() {
        val root=java.nio.file.Files.createTempDirectory("fiteasy-host-").toFile();val previous=System.getProperty("fiteasy.home")
        System.setProperty("fiteasy.home",root.absolutePath)
        val platform=DesktopPlatform();if(previous==null)System.clearProperty("fiteasy.home")else System.setProperty("fiteasy.home",previous)
        val host=SyncHost(platform)
        try {
            assertFalse(host.always);host.foreground(true);assertFalse(host.engine.isListening,"Nothing listens until the page is opened")
            host.page(true);assertTrue(host.engine.isListening);host.foreground(false);assertFalse(host.engine.isListening,"The page alone stops in the background")
            host.foreground(true);host.page(false);assertFalse(host.engine.isListening)
            host.setAlways(true);assertTrue(host.engine.isListening,"Ready without the page")
            host.foreground(false);assertTrue(host.engine.isListening,"A computer keeps waiting in the tray")
            assertTrue(SyncHost(platform).always,"The choice is kept on the device")
            host.setAlways(false);assertFalse(host.engine.isListening);assertFalse(SyncHost(platform).always)
        }finally {host.close();platform.close();root.deleteRecursively()}
    }
    @Test fun newestWinsCopiesAndReplaceRules() {
        val local=SharedData(entries=listOf(entry("old",100,id="x"),entry("mine",100,id="m")),notes=listOf(Note(id="n",title="kept",updated=500)))
        val incoming=selected(SharedData(entries=listOf(entry("new",200,id="x"),entry("theirs",100,id="t")),notes=listOf(Note(id="n",title="older",updated=400,deleted=400))),syncSections)
        val (merged,counts)=applySync(local,incoming,ImportMode.MERGE)
        assertEquals("new",merged.entries.first {it.id=="x"}.name);assertEquals(3,merged.entries.size);assertEquals("kept",merged.notes.single().title)
        assertEquals(SyncCounts(added=1,updated=1,kept=1),counts)
        // A deletion made later travels as a newer version of the record.
        val deleted=selected(SharedData(notes=listOf(Note(id="n",title="kept",updated=600,deleted=600))),setOf("notes"))
        assertNotNull(applySync(local,deleted,ImportMode.MERGE).first.notes.single().deleted)
        assertEquals(4,applySync(local,incoming,ImportMode.COPIES).first.entries.size)
        assertEquals(incoming.data,applySync(local,incoming,ImportMode.REPLACE).first)
        // Replacement of chosen sections (D113): only they become what was received; in one direction only.
        applySync(local,selected(SharedData(),setOf("notes")),ImportMode.REPLACE).first.let {assertTrue(it.notes.isEmpty());assertEquals(local.entries,it.entries)}
        val base=SharedData(entries=listOf(entry("meal",100,id="e1").copy(foodId="f-mine"),Entry(id="w1",type="drink",name="water",amount=1.0,unit="ml",updated=100)),products=listOf(Food(id="f-mine",name="Mine",kind="product"),Food(id="f-idle",name="Idle",kind="product")),notes=listOf(Note(id="n",title="kept",updated=500)),profile=Profile(age=50),goals=Goals(steps=9000))
        val part=selected(SharedData(entries=listOf(Entry(id="w2",type="drink",name="tea",amount=2.0,unit="ml",updated=100),entry("not chosen",100,id="e2")),products=listOf(Food(id="f-theirs",name="Theirs",kind="product")),notes=listOf(Note(id="n2",title="theirs",updated=1)),profile=Profile(age=33),goals=Goals(steps=1)),setOf("water","notes","catalogues","profile"))
        val replaced=applySync(base,part,ImportMode.REPLACE).first
        assertEquals(setOf("e1","w2"),replaced.entries.map {it.id}.toSet());assertEquals(listOf("theirs"),replaced.notes.map {it.title});assertEquals(33,replaced.profile.age);assertEquals(9000,replaced.goals.steps)
        assertEquals(setOf("f-theirs","f-mine"),replaced.products.map {it.id}.toSet(),"The catalogue is theirs, plus the product a record kept here still names")
        assertEquals(setOf("f-mine","f-idle","f-theirs"),replaceSections(base,part.data.copy(notes=emptyList()),setOf("water")).products.map {it.id}.toSet(),"Without the catalogue section nothing is removed from it")
        assertTrue(SyncEngine.allowed(SyncDirection.SEND,ImportMode.REPLACE,listOf("notes")) && SyncEngine.allowed(SyncDirection.RECEIVE,ImportMode.REPLACE,null));assertFalse(SyncEngine.allowed(SyncDirection.BOTH,ImportMode.REPLACE,null) || SyncEngine.allowed(SyncDirection.SEND,ImportMode.MERGE,emptyList()))
        // The profile comes only while this device still has the default one.
        val profile=selected(SharedData(profile=Profile(age=33)),setOf("profile"))
        assertEquals(33,applySync(SharedData(),profile,ImportMode.MERGE).first.profile.age);assertEquals(50,applySync(SharedData(profile=Profile(age=50)),profile,ImportMode.MERGE).first.profile.age)
        // Reminders: merge adds the unknown ones, copies get new ids, replace takes the list, nothing carried changes nothing.
        val mine=listOf(Reminder(id="a",name="mine"),Reminder(id="row",name="bound",scheduleId="s",rowId="r"));val theirs=listOf(Reminder(id="a",name="theirs"),Reminder(id="b",name="new"),Reminder(id="row2",name="same row",scheduleId="s",rowId="r"))
        assertEquals(listOf("mine","bound","new"),mergeReminders(mine,theirs,ImportMode.MERGE).map {it.name});assertEquals(mine,mergeReminders(mine,null,ImportMode.REPLACE))
        assertEquals(listOf("theirs","new","same row"),mergeReminders(mine,theirs,ImportMode.REPLACE).map {it.name})
        // The reminder changed later wins a merge and keeps the melody of this device; an older one changes nothing (D113).
        mergeReminders(listOf(Reminder(id="a",name="mine",updated=10,soundUri="C:\\m.wav")),listOf(Reminder(id="a",name="theirs",updated=20)),ImportMode.MERGE).single().let {assertEquals("theirs",it.name);assertEquals("C:\\m.wav",it.soundUri)}
        assertEquals("mine",mergeReminders(listOf(Reminder(id="a",name="mine",updated=30)),listOf(Reminder(id="a",name="theirs",updated=20)),ImportMode.MERGE).single().name)
        assertEquals(0,codec.decodeFromString<Reminder>("""{"name":"old"}""").updated)
        val copies=mergeReminders(mine,theirs,ImportMode.COPIES);assertEquals(4,copies.size);assertEquals(4,copies.map {it.id}.distinct().size)
        assertTrue("reminders" in syncSections);assertNull(selected(SharedData(),syncSections).reminders)
        val quiet=Snapshot(local=LocalData(reminders=mine.take(1),remindersOff=true));assertTrue(effectiveReminders(quiet).none {it.enabled});assertTrue(effectiveReminders(quiet.copy(local=quiet.local.copy(remindersOff=false))).single().enabled);assertTrue(quiet.local.reminders.single().enabled)
        assertEquals("192.168.1.20",SyncAddress.host(" 192.168.1.20 "));assertNull(SyncAddress.host("300.1.1.1"));assertEquals("laptop.local",SyncAddress.host("laptop.local"));assertNull(SyncAddress.port("70000"))
    }

    @Test fun thePortIsChosenAutomaticallyOrSetByHandAndATakenOneIsSaid()=runBlocking {
        val a=Device("PortA")
        // Another program on two ports: the usual one and the one the person will type.
        val usual=runCatching {java.net.ServerSocket(SyncEngine.DEFAULT_PORT)}.getOrNull()
        val other=java.net.ServerSocket(0);val taken=other.localPort
        try {
            assertEquals(0,a.store.port);assertFalse(SyncEngine.portFree(taken));assertFalse(SyncEngine.portFree(80));assertFalse(SyncEngine.validPort(1023));assertTrue(SyncEngine.validPort(1024) && SyncEngine.validPort(65535) && !SyncEngine.validPort(65536))
            // Automatic: the usual port is taken, a free one is used, and that is said.
            a.engine.startListening()
            val automatic=a.engine.listening.value!!.port
            if(usual!=null){assertNotEquals(SyncEngine.DEFAULT_PORT,automatic);assertEquals(SyncEngine.DEFAULT_PORT,a.engine.portBusy.value)}
            languageNames.keys.forEach {language->
                val (line,problem)=portStatus(0,automatic,a.engine.portBusy.value,language)
                assertTrue(automatic.toString() in line && !problem && "{" !in line,language)
                if(language !in setOf("ru","be"))assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(line),"$language: $line")
            }
            a.engine.stopListening()
            // By hand, taken: the device does not listen and the page says which port is taken.
            a.store.port=taken;assertEquals(taken,a.store.port)
            a.engine.startListening()
            assertNull(a.engine.listening.value);assertFalse(a.engine.isListening);assertEquals(taken,a.engine.portBusy.value)
            assertTrue(portStatus(taken,null,taken,"ru").let {it.second && taken.toString() in it.first})
            // By hand, free: exactly that port, kept over a restart of the store.
            other.close();assertTrue(SyncEngine.portFree(taken))
            a.engine.stopListening();a.engine.startListening()
            assertEquals(taken,a.engine.listening.value!!.port);assertNull(a.engine.portBusy.value);assertEquals(taken,SyncStore(a.platform).port)
            assertFalse(SyncEngine.portFree(taken),"The port this app waits on is taken too")
            assertEquals(false,portStatus(taken,taken,null,"en").second)
            // What the dialog says for a typed port, in every language.
            languageNames.keys.forEach {language->
                val lines=listOf(portCheckText("12",taken,null,language),portCheckText(taken.toString(),taken,null,language),portCheckText("50001",taken,null,language),portCheckText("50001",taken,true,language),portCheckText("50001",taken,false,language))
                assertEquals(listOf(true,false,false,false,true),lines.map {it.second},language);assertEquals(5,lines.map {it.first}.distinct().size,language)
                if(language !in setOf("ru","be"))lines.forEach {assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(it.first),"$language: ${it.first}")}
                listOf("Порт этого устройства","Автоматически","Вручную").forEach {if(language!="ru")assertNotEquals(it,translate(it,language),"$language/$it")}
                assertTrue(helpTree(language).first {it.id=="sync"}.sections.last().body.contains("47821"),language)
            }
            // A value that is not a port is not kept: automatic again.
            a.store.port=80;assertEquals(0,a.store.port)
        } finally {a.close();runCatching {other.close()};runCatching {usual?.close()}}
    }
}
