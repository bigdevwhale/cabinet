package app.cabinet.ui.halls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Cabinet
import app.cabinet.data.Item
import app.cabinet.data.Series
import app.cabinet.data.Status
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.add.rememberAdder
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.R12
import app.cabinet.ui.components.R16
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.ThingTile
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

/** Order of the grid: 0 newest first, 1 by name, 2 by colour, 3 oldest arrivals first. */
fun ordered(items: List<Item>, order: Int): List<Item> = when (order) {
    1 -> items.sortedBy { it.title.ifBlank { "￿" }.lowercase() }
    2 -> items.sortedWith(compareBy<Item> { if (it.hue < 0) 999 else it.hue }.thenBy { it.id })
    3 -> items.sortedWith(compareBy<Item> { it.acquired ?: Long.MAX_VALUE }.thenBy { it.created })
    else -> items.sortedByDescending { it.created }
}

/** A hall: its cabinets as chips, its things as a grid of soft tiles of different heights. */
@Composable
fun HallScreen(id: Long, h: Home, nav: Nav, cabinet: Long? = null) {
    val c = LocalContext.current.container
    val hall = h.hallOf[id] ?: run { nav.close(); return }
    val adder = rememberAdder(h, nav)
    val cabs = h.cabinets.filter { it.hall == id }
    var only by remember { mutableStateOf(cabinet) }
    var order by remember { mutableIntStateOf(0) }
    var cabDialog by remember { mutableStateOf<Cabinet?>(null) }
    var newCabinet by remember { mutableStateOf(false) }
    var newSeries by remember { mutableStateOf(false) }
    val mine = h.items.filter { it.hall == id }
    val shown = ordered(mine.filter { only == null || it.cabinet == only }, order)
    val tint = h.tint(id)
    val series = h.series.filter { it.hall == id }
    val orders = listOf(R.string.order_new, R.string.order_name, R.string.order_colour, R.string.order_arrival)

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val colW = (maxWidth - 16.dp * 2 - 10.dp) / 2
            LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2), Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalItemSpacing = 10.dp) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        TopBar(hall.name, pluralStringResource(R.plurals.items_n, mine.count { it.owned }, mine.count { it.owned }), nav::close) {
                            IconBtn(Icons.Outlined.FactCheck, { nav.open(Overlay.Inventory(id)) }, desc = stringResource(R.string.inv_title))
                            IconBtn(Icons.Outlined.Settings, { nav.open(Overlay.HallEdit(id)) }, desc = stringResource(R.string.hall_edit))
                        }
                        // Cabinets as filters.
                        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Chip(stringResource(R.string.filter_all) + "  " + mine.size, only == null) { only = null }
                            for (cb in cabs) Chip(cb.name + "  " + mine.count { it.cabinet == cb.id }, only == cb.id) { only = if (only == cb.id) null else cb.id }
                            Box(Modifier.size(38.dp).clip(R12).background(C.surface2).clickable { newCabinet = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, stringResource(R.string.cabinet_new), tint = C.ink, modifier = Modifier.size(18.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.clip(R12).clickable { order = (order + 1) % orders.size }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.SwapVert, null, tint = C.muted, modifier = Modifier.size(18.dp))
                                Text(stringResource(orders[order]), style = T.Label.copy(fontSize = 13.sp), modifier = Modifier.padding(start = 4.dp))
                            }
                            Box(Modifier.weight(1f))
                            only?.let { cid -> h.cabinetOf[cid] }?.let { cb ->
                                Row(Modifier.clip(R12).clickable { cabDialog = cb }.padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Edit, null, tint = C.muted, modifier = Modifier.size(16.dp))
                                    Text(stringResource(R.string.cabinet_edit), style = T.Label.copy(fontSize = 13.sp), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                        if (series.isNotEmpty() && only == null) {
                            Row(Modifier.padding(top = 6.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (s in series) SeriesCard(s, h)
                                SeriesAdd { newSeries = true }
                            }
                        }
                        if (shown.isEmpty()) Text(stringResource(R.string.cabinet_empty), style = T.Body, modifier = Modifier.padding(vertical = 30.dp))
                    }
                }
                items(shown, key = { it.id }) { i ->
                    val photo = (colW / i.aspect.coerceIn(0.45f, 1.8f)).coerceIn(110.dp, 210.dp)
                    ThingTile(i, h.inv(i) + if (hall.pearl == i.id) "  ★" else "", tint, photoHeight = photo,
                        wishLabel = stringResource(R.string.status_wish), dustyLabel = stringResource(R.string.tag_no_story),
                        spareLabel = if (i.copies > 1) "×${i.copies}" else null) { nav.open(Overlay.Item(i.id)) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(stringResource(R.string.wish_new), { adder.start(id, only, Status.WISH) }, kind = Kind.SOFT)
            Btn(stringResource(R.string.add_t), { adder.start(id, only) }, kind = Kind.LIME, icon = Icons.Rounded.Add)
        }
    }

    if (newCabinet) NameDialog(stringResource(R.string.cabinet_new), "", { newCabinet = false }) { name ->
        c.write { dao.add(Cabinet(hall = id, name = name, sort = cabs.size)) }
    }
    cabDialog?.let { cb ->
        NameDialog(stringResource(R.string.cabinet_edit), cb.name, { cabDialog = null },
            onDelete = if (cabs.size > 1) ({
                val to = cabs.first { it.id != cb.id }
                c.write { dao.moveAll(cb.id, to.id); dao.drop(cb) }
                only = null
            }) else null) { name -> c.write { dao.save(cb.copy(name = name)) } }
    }
    if (newSeries) SeriesDialog(null, { newSeries = false }) { name, size -> c.write { dao.add(Series(hall = id, name = name, size = size)) } }
}

/** A series: how many of its pieces are here, the missing places dashed. */
@Composable
private fun SeriesCard(s: Series, h: Home) {
    val c = LocalContext.current.container
    val members = h.items.filter { it.series == s.id }
    val have = members.count { it.owned }
    var edit by remember { mutableStateOf(false) }
    Card(Modifier.width(220.dp), pad = 12.dp, onClick = { edit = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, style = T.Strong.copy(fontSize = 13.sp), maxLines = 1, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.series_have, have, s.size), style = T.Label.copy(color = if (have >= s.size) C.accent else C.muted))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in members.filter { it.owned }.take(6)) Box(Modifier.size(28.dp, 34.dp).clip(RoundedCornerShape(8.dp)).background(h.tint(i.hall))) { Face(i, Modifier.fillMaxSize().padding(2.dp), dim = false) }
            repeat((s.size - have).coerceIn(0, 6 - have.coerceAtMost(6))) {
                Canvas(Modifier.size(28.dp, 34.dp)) {
                    drawRoundRect(C.muted.copy(alpha = .6f), style = Stroke(1.3f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))), cornerRadius = CornerRadius(8f * density))
                }
            }
        }
    }
    if (edit) SeriesDialog(s, { edit = false }, onDelete = { c.write { dao.unseries(s.id); dao.drop(s) } }) { name, size -> c.write { dao.save(s.copy(name = name, size = size)) } }
}

