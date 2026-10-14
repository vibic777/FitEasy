package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The window's own title area when the app draws it (Windows, D082): the app header is the title bar and the system
 * minimise / maximise / close buttons sit over its right end. Zero height means an ordinary platform title bar or none (Android).
 */
class WindowBar(val height:Dp=0.dp,val rightInset:Dp=0.dp,val drag:Modifier=Modifier,val darkControls:(Boolean)->Unit={})
val LocalWindowBar=compositionLocalOf {WindowBar()}

/** Header row that doubles as the title bar: empty space drags the window, its buttons stay clickable. */
@Composable fun WindowBarRow(modifier:Modifier=Modifier,content:@Composable RowScope.()->Unit) {
    val bar=LocalWindowBar.current
    Row(modifier.fillMaxWidth().then(if(bar.height>0.dp)Modifier.heightIn(min=bar.height).then(bar.drag)else Modifier),verticalAlignment=Alignment.CenterVertically) {
        content()
        if(bar.rightInset>0.dp)Spacer(Modifier.width(bar.rightInset+8.dp))
    }
}
/** Screens without a header of their own keep a strip to drag the window by and stay clear of the system buttons. */
@Composable fun WindowBarSpace() {
    val bar=LocalWindowBar.current
    if(bar.height>0.dp)Box(Modifier.fillMaxWidth().height(bar.height).then(bar.drag))
}
