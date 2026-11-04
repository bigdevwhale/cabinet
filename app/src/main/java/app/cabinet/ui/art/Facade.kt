package app.cabinet.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.cabinet.ui.theme.C
import kotlin.random.Random

/**
 * The house of the collection, growing with it: a glass cabinet in the hall (stage 0), a room of its own,
 * a little house, a portico, a wing, a dome, a second wing, and at last a crest on the pediment.
 * [visitors] small figures stroll in front; at night the windows are lit.
 */
@Composable
fun Facade(stage: Int, visitors: Int, modifier: Modifier = Modifier, seed: Long = 0) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val ground = h * 0.86f
        val ink = C.ink
        val stroke = Stroke(1.6f * density)
        // Sky and ground.
        drawRect(Brush.verticalGradient(listOf(if (C.dark) Color(0xFF0E1420) else Color(0xFFF6EFE1), if (C.dark) Color(0xFF1B1A22) else C.bg), endY = ground))
        if (C.dark) { val r = Random(7); repeat(26) { drawCircle(Color.White.copy(alpha = .3f + r.nextFloat() * .5f), 0.8f * density + r.nextFloat() * density, Offset(r.nextFloat() * w, r.nextFloat() * ground * 0.6f)) } }
        drawRect(C.surface2, Offset(0f, ground), Size(w, h - ground))
        drawLine(ink.copy(alpha = .5f), Offset(0f, ground), Offset(w, ground), stroke.width)

        val wall = if (C.dark) Color(0xFF2A2620) else Color(0xFFFFFBF2)
        val window = if (C.dark) Color(0xFFF2C66D) else Color(0xFFDCE3E2)
        val cx = w / 2

        if (stage == 0) {
            // A glass cabinet standing in a hallway.
            val cw = h * 0.42f; val ch = h * 0.66f
            val r = Rect(cx - cw / 2, ground - ch, cx + cw / 2, ground)
            drawRect(C.wood, r.topLeft, r.size)
            drawRect(C.back, Offset(r.left + cw * 0.1f, r.top + ch * 0.12f), Size(cw * 0.8f, ch * 0.78f))
            for (k in 1..3) drawLine(C.woodLight, Offset(r.left + cw * 0.1f, r.top + ch * (0.12f + k * 0.195f)), Offset(r.right - cw * 0.1f, r.top + ch * (0.12f + k * 0.195f)), 2f * density)
            drawRect(ink, r.topLeft, r.size, style = stroke)
            drawRect(C.woodDark, Offset(r.left - cw * 0.06f, r.top - ch * 0.06f), Size(cw * 1.12f, ch * 0.07f))
            visitorsWalk(visitors.coerceAtMost(2), ground, seed)
            return@Canvas
        }

        val bodyW = w * (0.3f + 0.07f * stage.coerceAtMost(5))
        val bodyH = h * (0.36f + 0.03f * stage.coerceAtMost(4))
        val top = ground - bodyH

        // Wings.
        fun wing(left: Boolean) {
            val ww = bodyW * 0.36f; val wh = bodyH * 0.72f
            val x0 = if (left) cx - bodyW / 2 - ww + 2 else cx + bodyW / 2 - 2
            drawRect(wall, Offset(x0, ground - wh), Size(ww, wh))
            drawRect(ink, Offset(x0, ground - wh), Size(ww, wh), style = stroke)
            drawRect(C.surface2, Offset(x0 - 3, ground - wh - 8 * density), Size(ww + 6, 8 * density))
            drawRect(ink, Offset(x0 - 3, ground - wh - 8 * density), Size(ww + 6, 8 * density), style = stroke)
            for (k in 0 until 2) {
                val wx = x0 + ww * (0.22f + k * 0.36f)
                arched(Rect(wx, ground - wh * 0.78f, wx + ww * 0.2f, ground - wh * 0.25f), window, ink, stroke.width)
            }
        }
        if (stage >= 4) wing(false)
        if (stage >= 6) wing(true)

        // Dome.
        if (stage >= 5) {
            val dr = bodyW * 0.24f
            val dy = top - h * 0.1f
            drawRect(wall, Offset(cx - dr * 0.9f, dy), Size(dr * 1.8f, h * 0.1f))
            drawRect(ink, Offset(cx - dr * 0.9f, dy), Size(dr * 1.8f, h * 0.1f), style = stroke)
            drawArc(C.green.copy(alpha = .85f), 180f, 180f, true, Offset(cx - dr, dy - dr), Size(dr * 2, dr * 2))
            drawArc(ink, 180f, 180f, false, Offset(cx - dr, dy - dr), Size(dr * 2, dr * 2), style = stroke)
            drawLine(ink, Offset(cx, dy - dr), Offset(cx, dy - dr - 12 * density), stroke.width)
            drawCircle(C.accent, 3 * density, Offset(cx, dy - dr - 12 * density))
        }

        // Main block.
        drawRect(wall, Offset(cx - bodyW / 2, top), Size(bodyW, bodyH))
        drawRect(ink, Offset(cx - bodyW / 2, top), Size(bodyW, bodyH), style = stroke)
        // Steps.
        for (k in 0 until 3) {
            val sw = bodyW * (0.6f + k * 0.08f)
            drawRect(C.surface2, Offset(cx - sw / 2, ground - (3 - k) * 4 * density), Size(sw, 4 * density))
            drawRect(ink.copy(alpha = .6f), Offset(cx - sw / 2, ground - (3 - k) * 4 * density), Size(sw, 4 * density), style = Stroke(density))
        }
        // Pediment.
        val pedH = bodyH * 0.28f
        val ped = Path().apply { moveTo(cx - bodyW / 2 - 6, top); lineTo(cx, top - pedH); lineTo(cx + bodyW / 2 + 6, top); close() }
        drawPath(ped, wall); drawPath(ped, ink, style = stroke)
        if (stage >= 7) { // the crest
            val r = pedH * 0.32f
            val cy = top - pedH * 0.38f
            val shield = Path().apply { moveTo(cx - r * 0.8f, cy - r); lineTo(cx + r * 0.8f, cy - r); lineTo(cx + r * 0.8f, cy); quadraticTo(cx + r * 0.7f, cy + r * 0.8f, cx, cy + r * 1.1f); quadraticTo(cx - r * 0.7f, cy + r * 0.8f, cx - r * 0.8f, cy); close() }
            drawPath(shield, C.accent); drawPath(shield, ink, style = stroke)
        } else drawCircle(ink.copy(alpha = .5f), pedH * 0.12f, Offset(cx, top - pedH * 0.36f), style = stroke)

        // Columns from stage 3, windows before that.
        if (stage >= 3) {
            val n = if (stage >= 5) 6 else 4
            val span = bodyW * 0.84f
            for (k in 0 until n) {
                val x = cx - span / 2 + span * k / (n - 1)
                val cw = bodyW * 0.045f
                drawRect(wall, Offset(x - cw / 2, top + 6 * density), Size(cw, bodyH - 18 * density))
                drawLine(ink, Offset(x - cw / 2, top + 6 * density), Offset(x - cw / 2, ground - 12 * density), stroke.width)
                drawLine(ink, Offset(x + cw / 2, top + 6 * density), Offset(x + cw / 2, ground - 12 * density), stroke.width)
                drawRect(ink, Offset(x - cw * 0.8f, top + 2 * density), Size(cw * 1.6f, 4 * density), style = stroke)
            }
        } else {
            for (k in 0 until 2) {
                val wx = cx + (if (k == 0) -1 else 1) * bodyW * 0.3f - bodyW * 0.08f
                arched(Rect(wx, top + bodyH * 0.2f, wx + bodyW * 0.16f, top + bodyH * 0.62f), window, ink, stroke.width)
            }
        }
        // The door.
        val dw = bodyW * 0.16f; val dh = bodyH * 0.5f
        arched(Rect(cx - dw / 2, ground - 12 * density - dh, cx + dw / 2, ground - 12 * density), if (C.dark) window.copy(alpha = .8f) else C.wood, ink, stroke.width)
        if (stage == 1) { // a hand-painted sign over the door of the room
            drawRoundRect(C.accent, Offset(cx - dw, top + bodyH * 0.08f), Size(dw * 2, bodyH * 0.12f), CornerRadius(3 * density))
        }
        visitorsWalk(visitors.coerceAtMost(3 + stage), ground, seed)
    }
}

