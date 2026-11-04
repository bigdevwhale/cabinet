package app.cabinet.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.data.Item
import app.cabinet.domain.Museum
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

enum class Filter { ALL, DUSTY, WISH, TRADE, PEARLS }

/** Lets other screens open the catalogue already filtered. */
object Catalog {
    var preset by mutableStateOf<Filter?>(null)
}

/** True when a thing answers a free-text query: name, number, place, story, fields, tags, where it lives. */
fun matches(i: Item, q: String, h: Home): Boolean {
    val words = q.lowercase().split(' ', ',').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val hay = listOf(i.title, h.inv(i), i.place, i.story, i.fields.replace('=', ' '), i.tags, i.storage, i.price,
        h.hallOf[i.hall]?.name ?: "", h.cabinetOf[i.cabinet]?.name ?: "", h.series.firstOrNull { it.id == i.series }?.name ?: "").joinToString(" ").lowercase()
    return words.all { it in hay }
}

/** The card catalogue: every thing, searchable, with the funds a collector keeps apart. */
@Composable
fun CatalogScreen(h: Home, nav: Nav) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Catalog.preset ?: Filter.ALL) }
    androidx.compose.runtime.LaunchedEffect(Catalog.preset) { Catalog.preset?.let { filter = it; Catalog.preset = null } }
    var hall by remember { mutableStateOf<Long?>(null) }
    val pearls = h.halls.map { it.pearl }.toSet()
    val list = h.items.filter { i ->
        (hall == null || i.hall == hall) && when (filter) {
            Filter.ALL -> true
            Filter.DUSTY -> Museum.dusty(i)
            Filter.WISH -> !i.owned
            Filter.TRADE -> i.owned && i.copies > 1
            Filter.PEARLS -> i.id in pearls || (i.owned && i.condition == 4)
        } && matches(i, query, h)
    }.sortedByDescending { it.inv }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            Text(stringResource(R.string.catalog_title), style = T.Title, modifier = Modifier.padding(start = 4.dp))
            Field(query, { query = it }, stringResource(R.string.search), Modifier.padding(top = 8.dp), hint = stringResource(R.string.search_hint))
        }
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((f, l) in listOf(Filter.ALL to R.string.filter_all, Filter.DUSTY to R.string.filter_dusty, Filter.WISH to R.string.filter_wish,
                Filter.TRADE to R.string.filter_trade, Filter.PEARLS to R.string.filter_pearls)) {
                val n = h.items.count { i -> (hall == null || i.hall == hall) && when (f) {
                    Filter.ALL -> true; Filter.DUSTY -> Museum.dusty(i); Filter.WISH -> !i.owned
                    Filter.TRADE -> i.owned && i.copies > 1; Filter.PEARLS -> i.id in pearls || (i.owned && i.condition == 4) } }
                if (n > 0 || f == Filter.ALL || f == filter) Chip(stringResource(l) + "  " + n, filter == f) { filter = f }
            }
        }
        if (h.halls.size > 1) Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(stringResource(R.string.filter_all_halls), hall == null) { hall = null }
            for (hl in h.halls) Chip(hl.name, hall == hl.id) { hall = hl.id }
        }
        Text(pluralStringResource(R.plurals.found_n, list.size, list.size) + (if (filter == Filter.TRADE) " · " + stringResource(R.string.trade_hint) else ""),
            style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            items(list, key = { it.id }) { i -> CatalogRow(i, h, nav) }
            if (list.isEmpty()) item {
                Text(stringResource(if (query.isNotBlank()) R.string.search_none else R.string.filter_none), style = T.Body.copy(color = C.muted), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
        }
    }
}

@Composable
private fun CatalogRow(i: Item, h: Home, nav: Nav) {
    Card(Modifier.padding(bottom = 8.dp).fillMaxWidth(), pad = 12.dp, onClick = { nav.open(Overlay.Item(i.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Face(i, Modifier.size(58.dp).background(C.surface2, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(4.dp), dust = if (Museum.dusty(i)) .55f else 0f)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(i.title.ifBlank { stringResource(R.string.untitled) }, style = T.Strong.copy(color = if (i.title.isBlank()) C.muted else C.ink), maxLines = 1)
                Text(h.inv(i) + " · " + listOfNotNull(h.hallOf[i.hall]?.name, h.cabinetOf[i.cabinet]?.name).joinToString(" › "), style = T.Small, maxLines = 1)
                val extra = when {
                    !i.owned -> stringResource(R.string.status_wish)
                    i.copies > 1 -> pluralStringResource(R.plurals.copies_spare, i.copies - 1, i.copies, i.copies - 1)
                    i.place.isNotBlank() -> i.place
                    else -> null
                }
                if (extra != null) Text(extra, style = T.Label.copy(color = if (!i.owned || i.copies > 1) C.accent else C.ink2), maxLines = 1)
            }
        }
    }
}
