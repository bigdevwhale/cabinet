package app.cabinet.ui.halls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Cabinet
import app.cabinet.data.Series
import app.cabinet.data.Status
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.add.rememberAdder
import app.cabinet.ui.art.Shelves
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Caps
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

@Composable
fun HallScreen(id: Long, h: Home, nav: Nav) {
    val c = LocalContext.current.container
    val hall = h.hallOf[id] ?: run { nav.close(); return }
    val adder = rememberAdder(h, nav)
    val cabs = h.cabinets.filter { it.hall == id }
    val mine = h.items.filter { it.hall == id }
    val owned = mine.count { it.owned }
    var newCabinet by remember { mutableStateOf(false) }
    var newSeries by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(hall.name, pluralStringResource(R.plurals.items_n, owned, owned), nav::close) {
                IconBtn(Icons.Outlined.FactCheck, { nav.open(Overlay.Inventory(id)) }, tint = C.muted)
                IconBtn(Icons.Outlined.Tune, { nav.open(Overlay.HallEdit(id)) }, tint = C.muted)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                h.item(hall.pearl)?.let { p ->
                    Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth(), color = C.back, onClick = { nav.open(Overlay.Item(p.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Face(p, Modifier.size(96.dp))
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(stringResource(R.string.pearl).uppercase(), style = T.Caps.copy(color = C.accent))
                                Text(p.title.ifBlank { h.inv(p) }, style = T.Heading.copy(color = androidx.compose.ui.graphics.Color(0xFFF6EEDC)))
                                if (p.place.isNotBlank()) Text(p.place, style = T.Small.copy(color = androidx.compose.ui.graphics.Color(0xFFCFC4B0)))
                            }
                        }
                    }
                }
                for (cab in cabs) {
                    val items = ordered(mine.filter { it.cabinet == cab.id }, cab.order)
                    Row(Modifier.fillMaxWidth().clickable { nav.open(Overlay.Cabinet(cab.id)) }.padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cab.name, style = T.Heading, modifier = Modifier.weight(1f))
                        Text(items.count { it.owned }.toString(), style = T.Label)
                        Icon(Icons.Outlined.ChevronRight, null, tint = C.muted)
                    }
                    Shelves(items.take(12), hall.code, Modifier.padding(horizontal = 12.dp), pearl = hall.pearl, tall = 84.dp, emptyRows = if (items.isEmpty()) 1 else 0,
                        onTap = { nav.open(Overlay.Item(it.id)) })
                    if (items.size > 12) Text(stringResource(R.string.cabinet_more, items.size - 12), style = T.Label.copy(color = C.accent),
                        modifier = Modifier.clickable { nav.open(Overlay.Cabinet(cab.id)) }.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(stringResource(R.string.cabinet_new), { newCabinet = true }, Modifier.weight(1f), Kind.GHOST)
                    Btn(stringResource(R.string.wish_new), { adder.start(id, null, Status.WISH) }, Modifier.weight(1f), Kind.GHOST)
                }

                val series = h.series.filter { it.hall == id }
                Caps(stringResource(R.string.series_caps), right = stringResource(R.string.series_new), onRight = { newSeries = true })
                if (series.isEmpty()) Text(stringResource(R.string.series_empty), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp))
                for (s in series) SeriesRow(s, h, nav)
                Gap(96.dp)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).shadow(8.dp, CircleShape).size(58.dp).clip(CircleShape).background(C.accent)
            .clickable { adder.start(id) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, stringResource(R.string.add_t), tint = C.onAccent, modifier = Modifier.size(30.dp))
        }
    }

    if (newCabinet) NameDialog(stringResource(R.string.cabinet_new), "", { newCabinet = false }) { name ->
        c.write { dao.add(Cabinet(hall = id, name = name, sort = cabs.size)) }
    }
    if (newSeries) SeriesDialog(null, { newSeries = false }) { name, size -> c.write { dao.add(Series(hall = id, name = name, size = size)) } }
}

