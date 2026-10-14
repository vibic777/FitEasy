package com.fiteasy.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A settings page on the whole screen (D087), with the same slots as AlertDialog so long forms move here unchanged:
 * back arrow and title on top, the form in the middle, its actions pinned at the bottom.
 */
/** True inside the Windows window: a page then covers the window itself instead of floating over it with a margin. */
val LocalDesktopWindow=androidx.compose.runtime.staticCompositionLocalOf {false}
private object WindowOrigin:androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(anchorBounds:androidx.compose.ui.unit.IntRect,windowSize:androidx.compose.ui.unit.IntSize,layoutDirection:androidx.compose.ui.unit.LayoutDirection,popupContentSize:androidx.compose.ui.unit.IntSize)=androidx.compose.ui.unit.IntOffset.Zero
}
/**
 * A page over everything (D108). A dialog on a computer is laid out with a margin and a dimmed edge, which reads as a
 * window over the window; there the page is a layer that covers the whole window. On the phone the dialog already fills
 * the screen and keeps the system Back button and the keyboard behaviour.
 */
@Composable fun FullPage(onDismissRequest:()->Unit,content:@Composable ()->Unit) {
    if(LocalDesktopWindow.current)androidx.compose.ui.window.Popup(popupPositionProvider=WindowOrigin,onDismissRequest=onDismissRequest,properties=androidx.compose.ui.window.PopupProperties(focusable=true)) {Surface(Modifier.fillMaxSize(),content=content)}
    else Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(usePlatformDefaultWidth=false)) {Surface(Modifier.fillMaxSize(),content=content)}
}
@Composable fun SettingsPage(onDismissRequest:()->Unit,title:@Composable ()->Unit,text:@Composable ()->Unit,confirmButton:@Composable ()->Unit,dismissButton:(@Composable ()->Unit)?=null) {
    FullPage(onDismissRequest) {
        Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
            WindowBarRow {
                IconAction("previous","Назад",onClick=onDismissRequest)
                Box(Modifier.weight(1f).padding(start=8.dp)) {ProvideTextStyle(MaterialTheme.typography.titleLarge,title)}
            }
            // The form takes the whole window (D108): nothing is edited in a narrowed window over the page.
            Box(Modifier.weight(1f).fillMaxWidth()) {DialogBackdrop(text)}
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End),verticalAlignment=Alignment.CenterVertically) {dismissButton?.invoke();confirmButton()}
        }
    }
}
/** One action of a settings page as a row of its own: icon in a tinted circle, name, and a line that says what is inside (D104). */
@Composable fun MenuRow(icon:String,title:String,comment:String,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainerLow,tonalElevation=1.dp) {
        Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(9.dp)){RigSymbol(icon,MaterialTheme.colorScheme.onPrimaryContainer)}}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.titleMedium);if(comment.isNotEmpty())Text(comment,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}
/** Heading that splits a long settings form into named groups. */
@Composable fun SettingsGroup(title:String) {
    Spacer(Modifier.height(4.dp));Text(title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
}
