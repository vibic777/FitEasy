package com.fiteasy.desktop

import com.fiteasy.shared.*
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class CatalogueTransferTest {
    /** The whole built-in catalogue, as the Export button sends it (the export that did not work in 0.4.21). */
    @Test fun theBuiltInCatalogueIsExportedAndReadBack() {
        val stock=listOf("products.json","dishes.json").flatMap {name->codec.decodeFromString<List<Food>>(java.io.File("../shared/resources/$name").readText())}
        assertTrue(stock.size>5000)
        val impossible=stock.filter {it!=cleanFood(it)};java.io.File("build/review/catalogue-impossible.txt").also {it.parentFile.mkdirs()}.writeText(impossible.joinToString("\n") {it.id+" "+it.name+" "+it.nutrients})
        assertTrue(impossible.isNotEmpty() && impossible.size<200,"Records with an impossible nutrient: ${impossible.size}")
        catalogueKinds.map {setOf(it)}.plus(element=catalogueKinds).forEach {kinds->
            val bytes=exportCatalogue(SharedData(),stock,kinds)
            val document=readCatalogueDocument(bytes)
            assertEquals(stock.count {it.kind in kinds},document.foods.size,kinds.toString())
        }
    }
    private val apple=Food("apple","Apple","product",updated=1)
    private val soup=Food("soup","Soup","dish",updated=1)
    @Test fun selectionRoundTripAndValidation() {
        val document=readCatalogueDocument(exportCatalogue(SharedData(),listOf(apple,soup),setOf("product")))
        assertEquals(listOf(apple),document.foods)
        assertFails {readCatalogueDocument("{}".toByteArray())}
        assertFails {validateCatalogue(document.copy(foods=listOf(apple,apple)))}
        assertFails {validateCatalogue(document.copy(foods=listOf(soup)))}
        assertFails {validateCatalogue(document.copy(foods=listOf(apple.copy(nutrients=Nutrients(kcal=Double.NaN)))))}
        assertFails {readCatalogueDocument(ByteArray(32*1024*1024+1))}
    }
    @Test fun modesPreserveOtherDataAndHideStockOnReplace() {
        val entry=Entry(type="food",name="Old apple",amount=100.0,unit="g",foodId=apple.id)
        val local=SharedData(entries=listOf(entry),dishes=listOf(soup),favorites=setOf(apple.id))
        val incoming=CatalogueDocument(categories=setOf("product"),foods=listOf(apple.copy(name="New apple",updated=2)))
        val merged=importCatalogue(local,listOf(apple,soup),incoming,ImportMode.MERGE,now=10)
        assertEquals("New apple",merged.products.single().name)
        assertEquals(local.entries,merged.entries)
        val copies=importCatalogue(local,listOf(apple,soup),incoming,ImportMode.COPIES,now=10)
        assertNotEquals(apple.id,copies.products.single().id)
        assertEquals(2,effectiveCatalogue(copies,listOf(apple)).count {it.kind=="product"})
        val replaced=importCatalogue(local,listOf(apple,soup),incoming.copy(foods=emptyList()),ImportMode.REPLACE,now=10)
        assertEquals(10,replaced.products.single().deleted)
        assertTrue(effectiveCatalogue(replaced,listOf(apple)).none {it.kind=="product" && it.deleted==null})
        assertEquals(local.dishes,replaced.dishes)
        assertEquals(local.entries,replaced.entries)
        assertEquals(local.favorites,replaced.favorites)
    }
    @Test fun foreignKindCannotOverwriteSameIdentity() {
        assertFails {importCatalogue(SharedData(),listOf(apple),CatalogueDocument(categories=setOf("dish"),foods=listOf(soup.copy(id=apple.id))),ImportMode.REPLACE)}
    }
}
