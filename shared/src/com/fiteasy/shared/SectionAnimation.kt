package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Clock is read only in draw, so frames never recompose the diary. */
@Composable fun SectionAnimation(section:String) {
    val a=LocalAppearance.current
    if(section !in sectionColors)return
    var visible by remember {mutableStateOf(true)}
    // A window behind another one, or behind a dialog, has nobody watching the decoration.
    val enabled=a.animations && visible && LocalWindowInfo.current.isWindowFocused
    val dark=MaterialTheme.colorScheme.surface.luminance()<.179f
    val color=remember(section,dark,a.accent){iconColor(section,dark,a.accent)}
    val clock=produceState(0f,section,enabled) {
        value=0f
        if(enabled){val start=System.nanoTime();while(true){value=((System.nanoTime()-start)/1e9/4).toFloat();delay(50)}}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Canvas(Modifier.size(if(a.style=="rainbow")76.dp else 64.dp).onGloballyPositioned {val bounds=it.boundsInWindow();visible=bounds.width>0 && bounds.height>0}) {
        scale(size.width/26,size.height/26,pivot=Offset.Zero){drawFilledSymbol(section,color,if(a.style=="rainbow")3f else 2f,clock.value,enabled)}
    }}
}
