package app.cabinet.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.cabinet.R
import app.cabinet.ui.theme.C

/** The dioramas are rendered by tools/art/render.sh (Blender) and packed at 1080×700 with a transparent background. */
private const val ASPECT = 1080f / 700f

private val DAY = intArrayOf(R.drawable.facade_0_day, R.drawable.facade_1_day, R.drawable.facade_2_day, R.drawable.facade_3_day,
    R.drawable.facade_4_day, R.drawable.facade_5_day, R.drawable.facade_6_day, R.drawable.facade_7_day)
private val NIGHT = intArrayOf(R.drawable.facade_0_night, R.drawable.facade_1_night, R.drawable.facade_2_night, R.drawable.facade_3_night,
    R.drawable.facade_4_night, R.drawable.facade_5_night, R.drawable.facade_6_night, R.drawable.facade_7_night)

/**
 * The house of the collection as a miniature on a walnut plinth, growing with it: a cabinet of curiosities in a
 * corner of a hallway (stage 0 and the cover), a shop-front room, a house-museum, a portico, a wing, a dome,
 * a second wing, a crest with flags. By day, or lit up at night with the dark theme. The little painted figures
 * on the plaza are part of each render.
 */
@Composable
fun Facade(stage: Int, @Suppress("UNUSED_PARAMETER") visitors: Int = 0, modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") seed: Long = 0) {
    val st = stage.coerceIn(0, 7)
    Image(painterResource(if (C.dark) NIGHT[st] else DAY[st]), null, modifier.fillMaxWidth().aspectRatio(ASPECT), contentScale = ContentScale.Fit)
}
