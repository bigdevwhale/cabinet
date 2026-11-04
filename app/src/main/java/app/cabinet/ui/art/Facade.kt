package app.cabinet.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.ui.theme.C
import kotlin.random.Random

/** The facades are painted by tools/art/facade.py at 1080×780, the top 140 px left to the sky. */
private const val ASPECT = 1080f / 780f
/** Where the plaza begins in those paintings, as a share of the height. */
private const val GROUND = 588f / 780f

private val DAY = intArrayOf(R.drawable.facade_0_day, R.drawable.facade_1_day, R.drawable.facade_2_day, R.drawable.facade_3_day,
    R.drawable.facade_4_day, R.drawable.facade_5_day, R.drawable.facade_6_day, R.drawable.facade_7_day)
private val NIGHT = intArrayOf(R.drawable.facade_0_night, R.drawable.facade_1_night, R.drawable.facade_2_night, R.drawable.facade_3_night,
    R.drawable.facade_4_night, R.drawable.facade_5_night, R.drawable.facade_6_night, R.drawable.facade_7_night)
private val PEOPLE = intArrayOf(R.drawable.visitor_0, R.drawable.visitor_1, R.drawable.visitor_2, R.drawable.visitor_3,
    R.drawable.visitor_4, R.drawable.visitor_5, R.drawable.visitor_6)

/**
 * The house of the collection, growing with it: a cabinet of curiosities in a hallway (stage 0 and the cover),
 * a shop-front room, a house-museum, a portico, a wing, a dome, a second wing, and a crest with flags.
 * By day or lit up at night, with the theme. [visitors] stand on the plaza in front.
 */
@Composable
fun Facade(stage: Int, visitors: Int, modifier: Modifier = Modifier, seed: Long = 0) {
    val st = stage.coerceIn(0, 7)
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(ASPECT)) {
        Image(painterResource(if (C.dark) NIGHT[st] else DAY[st]), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        val spots = remember(st, visitors, seed) { spots(st, visitors, seed) }
        val w = maxWidth; val h = maxHeight
        for ((x, depth, who) in spots) {
            val tall = h * (0.085f + 0.04f * depth)
            val width = tall * (80f / 140f)
            Image(painterResource(PEOPLE[who]), null,
                Modifier.offset(x = w * x - width / 2, y = h * (GROUND + 0.035f + 0.11f * depth) - tall).width(width).height(tall))
        }
    }
}

/** Where visitors stand: across the plaza, clear of the doorway, nearer ones lower and larger. */
private fun spots(stage: Int, visitors: Int, seed: Long): List<Triple<Float, Float, Int>> {
    val n = visitors.coerceIn(0, if (stage == 0) 2 else 9)
    val r = Random(seed * 31 + stage)
    val out = mutableListOf<Triple<Float, Float, Int>>()
    var tries = 0
    while (out.size < n && tries++ < 200) {
        val x = 0.06f + r.nextFloat() * 0.88f
        if (stage > 0 && kotlin.math.abs(x - 0.5f) < 0.05f) continue // the doorway stays clear
        if (stage == 0 && kotlin.math.abs(x - 0.5f) < 0.2f) continue // and so does the cabinet
        if (out.any { kotlin.math.abs(it.first - x) < 0.055f }) continue
        out += Triple(x, r.nextFloat(), r.nextInt(PEOPLE.size))
    }
    return out.sortedBy { it.second }
}

@Suppress("unused")
@Composable
private fun Preview() = Box(Modifier.width(360.dp)) { Facade(5, 6) }
