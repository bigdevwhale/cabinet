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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.cabinet.R

/**
 * A cabinet of curiosities. Light: an ivory room by day, walnut cabinets, brass labels.
 * Dark: the same room after closing, every shelf under its own small lamp.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val ink: Color, val ink2: Color, val muted: Color,
    val accent: Color, val accentSoft: Color, val onAccent: Color,
    val red: Color, val redSoft: Color, val green: Color, val greenSoft: Color,
    /** Cabinet wood: the frame, its shadow side and its lit edge. */
    val wood: Color, val woodDark: Color, val woodLight: Color,
    /** The inside back of a cabinet and the light that falls on it. */
    val back: Color, val backLight: Color,
    /** Inventory cards. */
    val paper: Color, val paperInk: Color, val paperLine: Color,
    val dust: Color,
    val scrim: Color,
)

val Light = Palette(
    dark = false,
    bg = Color(0xFFF3EEE4), surface = Color(0xFFFFFCF6), surface2 = Color(0xFFEAE3D5), line = Color(0xFFDDD4C3),
    ink = Color(0xFF231C15), ink2 = Color(0xFF4A4036), muted = Color(0xFF8A7D6B),
    accent = Color(0xFF9C7631), accentSoft = Color(0xFFF1E5C9), onAccent = Color(0xFFFFFFFF),
    red = Color(0xFFA8362E), redSoft = Color(0xFFF5DFD9), green = Color(0xFF3E6B4C), greenSoft = Color(0xFFDDEBDF),
    wood = Color(0xFF6E4529), woodDark = Color(0xFF4A2D1A), woodLight = Color(0xFF946243),
    back = Color(0xFF2F4638), backLight = Color(0xFF4C6B55),
    paper = Color(0xFFFBF6EA), paperInk = Color(0xFF2B231B), paperLine = Color(0xFFE5D8BE),
    dust = Color(0xFFD9D2C4),
    scrim = Color(0x99141210),
)

val Dark = Palette(
    dark = true,
    bg = Color(0xFF13110E), surface = Color(0xFF1E1A16), surface2 = Color(0xFF29241E), line = Color(0xFF362F27),
    ink = Color(0xFFF2EBDD), ink2 = Color(0xFFCEC4B3), muted = Color(0xFF948977),
    accent = Color(0xFFD7B062), accentSoft = Color(0xFF3A301D), onAccent = Color(0xFF1C1608),
    red = Color(0xFFE0705E), redSoft = Color(0xFF3B221D), green = Color(0xFF7DB78E), greenSoft = Color(0xFF1D3326),
    wood = Color(0xFF4B301E), woodDark = Color(0xFF2C1B10), woodLight = Color(0xFF6D4A31),
    back = Color(0xFF1A221C), backLight = Color(0xFF3D4A38),
    paper = Color(0xFFEDE4D0), paperInk = Color(0xFF2B231B), paperLine = Color(0xFFD5C6A6),
    dust = Color(0xFF8C8577),
    scrim = Color(0xCC000000),
)

/** The palette in use. Read from anywhere, drawing code included; switching theme recomposes and redraws. */
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
    val green get() = p.green
    val greenSoft get() = p.greenSoft
    val wood get() = p.wood
    val woodDark get() = p.woodDark
    val woodLight get() = p.woodLight
    val back get() = p.back
    val backLight get() = p.backLight
    val paper get() = p.paper
    val paperInk get() = p.paperInk
    val paperLine get() = p.paperLine
    val dust get() = p.dust
    val scrim get() = p.scrim
}

/** EB Garamond by Georg Duffner and Octavio Pardo (SIL OFL): the voice of old museum catalogues. */
val Serif = FontFamily(
    Font(R.font.garamond_regular, FontWeight.Normal),
    Font(R.font.garamond_medium, FontWeight.Medium),
    Font(R.font.garamond_semibold, FontWeight.SemiBold),
    Font(R.font.garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Inter by Rasmus Andersson (SIL OFL): everything that has to be read fast. */
val Sans = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** PT Mono by ParaType (SIL OFL): the typewriter of the inventory cards. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object T {
    val Hero get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 42.sp, color = C.ink)
    val Title get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 29.sp, lineHeight = 33.sp, color = C.ink)
    val Heading get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp, color = C.ink)
    val Italic get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 25.sp, color = C.ink2)
    val Body get() = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink)
    val Strong get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = C.ink)
    val Small get() = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Label get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, color = C.muted)
    val Caps get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp, color = C.muted)
    val Button get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp)
    val Number get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 32.sp, color = C.ink)
    /** Typed on an inventory card. */
    val Type get() = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 21.sp, color = C.paperInk)
    val TypeSmall get() = TextStyle(fontFamily = Mono, fontSize = 11.5.sp, lineHeight = 15.sp, color = C.paperInk.copy(alpha = .62f))
    /** Engraved on a brass label. */
    val Brass get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, lineHeight = 13.sp, letterSpacing = 0.3.sp, color = Color(0xFF3A2A10))
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