/** A series: its places in order, the missing ones dashed. */
@Composable
private fun SeriesRow(s: Series, h: Home, nav: Nav) {
    val c = LocalContext.current.container
    val members = h.items.filter { it.series == s.id }.sortedBy { it.title }
    val have = members.count { it.owned }
    var edit by remember { mutableStateOf(false) }
    Card(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp).fillMaxWidth(), pad = 14.dp, onClick = { edit = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, style = T.Strong, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.series_have, have, s.size), style = T.Label.copy(color = if (have >= s.size) C.green else C.muted))
        }
        if (have >= s.size) Text(stringResource(R.string.series_full), style = T.Italic.copy(color = C.accent), modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in members) Face(i, Modifier.size(52.dp, 64.dp).clickable { nav.open(Overlay.Item(i.id)) })
            repeat((s.size - members.size).coerceAtLeast(0)) {
                Canvas(Modifier.size(52.dp, 64.dp)) {
                    drawRoundRect(C.muted.copy(alpha = .6f), style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))), cornerRadius = CornerRadius(8f * density))
                }
            }
        }
    }
    if (edit) SeriesDialog(s, { edit = false }, onDelete = { c.write { dao.unseries(s.id); dao.drop(s) } }) { name, size -> c.write { dao.save(s.copy(name = name, size = size)) } }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Scrim(onDismiss) {
        Card(Modifier.fillMaxWidth()) {
            Text(title, style = T.Heading)
            // A shelf with the cabinet's brass label, as it will look in the hall.
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(C.backLight, C.back))).height(70.dp), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().height(22.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(C.woodLight, C.wood, C.woodDark))))
                app.cabinet.ui.components.BrassLabel(name.ifBlank { "…" }, Modifier.padding(bottom = 4.dp))
            }
            Field(name, { name = it }, stringResource(R.string.name), Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) Btn(stringResource(R.string.delete), { onDelete(); onDismiss() }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.save), { onSave(name.trim()); onDismiss() }, Modifier.weight(1f), Kind.ACCENT, enabled = name.isNotBlank())
            }
        }
    }
}

@Composable
private fun SeriesDialog(s: Series?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String, Int) -> Unit) {
    var name by remember { mutableStateOf(s?.name ?: "") }
    var size by remember { mutableStateOf(s?.size?.toString() ?: "") }
    Scrim(onDismiss) {
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(if (s == null) R.string.series_new else R.string.series_edit), style = T.Heading)
            Text(stringResource(R.string.series_hint), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            Field(name, { name = it }, stringResource(R.string.name), Modifier.padding(top = 12.dp))
            Field(size.filter { it.isDigit() }.take(3), { size = it }, stringResource(R.string.series_size), Modifier.padding(top = 8.dp), number = true)
            // The empty places the series will have, live.
            val slots = (size.toIntOrNull() ?: 0).coerceIn(0, 500)
            if (slots > 0) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(slots.coerceAtMost(8)) {
                    Canvas(Modifier.size(26.dp, 34.dp)) {
                        drawRoundRect(C.muted.copy(alpha = .7f),
                            style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))), cornerRadius = CornerRadius(5f * density))
                    }
                }
                if (slots > 8) Text("+${slots - 8}", style = T.Label)
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) Btn(stringResource(R.string.delete), { onDelete(); onDismiss() }, Modifier.weight(1f), Kind.GHOST)
                val n = size.toIntOrNull() ?: 0
                Btn(stringResource(R.string.save), { onSave(name.trim(), n); onDismiss() }, Modifier.weight(1f), Kind.ACCENT, enabled = name.isNotBlank() && n in 2..500)
            }
        }
    }
}

@Composable
fun CabinetScreen(id: Long, h: Home, nav: Nav) {
    val c = LocalContext.current.container
    val cab = h.cabinetOf[id] ?: run { nav.close(); return }
    val hall = h.hallOf[cab.hall] ?: return
    val adder = rememberAdder(h, nav)
    val items = ordered(h.items.filter { it.cabinet == id }, cab.order)
    var rename by remember { mutableStateOf(false) }
    val siblings = h.cabinets.filter { it.hall == cab.hall }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(cab.name, hall.name, nav::close) { IconBtn(Icons.Outlined.Tune, { rename = true }, tint = C.muted) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((o, label) in listOf(0 to R.string.order_hand, 1 to R.string.order_colour, 2 to R.string.order_arrival, 3 to R.string.order_name))
                        app.cabinet.ui.components.Chip(stringResource(label), cab.order == o) { c.write { dao.save(cab.copy(order = o)) } }
                }
                Text(stringResource(if (cab.order == 0) R.string.order_hand_hint else R.string.order_auto_hint), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                Shelves(items, hall.code, Modifier.padding(horizontal = 12.dp), pearl = hall.pearl, onTap = { nav.open(Overlay.Item(it.id)) },
                    onMove = if (cab.order == 0) { list -> c.write { dao.save(list.mapIndexed { k, i -> i.copy(sort = k + 1) }) } } else null)
                if (items.isEmpty()) Text(stringResource(R.string.cabinet_empty), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                Gap(96.dp)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).shadow(8.dp, CircleShape).size(58.dp).clip(CircleShape).background(C.accent)
            .clickable { adder.start(cab.hall, id) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, stringResource(R.string.add_t), tint = C.onAccent, modifier = Modifier.size(30.dp))
        }
    }
    if (rename) NameDialog(stringResource(R.string.cabinet_edit), cab.name, { rename = false },
        onDelete = if (siblings.size > 1) ({
            val to = siblings.first { it.id != id }
            c.write { dao.moveAll(id, to.id); dao.drop(cab) }
            nav.close()
        }) else null) { name -> c.write { dao.save(cab.copy(name = name)) } }
}
