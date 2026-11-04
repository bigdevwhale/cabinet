package app.cabinet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.cabinet.R

/**
 * Light and modern: warm white, soft rounded cards, a pastel colour for every hall, a dark floating bar with a
 * lime «+». The photos of the things are the only pictures; nothing is drawn for decoration.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val ink: Color, val ink2: Color, val muted: Color,
    val accent: Color, val accentSoft: Color, val onAccent: Color,
    val red: Color, val redSoft: Color,
    /** The «+» and other moments of joy. */
    val lime: Color, val onLime: Color,
    /** The floating bottom bar. */
    val bar: Color, val onBar: Color, val barMuted: Color,
    /** One soft colour per hall, in order. */
    val halls: List<Color>,
    val scrim: Color,
    val shadow: Color,
)

val Light = Palette(
    dark = false,
    bg = Color(0xFFFAF9F7), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF1EEE9), line = Color(0xFFE8E4DD),
    ink = Color(0xFF1D1B19), ink2 = Color(0xFF45413B), muted = Color(0xFF8C877F),
    accent = Color(0xFF2F6B4F), accentSoft = Color(0xFFE3EFE7), onAccent = Color(0xFFFFFFFF),
    red = Color(0xFFC8553D), redSoft = Color(0xFFF8E3DD),
    lime = Color(0xFFB8E07A), onLime = Color(0xFF1D1B19),
    bar = Color(0xFF1D1B19), onBar = Color(0xFFFFFFFF), barMuted = Color(0xFF8C877F),
    halls = listOf(Color(0xFFE8F0E3), Color(0xFFF6E6DC), Color(0xFFE5ECF4), Color(0xFFF3EEDB), Color(0xFFEFE4F1), Color(0xFFE3EFEF), Color(0xFFF4E3E6), Color(0xFFECEAE0)),
    scrim = Color(0x8C141210),
    shadow = Color(0x14000000),
)

val Dark = Palette(
    dark = true,
    bg = Color(0xFF121211), surface = Color(0xFF1C1B1A), surface2 = Color(0xFF262523), line = Color(0xFF302E2B),
    ink = Color(0xFFF2F0EC), ink2 = Color(0xFFCFCAC2), muted = Color(0xFF8F8A82),
    accent = Color(0xFF86C9A2), accentSoft = Color(0xFF1F3329), onAccent = Color(0xFF0F1A14),
    red = Color(0xFFE88A72), redSoft = Color(0xFF3A2520),
    lime = Color(0xFFB8E07A), onLime = Color(0xFF1D1B19),
    bar = Color(0xFF2A2927), onBar = Color(0xFFFFFFFF), barMuted = Color(0xFF8F8A82),
    halls = listOf(Color(0xFF243127), Color(0xFF392C24), Color(0xFF232D39), Color(0xFF353021), Color(0xFF322739), Color(0xFF213333), Color(0xFF39262A), Color(0xFF2E2D26)),
    scrim = Color(0xCC000000),
    shadow = Color(0x00000000),
)

/** The palette in use. Read from anywhere; switching theme recomposes. */
object C {
    var p by mutableStateOf(Light)
    val dark get() = p.dark
    val bg get() = p.bg
    val surface get() = p.surface
    val surface2 get() = p.surface2
    val line get() = p.line
    val ink get() = p.ink
    val ink2 get() = p.ink2
    val muted get() = p.muted
    val accent get() = p.accent
    val accentSoft get() = p.accentSoft
    val onAccent get() = p.onAccent
    val red get() = p.red
    val redSoft get() = p.redSoft
    val lime get() = p.lime
    val onLime get() = p.onLime
    val bar get() = p.bar
    val onBar get() = p.onBar
    val barMuted get() = p.barMuted
    val scrim get() = p.scrim
    val shadow get() = p.shadow

    /** A hall's own soft colour, by its place in the list. */
    fun hall(index: Int): Color = p.halls[((index % p.halls.size) + p.halls.size) % p.halls.size]
}

/** Manrope by Mikhail Sharanda (SIL OFL). */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

object T {
    val Hero get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-0.02).em, color = C.ink)
    val Title get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = (-0.015).em, color = C.ink)
    val Heading get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 23.sp, color = C.ink)
    val Strong get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, color = C.ink)
    val Body get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink2)
    val Small get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Label get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = C.muted)
    val Caps get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em, color = C.muted)
    val Button get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
    val Number get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, color = C.ink)
    val Quote get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp, color = C.ink2)
}

/** 0: as the system, 1: light, 2: dark. */
@Composable
fun CabinetTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) Dark else Light
    if (C.p != p) C.p = p
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        if (C.p != p) C.p = p
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = p.accent, onPrimary = p.onAccent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.red, surfaceContainerHigh = p.surface, surfaceContainer = p.surface)
        else lightColorScheme(primary = p.accent, onPrimary = p.onAccent, background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.red, surfaceContainerHigh = p.surface, surfaceContainer = p.surface),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
