package com.fiteasy.desktop

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** AUTO leaves the choice to the drawing library: Direct3D, and by itself OpenGL or software when the graphics driver refuses (D102). */
val windowsRenderModes=setOf("AUTO","SOFTWARE_FAST","DIRECT3D","OPENGL")
fun storedRenderMode(directory:File):String=runCatching {File(directory,"renderer-mode.txt").takeIf {it.isFile && it.length()<=32}?.readText()?.trim()?.takeIf {it in windowsRenderModes}}.getOrNull() ?: "AUTO"
fun storeRenderMode(directory:File,mode:String) {
    require(mode in windowsRenderModes)
    val target=File(directory,"renderer-mode.txt").toPath()
    val temporary=Files.createTempFile(directory.toPath(),"renderer-",".tmp")
    try {Files.writeString(temporary,mode);try {Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE)}catch(e:java.nio.file.AtomicMoveNotSupportedException){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING)}}finally{Files.deleteIfExists(temporary)}
}
fun configureWindowRendering(directory:File) {
    // Read before application/Skiko initialization; explicit JVM/environment overrides retain priority.
    if(System.getProperty("skiko.renderApi")==null && System.getenv("SKIKO_RENDER_API")==null)storedRenderMode(directory).takeIf {it!="AUTO"}?.let {System.setProperty("skiko.renderApi",it)}
}
