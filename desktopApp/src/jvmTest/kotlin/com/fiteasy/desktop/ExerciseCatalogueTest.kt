package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class ExerciseCatalogueTest {
    @Test fun catalogueIsOfflineLocalizedAndValid(){
        assertEquals(15,exerciseCatalogue.size)
        assertEquals(15,exerciseCatalogue.map {it.id}.distinct().size)
        languageNames.keys.forEach {language->
            assertEquals(15,exerciseNames(language).size,language);assertEquals(15,exerciseInstructions(language).size,language)
            // D126: every card says how the movement is done, in its own words and in the language asked for.
            assertEquals(15,exerciseInstructions(language).toSet().size,language);exerciseInstructions(language).forEach {assertTrue(it.length>(if(language in setOf("zh","ja","ko"))25 else 60),"$language: $it")}
            if(language !in setOf("ru","be"))(exerciseNames(language)+exerciseInstructions(language)).forEach {assertFalse(Regex("[А-Яа-яЁё]").containsMatchIn(it),"$language: $it")}
            exerciseCatalogue.forEach {card->val e=card.exercise(language);assertTrue(e.name.isNotBlank());assertTrue(e.instructions.isNotBlank());assertEquals(card.id,e.catalogueId);assertTrue(e.technique in allExerciseTechniques);validate(SharedData(routines=listOf(Routine(name="Example",instructions="",minutes=1,exercises=listOf(e)))))}
        }
    }
    /** D126: the seven gentle cards keep the lengths of the limbs in every frame and are drawn as one whole person; the sheet goes to build/review. */
    @Test fun theGentleExercisesMoveAWholePersonWithoutStretchingIt(){
        val keys=listOf("armsup","twist","march","sidelegraise","necktilt","sitstand","kneeext")
        assertTrue(exerciseCatalogue.map {it.technique}.containsAll(keys) && allExerciseTechniques.keys.containsAll(keys))
        val standing=rigBase();val side=300
        val limbs=listOf("ls" to "le","le" to "lw","rs" to "re","re" to "rw","lh" to "lk","lk" to "la","rh" to "rk","rk" to "ra","neck" to "head")
        val sheet=java.awt.image.BufferedImage(side*4,side*keys.size,java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g=sheet.createGraphics()
        try {keys.forEachIndexed {row,key->
            val clip=rigTemplate(key);validateRig(clip)
            assertTrue(rigIsMoving(clip),key);assertEquals(0f,clip.frames.first().time);assertEquals(3f,clip.frames.last().time);assertEquals(clip.frames.first().pose.points,clip.frames.last().pose.points,"$key ends where it began")
            clip.frames.forEach {frame->limbs.forEach {(a,b)->assertEquals(rigLength(standing.points.getValue(a),standing.points.getValue(b)),rigLength(frame.pose.points.getValue(a),frame.pose.points.getValue(b)),.35f,"$key: $a-$b")}}
            listOf(0f,.75f,1.5f,2.25f).forEachIndexed {column,time->
                val bitmap=androidx.compose.ui.graphics.ImageBitmap(side,side)
                androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f),androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(bitmap),androidx.compose.ui.geometry.Size(side.toFloat(),side.toFloat())){drawRect(androidx.compose.ui.graphics.Color.White);drawRig(clip,rigSample(clip,time),time,column%2==0,androidx.compose.ui.graphics.Color(0xFF168B70))}
                val pixels=IntArray(side*side).also {bitmap.readPixels(it)}
                assertTrue(pixels.count {it!=-1}>side*side/60,"$key at $time is drawn")
                val frame=java.awt.image.BufferedImage(side,side,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,side,side,pixels,0,side);g.drawImage(frame,column*side,row*side,null)
            }
        }}finally {g.dispose()}
        assertTrue(javax.imageio.ImageIO.write(sheet,"png",java.io.File("build/review/rig-gentle.png").also {it.parentFile.mkdirs()}))
    }
    @Test fun categoriesAndEquipmentAreTranslatedInEveryLanguage(){
        assertEquals("Мобильность",libraryText("Mobility","ru"));assertEquals("Mobility",libraryText("Mobility","en"))
        val labels=(exerciseCatalogue.map {it.category}+exerciseCatalogue.map {it.equipment}).distinct()
        languageNames.keys.filter {it!="en"}.forEach {language->labels.forEach {label->assertNotEquals(label,libraryText(label,language),"$language/$label")}}
    }
    @Test fun searchAndFiltersCombine(){
        val curls=searchExercises("biceps",null,null,"en");assertEquals(listOf("curl"),curls.map {it.technique})
        assertTrue(searchExercises("", "Strength","Dumbbells","ru").all {it.technique in setOf("curl","raise")})
        assertTrue(searchExercises("not-an-exercise",null,null,"ru").isEmpty())
        assertEquals(exerciseCatalogue.size,searchExercises("",null,null,"ru").size)
    }
    @Test fun programKeepsAnEditableSnapshot(){
        val e=exerciseCatalogue.first {it.technique=="curl"}.exercise("ru").copy(name="My exercise",sets=3,seconds=45,loadKg=2.0)
        val r=Routine(name="Custom",instructions="",minutes=3,exercises=listOf(e))
        val restored=codec.decodeFromString<Routine>(codec.encodeToString(r))
        assertEquals(r,restored);assertEquals("My exercise",restored.exercises.single().name)
        assertNotEquals(e.name,exerciseCatalogue.first {it.technique=="curl"}.exercise("ru").name)
    }
}
