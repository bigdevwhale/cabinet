package app.cabinet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

object Fmt {
    fun date(day: Long, ru: Boolean): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH))
            .removeSuffix(" г.")

    fun short(day: Long, ru: Boolean): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)).replace(".", "")

    fun n(x: Int): String = "%,d".format(Locale.ENGLISH, x).replace(',', ' ')
}

/**
 * The face of a thing as it stands on a shelf. [silhouette] draws a wished-for thing as a flat shape;
 * a thing with no photo yet is a dashed outline. Under a dust cover when [dust] > 0.
 */
@Composable
fun Face(item: Item, modifier: Modifier = Modifier, big: Boolean = false, silhouette: Boolean = !item.owned, dust: Float = 0f, fit: ContentScale = ContentScale.Fit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val key = "${item.id}:$big:${item.updated}"
    val state = remember(key) { mutableStateOf<ImageBitmap?>(c.photos.cached(item.id, big, item.updated)?.asImageBitmap()) }
    LaunchedEffect(key) { if (state.value == null) state.value = c.photos.load(item.id, big, item.updated)?.asImageBitmap() }
    val busy by remember(item.id) { c.busy.map { item.id in it } }.let { f -> androidx.compose.runtime.produceState(false, f) { f.collect { value = it } } }
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        val bmp = state.value
        when {
            bmp != null && silhouette -> Image(bmp, null, Modifier.fillMaxSize(), contentScale = fit, alignment = Alignment.BottomCenter,
                colorFilter = ColorFilter.tint(C.ink.copy(alpha = if (C.dark) .28f else .16f)))
            bmp != null -> Image(bmp, null, Modifier.fillMaxSize().then(if (!item.cut) Modifier.padding(2.dp).border(3.dp, C.woodLight.copy(alpha = .8f)) else Modifier),
                contentScale = fit, alignment = Alignment.BottomCenter)
            else -> Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawRoundRect(C.muted.copy(alpha = .6f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))), cornerRadius = CornerRadius(10f))
            }
        }
        if (dust > 0f && bmp != null && !silhouette) DustCover(dust, Modifier.fillMaxSize())
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center).padding(8.dp), color = C.accent, strokeWidth = 2.dp)
    }
}

/** A grey cloth thrown over a thing whose card is still empty; it thins as the card fills. */
@Composable
fun DustCover(amount: Float, modifier: Modifier) {
    Canvas(modifier) {
        val a = amount.coerceIn(0f, 1f)
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h); cubicTo(w * 0.02f, h * 0.6f, w * 0.12f, h * 0.18f, w * 0.5f, h * 0.06f)
            cubicTo(w * 0.88f, h * 0.18f, w * 0.98f, h * 0.6f, w * 0.9f, h)
            quadraticTo(w * 0.7f, h * 0.95f, w * 0.5f, h); quadraticTo(w * 0.3f, h * 0.95f, w * 0.1f, h); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(C.dust.copy(alpha = .93f * a), C.dust.copy(alpha = .98f * a), Color(0xFF6E675C).copy(alpha = .9f * a))))
        for (k in 0 until 4) {
            val x = w * (0.28f + k * 0.15f)
            drawLine(Color.Black.copy(alpha = .1f * a), Offset(x, h * 0.25f), Offset(x + w * 0.03f * (k - 1.5f), h), 2.dp.toPx())
        }
    }
}

/** A small brass label under a thing: its inventory number. */
@Composable
fun BrassLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = T.Brass, maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
        modifier = modifier.wrapContentWidth(unbounded = true).clip(RoundedCornerShape(2.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFE9CF8C), Color(0xFFC7A04F), Color(0xFFA9853A))))
            .padding(horizontal = 6.dp, vertical = 1.dp))
}

/** A text field in the museum's style. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, lines: Int = 1, number: Boolean = false, hint: String? = null) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        singleLine = lines == 1, minLines = lines, textStyle = T.Body,
        placeholder = hint?.let { { Text(it, style = T.Body.copy(color = C.muted)) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.accent, unfocusedBorderColor = C.line, focusedLabelColor = C.accent, unfocusedLabelColor = C.muted, cursorColor = C.accent),
    )
}

/** An index card with ruled lines, typed on. */
@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(C.paper)) {
        Canvas(Modifier.matchParentSize()) {
            val step = 21.sp.toPx()
            var y = 34.dp.toPx() + step
            while (y < size.height) { drawLine(C.paperLine, Offset(0f, y), Offset(size.width, y), 1f); y += step }
            drawLine(Color(0x55B54A3C), Offset(14.dp.toPx(), 0f), Offset(14.dp.toPx(), size.height), 1.2f)
            drawRect(Color.Black.copy(alpha = .06f), topLeft = Offset(0f, size.height - 3f), size = Size(size.width, 3f))
        }
        Column(Modifier.padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) { content() }
    }
}

/** The inventory number as shown everywhere. */
fun invOf(code: String, item: Item) = Museum.inv(code, item.inv)
