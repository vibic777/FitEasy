package com.fiteasy.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a hint stays once it is shown (D117): long enough to read it and to tap it. */
const val HINT_MILLIS=5000L
/** A mouse resting on a control this long shows its hint. */
private const val HOVER_MILLIS=600L

/**
 * Opens the help at what a hint names; null where there is no help to open (the lock screen, a preview). Given the text
 * of the hint as shown, in the language of the screen.
 */
val LocalHelpOpener=staticCompositionLocalOf<((String)->Unit)?> {null}

/**
 * The hint of a control (D117): holding a finger on it, or resting the mouse on it, shows its name for five seconds. While
 * the hint is on the screen a tap on it opens the help where that control is described. The held press itself does not
 * act: the control is not clicked by the gesture that asked what it is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HintBox(hint:String,enabled:Boolean=true,content:@Composable ()->Unit) {
    var shown by remember {mutableStateOf(false)}
    val active=enabled && hint.isNotEmpty()
    val openHelp=LocalHelpOpener.current
    val scope=rememberCoroutineScope()
    var resting by remember {mutableStateOf<Job?>(null)}
    LaunchedEffect(shown){if(shown){delay(HINT_MILLIS);shown=false}}
    Box(Modifier.pointerInput(active) {
        if(!active)return@pointerInput
        awaitEachGesture {
            val pass=PointerEventPass.Initial
            val down=awaitFirstDown(requireUnconsumed=false,pass=pass)
            if(down.type==PointerType.Mouse){resting?.cancel();shown=false;return@awaitEachGesture}
            try {withTimeout(viewConfiguration.longPressTimeoutMillis){waitForUpOrCancellation(pass)}}
            catch(_:PointerEventTimeoutCancellationException) {
                shown=true
                // The rest of this press belongs to the hint, not to the control under the finger.
                do {val event=awaitPointerEvent(pass);event.changes.forEach {it.consume()}}while(event.changes.any {it.pressed})
            }
        }
    }.pointerInput(active) {
        if(!active)return@pointerInput
        awaitPointerEventScope {while(true) {
            val event=awaitPointerEvent(PointerEventPass.Main)
            if(event.changes.firstOrNull()?.type!=PointerType.Mouse)continue
            when(event.type) {
                PointerEventType.Enter->{resting?.cancel();resting=scope.launch {delay(HOVER_MILLIS);shown=true}}
                PointerEventType.Exit->resting?.cancel()
            }
        }}
    }) {
        content()
        if(shown && active)Popup(popupPositionProvider=TooltipDefaults.rememberPlainTooltipPositionProvider(),onDismissRequest={shown=false},properties=PopupProperties(focusable=false)) {
            HintBubble(hint,openHelp?.let {open->{shown=false;open(hint)}})
        }
    }
}

/** What a shown hint looks like: its text and, where the help can be opened from it, the question mark that says so. */
@Composable fun HintBubble(hint:String,onHelp:(()->Unit)?) {
    Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.inverseSurface,contentColor=MaterialTheme.colorScheme.inverseOnSurface,shadowElevation=3.dp,
        modifier=Modifier.padding(4.dp).widthIn(max=280.dp).then(if(onHelp!=null)Modifier.clickable(role=Role.Button,onClick=onHelp) else Modifier)) {
        Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Text(hint,style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f,fill=false))
            if(onHelp!=null)RigSymbol("help",MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}
