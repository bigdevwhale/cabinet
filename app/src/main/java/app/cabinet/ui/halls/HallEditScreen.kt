package app.cabinet.ui.halls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Seal
import app.cabinet.domain.Museum
import app.cabinet.domain.Presets
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch

/** A new hall from a ready kind, or an existing hall's name, number code and card fields. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HallEditScreen(id: Long, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val hall = h.hallOf[id]
    var preset by remember { mutableStateOf(Presets.ALL.first()) }
    var name by remember { mutableStateOf(hall?.name ?: preset.names(ru).hall) }
    var code by remember { mutableStateOf(hall?.code ?: Museum.code(name)) }
    val fields = remember { mutableStateListOf<String>().apply { addAll(hall?.fieldList ?: preset.names(ru).fields) } }
    var newField by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val count = h.items.count { it.hall == id }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(if (hall == null) R.string.hall_new else R.string.hall_edit), null, nav::close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (hall == null) {
                Text(stringResource(R.string.hall_kind), style = T.Caps, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (p in Presets.ALL) Chip(p.names(ru).hall, p == preset) {
                        if (name == preset.names(ru).hall || name.isBlank()) { name = p.names(ru).hall; code = Museum.code(name) }
                        preset = p; fields.clear(); fields.addAll(p.names(ru).fields)
                    }
                }
                Text(stringResource(R.string.hall_kind_hint, preset.names(ru).cabinets.joinToString(", ")), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            }
            Field(name, { name = it; if (hall == null) code = Museum.code(it) }, stringResource(R.string.hall_name), Modifier.padding(top = 14.dp))
            Field(code, { code = it.uppercase().take(4) }, stringResource(R.string.hall_code), Modifier.padding(top = 8.dp), hint = "КР")
            Text(stringResource(R.string.hall_code_hint, Museum.inv(code, 147)), style = T.Small, modifier = Modifier.padding(top = 4.dp))

            Text(stringResource(R.string.hall_fields), style = T.Caps, modifier = Modifier.padding(top = 20.dp))
            Text(stringResource(R.string.hall_fields_hint), style = T.Small, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (f in fields.toList()) Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, C.line, RoundedCornerShape(50)).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(f, style = T.Label.copy(color = C.ink))
                    Icon(Icons.Rounded.Close, null, tint = C.muted, modifier = Modifier.padding(start = 4.dp).size(18.dp).clickable { fields.remove(f) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(newField, { newField = it }, stringResource(R.string.hall_field_new), Modifier.weight(1f).padding(top = 8.dp))
                Box(Modifier.padding(start = 8.dp, top = 8.dp).size(48.dp).clip(CircleShape).background(if (newField.isBlank()) C.surface2 else C.accent)
                    .clickable(enabled = newField.isNotBlank()) { val f = newField.trim().replace("=", ""); if (f !in fields) fields.add(f); newField = "" },
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = if (newField.isBlank()) C.muted else C.onAccent) }
            }
            if (hall != null) {
                Btn(stringResource(R.string.hall_delete), { confirm = true }, Modifier.padding(top = 28.dp).fillMaxWidth(), Kind.GHOST)
            }
        }
        Box(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().padding(16.dp)) {
            Btn(stringResource(if (hall == null) R.string.hall_open else R.string.save), {
                val n = name.trim(); val cd = code.trim().ifBlank { Museum.code(n) }
                scope.launch {
                    if (hall == null) {
                        val newId = c.addHall(preset, n)
                        c.dao.hall(newId)?.let { c.dao.save(it.copy(code = cd, fields = fields.joinToString("\n"))) }
                        nav.replace(Overlay.Hall(newId))
                    } else {
                        c.dao.save(hall.copy(name = n, code = cd, fields = fields.joinToString("\n")))
                        nav.close()
                    }
                }
            }, Modifier.fillMaxWidth(), Kind.ACCENT, enabled = name.isNotBlank())
        }
    }
    if (confirm && hall != null) Scrim({ confirm = false }) {
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.hall_delete_t, hall.name), style = T.Heading)
            Text(pluralStringResource(R.plurals.hall_delete_b, count, count), style = T.Body, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(stringResource(R.string.cancel), { confirm = false }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.delete), {
                    scope.launch {
                        for (i in c.dao.itemsNow().filter { it.hall == hall.id }) c.delete(i)
                        c.dao.cabinetsNow().filter { it.hall == hall.id }.forEach { c.dao.drop(it) }
                        c.dao.drop(hall)
                    }
                    nav.stack.clear()
                }, Modifier.weight(1f), Kind.RED)
            }
        }
    }
}

/** The season's inventory: walk the hall, tick what is in its place, get the seal. */
@Composable
fun InventoryScreen(hallId: Long, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val hall = h.hallOf[hallId] ?: run { nav.close(); return }
    val season = Museum.season()
    val today = Museum.today()
    val items = h.items.filter { it.hall == hallId && it.owned }.sortedBy { it.inv }
    val seal = h.seals.firstOrNull { it.hall == hallId && it.season == season }
    val found = remember { mutableStateListOf<Long>().apply { addAll(items.filter { seal != null && it.checked == seal.day }.map { it.id }) } }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.inv_title), hall.name, nav::close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (seal != null) Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = C.redSoft) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaxSeal(Modifier.size(56.dp))
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(stringResource(R.string.inv_sealed), style = T.Strong)
                        Text(stringResource(R.string.inv_sealed_b, Fmt.date(seal.day, ru), seal.found, seal.missing), style = T.Small)
                    }
                }
            } else Text(stringResource(R.string.inv_hint), style = T.Small, modifier = Modifier.padding(bottom = 12.dp))
            for (i in items) {
                val on = i.id in found
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (on) found.remove(i.id) else found.add(i.id) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Face(i, Modifier.size(48.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(i.title.ifBlank { h.inv(i) }, style = T.Strong, maxLines = 1)
                        Text(h.inv(i) + (if (i.storage.isNotBlank()) " · " + i.storage else ""), style = T.Small, maxLines = 1)
                    }
                    Box(Modifier.size(28.dp).clip(CircleShape).background(if (on) C.green else C.surface2), contentAlignment = Alignment.Center) {
                        if (on) Icon(Icons.Rounded.Check, null, tint = C.surface, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().padding(16.dp)) {
            Btn(stringResource(R.string.inv_seal, found.size, items.size), {
                c.write {
                    dao.save(items.filter { it.id in found }.map { it.copy(checked = today) })
                    dao.add(Seal(hallId, season, today, found.size, items.size - found.size))
                }
                nav.close()
            }, Modifier.fillMaxWidth(), Kind.RED, enabled = items.isNotEmpty())
        }
    }
}

/** A red wax seal with the museum's mark pressed in. */
@Composable
fun WaxSeal(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val r = size.minDimension / 2
        val center = this.center
        val blob = androidx.compose.ui.graphics.Path().apply {
            for (k in 0..24) {
                val a = k / 24.0 * Math.PI * 2
                val rr = r * (0.92f + if (k % 2 == 0) 0.08f else 0f)
                val x = center.x + (Math.cos(a) * rr).toFloat(); val y = center.y + (Math.sin(a) * rr).toFloat()
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(blob, androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFC94A3E), androidx.compose.ui.graphics.Color(0xFF8E2A22)), center, r))
        drawCircle(androidx.compose.ui.graphics.Color(0xFF7A231C), r * 0.62f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.07f))
        // A tiny cabinet pressed into the wax.
        val w = r * 0.5f; val hh = r * 0.62f
        drawRect(androidx.compose.ui.graphics.Color(0xFF7A231C), androidx.compose.ui.geometry.Offset(center.x - w / 2, center.y - hh / 2), androidx.compose.ui.geometry.Size(w, hh),
            style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.08f))
        drawLine(androidx.compose.ui.graphics.Color(0xFF7A231C), androidx.compose.ui.geometry.Offset(center.x - w / 2, center.y), androidx.compose.ui.geometry.Offset(center.x + w / 2, center.y), r * 0.07f)
    }
}
