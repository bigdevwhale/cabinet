package app.cabinet.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.container
import app.cabinet.data.Item
import app.cabinet.domain.Museum
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

val R22 = RoundedCornerShape(22.dp)
val R16 = RoundedCornerShape(16.dp)
val R12 = RoundedCornerShape(12.dp)

fun Modifier.room() = this.background(C.bg)

/** A soft shadow under light cards; nothing at night. */
fun Modifier.lift(shape: Shape = R22): Modifier = if (C.dark) this else this.shadow(14.dp, shape, ambientColor = C.shadow, spotColor = C.shadow)

object Fmt {
    fun date(day: Long, ru: Boolean): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)).removeSuffix(" г.")

    /** The date as exactly as it is known: «12 марта 2019», «март 2019» or «2019» ([level] 0, 1, 2). */
    fun date(day: Long, level: Int, ru: Boolean): String {
        val loc = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val d = LocalDate.ofEpochDay(day)
        return when (level) {
            2 -> d.year.toString()
            1 -> d.format(DateTimeFormatter.ofPattern("LLLL yyyy", loc))
            else -> date(day, ru)
        }
    }

    /** [day] cut to what [level] keeps: the 1st of its month, or 1 January of its year. */
    fun trim(day: Long, level: Int): Long {
        val d = LocalDate.ofEpochDay(day)
        return when (level) {
            2 -> d.withDayOfYear(1).toEpochDay()
            1 -> d.withDayOfMonth(1).toEpochDay()
            else -> day
        }
    }

    fun short(day: Long, ru: Boolean): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)).replace(".", "")

    fun n(x: Int): String = "%,d".format(Locale.ENGLISH, x).replace(',', ' ')
}

/** A white rounded card (a dark one at night). */
@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 16.dp, color: Color = C.surface, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.lift().clip(R22).background(color).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

enum class Kind { PRIMARY, ACCENT, LIME, SOFT, RED, GHOST }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.PRIMARY, enabled: Boolean = true, icon: ImageVector? = null) {
    val (bg, fg) = when (kind) {
        Kind.PRIMARY -> C.ink to C.bg
        Kind.ACCENT -> C.accent to C.onAccent
        Kind.LIME -> C.lime to C.onLime
        Kind.SOFT -> C.surface2 to C.ink
        Kind.RED -> C.red to Color.White
        Kind.GHOST -> Color.Transparent to C.ink
    }
    Row(
        modifier.clip(R16).background(if (enabled) bg else C.surface2).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val col = if (enabled) fg else C.muted
        if (icon != null) Icon(icon, null, tint = col, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        Text(text, style = T.Button.copy(color = col), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A square-ish rounded icon button. */
@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.ink, bg: Color = C.surface, size: Dp = 40.dp, desc: String? = null, round: Boolean = false) {
    Box(modifier.size(size).then(if (bg == C.surface) Modifier.lift(if (round) CircleShape else R12) else Modifier).clip(if (round) CircleShape else R12).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * .5f))
    }
}

/** The title row of a page: a back button and the title, actions on the right. */
@Composable
fun TopBar(title: String, sub: String? = null, onBack: (() -> Unit)? = null, right: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Box(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { right?.invoke() }
        }
        if (sub != null) Text(sub, style = T.Small, modifier = Modifier.padding(top = 12.dp))
        Text(title, style = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (sub != null) 0.dp else 12.dp))
    }
}

/** A section heading with an optional action on the right. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.Heading, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = T.Label.copy(color = C.accent, fontSize = 13.sp),
            modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Label.copy(color = if (on) C.bg else C.ink, fontSize = 13.sp),
        modifier = Modifier.clip(R12).background(if (on) C.ink else C.surface2).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp))
}

/** A small coloured label. */
@Composable
fun Tag(text: String, bg: Color = C.surface, fg: Color = C.ink2) {
    Text(text, style = T.Label.copy(color = fg, fontSize = 11.sp), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = C.accent, track: Color = C.surface2, h: Dp = 6.dp) {
    Box(modifier.height(h).fillMaxWidth().clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(h).clip(RoundedCornerShape(50)).background(color))
    }
}

