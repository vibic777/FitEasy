package com.fiteasy.shared

import kotlinx.serialization.Serializable
import java.security.MessageDigest

val appearanceSections=linkedMapOf("today" to "Сегодня","food" to "Питание","water" to "Вода","activity" to "Активность","progress" to "Прогресс","profile" to "Профиль","settings" to "Профиль и настройки","schedules" to "Расписания","calendar" to "Календарь","notes" to "Заметки")
val appearanceStyles=linkedMapOf("calm" to "Спокойная","blossom" to "Цветочная","air" to "Воздушная","geometry" to "Геометрическая","rainbow" to "Разноцветная","pink" to "Розовая","violet" to "Фиолетовая")
@Serializable data class SectionBackground(val color:Long?=null,val image:String?=null,val alpha:Float=.15f)
fun Appearance.sectionBackground(section:String)=sectionBackgrounds[section] ?: SectionBackground(image=if(background!=null)"legacy" else null,alpha=backgroundAlpha)
fun Appearance.backgroundBytes(section:String):String?=sectionBackground(section).image?.let {if(it=="legacy")background else backgroundImages[it]}
fun rgbColor(red:String,green:String,blue:String):Long? {
    val r=red.toIntOrNull()?.takeIf {it in 0..255} ?: return null
    val g=green.toIntOrNull()?.takeIf {it in 0..255} ?: return null
    val b=blue.toIntOrNull()?.takeIf {it in 0..255} ?: return null
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}
fun applyBackground(a:Appearance,sections:Set<String>,change:(SectionBackground)->SectionBackground,image:String?=null):Appearance {
    require(sections.isNotEmpty() && sections.all {it in appearanceSections})
    val images=if(image==null)a.backgroundImages else a.backgroundImages+(MessageDigest.getInstance("SHA-256").digest(java.util.Base64.getDecoder().decode(image)).joinToString(""){"%02x".format(it)} to image)
    val imageId=if(image==null)null else images.entries.first {it.value==image}.key
    val backgrounds=a.sectionBackgrounds+sections.associateWith {id->change(a.sectionBackground(id)).let {if(imageId!=null)it.copy(image=imageId) else it}}
    val referenced=backgrounds.values.mapNotNull {it.image}.toSet()
    return a.copy(sectionBackgrounds=backgrounds,backgroundImages=images.filterKeys {it in referenced}).also {validateLocal(LocalData(appearance=it))}
}
fun applyStyle(a:Appearance,id:String):Appearance {
    require(id in appearanceStyles)
    val accent=when(id){"blossom"->0xFFFF1684L;"air"->0xFF00B8EAL;"geometry"->0xFF375BFFL;"rainbow"->0xFFA43AFFL;"pink"->0xFFE91E8CL;"violet"->0xFF8338C9L;else->0xFF1B7F5CL}
    return a.copy(style=id,accent=accent)
}
