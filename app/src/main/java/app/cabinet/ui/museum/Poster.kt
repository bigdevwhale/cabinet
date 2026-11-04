package app.cabinet.ui.museum

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Item
import app.cabinet.ui.theme.Light
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/** An exhibition poster: the theme, the title, the things on soft tiles, the museum's name. Shared as a picture. */
object Poster {
    suspend fun share(ctx: Context, title: String, sub: String, museum: String, items: List<Item>, tints: List<Int> = emptyList()) {
        val c = ctx.container
        val faces = items.mapNotNull { c.photos.load(it.id, true, it.updated) }
        val f = withContext(Dispatchers.Default) {
            val w = 1080; val h = 1350
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            cv.drawColor(Light.bg.toArgb())
            val bold = ResourcesCompat.getFont(ctx, R.font.manrope_extrabold)
            val semi = ResourcesCompat.getFont(ctx, R.font.manrope_semibold)
            val m = 80f
            p.typeface = semi; p.color = Light.accent.toArgb(); p.textSize = 34f
            cv.drawText(sub, m, 150f, p)
            p.typeface = bold; p.color = Light.ink.toArgb(); p.textSize = 92f; p.letterSpacing = -0.02f
            val lines = wrap(title, p, w - 2 * m)
            lines.forEachIndexed { i, l -> cv.drawText(l, m, 260f + i * 100f, p) }
            p.letterSpacing = 0f

            // Tiles, three to a row.
            val top = 260f + lines.size * 100f + 20f
            val gap = 24f
            val cols = if (faces.size <= 4) 2 else 3
            val tw = (w - 2 * m - gap * (cols - 1)) / cols
            val th = tw * 1.18f
            val pal = Light.halls.map { it.toArgb() }
            faces.take(cols * 2).forEachIndexed { k, b ->
                val x = m + (k % cols) * (tw + gap); val y = top + (k / cols) * (th + gap)
                if (y + th > h - 150) return@forEachIndexed
                p.color = tints.getOrNull(k) ?: pal[k % pal.size]
                cv.drawRoundRect(RectF(x, y, x + tw, y + th), 40f, 40f, p)
                val pad = 26f
                val s = min((tw - 2 * pad) / b.width, (th - 2 * pad) / b.height)
                val bw = b.width * s; val bh = b.height * s
                cv.drawBitmap(b, null, RectF(x + (tw - bw) / 2, y + (th - bh) / 2, x + (tw + bw) / 2, y + (th + bh) / 2), p)
            }
            p.typeface = semi; p.color = Light.muted.toArgb(); p.textSize = 34f
            cv.drawText(museum, m, h - 80f, p)
            p.color = Light.lime.toArgb()
            cv.drawRoundRect(RectF(w - m - 64f, h - 124f, w - m, h - 60f), 20f, 20f, p)
            val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
            File(dir, "exhibition.png").also { out -> out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun wrap(text: String, p: Paint, max: Float): List<String> {
        val out = mutableListOf<String>(); var cur = ""
        for (word in text.split(' ')) {
            val t = if (cur.isEmpty()) word else "$cur $word"
            if (p.measureText(t) > max && cur.isNotEmpty()) { out += cur; cur = word } else cur = t
        }
        if (cur.isNotEmpty()) out += cur
        return out.take(3)
    }
}
