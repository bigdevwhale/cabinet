package app.cabinet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The photo store, all inside the app's own files:
 * `<id>_<n>.jpg` the photos as taken, `<id>_face.png` the face on the shelf (cut out, or the photo itself),
 * `<id>_t.png` a small copy of the face for lists.
 */
class Photos(context: Context) {
    private val app = context.applicationContext
    val dir = File(app.filesDir, "photos").apply { mkdirs() }
    /** Photos from the system camera land here first. */
    val inbox = File(app.cacheDir, "camera").apply { mkdirs() }

    fun original(id: Long, n: Int) = File(dir, "${id}_$n.jpg")
    fun face(id: Long) = File(dir, "${id}_face.png")
    fun thumb(id: Long) = File(dir, "${id}_t.png")

    private val cache = object : LruCache<String, Bitmap>(40 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Drops cached pictures of a thing whose photos changed. */
    fun forget(id: Long) {
        cache.snapshot().keys.filter { it.startsWith("$id:") }.forEach { cache.remove(it) }
    }

    fun cached(id: Long, big: Boolean, stamp: Long): Bitmap? = cache.get("$id:$big:$stamp")

    /** The face picture: [big] for the card, otherwise the thumbnail. [stamp] (the item's update time) keeps old copies out. */
    suspend fun load(id: Long, big: Boolean, stamp: Long): Bitmap? {
        val key = "$id:$big:$stamp"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val f = if (big) face(id) else thumb(id).takeIf { it.exists() } ?: face(id)
            if (!f.exists()) null else BitmapFactory.decodeFile(f.path)?.also { cache.put(key, it) }
        }
    }

    suspend fun loadOriginal(id: Long, n: Int, px: Int = 1600): Bitmap? = withContext(Dispatchers.IO) { decode(original(id, n), px) }

    /** Reads a picked or shot picture upright and no larger than [px] on its long side. */
    suspend fun read(uri: Uri, px: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return@runCatching null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= px) sample *= 2
            val bmp = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@runCatching null
            val rot = app.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            upright(bmp, rot).fit(px)
        }.getOrNull()
    }

    private fun decode(f: File, px: Int): Bitmap? {
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= px) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun upright(b: Bitmap, deg: Int): Bitmap =
        if (deg == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(deg.toFloat()) }, true)

    /** Keeps photo [n] of a thing. */
    suspend fun keep(id: Long, n: Int, bmp: Bitmap) = withContext(Dispatchers.IO) {
        original(id, n).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    /** Removes photo [n], shifting the later ones down. */
    suspend fun remove(id: Long, n: Int, count: Int) = withContext(Dispatchers.IO) {
        original(id, n).delete()
        for (k in n + 1 until count) original(id, k).renameTo(original(id, k - 1))
    }

    /** Makes photo [n] the first one. */
    suspend fun toFront(id: Long, n: Int) = withContext(Dispatchers.IO) {
        if (n == 0) return@withContext
        val tmp = File(dir, "${id}_swap.jpg")
        original(id, 0).renameTo(tmp); original(id, n).renameTo(original(id, 0)); tmp.renameTo(original(id, n))
    }

    fun deleteAll(id: Long) {
        dir.listFiles { f -> f.name.startsWith("${id}_") }?.forEach { it.delete() }
        forget(id)
    }

    /** What making a face gave. */
    data class Face(val cut: Boolean, val hue: Int, val aspect: Float, val picture: Bitmap)

    /**
     * Makes the shelf face of a thing from its first photo: cut out of the background when [cut] and the
     * segmenter can, otherwise the photo itself. Writes the face and its thumbnail.
     */
    suspend fun makeFace(id: Long, photo: Bitmap, cut: Boolean): Face = withContext(Dispatchers.Default) {
        val small = photo.fit(1280)
        val cutout = if (cut) cutOut(small) else null
        val face = cutout ?: small
        writeFace(id, face)
        Face(cutout != null, hue(face), face.width.toFloat() / face.height, face)
    }

    /** Writes a ready face (the demo draws its own). */
    suspend fun writeFace(id: Long, face: Bitmap) = withContext(Dispatchers.IO) {
        face(id).outputStream().use { face.compress(Bitmap.CompressFormat.PNG, 100, it) }
        thumb(id).outputStream().use { face.fit(360).compress(Bitmap.CompressFormat.PNG, 100, it) }
        forget(id)
    }

    /** The subject alone on transparency, cropped to it; null when there is no clear subject or no segmenter. */
    fun cutOut(src: Bitmap): Bitmap? = runCatching {
        val seg = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
        try {
            val fg = Tasks.await(seg.process(InputImage.fromBitmap(src, 0)), 40, TimeUnit.SECONDS).foregroundBitmap ?: return@runCatching null
            cropToAlpha(fg)
        } finally { seg.close() }
    }.getOrNull()

    companion object {
        fun Bitmap.fit(px: Int): Bitmap {
            val m = max(width, height)
            if (m <= px) return this
            val k = px.toFloat() / m
            return Bitmap.createScaledBitmap(this, (width * k).roundToInt().coerceAtLeast(1), (height * k).roundToInt().coerceAtLeast(1), true)
        }

        /** Crops to the visible part; null if almost nothing is visible. */
        fun cropToAlpha(b: Bitmap): Bitmap? {
            val w = b.width; val h = b.height
            val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
            var l = w; var t = h; var r = -1; var bo = -1; var n = 0
            for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 24) {
                n++; if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > bo) bo = y
            }
            if (r < 0 || n < w * h / 60) return null
            val pad = (max(r - l, bo - t) * 0.03f).roundToInt()
            l = (l - pad).coerceAtLeast(0); t = (t - pad).coerceAtLeast(0); r = (r + pad).coerceAtMost(w - 1); bo = (bo + pad).coerceAtMost(h - 1)
            return Bitmap.createBitmap(b, l, t, r - l + 1, bo - t + 1)
        }

        /** The thing's main colour as a hue, or -1 when it is mostly grey, black or white. */
        fun hue(b: Bitmap): Int {
            val s = Bitmap.createScaledBitmap(b, 48, (48 * b.height / b.width.coerceAtLeast(1)).coerceIn(8, 96), true)
            val px = IntArray(s.width * s.height); s.getPixels(px, 0, s.width, 0, 0, s.width, s.height)
            val bins = FloatArray(24); var total = 0f
            val hsv = FloatArray(3)
            for (p in px) {
                if ((p ushr 24) < 128) continue
                Color.colorToHSV(p, hsv)
                total++
                if (hsv[1] < 0.28f || hsv[2] < 0.22f) continue
                bins[(hsv[0] / 15f).toInt().coerceIn(0, 23)] += hsv[1] * hsv[2]
            }
            val best = bins.indices.maxBy { bins[it] }
            return if (total == 0f || bins[best] < total * 0.06f) -1 else best * 15 + 7
        }

        /** A picture on a plain ground, for the lens. */
        fun onGround(b: Bitmap, px: Int, ground: Int = Color.rgb(236, 234, 230)): Bitmap {
            val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val c = Canvas(out); c.drawColor(ground)
            val k = px * 0.92f / max(b.width, b.height)
            val w = b.width * k; val h = b.height * k
            c.drawBitmap(b, null, android.graphics.RectF((px - w) / 2, (px - h) / 2, (px + w) / 2, (px + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            return out
        }
    }
}
