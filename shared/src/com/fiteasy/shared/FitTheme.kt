package com.fiteasy.shared

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalAppearance=staticCompositionLocalOf {Appearance()}
/** Whether an exercise the person starts may move. Follows the Appearance switch only: battery economy stops decorative animation, not this (D095). */
val LocalExerciseMotion=staticCompositionLocalOf {true}
/** Battery economy is on: decorative animation is stopped for that reason, and screens that show motion say so (D098). */
val LocalBatteryEconomy=staticCompositionLocalOf {false}

@Composable fun Card(modifier:Modifier=Modifier,colors:CardColors=CardDefaults.cardColors(),content:@Composable ColumnScope.()->Unit) {
    val style=LocalAppearance.current.style
    val border=when(style){"blossom","pink"->BorderStroke(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.45f));"geometry"->BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant);"rainbow"->null;else->null}
    androidx.compose.material3.Card(modifier=modifier,colors=colors,border=border,content=content)
}

/** Page, sheet and soft tone of each style: light to dark. Text keeps the scheme's own on-colours. */
val styleTones=mapOf(
    "blossom" to (longArrayOf(0xFFFFF1F6,0xFFFFFFFF,0xFFFFD9E6) to longArrayOf(0xFF1F1218,0xFF2B1A22,0xFF4A2A38)),
    "air" to (longArrayOf(0xFFEAF7FF,0xFFFFFFFF,0xFFCFEBFB) to longArrayOf(0xFF0E1A21,0xFF16262F,0xFF23404E)),
    "geometry" to (longArrayOf(0xFFEEF2FF,0xFFFFFFFF,0xFFD9E2FF) to longArrayOf(0xFF10131F,0xFF1A1F30,0xFF2A3250)),
    "rainbow" to (longArrayOf(0xFFF7EFFF,0xFFFFFFFF,0xFFE8D9FF) to longArrayOf(0xFF171022,0xFF221832,0xFF3A2A55)),
    "pink" to (longArrayOf(0xFFFFE4F0,0xFFFFF6FA,0xFFFFC7DE) to longArrayOf(0xFF24101A,0xFF331826,0xFF5A2A42)),
    "violet" to (longArrayOf(0xFFEFE4FF,0xFFFAF6FF,0xFFDCC8FF) to longArrayOf(0xFF170F26,0xFF231738,0xFF3D2A60))
)
// Notio Android/Windows palette values; see NOTICE.md for attribution.
val fitPalettes=listOf(0xFF1B7F5CL,0xFFC2185BL,0xFFB84200L,0xFFC62828L,0xFF01679FL,0xFF1A3FC0L,0xFF4C7A1EL,0xFF000000L,0xFF616161L,0xFF2E2E2EL,0xFF7A5A00L,0xFF8338C9L,0xFFB0007EL)
val vividPalettes=listOf(0xFFFF1684L,0xFFFF5252L,0xFFFF8500L,0xFFFFD600L,0xFF00CFA0L,0xFF00B8EAL,0xFF375BFFL,0xFFA43AFFL)
private val nightAccents=listOf(0xFF7FD8B5L,0xFFFFB0CBL,0xFFFFB68DL,0xFFFFB4ABL,0xFF8DCFFFL,0xFFB7C4FFL,0xFFC5E17AL,0xFFD0D0D0L,0xFFBDBDBDL,0xFFFFFFFFL,0xFFFFD600L,0xFFD9B8FFL,0xFFFFABDCL)
fun colorContrast(a:Color,b:Color):Float=(maxOf(a.luminance(),b.luminance())+.05f)/(minOf(a.luminance(),b.luminance())+.05f)
fun readableAccent(seed:Color,dark:Boolean):Color {
    val surface=if(dark)Color(0xFF211F26)else Color.White
    val target=if(dark)Color.White else Color.Black
    return (0..20).asSequence().map {androidx.compose.ui.graphics.lerp(seed,target,it/20f)}.first {colorContrast(it,surface)>=4.5f}
}
fun fitColorScheme(accent:Long,dark:Boolean):ColorScheme {
    val index=fitPalettes.indexOf(accent)
    val seed=Color(accent)
    val primary=readableAccent(if(dark && index>=0)Color(nightAccents[index])else seed,dark)
    val onPrimary=if(primary.luminance()>0.179f)Color.Black else Color.White
    return if(dark) darkColorScheme(primary=primary,onPrimary=onPrimary,secondary=primary,
        background=if(accent==0xFF000000L)Color.Black else Color(0xFF141218),surface=Color(0xFF211F26),
        primaryContainer=androidx.compose.ui.graphics.lerp(seed,Color.Black,0.65f),onPrimaryContainer=Color(0xFFF4F4F4))
    else lightColorScheme(primary=primary,onPrimary=if(primary.luminance()>0.179f)Color.Black else Color.White,secondary=primary,background=Color(0xFFF7F7F7),surface=Color.White,
        primaryContainer=androidx.compose.ui.graphics.lerp(seed,Color.White,0.88f),onPrimaryContainer=Color(0xFF202020))
}

