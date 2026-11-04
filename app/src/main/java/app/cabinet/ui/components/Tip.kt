package app.cabinet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Settings
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.Serif
import app.cabinet.ui.theme.T

/** The screens that explain themselves on the first visit: the key, the painting at the top, the text (title, then points). */
enum class TipOf(val art: Int, val text: Int) {
    MUSEUM(R.drawable.facade_3_day, R.array.tip_museum),
    HALLS(R.drawable.kind_cans, R.array.tip_halls),
    HALL(R.drawable.kind_figures, R.array.tip_hall),
    CABINET(R.drawable.facade_0_day, R.array.tip_cabinet),
    ITEM(R.drawable.kind_own, R.array.tip_item),
    EDIT(R.drawable.kind_paper, R.array.tip_edit),
    CHECK(R.drawable.kind_coins, R.array.tip_check),
    CATALOG(R.drawable.kind_records, R.array.tip_catalog),
    BOOK(R.drawable.facade_2_day, R.array.tip_book),
    SHOW(R.drawable.facade_7_day, R.array.tip_show),
    INVENTORY(R.drawable.kind_magnets, R.array.tip_inventory),
    HALL_NEW(R.drawable.kind_cars, R.array.tip_hall_new),
}

/**
 * The first time a screen opens, a card rises from the bottom: a painting, what this place is for, and two to
 * four things to do here. Shown once per screen; «Don't show tips» turns them all off (Settings brings them back).
 */
@Composable
fun Tip(tip: TipOf, s: Settings) {
    val c = LocalContext.current.container
    var gone by remember { mutableStateOf(false) }
    if (gone || s.tipsOff || tip.name in s.tips) return
    val lines = stringArrayResource(tip.text)
    fun close(all: Boolean) {
        gone = true
        c.write { settings.update { it.copy(tips = it.tips + tip.name, tipsOff = it.tipsOff || all) } }
    }
    Scrim({ close(false) }, bottom = true) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.surface)) {
            Box {
                Image(painterResource(if (C.dark) night(tip.art) else tip.art), null, Modifier.fillMaxWidth().aspectRatio(2.2f), contentScale = ContentScale.Crop)
                Text(stringResource(R.string.tip_caps).uppercase(), style = T.Caps.copy(color = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.padding(14.dp).background(androidx.compose.ui.graphics.Color(0x66000000), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Column(Modifier.navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp)) {
                Text(lines[0], style = T.Title)
                lines.drop(1).forEachIndexed { k, line ->
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accentSoft), contentAlignment = Alignment.Center) {
                            Text((k + 1).toString(), style = T.Strong.copy(color = C.accent, fontFamily = Serif, fontSize = 14.sp))
                        }
                        Text(line, style = T.Body, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    }
                }
                Btn(stringResource(R.string.tip_ok), { close(false) }, Modifier.padding(top = 20.dp).fillMaxWidth(), Kind.ACCENT)
                Text(stringResource(R.string.tip_off), style = T.Label, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 12.dp))
            }
        }
    }
}

private fun night(art: Int): Int = when (art) {
    R.drawable.facade_0_day -> R.drawable.facade_0_night
    R.drawable.facade_2_day -> R.drawable.facade_2_night
    R.drawable.facade_3_day -> R.drawable.facade_3_night
    R.drawable.facade_7_day -> R.drawable.facade_7_night
    else -> art
}
