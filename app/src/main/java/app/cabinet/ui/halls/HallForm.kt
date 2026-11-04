package app.cabinet.ui.halls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Cabinet
import app.cabinet.domain.Museum
import app.cabinet.domain.Preset
import app.cabinet.domain.Presets
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.PaperCard
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.Serif
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch

/** The painted showcase of each kind of hall (tools/art/kinds.py). */
fun kindArt(key: String): Int = when (key) {
    "cans" -> R.drawable.kind_cans
    "figures" -> R.drawable.kind_figures
    "magnets" -> R.drawable.kind_magnets
    "coins" -> R.drawable.kind_coins
    "paper" -> R.drawable.kind_paper
    "records" -> R.drawable.kind_records
    "cars" -> R.drawable.kind_cars
    else -> R.drawable.kind_own
}

/** «What do you collect?» as a wall of small lit showcases, two to a row. */
@Composable
fun KindGrid(selected: Preset?, ru: Boolean, onPick: (Preset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in Presets.ALL.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (p in row) {
                val on = p == selected
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) C.accentSoft else C.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) C.accent else C.line, RoundedCornerShape(16.dp)).clickable { onPick(p) }) {
                    Box {
                        Image(painterResource(kindArt(p.key)), null, Modifier.fillMaxWidth().aspectRatio(1.6f), contentScale = ContentScale.Crop)
                        if (on) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape).background(C.accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = C.onAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(p.names(ru).hall, style = T.Strong.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/**
 * The hall's sign as it will hang over the door: a walnut board with an engraved brass plate,
 * the first inventory number, and the cabinets lined up underneath. Follows the form as you type.
 */
@Composable
fun HallSign(name: String, code: String, cabinets: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(C.backLight, C.back))).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(0xFF946243), Color(0xFF6E4529), Color(0xFF4A2D1A))))
            .padding(10.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF1DB9C), Color(0xFFCDA653), Color(0xFFA9853A))))
                .padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name.ifBlank { "…" }, style = T.Heading.copy(color = Color(0xFF3A2A10), fontFamily = Serif), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(Museum.inv(code, 1) + " · " + Museum.inv(code, 2) + " · …", style = T.Brass.copy(fontSize = 11.sp))
            }
        }
        // The cabinets of the hall, small arched silhouettes with their names.
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            for (c in cabinets.take(5)) Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(44.dp, 58.dp)) {
                    val w = size.width; val h = size.height
                    drawArc(Color(0xFF8A5A36), 180f, 180f, true, Offset(0f, 0f), Size(w, w))
                    drawRect(Color(0xFF8A5A36), Offset(0f, w / 2), Size(w, h - w / 2))
                    val m = 4.dp.toPx()
                    drawArc(Color(0xFF1C2A22), 180f, 180f, true, Offset(m, m), Size(w - 2 * m, w - 2 * m))
                    drawRect(Color(0xFF1C2A22), Offset(m, w / 2), Size(w - 2 * m, h - w / 2 - m))
                    for (k in 1..2) drawRect(Color(0xFFC48A50), Offset(m, w / 2 + (h - w / 2 - m) * k / 3f), Size(w - 2 * m, 2.dp.toPx()))
                }
                Text(c, style = T.Label.copy(color = Color(0xFFE9DCC0), fontSize = 10.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            if (cabinets.size > 5) Text("+${cabinets.size - 5}", style = T.Label.copy(color = Color(0xFFE9DCC0)), modifier = Modifier.padding(bottom = 22.dp))
        }
    }
}