@Composable fun FitTheme(appearance:Appearance,colors:ColorScheme,exerciseMotion:Boolean=appearance.animations,economy:Boolean=false,content:@Composable ()->Unit) {
    val density=LocalDensity.current
    val shapes=remember(appearance.style) {when(appearance.style){
        "blossom"->Shapes(small=RoundedCornerShape(18.dp),medium=RoundedCornerShape(26.dp),large=RoundedCornerShape(32.dp))
        "air"->Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp))
        "geometry"->Shapes(small=RoundedCornerShape(6.dp),medium=RoundedCornerShape(10.dp),large=RoundedCornerShape(14.dp))
        "rainbow"->Shapes(small=RoundedCornerShape(24.dp),medium=RoundedCornerShape(30.dp),large=RoundedCornerShape(36.dp))
        "pink"->Shapes(small=RoundedCornerShape(20.dp),medium=RoundedCornerShape(28.dp),large=RoundedCornerShape(34.dp))
        "violet"->Shapes(small=RoundedCornerShape(14.dp),medium=RoundedCornerShape(22.dp),large=RoundedCornerShape(28.dp))
        else->Shapes()
    }}
    val themed=remember(colors,appearance.style) {
        val dark=colors.surface.luminance()<.179f
        // Clean pastel (light) or deep (dark) tones of the style's own hue, also for cards: no grey mixed in (D084).
        val tone=styleTones[appearance.style]?.let {if(dark)it.second else it.first}
        val base=if(tone==null)colors else {
            val page=Color(tone[0]);val sheet=Color(tone[1]);val soft=Color(tone[2])
            fun level(f:Float)=androidx.compose.ui.graphics.lerp(sheet,soft,f)
            colors.copy(background=page,surface=sheet,surfaceVariant=soft,surfaceContainerLowest=sheet,surfaceContainerLow=level(.2f),surfaceContainer=level(.4f),surfaceContainerHigh=level(.6f),surfaceContainerHighest=level(.8f),
                secondaryContainer=soft,surfaceBright=sheet,surfaceDim=level(.5f),outlineVariant=androidx.compose.ui.graphics.lerp(soft,colors.primary,.35f))
        }
        if(appearance.style=="rainbow"){
        val secondary=readableAccent(Color(0xFF00B8EAL),dark)
        val tertiary=readableAccent(Color(0xFFFF1684L),dark)
        base.copy(secondary=secondary,onSecondary=if(secondary.luminance()>.179f)Color.Black else Color.White,tertiary=tertiary,onTertiary=if(tertiary.luminance()>.179f)Color.Black else Color.White)
    }else base}
    CompositionLocalProvider(LocalDensity provides Density(density.density,density.fontScale*appearance.textScale),LocalUnits provides appearance.units,LocalAppearance provides appearance,LocalExerciseMotion provides exerciseMotion,LocalBatteryEconomy provides economy) {
        // System window buttons follow the app background when the header is the title bar.
        val bar=LocalWindowBar.current;val darkSurface=themed.surface.luminance()<.5f
        LaunchedEffect(bar,darkSurface){bar.darkControls(darkSurface)}
        MaterialTheme(colorScheme=themed,shapes=shapes,content=content)
    }
}
