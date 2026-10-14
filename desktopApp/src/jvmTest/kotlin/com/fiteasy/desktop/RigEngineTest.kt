package com.fiteasy.desktop

import com.fiteasy.shared.*
import com.fiteasy.shared.sync.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*
import kotlin.math.abs

class RigEngineTest {
    @Test fun sixTemplatesAndBoneLengths(){
        assertEquals(setOf("plank","pushup","squat","raise","curl","pullup","heelraise","toeraise","armsup","twist","march","sidelegraise","necktilt","sitstand","kneeext"),exerciseCatalogue.map {it.technique}.toSet())
        exerciseCatalogue.forEach {card->val c=rigTemplate(card.technique);validateRig(c);for(t in listOf(0f,.5f,1f,1.5f,2.5f,3f,300f)){
            val sample=rigSample(c,t);rigBones.forEach {(a,b)->assertTrue(abs(rigLength(c.frames.first().pose.points.getValue(a),c.frames.first().pose.points.getValue(b))-rigLength(sample.points.getValue(a),sample.points.getValue(b)))<.02f,"${card.technique}: $a $b")}
        }}
        assertFalse(rigIsMoving(rigTemplate("plank")));assertTrue(rigIsMoving(rigTemplate("curl")))
    }
    @Test fun automaticLayersSwapWithRotationAndManualCorrection(){
        val pose=rigBase().copy(yaw=90f)
        assertTrue(rigDepth(pose,listOf("ls","le"))>rigDepth(pose,listOf("rs","re")))
        val reverse=pose.copy(yaw=-90f)
        assertTrue(rigDepth(reverse,listOf("ls","le"))<rigDepth(reverse,listOf("rs","re")))
        assertTrue(rigDepth(reverse.copy(layers=mapOf("le" to "front")),listOf("ls","le"))>100f)
        val twist=pose.copy(yaw=0f,torsoYaw=90f)
        assertTrue(rigDepth(twist,listOf("ls","le"))>rigDepth(twist,listOf("rs","re")))
    }
    @Test fun interpolationAnglesEndpointsAndStaticSpin(){
        assertEquals(180f,rigAngle(170f,-170f,.5f))
        val c=rigTemplate("curl").copy(loop=false,lockLengths=false)
        assertEquals(c.frames.last().pose.points,rigSample(c,999f).points)
        assertEquals(c.frames.first().pose.points,rigSample(c.copy(loop=true),c.duration).points)
        assertTrue(rigIsMoving(rigTemplate("plank").copy(props=listOf(RigProp(kind="rotation",spin=90f)))))
    }
    @Test fun sideDragAndTwistProjectionAreFinite(){
        listOf(0f,90f,-90f,180f).forEach {yaw->val pose=rigBase().copy(yaw=yaw,torsoYaw=35f);val dragged=rigDrag(pose,"rw",65f,40f);val q=rigProject(rigWorldPoint(dragged,"rw"),yaw);assertTrue(q.x.isFinite());assertEquals(65f,q.x,.01f);assertEquals(40f,q.y)}
    }
    @Test fun pullupContactsHoldAndUnreachableTargetsStayBounded(){
        val c=rigTemplate("pullup")
        val sample=rigSample(c,1.5f)
        listOf("lw","rw").forEach {key->assertTrue(rigLength(sample.points.getValue(key),c.frames.first().pose.points.getValue(key))<.1f)}
        val target=c.frames.first().pose.copy(contacts=mapOf("rw" to RigPoint(100f,100f,100f)))
        val constrained=rigConstrain(c.frames.first().pose,target)
        constrained.points.values.forEach {assertTrue(it.x.isFinite() && it.y.isFinite() && it.z.isFinite())}
    }
    @Test fun openFilesValidationAndSvgEscaping(){
        val c=rigTemplate("curl").copy(props=listOf(RigProp(kind="label",name="<&\"")))
        assertEquals(c,importRig(exportRig(c)))
        assertTrue(exportRigSvg(c,c.frames.first().pose).toString(Charsets.UTF_8).contains("&lt;&amp;&quot;"))
        assertFails {importRig(ByteArray(1_048_577))}
        assertFails {validateRig(c.copy(duration=Float.NaN))}
        assertFails {validateRig(c.copy(frames=listOf(c.frames.first().copy(time=1f))))}
        assertFails {validateRig(c.copy(props=listOf(RigProp(kind="script"))))}
        val broken=c.frames.first().copy(pose=c.frames.first().pose.copy(points=emptyMap()))
        assertFails {validateRig(c.copy(frames=listOf(broken)))}
    }
    @Test fun assetsBackupsTransfersAndOldDefaults(){
        val e=Exercise(name="Custom",rig=rigTemplate("raise"));val asset=ExerciseAsset(id="a",exercise=e,updated=1);val d=SharedData(exerciseAssets=listOf(asset),routines=listOf(Routine(name="R",instructions="",minutes=1,exercises=listOf(e))))
        assertEquals(d,readBackup(prepareBackup(d,emptyList(),null,""),"").data)
        assertEquals(listOf(asset),selected(d,setOf("activity")).data.exerciseAssets)
        assertTrue(selected(d,setOf("notes")).data.exerciseAssets.isEmpty())
        assertNotEquals("a",mergeData(SharedData(),d,ImportMode.COPIES).exerciseAssets.single().id)
        val revised=asset.copy(updated=2,exercise=e.copy(name="Edited"));assertEquals(revised,mergeData(d,d.copy(exerciseAssets=listOf(revised)),ImportMode.MERGE).exerciseAssets.single())
        assertNull(codec.decodeFromString<Exercise>("{\"name\":\"Old\"}").rig)
        assertTrue(resetData(Snapshot(shared=d),setOf("activity")).shared.exerciseAssets.isEmpty())
    }
    @Test fun rigLabelsInTenLanguages(){
        languageNames.keys.forEach {lang->(rigJoints+rigEquipment+listOf("Pin contact","Fixed bone lengths","Torso rotation","Export JSON","Discard changes?")).forEach {assertTrue(rigText(it,lang).isNotBlank())}}
    }
}
