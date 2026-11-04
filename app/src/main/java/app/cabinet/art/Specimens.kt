package app.cabinet.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The demo museum's things, drawn in code on transparency as if cut out of a photo:
 * 0.33 cans with made-up labels and a family of crocodiles in clay, plush, metal, wood and glass.
 */
object Specimens {

    enum class Pattern { STRIPES, DOTS, WAVES, DIAGONAL, SUN, PLAIN, CHECK }

    data class Can(val base: Int, val accent: Int, val ink: Int, val brand: String, val pattern: Pattern, val slim: Boolean = false, val seed: Int = 0)

    fun can(c: Can, h: Int = 560): Bitmap {
        val w = if (c.slim) (h * 0.42f).toInt() else (h * 0.56f).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val top = h * 0.07f; val bottom = h * 0.95f
        val l = w * 0.04f; val r = w * 0.96f
        val e = w * 0.07f // ellipse half-height of rims

        // Body.
        val body = Path().apply {
            moveTo(l, top + e * 1.2f); lineTo(l, bottom - e * 0.6f)
            quadTo(l + 2, bottom + e * 0.4f, l + w * 0.12f, bottom + e * 0.5f)
            lineTo(r - w * 0.12f, bottom + e * 0.5f); quadTo(r - 2, bottom + e * 0.4f, r, bottom - e * 0.6f)
            lineTo(r, top + e * 1.2f); close()
        }
        cv.save(); cv.clipPath(body)
        p.color = c.base; cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        label(cv, c, l, top + e * 1.2f, r, bottom - e * 0.6f)
        // Cylinder light: dark edges, a soft highlight a third of the way in.
        p.shader = LinearGradient(l, 0f, r, 0f,
            intArrayOf(0x88000000.toInt(), 0x22000000, 0x00000000, 0x55FFFFFF, 0x00000000, 0x22000000, 0x99000000.toInt()),
            floatArrayOf(0f, .1f, .22f, .32f, .45f, .8f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null
        cv.restore()

        // Bottom rim (silver).
        metal(cv, p, RectF(l + w * 0.1f, bottom - e * 0.3f, r - w * 0.1f, bottom + e * 0.55f), w)
        // Neck and lid.
        val neck = Path().apply {
            moveTo(l, top + e * 1.25f); quadTo(l + w * 0.02f, top + e * 0.2f, l + w * 0.1f, top)
            lineTo(r - w * 0.1f, top); quadTo(r - w * 0.02f, top + e * 0.2f, r, top + e * 1.25f); close()
        }
        p.shader = LinearGradient(l, 0f, r, 0f, intArrayOf(0xFF8E9298.toInt(), 0xFFE9ECEF.toInt(), 0xFFC7CBD0.toInt(), 0xFF6C7076.toInt()), floatArrayOf(0f, .3f, .6f, 1f), Shader.TileMode.CLAMP)
        cv.drawPath(neck, p); p.shader = null
        val lid = RectF(l + w * 0.1f, top - e * 0.7f, r - w * 0.1f, top + e * 0.7f)
        p.color = 0xFFB9BEC4.toInt(); cv.drawOval(lid, p)
        p.color = 0xFFD9DDE1.toInt(); cv.drawOval(RectF(lid.left + w * 0.04f, lid.top + e * 0.2f, lid.right - w * 0.04f, lid.bottom - e * 0.15f), p)
        // Tab.
        p.color = 0xFF9BA0A6.toInt()
        cv.drawRoundRect(RectF(w * 0.42f, top - e * 0.35f, w * 0.62f, top + e * 0.3f), e * 0.3f, e * 0.3f, p)
        p.color = 0xFFC9CDD2.toInt()
        cv.drawOval(RectF(w * 0.47f, top - e * 0.2f, w * 0.57f, top + e * 0.15f), p)
        return bmp
    }

    private fun metal(cv: Canvas, p: Paint, rect: RectF, w: Int) {
        p.shader = LinearGradient(rect.left, 0f, rect.right, 0f, intArrayOf(0xFF7E838A.toInt(), 0xFFE2E5E8.toInt(), 0xFFA9AEB4.toInt(), 0xFF5F636A.toInt()), floatArrayOf(0f, .3f, .65f, 1f), Shader.TileMode.CLAMP)
        cv.drawOval(rect, p); p.shader = null
    }

    private fun label(cv: Canvas, c: Can, l: Float, t: Float, r: Float, b: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = r - l; val h = b - t
        val rnd = Random(c.seed)
        p.color = c.accent
        when (c.pattern) {
            Pattern.STRIPES -> { var y = t + h * 0.08f; while (y < b) { cv.drawRect(l, y, r, y + h * 0.045f, p); y += h * 0.11f } }
            Pattern.DOTS -> for (i in 0 until 46) cv.drawCircle(l + rnd.nextFloat() * w, t + rnd.nextFloat() * h, w * (0.025f + rnd.nextFloat() * 0.05f), p)
            Pattern.WAVES -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = h * 0.03f
                for (k in 0 until 7) {
                    val y0 = t + h * (0.1f + k * 0.13f)
                    val path = Path().apply { moveTo(l, y0); var x = l; while (x < r) { quadTo(x + w * 0.1f, y0 - h * 0.035f, x + w * 0.2f, y0); quadTo(x + w * 0.3f, y0 + h * 0.035f, x + w * 0.4f, y0); x += w * 0.4f } }
                    cv.drawPath(path, p)
                }
                p.style = Paint.Style.FILL
            }
            Pattern.DIAGONAL -> {
                cv.save(); cv.rotate(-28f, l + w / 2, t + h / 2)
                var x = l - w; while (x < r + w) { cv.drawRect(x, t - h, x + w * 0.12f, b + h, p); x += w * 0.3f }
                cv.restore()
            }
            Pattern.SUN -> {
                val cx = l + w * 0.5f; val cy = t + h * 0.62f
                for (k in 0 until 16) {
                    val a0 = k * 2 * PI / 16; val a1 = a0 + PI / 16
                    val path = Path().apply { moveTo(cx, cy); lineTo(cx + cos(a0).toFloat() * h, cy + sin(a0).toFloat() * h); lineTo(cx + cos(a1).toFloat() * h, cy + sin(a1).toFloat() * h); close() }
                    cv.drawPath(path, p)
                }
                p.color = c.ink; cv.drawCircle(cx, cy, w * 0.2f, p)
            }
            Pattern.CHECK -> {
                val s = w / 6f
                var row = 0; var y = t
                while (y < b) { var x = l + if (row % 2 == 0) 0f else s; while (x < r) { cv.drawRect(x, y, x + s, y + s, p); x += 2 * s }; y += s; row++ }
            }
            Pattern.PLAIN -> { cv.drawRect(l, t + h * 0.62f, r, t + h * 0.7f, p); cv.drawRect(l, t + h * 0.18f, r, t + h * 0.2f, p) }
        }
        // Brand on a band.
        val band = RectF(l, t + h * 0.33f, r, t + h * 0.5f)
        p.color = c.base; p.alpha = if (c.pattern == Pattern.PLAIN) 0 else 235
        cv.drawRect(band, p); p.alpha = 255
        p.color = c.ink; p.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); p.textAlign = Paint.Align.CENTER
        p.textSize = h * 0.11f
        while (p.measureText(c.brand) > w * 0.8f) p.textSize *= 0.92f
        cv.drawText(c.brand, l + w / 2, band.centerY() + p.textSize * 0.35f, p)
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); p.textSize = h * 0.035f
        cv.drawText("0,33 L", l + w / 2, t + h * 0.93f, p)
    }

    // ——— Crocodiles ———

    enum class Croc { CLAY, PLUSH, KEYRING, WOOD, GLASS, BRASS, SITTING }

    data class Crocodile(val kind: Croc, val body: Int, val belly: Int, val seed: Int = 0, val accent: Int = Color.WHITE)

    fun crocodile(c: Crocodile, wpx: Int = 640): Bitmap = when (c.kind) {
        Croc.SITTING -> sitting(c, wpx)
        Croc.KEYRING -> keyring(c, wpx)
        else -> lying(c, wpx)
    }

    /** The side view every crocodile shares: tail, body, snout, four legs, scutes on the back. */
    private fun side(cv: Canvas, c: Crocodile, x0: Float, y0: Float, len: Float) {
        val h = len * 0.26f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val fat = if (c.kind == Croc.PLUSH) 1.3f else 1f
        val back = y0; val belly = y0 + h * fat
        val body = Path().apply {
            moveTo(x0, y0 + h * 0.55f) // tail tip
            cubicTo(x0 + len * 0.15f, y0 + h * 0.2f, x0 + len * 0.3f, back - h * 0.05f, x0 + len * 0.45f, back - h * 0.1f)
            cubicTo(x0 + len * 0.6f, back - h * 0.15f, x0 + len * 0.7f, back - h * 0.05f, x0 + len * 0.76f, back - h * 0.12f) // head bump
            quadTo(x0 + len * 0.8f, back - h * 0.32f, x0 + len * 0.84f, back - h * 0.08f) // eye ridge
            cubicTo(x0 + len * 0.92f, back + h * 0.05f, x0 + len * 0.99f, back + h * 0.12f, x0 + len, back + h * 0.3f) // snout top
            quadTo(x0 + len * 1.005f, back + h * 0.52f, x0 + len * 0.97f, back + h * 0.55f)
            lineTo(x0 + len * 0.8f, back + h * 0.62f) // jaw
            cubicTo(x0 + len * 0.7f, belly, x0 + len * 0.5f, belly + h * 0.05f, x0 + len * 0.32f, belly - h * 0.12f)
            cubicTo(x0 + len * 0.2f, y0 + h * 0.75f, x0 + len * 0.08f, y0 + h * 0.66f, x0, y0 + h * 0.6f)
            close()
        }
        // Legs first, behind the body.
        p.color = darker(c.body, 0.8f)
        for ((lx, far) in listOf(0.36f to true, 0.62f to true, 0.31f to false, 0.57f to false)) {
            val x = x0 + len * lx; val col = if (far) darker(c.body, 0.72f) else c.body
            p.color = col
            val leg = Path().apply {
                moveTo(x, belly - h * 0.3f); lineTo(x - len * 0.035f, belly + h * 0.35f)
                lineTo(x - len * 0.06f, belly + h * 0.42f); lineTo(x + len * 0.035f, belly + h * 0.42f); lineTo(x + len * 0.045f, belly - h * 0.3f); close()
            }
            cv.drawPath(leg, p)
        }
        p.color = c.body; cv.drawPath(body, p)
        cv.save(); cv.clipPath(body)
        // Belly.
        p.color = c.belly
        cv.drawOval(RectF(x0 + len * 0.2f, belly - h * 0.38f, x0 + len * 0.82f, belly + h * 0.4f), p)
        when (c.kind) {
            Croc.WOOD -> {
                // Grain and the segment cuts of a jointed wooden toy.
                p.style = Paint.Style.STROKE; p.strokeWidth = len * 0.004f; p.color = darker(c.body, 0.78f)
                val r = Random(c.seed)
                for (k in 0 until 18) { val y = y0 - h * 0.3f + k * h * 0.09f; cv.drawLine(x0, y + r.nextFloat() * 6, x0 + len, y + r.nextFloat() * 10, p) }
                p.strokeWidth = len * 0.008f; p.color = darker(c.body, 0.5f)
                for (sx in floatArrayOf(0.18f, 0.3f, 0.42f, 0.55f, 0.68f)) cv.drawLine(x0 + len * sx, y0 - h, x0 + len * sx - len * 0.01f, belly + h, p)
                p.style = Paint.Style.FILL
            }
            Croc.PLUSH -> {
                p.color = Color.argb(40, 255, 255, 255); val r = Random(c.seed)
                for (k in 0 until 900) cv.drawCircle(x0 + r.nextFloat() * len, y0 - h * 0.3f + r.nextFloat() * h * 1.6f, len * 0.003f, p)
            }
            else -> {}
        }
        // Scutes along the back.
        p.color = darker(c.body, 0.7f)
        var sx = 0.08f
        while (sx < 0.72f) {
            val x = x0 + len * sx; val y = yOnBack(sx, y0, h)
            cv.drawOval(RectF(x - len * 0.018f, y + h * 0.04f, x + len * 0.018f, y + h * 0.16f), p)
            sx += 0.045f
        }
        // Light: glaze, glass and brass shine; plush and wood stay matte.
        val shine = when (c.kind) { Croc.CLAY -> 120; Croc.GLASS -> 160; Croc.BRASS -> 150; else -> 30 }
        p.shader = LinearGradient(0f, y0 - h * 0.3f, 0f, belly + h * 0.4f, intArrayOf(Color.argb(shine, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(70, 0, 0, 0)), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(x0 - 10, y0 - h, x0 + len + 10, belly + h, p); p.shader = null
        cv.restore()
        // Eye and nostril.
        val ex = x0 + len * 0.805f; val ey = back - h * 0.14f
        p.color = if (c.kind == Croc.PLUSH) Color.WHITE else Color.rgb(250, 240, 200)
        cv.drawCircle(ex, ey, len * 0.022f, p)
        p.color = Color.rgb(25, 22, 18); cv.drawCircle(ex + len * 0.004f, ey, len * (if (c.kind == Croc.PLUSH) 0.013f else 0.008f), p)
        p.color = Color.WHITE; cv.drawCircle(ex + len * 0.008f, ey - len * 0.006f, len * 0.004f, p)
        p.color = darker(c.body, 0.5f); cv.drawCircle(x0 + len * 0.975f, back + h * 0.24f, len * 0.006f, p)
        // Smile and teeth.
        p.style = Paint.Style.STROKE; p.strokeWidth = len * 0.006f; p.color = darker(c.body, 0.45f)
        val mouth = Path().apply { moveTo(x0 + len * 0.97f, back + h * 0.5f); quadTo(x0 + len * 0.88f, back + h * 0.56f, x0 + len * 0.78f, back + h * 0.42f) }
        cv.drawPath(mouth, p); p.style = Paint.Style.FILL
        if (c.kind != Croc.PLUSH) {
            p.color = Color.rgb(250, 248, 240)
            for (k in 0 until 5) { val tx = x0 + len * (0.84f + k * 0.026f); val ty = back + h * (0.53f - k * 0.004f)
                cv.drawPath(Path().apply { moveTo(tx, ty); lineTo(tx + len * 0.008f, ty + h * 0.09f); lineTo(tx + len * 0.016f, ty); close() }, p) }
        }
        if (c.kind == Croc.PLUSH) { // a stitched tag on the tail
            p.color = c.accent; cv.drawRect(x0 + len * 0.12f, y0 + h * 0.58f, x0 + len * 0.17f, y0 + h * 0.85f, p)
        }
    }

    private fun yOnBack(t: Float, y0: Float, h: Float): Float = y0 + h * (0.55f - 0.65f * (t / 0.45f).coerceAtMost(1f)) - h * 0.1f

    private fun lying(c: Crocodile, wpx: Int): Bitmap {
        val hpx = (wpx * 0.42f).toInt()
        val bmp = Bitmap.createBitmap(wpx, hpx, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val len = wpx * 0.94f
        if (c.kind == Croc.GLASS) {
            val layer = cv.saveLayerAlpha(0f, 0f, wpx.toFloat(), hpx.toFloat(), 200)
            side(cv, c, wpx * 0.03f, hpx * 0.32f, len); cv.restoreToCount(layer)
        } else side(cv, c, wpx * 0.03f, hpx * 0.32f, len)
        return Photos2.trim(bmp)
    }

    private fun keyring(c: Crocodile, wpx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(wpx, (wpx * 0.9f).toInt(), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val len = wpx * 0.86f
        val y0 = wpx * 0.5f
        side(cv, c, wpx * 0.07f, y0, len)
        // Chain up from the back to a split ring.
        val ax = wpx * 0.5f; val ay = y0 - len * 0.03f
        p.style = Paint.Style.STROKE; p.strokeWidth = wpx * 0.012f
        p.shader = LinearGradient(0f, 0f, wpx.toFloat(), 0f, intArrayOf(0xFF8C9096.toInt(), 0xFFF0F2F4.toInt(), 0xFF7A7E85.toInt()), null, Shader.TileMode.MIRROR)
        var y = ay
        var k = 0
        while (y > wpx * 0.22f) { val r = RectF(ax - wpx * 0.018f, y - wpx * 0.045f, ax + wpx * 0.018f, y); if (k % 2 == 0) cv.drawOval(r, p) else cv.drawLine(ax, y, ax, y - wpx * 0.045f, p); y -= wpx * 0.04f; k++ }
        p.strokeWidth = wpx * 0.02f
        cv.drawCircle(ax, wpx * 0.13f, wpx * 0.1f, p)
        p.shader = null
        return Photos2.trim(bmp)
    }

    private fun sitting(c: Crocodile, wpx: Int): Bitmap {
        // A clay crocodile sitting up like a little person, tail curled round its feet.
        val hpx = (wpx * 1.25f).toInt()
        val bmp = Bitmap.createBitmap(wpx, hpx, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = wpx * 0.5f
        // Tail.
        p.color = darker(c.body, 0.85f)
        cv.drawPath(Path().apply { moveTo(cx - wpx * 0.2f, hpx * 0.9f); cubicTo(cx - wpx * 0.5f, hpx * 0.98f, cx + wpx * 0.2f, hpx * 1.02f, cx + wpx * 0.46f, hpx * 0.86f)
            quadTo(cx + wpx * 0.1f, hpx * 0.98f, cx - wpx * 0.1f, hpx * 0.84f); close() }, p)
        // Body (pear).
        p.color = c.body
        val bodyR = RectF(cx - wpx * 0.3f, hpx * 0.42f, cx + wpx * 0.3f, hpx * 0.95f)
        cv.drawOval(bodyR, p)
        p.color = c.belly; cv.drawOval(RectF(cx - wpx * 0.18f, hpx * 0.5f, cx + wpx * 0.18f, hpx * 0.92f), p)
        p.color = darker(c.belly, 0.85f); p.style = Paint.Style.STROKE; p.strokeWidth = wpx * 0.008f
        for (k in 1..5) { val y = hpx * (0.52f + k * 0.065f); cv.drawLine(cx - wpx * 0.15f, y, cx + wpx * 0.15f, y, p) }
        p.style = Paint.Style.FILL
        // Feet.
        p.color = darker(c.body, 0.85f)
        cv.drawOval(RectF(cx - wpx * 0.3f, hpx * 0.9f, cx - wpx * 0.06f, hpx * 0.98f), p)
        cv.drawOval(RectF(cx + wpx * 0.06f, hpx * 0.9f, cx + wpx * 0.3f, hpx * 0.98f), p)
        // Arms holding the belly.
        p.color = c.body
        cv.drawOval(RectF(cx - wpx * 0.36f, hpx * 0.55f, cx - wpx * 0.12f, hpx * 0.66f), p)
        cv.drawOval(RectF(cx + wpx * 0.12f, hpx * 0.55f, cx + wpx * 0.36f, hpx * 0.66f), p)
        // Head with a long snout pointing up and forward.
        val head = Path().apply {
            moveTo(cx - wpx * 0.26f, hpx * 0.46f)
            cubicTo(cx - wpx * 0.3f, hpx * 0.24f, cx - wpx * 0.12f, hpx * 0.12f, cx + wpx * 0.02f, hpx * 0.16f)
            cubicTo(cx + wpx * 0.2f, hpx * 0.1f, cx + wpx * 0.42f, hpx * 0.14f, cx + wpx * 0.46f, hpx * 0.24f)
            quadTo(cx + wpx * 0.47f, hpx * 0.32f, cx + wpx * 0.38f, hpx * 0.33f)
            cubicTo(cx + wpx * 0.2f, hpx * 0.36f, cx + wpx * 0.1f, hpx * 0.42f, cx + wpx * 0.24f, hpx * 0.48f)
            close()
        }
        cv.drawPath(head, p)
        p.color = c.belly; p.style = Paint.Style.STROKE; p.strokeWidth = wpx * 0.012f
        cv.drawPath(Path().apply { moveTo(cx + wpx * 0.44f, hpx * 0.29f); quadTo(cx + wpx * 0.2f, hpx * 0.33f, cx + wpx * 0.02f, hpx * 0.3f) }, p)
        p.style = Paint.Style.FILL
        // Eyes on top.
        for (ex in floatArrayOf(cx - wpx * 0.12f, cx + wpx * 0.03f)) {
            p.color = c.body; cv.drawCircle(ex, hpx * 0.15f, wpx * 0.07f, p)
            p.color = Color.WHITE; cv.drawCircle(ex, hpx * 0.145f, wpx * 0.05f, p)
            p.color = Color.rgb(25, 22, 18); cv.drawCircle(ex + wpx * 0.012f, hpx * 0.145f, wpx * 0.025f, p)
            p.color = Color.WHITE; cv.drawCircle(ex + wpx * 0.02f, hpx * 0.135f, wpx * 0.009f, p)
        }
        // Cheeks and a bow, because clay crocodiles always get one.
        p.color = Color.argb(90, 255, 120, 120); cv.drawCircle(cx + wpx * 0.12f, hpx * 0.27f, wpx * 0.035f, p)
        p.color = c.accent
        cv.drawPath(Path().apply { moveTo(cx, hpx * 0.47f); lineTo(cx - wpx * 0.1f, hpx * 0.43f); lineTo(cx - wpx * 0.1f, hpx * 0.51f); close() }, p)
        cv.drawPath(Path().apply { moveTo(cx, hpx * 0.47f); lineTo(cx + wpx * 0.1f, hpx * 0.43f); lineTo(cx + wpx * 0.1f, hpx * 0.51f); close() }, p)
        cv.drawCircle(cx, hpx * 0.47f, wpx * 0.025f, p)
        // Glaze.
        p.shader = RadialGradient(cx - wpx * 0.12f, hpx * 0.35f, wpx * 0.6f, intArrayOf(Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(60, 0, 0, 0)), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP)
        cv.drawRect(0f, 0f, wpx.toFloat(), hpx.toFloat(), p)
        p.xfermode = null; p.shader = null
        return Photos2.trim(bmp)
    }

    fun darker(c: Int, k: Float) = Color.argb(Color.alpha(c), (Color.red(c) * k).toInt(), (Color.green(c) * k).toInt(), (Color.blue(c) * k).toInt())
}

private object Photos2 {
    /** Crops a drawing to what was drawn, as a cut-out photo would be. */
    fun trim(b: Bitmap): Bitmap = app.cabinet.data.Photos.cropToAlpha(b) ?: b
}