/** A new hall from a kind of collection, or an existing hall's name, number code and card fields. */
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
    var codeTouched by remember { mutableStateOf(hall != null) }
    val fields = remember { mutableStateListOf<String>().apply { addAll(hall?.fieldList ?: preset.names(ru).fields) } }
    val cabinets = remember { mutableStateListOf<String>().apply { addAll(if (hall != null) h.cabinets.filter { it.hall == id }.map { it.name } else preset.names(ru).cabinets) } }
    var newField by remember { mutableStateOf("") }
    var newCabinet by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<Long?>(null) }
    val count = h.items.count { it.hall == id }
    val suggestions = stringArrayResource(R.array.field_ideas).filter { it !in fields }

    fun pick(p: Preset) {
        if (name == preset.names(ru).hall || name.isBlank()) { name = p.names(ru).hall; if (!codeTouched) code = Museum.code(name) }
        preset = p
        fields.clear(); fields.addAll(p.names(ru).fields)
        cabinets.clear(); cabinets.addAll(p.names(ru).cabinets)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(stringResource(if (hall == null) R.string.hall_new else R.string.hall_edit), if (hall == null) stringResource(R.string.hall_new_sub) else null, nav::close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            HallSign(name, code, cabinets, Modifier.fillMaxWidth().padding(top = 4.dp))

            if (hall == null) {
                Step(1, stringResource(R.string.hall_kind), stringResource(R.string.hall_kind_sub))
                KindGrid(preset, ru, ::pick)
            }

            Step(if (hall == null) 2 else 1, stringResource(R.string.hall_sign), stringResource(R.string.hall_sign_sub))
            Card(Modifier.fillMaxWidth(), pad = 16.dp) {
                Field(name, { name = it; if (!codeTouched) code = Museum.code(it) }, stringResource(R.string.hall_name))
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(code, { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(4); codeTouched = true }, stringResource(R.string.hall_code), Modifier.width(130.dp))
                    Text(stringResource(R.string.hall_code_hint, Museum.inv(code, 147)), style = T.Small, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
            }

            Step(if (hall == null) 3 else 2, stringResource(R.string.hall_cabinets), stringResource(if (hall == null) R.string.hall_cabinets_sub else R.string.hall_cabinets_sub_edit))
            Card(Modifier.fillMaxWidth(), pad = 14.dp) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (cb in cabinets.toList()) Row(Modifier.clip(RoundedCornerShape(10.dp)).background(C.surface2).padding(start = 12.dp, end = if (hall == null) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(cb, style = T.Label.copy(color = C.ink))
                        if (hall == null && cabinets.size > 1) Icon(Icons.Rounded.Close, null, tint = C.muted, modifier = Modifier.padding(start = 4.dp).size(18.dp).clickable { cabinets.remove(cb) })
                    }
                }
                if (hall == null) AddRow(newCabinet, { newCabinet = it }, stringResource(R.string.hall_cabinet_new)) {
                    val v = newCabinet.trim(); if (v.isNotEmpty() && v !in cabinets) cabinets.add(v); newCabinet = ""
                }
            }

            Step(if (hall == null) 4 else 3, stringResource(R.string.hall_fields), stringResource(R.string.hall_fields_hint))
            PaperCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.card_head, Museum.inv(code, 1)), style = T.TypeSmall)
                for (f in listOf(R.string.f_title, R.string.f_place, R.string.f_acquired, R.string.f_story)) TypedLine(stringResource(f), fixed = true)
                for (f in fields.toList()) TypedLine(f, onRemove = { fields.remove(f) })
            }
            if (suggestions.isNotEmpty()) FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in suggestions.take(10)) Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, C.line, RoundedCornerShape(50)).clickable { fields.add(s) }
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = C.accent, modifier = Modifier.size(16.dp))
                    Text(s, style = T.Label.copy(color = C.ink), modifier = Modifier.padding(start = 2.dp))
                }
            }
            AddRow(newField, { newField = it }, stringResource(R.string.hall_field_new)) {
                val f = newField.trim().replace("=", ""); if (f.isNotEmpty() && f !in fields) fields.add(f); newField = ""
            }

            if (hall != null) Btn(stringResource(R.string.hall_delete), { confirm = true }, Modifier.padding(top = 28.dp).fillMaxWidth(), Kind.GHOST)
            Box(Modifier.height(24.dp))
        }
        Box(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().padding(16.dp)) {
            Btn(stringResource(if (hall == null) R.string.hall_open else R.string.save), {
                val n = name.trim(); val cd = code.trim().ifBlank { Museum.code(n) }
                scope.launch {
                    if (hall == null) {
                        val newId = c.dao.add(app.cabinet.data.Hall(name = n, code = cd, fields = fields.joinToString("\n"), sort = h.halls.size))
                        cabinets.forEachIndexed { i, cb -> c.dao.add(Cabinet(hall = newId, name = cb, sort = i)) }
                        opened = newId
                    } else {
                        c.dao.save(hall.copy(name = n, code = cd, fields = fields.joinToString("\n")))
                        nav.close()
                    }
                }
            }, Modifier.fillMaxWidth(), Kind.ACCENT, enabled = name.isNotBlank() && cabinets.isNotEmpty())
        }
    }

    opened?.let { newId -> Opening(name, preset) { nav.replace(Overlay.Hall(newId)) } }

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

