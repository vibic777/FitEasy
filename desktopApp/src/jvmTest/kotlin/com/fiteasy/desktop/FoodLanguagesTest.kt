package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class FoodLanguagesTest {
    private val foods by lazy {listOf("products.json","dishes.json").flatMap {name->codec.decodeFromString<List<Food>>(java.io.File("../shared/resources/$name").readText())}}
    @Test fun namesFollowTheLanguageAndFallBackToEnglishWhole() {
        val broccoli=Food(id="usda:1",name="Broccoli, cooked, with cheese",kind="dish",source="USDA FoodData Central")
        assertEquals(broccoli.name,foodDisplayName(broccoli,"en"))
        assertEquals("Brokkoli, gekocht, mit Käse",foodDisplayName(broccoli,"de"))
        assertEquals("Brocoli, cuit, avec fromage",foodDisplayName(broccoli,"fr"))
        assertNotEquals(broccoli.name,foodDisplayName(broccoli,"ja"))
        // An unknown word keeps the whole name English instead of a mixture; an unknown language shows English.
        assertEquals("Broccoli, zzzunknownword",foodDisplayName(broccoli.copy(name="Broccoli, zzzunknownword"),"de"))
        assertEquals(broccoli.name,foodDisplayName(broccoli,"sv"))
        assertEquals("Carrots, cooked",foodDisplayName(broccoli.copy(name="Carrots, cooked",source="user"),"de"),"A person's own entry is never renamed")
        val latin=Regex("[A-Za-z]{3,}")
        val report=StringBuilder()
        foodLanguages.forEach {language->
            val covered=foodNameCoverage(foods,language);report.append("$language $covered of ${foods.size}\n")
            if(language in setOf("ru","be","zh","ja","ko"))foods.forEach {food->val shown=foodDisplayName(food,language)
                if(shown!=food.name)assertTrue(shown.split(' ',',','/','(',')','-').count {latin.containsMatchIn(it)}<=5,"$language mixed: $shown")}
        }
        listOf("ru","de","zh").forEach {l->foods.filter {foodDisplayName(it,l)!=it.name}.drop(40).take(10).forEach {report.append(it.name+" -> "+foodDisplayName(it,l)+"\n")}}
        // The next words to translate, most needed first.
        val missing=HashMap<String,Int>()
        foods.forEach {food->if(foodDisplayName(food,"de")==food.name)Regex("[A-Za-z][A-Za-z'-]*").findAll(food.name).forEach {w->val word=w.value.lowercase();if(foodDisplayName(food.copy(name=word),"de")==word && word.length>1)missing.merge(word,1,Int::plus)}}
        report.append("MISSING "+missing.size+"\n"+missing.entries.sortedByDescending {it.value}.joinToString(" ") {it.key})
        java.io.File("build/review/food-coverage.txt").also {it.parentFile.mkdirs()}.writeText(report.toString())
    }
}