private fun DrawScope.arched(r: Rect, fill: Color, ink: Color, sw: Float) {
    val p = Path().apply {
        moveTo(r.left, r.bottom); lineTo(r.left, r.top + r.width / 2)
        arcTo(Rect(r.left, r.top, r.right, r.top + r.width), 180f, 180f, false)
        lineTo(r.right, r.bottom); close()
    }
    drawPath(p, fill); drawPath(p, ink, style = Stroke(sw))
}

/** Little people in front of the house: hats, a child, a tourist with a bag. */
private fun DrawScope.visitorsWalk(n: Int, ground: Float, seed: Long) {
    val r = Random(seed)
    val ink = C.ink.copy(alpha = .8f)
    repeat(n) { k ->
        val x = size.width * (0.08f + (k + r.nextFloat() * 0.6f) / (n + 0.6f) * 0.86f)
        val tall = size.height * (if (r.nextInt(5) == 0) 0.1f else 0.16f) * (0.9f + r.nextFloat() * 0.2f)
        val y = ground + size.height * 0.06f
        val head = tall * 0.13f
        drawCircle(ink, head, Offset(x, y - tall + head))
        val body = Path().apply { moveTo(x - tall * 0.12f, y - tall * 0.28f); lineTo(x - tall * 0.09f, y - tall + head * 2.3f); lineTo(x + tall * 0.09f, y - tall + head * 2.3f); lineTo(x + tall * 0.12f, y - tall * 0.28f); close() }
        drawPath(body, ink)
        drawLine(ink, Offset(x - tall * 0.05f, y - tall * 0.3f), Offset(x - tall * 0.07f, y), tall * 0.06f)
        drawLine(ink, Offset(x + tall * 0.05f, y - tall * 0.3f), Offset(x + tall * 0.07f, y), tall * 0.06f)
        when (r.nextInt(3)) {
            0 -> drawRect(ink, Offset(x - head * 1.5f, y - tall + head * 0.15f), Size(head * 3f, head * 0.35f)) // a hat brim
            1 -> drawRect(ink, Offset(x + tall * 0.1f, y - tall * 0.5f), Size(tall * 0.12f, tall * 0.14f)) // a bag
            else -> {}
        }
    }
}