/** A numbered step of the form. */
@Composable
private fun Step(n: Int, title: String, sub: String) {
    Row(Modifier.padding(top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(C.accentSoft), contentAlignment = Alignment.Center) {
            Text(n.toString(), style = T.Strong.copy(color = C.accent, fontFamily = Serif, fontSize = 16.sp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = T.Heading.copy(fontSize = 20.sp))
            Text(sub, style = T.Small)
        }
    }
}

/** One typed line of the specimen card: «Brand: ______». */
@Composable
private fun TypedLine(label: String, fixed: Boolean = false, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", style = T.Type.copy(color = if (fixed) C.paperInk.copy(alpha = .45f) else C.paperInk), maxLines = 1)
        Text(" ____________", style = T.Type.copy(color = C.paperInk.copy(alpha = .25f)), maxLines = 1, modifier = Modifier.weight(1f))
        if (onRemove != null) Icon(Icons.Rounded.Close, null, tint = C.paperInk.copy(alpha = .5f), modifier = Modifier.size(20.dp).clickable(onClick = onRemove))
    }
}

/** A quiet text line with a round add button. */
@Composable
private fun AddRow(value: String, onChange: (String) -> Unit, hint: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.line, RoundedCornerShape(12.dp)).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (value.isEmpty()) Text(hint, style = T.Body.copy(color = C.muted))
            BasicTextField(value, onChange, textStyle = T.Body, singleLine = true, cursorBrush = SolidColor(C.accent), modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (value.isBlank()) C.surface2 else C.accent).clickable(enabled = value.isNotBlank(), onClick = onAdd),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, null, tint = if (value.isBlank()) C.muted else C.onAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/** The hall is open: a red ribbon is cut in front of its showcase. */
@Composable
private fun Opening(name: String, preset: Preset, onEnter: () -> Unit) {
    val cut = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(350); cut.animateTo(1f, tween(900)) }
    Scrim(null) {
        Card(Modifier.fillMaxWidth(), pad = 0.dp) {
            Box {
                Image(painterResource(kindArt(preset.key)), null, Modifier.fillMaxWidth().aspectRatio(1.6f), contentScale = ContentScale.Crop)
                // The ribbon: two halves falling apart.
                for (side in listOf(-1f, 1f)) Box(Modifier.align(Alignment.Center).fillMaxWidth(.5f).height(14.dp)
                    .graphicsLayer {
                        translationX = side * (size.width / 2) + side * cut.value * 60f
                        translationY = cut.value * 80f
                        rotationZ = side * cut.value * 25f
                        alpha = 1f - cut.value * .8f
                    }.background(Color(0xFFB5473A)))
            }
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.hall_opened_caps).uppercase(), style = T.Caps.copy(color = C.accent))
                Text(stringResource(R.string.hall_opened, name), style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.hall_opened_b), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Btn(stringResource(R.string.hall_enter), onEnter, Modifier.padding(top = 18.dp).fillMaxWidth(), Kind.ACCENT)
            }
        }
    }
}

