package com.fiteasy.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.fiteasy.shared.AppJournal
import com.fiteasy.shared.JournalLevel
import com.fiteasy.shared.WindowBar
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations

const val WINDOW_BAR_HEIGHT=56

/**
 * App header as the window title bar (D082), the way Notio-Windows does it: JetBrains Runtime keeps the system buttons,
 * Snap layouts, rounded corners and shadow while the app paints the bar. Any other Java keeps the ordinary title bar.
 */
fun installWindowBar(window:java.awt.Frame):WindowDecorations.CustomTitleBar? = runCatching {
    if(!JBR.isAvailable() || !JBR.isWindowDecorationsSupported())return@runCatching null
    val decorations=JBR.getWindowDecorations()
    decorations.createCustomTitleBar().also {
        it.height=WINDOW_BAR_HEIGHT.toFloat()
        decorations.setCustomTitleBar(window,it)
        // Everything is ordinary clickable content until the pointer rests on the bar itself; dialogs cover the strip safely.
        it.forceHitTest(true)
    }
}.onFailure {AppJournal.record("windows.titlebar.unavailable",JournalLevel.WARN,it)}.getOrNull()

/** Empty bar space drags, double-click maximises and right-click opens the system menu; the app's own buttons get their clicks. */
private fun dragModifier(bar:WindowDecorations.CustomTitleBar)=Modifier.pointerInput(bar) {
    awaitPointerEventScope {
        var inControl=false
        while(true) {
            val event=awaitPointerEvent(PointerEventPass.Main)
            if(event.type==PointerEventType.Exit){inControl=false;bar.forceHitTest(true);continue}
            val consumed=event.changes.any {it.isConsumed}
            if(event.type==PointerEventType.Press && consumed)inControl=true
            bar.forceHitTest(consumed || inControl)
            if(event.type==PointerEventType.Release)inControl=false
        }
    }
}

@Composable fun WindowScope.rememberWindowBar():WindowBar {
    val frame=window as java.awt.Frame
    val native=remember(frame) {installWindowBar(frame)} ?: return remember {WindowBar()}
    // The width of the system buttons is known only once the window is shown, and changes with display scale.
    var inset by remember {mutableFloatStateOf(native.rightInset)}
    DisposableEffect(frame) {
        val listener=object:java.awt.event.ComponentAdapter() {
            override fun componentResized(e:java.awt.event.ComponentEvent?){inset=native.rightInset}
            override fun componentShown(e:java.awt.event.ComponentEvent?){inset=native.rightInset}
        }
        frame.addComponentListener(listener)
        onDispose {frame.removeComponentListener(listener)}
    }
    return remember(native,inset) {WindowBar(WINDOW_BAR_HEIGHT.dp,inset.dp,dragModifier(native)) {dark->native.putProperty("controls.dark",dark)}}
}