@Composable
private fun SeriesAdd(onClick: () -> Unit) {
    Column(Modifier.width(120.dp).height(86.dp).clip(RoundedCornerShape(22.dp)).background(C.surface2).clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Icon(Icons.Rounded.Add, null, tint = C.ink, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.series_new), style = T.Label.copy(color = C.ink, fontSize = 13.sp))
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Scrim(onDismiss) {
        Card(Modifier.fillMaxWidth(), pad = 20.dp) {
            Text(title, style = T.Heading)
            Field(name, { name = it }, stringResource(R.string.name), Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) Btn(stringResource(R.string.delete), { onDelete(); onDismiss() }, Modifier.weight(1f), Kind.SOFT)
                Btn(stringResource(R.string.save), { onSave(name.trim()); onDismiss() }, Modifier.weight(1f), Kind.PRIMARY, enabled = name.isNotBlank())
            }
        }
    }
}

@Composable
private fun SeriesDialog(s: Series?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String, Int) -> Unit) {
    var name by remember { mutableStateOf(s?.name ?: "") }
    var size by remember { mutableStateOf(s?.size?.toString() ?: "") }
    Scrim(onDismiss) {
        Card(Modifier.fillMaxWidth(), pad = 20.dp) {
            Text(stringResource(if (s == null) R.string.series_new else R.string.series_edit), style = T.Heading)
            Text(stringResource(R.string.series_hint), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            Field(name, { name = it }, stringResource(R.string.name), Modifier.padding(top = 14.dp))
            Field(size.filter { it.isDigit() }.take(3), { size = it }, stringResource(R.string.series_size), Modifier.padding(top = 10.dp), number = true)
            val n = size.toIntOrNull() ?: 0
            if (n > 0) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(n.coerceAtMost(8)) {
                    Canvas(Modifier.size(24.dp, 30.dp)) {
                        drawRoundRect(C.muted.copy(alpha = .7f), style = Stroke(1.4f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))), cornerRadius = CornerRadius(6f * density))
                    }
                }
                if (n > 8) Text("+${n - 8}", style = T.Label)
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) Btn(stringResource(R.string.delete), { onDelete(); onDismiss() }, Modifier.weight(1f), Kind.SOFT)
                Btn(stringResource(R.string.save), { onSave(name.trim(), n); onDismiss() }, Modifier.weight(1f), Kind.PRIMARY, enabled = name.isNotBlank() && n in 2..500)
            }
        }
    }
}

/** The halls tab: every collection as a large soft card. */
@Composable
fun HallsScreen(h: Home, nav: Nav) {
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.halls_title), style = T.Hero)
                Text(stringResource(R.string.halls_sub), style = T.Small)
            }
        }
        items(h.halls.size) { k ->
            val hall = h.halls[k]
            val mine = h.items.filter { it.hall == hall.id }
            val owned = mine.count { it.owned }
            val pics = mine.filter { it.owned && it.photos > 0 }.sortedByDescending { it.created }.take(4)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(h.tint(hall.id))
                .clickable { nav.open(Overlay.Hall(hall.id)) }.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    for (i in pics) Face(i, Modifier.weight(1f).fillMaxSize(), dim = false)
                    repeat(4 - pics.size) { Box(Modifier.weight(1f)) }
                }
                Text(hall.name, style = T.Title.copy(fontSize = 22.sp), modifier = Modifier.padding(top = 12.dp))
                val cabs = h.cabinets.count { it.hall == hall.id }
                Text(pluralStringResource(R.plurals.items_n, owned, owned) + " · " + pluralStringResource(R.plurals.cabinets_n, cabs, cabs), style = T.Small.copy(color = C.ink2.copy(alpha = .75f)))
                val dusty = mine.count { app.cabinet.domain.Museum.dusty(it) }
                val wish = mine.count { !it.owned }
                if (dusty + wish > 0) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (dusty > 0) app.cabinet.ui.components.Tag(pluralStringResource(R.plurals.no_story_n, dusty, dusty))
                    if (wish > 0) app.cabinet.ui.components.Tag(stringResource(R.string.wish_short, wish), C.lime, C.onLime)
                }
            }
        }
        item {
            Btn(stringResource(R.string.hall_new), { nav.open(Overlay.HallEdit(0)) }, Modifier.padding(16.dp).fillMaxWidth(), Kind.SOFT, icon = Icons.Rounded.Add)
        }
    }
}

