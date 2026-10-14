package com.fiteasy.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

fun androidBackground(bytes:ByteArray):ByteArray {
    val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
    BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
    require(bounds.outWidth>0 && bounds.outHeight>0 && bounds.outWidth.toLong()*bounds.outHeight<=40_000_000){"Image dimensions unsupported"}
    val options=BitmapFactory.Options().apply {inSampleSize=((maxOf(bounds.outWidth,bounds.outHeight)+1079)/1080).coerceAtLeast(1)}
    val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image unavailable")
    val scale=1080.0/maxOf(decoded.width,decoded.height).coerceAtLeast(1080)
    val resized=Bitmap.createScaledBitmap(decoded,(decoded.width*scale).toInt().coerceAtLeast(1),(decoded.height*scale).toInt().coerceAtLeast(1),true)
    val flattened=Bitmap.createBitmap(resized.width,resized.height,Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(flattened).apply {drawColor(android.graphics.Color.WHITE);drawBitmap(resized,0f,0f,null)}
    try {return ByteArrayOutputStream().also {check(flattened.compress(Bitmap.CompressFormat.JPEG,80,it))}.toByteArray().also {require(it.size<=1048576)}}finally {flattened.recycle();if(resized!==decoded)resized.recycle();decoded.recycle()}
}
