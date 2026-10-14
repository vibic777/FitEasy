package com.fiteasy.shared

/** A suggested editable title; UUID remains the identity, including after LAN merge. */
fun automaticName(base:String,existing:Collection<String>):String {
    require(base.isNotBlank() && base.length<=480)
    val names=existing.map {it.trim().lowercase(java.util.Locale.ROOT)}.toHashSet()
    var index=1
    while("$base $index".lowercase(java.util.Locale.ROOT) in names)index++
    return "$base $index"
}
