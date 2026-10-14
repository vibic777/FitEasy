package com.fiteasy.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val catalogueKinds=setOf("product","dish")
@Serializable data class CatalogueDocument(val format:String="fiteasy-catalogue",val version:Int=1,val categories:Set<String>,val foods:List<Food>)
fun effectiveCatalogue(data:SharedData,stock:List<Food>)=(data.products+data.dishes+stock.map(::cleanFood)).distinctBy {it.id}
fun validateCatalogue(document:CatalogueDocument) {
    require(document.format=="fiteasy-catalogue" && document.version==1)
    require(document.categories.isNotEmpty() && catalogueKinds.containsAll(document.categories))
    require(document.foods.size<=20000 && document.foods.all {it.kind in document.categories})
    require(document.foods.all {it.id.length<=256 && it.name.length<=4096 && it.category.length<=1024 && it.cuisine.length<=1024 && it.aliases.length<=8192 && it.source.length<=4096 && it.barcode.length<=256 && it.updated>=0 && (it.deleted==null || it.deleted>=0)})
    validate(SharedData(products=document.foods.filter {it.kind=="product"},dishes=document.foods.filter {it.kind=="dish"}))
}
fun exportCatalogue(data:SharedData,stock:List<Food>,categories:Set<String>):ByteArray {
    val document=CatalogueDocument(categories=categories,foods=effectiveCatalogue(data,stock).filter {it.kind in categories && it.deleted==null})
    validateCatalogue(document)
    return codec.encodeToString(document).toByteArray(Charsets.UTF_8).also {require(it.size<=32*1024*1024)}
}
fun readCatalogueDocument(bytes:ByteArray):CatalogueDocument {
    require(bytes.size<=32*1024*1024)
    val text=Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    return codec.decodeFromString<CatalogueDocument>(text).also(::validateCatalogue)
}
fun importCatalogue(data:SharedData,stock:List<Food>,document:CatalogueDocument,mode:ImportMode,now:Long=System.currentTimeMillis()):SharedData {
    validateCatalogue(document)
    val existing=effectiveCatalogue(data,stock)
    require(document.foods.none {food->existing.any {it.id==food.id && it.kind!=food.kind}}) {"Catalogue kind mismatch"}
    val records=when(mode) {
        ImportMode.COPIES->document.foods.filter {it.deleted==null}.map {it.copy(id=newId(),updated=now)}
        ImportMode.MERGE->document.foods.map {incoming->existing.firstOrNull {it.id==incoming.id}?.let {local->if(incoming.updated>local.updated)incoming else local} ?: incoming}
        ImportMode.REPLACE->{
            val imported=document.foods.map {it.copy(updated=now)}
            val ids=imported.map {it.id}.toSet()
            imported+existing.filter {it.kind in document.categories && it.id !in ids}.map {it.copy(deleted=now,updated=now)}
        }
    }
    val changedIds=records.map {it.id}.toSet()
    val user=(data.products+data.dishes).filter {it.id !in changedIds}+records
    return data.copy(products=user.filter {it.kind=="product"},dishes=user.filter {it.kind=="dish"}).also(::validate)
}
