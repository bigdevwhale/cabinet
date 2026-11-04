package app.cabinet.ui.halls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import app.cabinet.R
import app.cabinet.data.Item
import app.cabinet.domain.Museum
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.art.ShelfStrip
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.Kind
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

/** Shelf order: 0 by hand, 1 by colour (a rainbow), 2 by arrival, 3 by name. */
fun ordered(items: List<Item>, order: Int): List<Item> = when (order) {
    1 -> items.sortedWith(compareBy<Item> { if (it.hue < 0) 999 else it.hue }.thenBy { it.sort })
    2 -> items.sortedWith(compareBy<Item> { it.acquired ?: Long.MAX_VALUE }.thenBy { it.created })
    3 -> items.sortedBy { it.title.ifBlank { "￿" }.lowercase() }
    else -> items.sortedWith(compareBy<Item> { it.sort }.thenBy { it.id })
}

@Composable
fun HallsScreen(h: Home, nav: Nav) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.halls_title), style = T.Title)
            Text(stringResource(R.string.halls_sub), style = T.Small)
        }
        for (hall in h.halls) {
            val mine = h.items.filter { it.hall == hall.id }
            val owned = mine.count { it.owned }
            val cabs = h.cabinets.count { it.hall == hall.id }
            Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth(), onClick = { nav.open(Overlay.Hall(hall.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(hall.name, style = T.Heading)
                        Text(pluralStringResource(R.plurals.items_n, owned, owned) + " · " + pluralStringResource(R.plurals.cabinets_n, cabs, cabs), style = T.Small)
                        val dusty = mine.count { Museum.dusty(it) }
                        val wish = mine.count { !it.owned }
                        val done = h.series.filter { it.hall == hall.id }.count { s -> mine.count { it.series == s.id && it.owned } >= s.size }
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (dusty > 0) Tag(pluralStringResource(R.plurals.dusty_short, dusty, dusty), C.surface2, C.ink2)
                            if (wish > 0) Tag(stringResource(R.string.wish_short, wish), C.accentSoft, C.accent)
                            if (done > 0) Tag(stringResource(R.string.series_done_short, done), C.greenSoft, C.green)
                        }
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = C.muted)
                }
                val show = (h.item(hall.pearl)?.let { listOf(it) } ?: emptyList()) + mine.filter { it.owned && it.id != hall.pearl && !Museum.dusty(it) }
                if (show.isNotEmpty()) ShelfStrip(show.take(10), Modifier.padding(top = 12.dp), tall = 60.dp)
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.Center) {
            Btn(stringResource(R.string.hall_new), { nav.open(Overlay.HallEdit(0)) }, Modifier.fillMaxWidth(), Kind.GHOST, icon = Icons.Rounded.Add)
        }
        Gap(8.dp)
    }
}

/** A small coloured label. */
@Composable
fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Text(text, style = T.Label.copy(color = fg, fontSize = 12.sp), modifier = Modifier.background(bg, androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
}
