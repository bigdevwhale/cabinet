package app.cabinet.ui.museum

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/** An exhibition poster: the title, the things on a lit shelf, the museum's name. Shared as a picture. */
object Poster {
    suspend fun share(ctx: Context, title: String, sub: String, museum: String, items: List<Item>) {
        val c = ctx.container
        val faces = items.mapNotNull { c.photos.load(it.id, true, it.updated) }
        val f = withContext(Dispatchers.Default) {
            val w = 1080; val h = 1350
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            // Deep green wall, a lamp from above.
            p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF243A2E.toInt(), 0xFF142019.toInt(), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
            p.shader = RadialGradient(w / 2f, h * 0.55f, w * 0.7f, 0x55F3D9A0, 0x00F3D9A0, Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null
            // Frame lines.
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFFC9A55A.toInt()
            cv.drawRect(40f, 40f, w - 40f, h - 40f, p)
            p.strokeWidth = 1.5f; cv.drawRect(54f, 54f, w - 54f, h - 54f, p)
            p.style = Paint.Style.FILL

            val serif = ResourcesCompat.getFont(ctx, R.font.garamond_medium)
            val italic = ResourcesCompat.getFont(ctx, R.font.garamond_italic)
            val sans = ResourcesCompat.getFont(ctx, R.font.inter_medium)
            p.textAlign = Paint.Align.CENTER
            p.typeface = sans; p.color = 0xFFC9A55A.toInt(); p.textSize = 30f; p.letterSpacing = 0.3f
            cv.drawText(sub.uppercase(), w / 2f, 170f, p)
            p.letterSpacing = 0f
            p.typeface = serif; p.color = 0xFFF6EEDC.toInt(); p.textSize = 96f
            val lines = wrap(title, p, w - 180f)
            lines.forEachIndexed { i, l -> cv.drawText(l, w / 2f, 290f + i * 104f, p) }

            // The shelf.
            val shelfY = h * 0.78f
            val row = faces.take(7)
            if (row.isNotEmpty()) {
                val tall = min(380f, (w - 160f) / row.sumOf { it.width.toDouble() / it.height }.toFloat().coerceAtLeast(1f))
                val widths = row.map { it.width * tall / it.height }
                val gap = ((w - 140f) - widths.sum()) / (row.size + 1)
                var x = 70f + gap
                for ((b, bw) in row.zip(widths)) {
                    p.color = 0x66000000; cv.drawOval(RectF(x + bw * 0.08f, shelfY - 12f, x + bw * 0.92f, shelfY + 10f), p)
                    cv.drawBitmap(b, null, RectF(x, shelfY - tall, x + bw, shelfY), p)
                    x += bw + gap
                }
            }
            p.shader = LinearGradient(0f, shelfY, 0f, shelfY + 46f, 0xFF8A5A3A.toInt(), 0xFF4A2D1A.toInt(), Shader.TileMode.CLAMP)
            cv.drawRect(70f, shelfY, w - 70f, shelfY + 46f, p); p.shader = null
            p.color = 0xFFA87650.toInt(); cv.drawRect(70f, shelfY, w - 70f, shelfY + 6f, p)

            p.typeface = italic; p.color = 0xFFE9DCC0.toInt(); p.textSize = 44f
            cv.drawText(museum, w / 2f, h - 110f, p)
            val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
            File(dir, "exhibition.png").also { out -> out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun wrap(text: String, p: Paint, max: Float): List<String> {
        val out = mutableListOf<String>(); var cur = ""
        for (w in text.split(' ')) {
            val t = if (cur.isEmpty()) w else "$cur $w"
            if (p.measureText(t) > max && cur.isNotEmpty()) { out += cur; cur = w } else cur = t
        }
        if (cur.isNotEmpty()) out += cur
        return out.take(3)
    }
}
