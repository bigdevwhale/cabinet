package app.cabinet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * «Do I have this already?» A small image model (MobileNet V3, from MediaPipe's image embedder) turns a picture
 * into a vector; alike things get alike vectors. It runs in Google Play services' TensorFlow Lite, on the phone.
 * Without it, a much plainer look (colours and shape of a tiny copy) stands in, so the check still works.
 */
class Lens(context: Context) {
    private val app = context.applicationContext
    private val lock = Mutex()
    private var model: InterpreterApi? = null
    private var failed = false

    /** True once the image model has loaded; false while it can't (no Play services) and the plain look is used. */
    var smart = false
        private set

    private fun open(): InterpreterApi? {
        if (model != null || failed) return model
        model = runCatching {
            Tasks.await(TfLite.initialize(app), 30, TimeUnit.SECONDS)
            val fd = app.assets.openFd("embedder.tflite")
            val buf = FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            InterpreterApi.create(buf, InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY))
        }.getOrNull()
        if (model == null) failed = true
        smart = model != null
        return model
    }

    /** The look of a picture (a cut-out is put on a plain ground first). */
    suspend fun look(picture: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        lock.withLock {
            val m = open()
            if (m != null) runCatching { deep(m, picture) }.getOrNull() ?: plain(picture) else plain(picture)
        }
    }

    private fun deep(m: InterpreterApi, picture: Bitmap): FloatArray {
        val s = Photos.onGround(picture, SIZE)
        val px = IntArray(SIZE * SIZE); s.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        val input = ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder())
        for (p in px) { input.putFloat(Color.red(p) / 255f); input.putFloat(Color.green(p) / 255f); input.putFloat(Color.blue(p) / 255f) }
        input.rewind()
        val out = Array(1) { FloatArray(DIM) }
        m.run(input, out)
        return norm(out[0])
    }

    /** Colours and shape of an 8×8 copy plus a hue histogram: rough, but needs nothing. */
    private fun plain(picture: Bitmap): FloatArray {
        val s = Bitmap.createScaledBitmap(Photos.onGround(picture, 64), 8, 8, true)
        val v = FloatArray(PLAIN)
        val px = IntArray(64); s.getPixels(px, 0, 8, 0, 0, 8, 8)
        val hsv = FloatArray(3)
        px.forEachIndexed { i, p ->
            v[i * 3] = Color.red(p) / 255f - .5f; v[i * 3 + 1] = Color.green(p) / 255f - .5f; v[i * 3 + 2] = Color.blue(p) / 255f - .5f
            Color.colorToHSV(p, hsv)
            if (hsv[1] > .25f) v[192 + (hsv[0] / 30f).toInt().coerceIn(0, 11)] += hsv[1] * 1.5f
        }
        return norm(v)
    }

    companion object {
        const val SIZE = 224
        const val DIM = 1024
        const val PLAIN = 204

        private fun norm(v: FloatArray): FloatArray {
            var s = 0f; for (x in v) s += x * x
            val k = if (s > 0f) 1f / sqrt(s) else 0f
            return FloatArray(v.size) { v[it] * k }
        }

        fun pack(v: FloatArray): ByteArray = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { v.forEach { putFloat(it) } }.array()

        fun unpack(b: ByteArray): FloatArray {
            val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(b.size / 4) { bb.getFloat() }
        }

        fun likeness(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var s = 0f; for (i in a.indices) s += a[i] * b[i]
            return s
        }
    }
}

/** One possible match from the lens, best first. */
data class Match(val item: Item, val score: Float) {
    /** 2: almost surely the same, 1: looks alike, 0: only a little. */
    val level: Int get() = when {
        score >= SURE -> 2
        score >= ALIKE -> 1
        else -> 0
    }

    companion object {
        const val SURE = 0.86f
        const val ALIKE = 0.72f

        /** The things most like [look], best first. Only looks of the same kind can be compared. */
        fun find(look: FloatArray, items: List<Item>, top: Int = 6): List<Match> =
            items.mapNotNull { i -> i.look?.let { Match(i, Lens.likeness(look, Lens.unpack(it))) } }
                .filter { it.score > 0.3f }.sortedByDescending { it.score }.take(top)
    }
}