/** A progress ring with the percentage inside. */
@Composable
fun Ring(progress: Float, modifier: Modifier = Modifier, size: Dp = 62.dp, color: Color = C.accent) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val w = 6.dp.toPx()
            drawArc(C.surface2, 0f, 360f, false, style = Stroke(w))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("${(progress * 100).toInt()}%", style = T.Label.copy(color = C.ink, fontSize = 13.sp))
    }
}

/** A dimmed layer over everything; tapping outside or Back closes it when [onDismiss] is given. */
@Composable
fun Scrim(onDismiss: (() -> Unit)?, bottom: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    if (onDismiss != null) BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(C.scrim).clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() }
            .padding(if (bottom) 0.dp else 16.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 520.dp).clickable(remember { MutableInteractionSource() }, null) {}) { content() }
    }
}

/** A bottom sheet. */
@Composable
fun Sheet(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg).padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(50)).background(C.line))
        content()
    }
}

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

/** A filled rounded text field with its label above. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, lines: Int = 1, number: Boolean = false, hint: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Text(label, style = T.Label, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        BasicTextField(
            value, onChange, textStyle = T.Body.copy(color = C.ink), singleLine = lines == 1, minLines = lines, cursorBrush = SolidColor(C.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(R16).background(C.surface2).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (value.isEmpty() && hint != null) Text(hint, style = T.Body.copy(color = C.muted))
                    inner()
                }
            },
        )
    }
}

private val GREY = ColorMatrix().apply { setToSaturation(0.15f) }

/**
 * The photo of a thing, cut out (or whole), loaded off the main thread. A thing whose card is still empty is shown
 * faded and grey until it gets its story; a thing with no photo yet shows a quiet placeholder.
 */
@Composable
fun Face(item: Item, modifier: Modifier = Modifier, big: Boolean = false, fit: ContentScale = ContentScale.Fit, center: Boolean = true, dim: Boolean = Museum.dusty(item)) {
    val c = LocalContext.current.container
    val key = "${item.id}:$big:${item.updated}"
    val state = remember(key) { mutableStateOf<ImageBitmap?>(c.photos.cached(item.id, big, item.updated)?.asImageBitmap()) }
    LaunchedEffect(key) { if (state.value == null) state.value = c.photos.load(item.id, big, item.updated)?.asImageBitmap() }
    val busy by remember(item.id) { c.busy.map { item.id in it } }.let { f -> produceState(false, f) { f.collect { value = it } } }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bmp = state.value
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize().then(if (dim) Modifier.alpha(.55f) else Modifier).then(if (!item.owned) Modifier.alpha(.5f) else Modifier),
            contentScale = fit, alignment = if (center) Alignment.Center else Alignment.BottomCenter, colorFilter = if (dim || !item.owned) ColorFilter.colorMatrix(GREY) else null)
        else if (item.photos == 0) Icon(Icons.Outlined.Inventory2, null, tint = C.muted.copy(alpha = .6f), modifier = Modifier.size(28.dp))
        if (busy) CircularProgressIndicator(Modifier.size(26.dp), color = C.accent, strokeWidth = 2.dp)
    }
}

/** A thing as a tile: its photo on its hall's colour, the name and number, a label when something is missing. */
@Composable
fun ThingTile(item: Item, number: String, hallColor: Color, modifier: Modifier = Modifier, photoHeight: Dp = 120.dp, wishLabel: String, dustyLabel: String, spareLabel: String? = null, titleLines: Int = 1, onClick: () -> Unit) {
    Column(modifier.clip(R22).background(hallColor).clickable(onClick = onClick).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(photoHeight)) {
            Face(item, Modifier.fillMaxSize().padding(4.dp))
            Row(Modifier.align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!item.owned) Tag(wishLabel, C.lime, C.onLime)
                else if (Museum.dusty(item)) Tag(dustyLabel)
                if (spareLabel != null) Tag(spareLabel)
            }
        }
        Text(item.title.ifBlank { "—" }, style = T.Strong.copy(fontSize = 13.5.sp, lineHeight = 17.sp), maxLines = 2, minLines = titleLines, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp))
        Text(number, style = T.Label.copy(fontSize = 11.sp, color = C.ink2.copy(alpha = .7f)), modifier = Modifier.padding(start = 2.dp, top = 1.dp))
    }
}

/** A thin rule. */
@Composable
fun Rule(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(C.line))
