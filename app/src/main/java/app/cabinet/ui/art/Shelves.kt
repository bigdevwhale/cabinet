package app.cabinet.ui.art

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cabinet.data.Item
import app.cabinet.domain.Museum
import app.cabinet.ui.components.BrassLabel
import app.cabinet.ui.components.Face
import app.cabinet.ui.theme.C
import kotlin.math.abs
import kotlin.math.roundToInt

/** Where a thing stands: its row and its box inside the cabinet, in dp. */
private data class Spot(val item: Item, val row: Int, val x: Dp, val w: Dp)

/** Fills the shelves left to right; each shelf's things are spread evenly along it. */
private fun layOut(items: List<Item>, width: Dp, tall: Dp, pad: Dp): List<Spot> {
    val inner = width - pad * 2
    val gap = 10.dp
    val rows = mutableListOf<MutableList<Pair<Item, Dp>>>()
    var used = 0.dp
    for (i in items) {
        val w = (tall * i.aspect.coerceIn(0.25f, 2.4f)).coerceIn(34.dp, inner * 0.9f)
        if (rows.isEmpty() || used + w + gap > inner) { rows += mutableListOf<Pair<Item, Dp>>(); used = 0.dp }
        rows.last() += i to w; used += w + gap
    }
    val out = mutableListOf<Spot>()
    rows.forEachIndexed { r, row ->
        val total = row.fold(0.dp) { a, p -> a + p.second }
        val free = inner - total
        // A full shelf spreads out; a short one keeps things close and centred, as a person would.
        val space = if (row.size > 1 && free / row.size < 40.dp) free / (row.size + 1) else minOf(free / (row.size + 1), 26.dp)
        var x = pad + (inner - total - space * (row.size - 1)) / 2
        for ((item, w) in row) { out += Spot(item, r, x, w); x += w + space }
    }
    return out
}

/**
 * A walnut cabinet with its glass open: things stand on shelves, a small lamp over each shelf.
 * With [onMove] the shelves are arranged by hand: hold a thing and carry it to its new place.
 */
@Composable
fun Shelves(
    items: List<Item>,
    code: String,
    modifier: Modifier = Modifier,
    pearl: Long = 0,
    tall: Dp = 104.dp,
    emptyRows: Int = 1,
    onTap: (Item) -> Unit,
    onMove: ((List<Item>) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val frame = 14.dp
        val rowH = tall + 46.dp
        val spots = remember(items, maxWidth) { layOut(items, maxWidth, tall, frame + 8.dp) }
        val rows = ((spots.maxOfOrNull { it.row } ?: -1) + 1 + emptyRows).coerceAtLeast(1)
        val height = rowH * rows + frame * 2 + 10.dp
        var dragged by remember { mutableStateOf<Long?>(null) }
        var drag by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current

        Box(Modifier.fillMaxWidth().height(height).drawBehind { cabinet(rows, rowH.toPx(), frame.toPx(), tall.toPx(), 1.dp.toPx()) }) {
            for (s in spots) {
                val isDragged = dragged == s.item.id
                val top = frame + 10.dp + rowH * s.row
                val label = Museum.inv(code, s.item.inv)
                val lift by animateDpAsState(if (isDragged) (-10).dp else 0.dp, label = "lift")
                Box(
                    Modifier
                        .zIndex(if (isDragged) 2f else 0f)
                        .offset { IntOffset((s.x.toPx() + if (isDragged) drag.x else 0f).roundToInt(), (top.toPx() + lift.toPx() + if (isDragged) drag.y else 0f).roundToInt()) }
                        .width(s.w).height(rowH)
                        .then(if (onMove != null) Modifier.pointerInput(s.item.id, items) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragged = s.item.id; drag = Offset.Zero },
                                onDrag = { ch, d -> ch.consume(); drag += d },
                                onDragEnd = {
                                    val cx = with(density) { s.x.toPx() + s.w.toPx() / 2 } + drag.x
                                    val cy = with(density) { top.toPx() + tall.toPx() / 2 } + drag.y
                                    val target = spots.minByOrNull { o ->
                                        val ox = with(density) { o.x.toPx() + o.w.toPx() / 2 }
                                        val oy = with(density) { (frame + 10.dp + rowH * o.row).toPx() + tall.toPx() / 2 }
                                        abs(ox - cx) + abs(oy - cy) * 2
                                    }
                                    if (target != null && target.item.id != s.item.id) {
                                        val list = items.toMutableList()
                                        val from = list.indexOfFirst { it.id == s.item.id }
                                        val to = list.indexOfFirst { it.id == target.item.id }
                                        list.add(to, list.removeAt(from))
                                        onMove(list)
                                    }
                                    dragged = null; drag = Offset.Zero
                                },
                                onDragCancel = { dragged = null; drag = Offset.Zero },
                            )
                        } else Modifier)
                        .clickable(remember { MutableInteractionSource() }, null) { onTap(s.item) },
                ) {
                    if (s.item.id == pearl) Canvas(Modifier.fillMaxSize()) { spotlight() }
                    // A soft shadow where it touches the shelf.
                    Canvas(Modifier.fillMaxWidth().height(tall + 6.dp)) {
                        drawOval(Color.Black.copy(alpha = if (s.item.owned) .35f else .12f), topLeft = Offset(size.width * 0.08f, size.height - 9.dp.toPx()), size = Size(size.width * 0.84f, 8.dp.toPx()))
                    }
                    Face(s.item, Modifier.fillMaxWidth().height(tall).graphicsLayer { if (isDragged) { scaleX = 1.06f; scaleY = 1.06f } },
                        dust = if (Museum.dusty(s.item)) 1f - Museum.fullness(s.item) * 0.6f else 0f)
                    BrassLabel(label, Modifier.align(Alignment.TopCenter).offset(y = tall + 7.dp))
                }
            }
        }
    }
}

