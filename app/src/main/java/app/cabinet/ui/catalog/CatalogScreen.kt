package app.cabinet.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.data.Item
import app.cabinet.domain.Museum
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.R16
import app.cabinet.ui.components.R22
import app.cabinet.ui.components.Tag
import app.cabinet.ui.components.lift
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

enum class Filter { ALL, DUSTY, WISH, TRADE, PEARLS }

/** Lets other screens open the search already filtered. */
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

/** Search: by words across every card, or by a photo («Do I have it?»), with the funds a collector keeps apart. */
@Composable
fun CatalogScreen(h: Home, nav: Nav) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Catalog.preset ?: Filter.ALL) }
    LaunchedEffect(Catalog.preset) { Catalog.preset?.let { filter = it; Catalog.preset = null } }
    var hall by remember { mutableStateOf<Long?>(null) }
    val pearls = h.halls.map { it.pearl }.toSet()
    fun pass(i: Item, f: Filter) = (hall == null || i.hall == hall) && when (f) {
        Filter.ALL -> true
        Filter.DUSTY -> Museum.dusty(i)
        Filter.WISH -> !i.owned
        Filter.TRADE -> i.owned && i.copies > 1
        Filter.PEARLS -> i.id in pearls || (i.owned && i.condition == 4)
    }
    val list = h.items.filter { pass(it, filter) && matches(it, query, h) }.sortedByDescending { it.inv }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text(stringResource(R.string.catalog_title), style = T.Hero, modifier = Modifier.padding(start = 2.dp))
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).lift(R16).clip(R16).background(C.surface).padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, tint = C.muted, modifier = Modifier.size(20.dp))
                        Box(Modifier.padding(start = 10.dp).weight(1f)) {
                            if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = T.Body.copy(color = C.muted), maxLines = 1)
                            BasicTextField(query, { query = it }, textStyle = T.Body.copy(color = C.ink), singleLine = true, cursorBrush = SolidColor(C.accent), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    // Search by photo: the «Do I have it?» lens.
                    Box(Modifier.padding(start = 8.dp).size(50.dp).clip(R16).background(C.accent).clickable { nav.open(Overlay.Check()) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PhotoCamera, stringResource(R.string.check_title), tint = C.onAccent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((f, l) in listOf(Filter.ALL to R.string.filter_all, Filter.DUSTY to R.string.filter_dusty, Filter.WISH to R.string.filter_wish,
                    Filter.TRADE to R.string.filter_trade, Filter.PEARLS to R.string.filter_pearls)) {
                    val n = h.items.count { pass(it, f) }
                    if (n > 0 || f == Filter.ALL || f == filter) Chip(stringResource(l) + "  " + n, filter == f) { filter = f }
                }
            }
            if (h.halls.size > 1) Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(stringResource(R.string.filter_all_halls), hall == null) { hall = null }
                for (hl in h.halls) Chip(hl.name, hall == hl.id) { hall = if (hall == hl.id) null else hl.id }
            }
            Text(pluralStringResource(R.plurals.found_n, list.size, list.size) + (if (filter == Filter.TRADE) " · " + stringResource(R.string.trade_hint) else ""),
                style = T.Small, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp))
        }
        items(list, key = { it.id }) { i -> CatalogRow(i, h, nav) }
        if (list.isEmpty()) item {
            Text(stringResource(if (query.isNotBlank()) R.string.search_none else R.string.filter_none), style = T.Body.copy(color = C.muted), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp))
        }
    }
}

@Composable
private fun CatalogRow(i: Item, h: Home, nav: Nav) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth().lift().clip(R22).background(C.surface).clickable { nav.open(Overlay.Item(i.id)) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(R16).background(h.tint(i.hall))) { Face(i, Modifier.fillMaxSize().padding(5.dp)) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(i.title.ifBlank { stringResource(R.string.untitled) }, style = T.Strong.copy(color = if (i.title.isBlank()) C.muted else C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(h.inv(i) + " · " + listOfNotNull(h.hallOf[i.hall]?.name, h.cabinetOf[i.cabinet]?.name).joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!i.owned) Tag(stringResource(R.string.status_wish), C.lime, C.onLime)
                else if (Museum.dusty(i)) Tag(stringResource(R.string.tag_no_story), C.surface2)
                if (i.copies > 1) Tag("×${i.copies}", C.surface2)
                if (i.owned && i.place.isNotBlank()) Text(i.place, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