/** The cabinet itself: frame, back wall, shelves and one lamp over each shelf. */
private fun DrawScope.cabinet(rows: Int, rowH: Float, frame: Float, tall: Float, dp: Float) {
    val w = size.width; val h = size.height
    // Frame.
    drawRoundRect(Brush.verticalGradient(listOf(C.woodLight, C.wood, C.woodDark)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10 * dp, 10 * dp))
    // Back wall with a fine velvet ribbing.
    val l = frame; val t = frame; val r = w - frame; val b = h - frame
    drawRect(C.back, Offset(l, t), Size(r - l, b - t))
    var x = l
    while (x < r) { drawLine(Color.Black.copy(alpha = .07f), Offset(x, t), Offset(x, b), 1f); x += 3 * dp }
    for (k in 0 until rows) {
        val y0 = t + 10 * dp + rowH * k
        // The lamp over this shelf.
        drawOval(Brush.radialGradient(listOf(C.backLight.copy(alpha = if (C.dark) .9f else .8f), Color.Transparent), center = Offset(w / 2, y0 + tall * 0.2f), radius = w * 0.6f),
            topLeft = Offset(l, y0 - 8 * dp), size = Size(r - l, tall + 16 * dp))
        // The shelf board: top surface, front edge, shadow under it.
        val sy = y0 + tall - 2 * dp
        drawRect(C.woodLight, Offset(l, sy), Size(r - l, 5 * dp))
        drawRect(Brush.verticalGradient(listOf(C.wood, C.woodDark), startY = sy + 5 * dp, endY = sy + 27 * dp), Offset(l, sy + 5 * dp), Size(r - l, 22 * dp))
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .4f), Color.Transparent), startY = sy + 27 * dp, endY = sy + 40 * dp),
            Offset(l, sy + 27 * dp), Size(r - l, 13 * dp))
    }
    // Inner shadow along the sides of the frame.
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .3f), Color.Transparent), startX = l, endX = l + 12 * dp), Offset(l, t), Size(12 * dp, b - t))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .3f)), startX = r - 12 * dp, endX = r), Offset(r - 12 * dp, t), Size(12 * dp, b - t))
}

/** A brighter cone over the pearl of the hall. */
private fun DrawScope.spotlight() {
    val w = size.width; val h = size.height
    val path = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.4f, -8f); lineTo(w * 0.6f, -8f); lineTo(w * 1.05f, h * 0.75f); lineTo(-w * 0.05f, h * 0.75f); close() }
    drawPath(path, Brush.verticalGradient(listOf(Color(0x55FFF3C8), Color(0x00FFF3C8))))
}

/** A shelf strip without a cabinet: a few things in a row, for previews. */
@Composable
fun ShelfStrip(items: List<Item>, modifier: Modifier = Modifier, tall: Dp = 64.dp) {
    BoxWithConstraints(modifier.fillMaxWidth().height(tall + 22.dp)) {
        val spots = remember(items, maxWidth) { layOut(items, maxWidth, tall, 8.dp).filter { it.row == 0 } }
        Canvas(Modifier.fillMaxSize()) {
            val sy = size.height - 14.dp.toPx()
            drawRect(C.woodLight, Offset(0f, sy), Size(size.width, 3.dp.toPx()))
            drawRect(Brush.verticalGradient(listOf(C.wood, C.woodDark), startY = sy, endY = size.height), Offset(0f, sy + 3.dp.toPx()), Size(size.width, size.height - sy))
        }
        for (s in spots) Box(Modifier.offset(x = s.x).width(s.w).height(tall + 8.dp)) {
            Face(s.item, Modifier.size(s.w, tall).align(Alignment.BottomCenter).offset(y = 0.dp), dust = if (Museum.dusty(s.item)) .9f else 0f)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Transparent))
    }
}
