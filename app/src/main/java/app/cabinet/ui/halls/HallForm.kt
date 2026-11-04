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
import app.cabinet.ui.components.R22
import app.cabinet.ui.components.R12
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Album
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch

/** The icon of each kind of hall. */
fun kindIcon(key: String): ImageVector = when (key) {
    "records" -> Icons.Outlined.Album
    "books" -> Icons.AutoMirrored.Outlined.MenuBook
    "coins" -> Icons.Outlined.Toll
    "cans" -> Icons.Outlined.LocalDrink
    "figures" -> Icons.Outlined.SmartToy
    "magnets" -> Icons.Outlined.PushPin
    "paper" -> Icons.Outlined.Mail
    "cars" -> Icons.Outlined.DirectionsCar
    else -> Icons.Outlined.Category
}

/** «What do you collect?» as soft tiles, two to a row. */
@Composable
fun KindGrid(selected: Preset?, ru: Boolean, onPick: (Preset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((r, row) in Presets.ALL.chunked(2).withIndex()) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((k, p) in row.withIndex()) {
                val on = p == selected
                Row(Modifier.weight(1f).clip(R22).background(if (on) C.ink else C.hall(r * 2 + k)).clickable { onPick(p) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(R12).background(if (on) C.lime else C.surface), contentAlignment = Alignment.Center) {
                        Icon(kindIcon(p.key), null, tint = if (on) C.onLime else C.ink, modifier = Modifier.size(20.dp))
                    }
                    Text(p.names(ru).hall, style = T.Strong.copy(fontSize = 13.5.sp, lineHeight = 17.sp, color = if (on) C.bg else C.ink), maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/** How the hall will look: its name, the first numbers, its cabinets. Follows the form as you type. */
@Composable
fun HallSign(name: String, code: String, cabinets: List<String>, modifier: Modifier = Modifier, tint: Int = 0) {
    Column(modifier.clip(RoundedCornerShape(26.dp)).background(C.hall(tint)).padding(18.dp)) {
        Text(name.ifBlank { "…" }, style = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(Museum.inv(code, 1) + " · " + Museum.inv(code, 2) + " · …", style = T.Small.copy(color = C.ink2.copy(alpha = .75f)), modifier = Modifier.padding(top = 2.dp))
        FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in cabinets.take(8)) Text(c, style = T.Label.copy(color = C.ink, fontSize = 12.sp), modifier = Modifier.clip(R12).background(C.surface).padding(horizontal = 10.dp, vertical = 6.dp))
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
            Card(Modifier.fillMaxWidth()) {
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
        Box(Modifier.size(28.dp).clip(CircleShape).background(C.ink), contentAlignment = Alignment.Center) {
            Text(n.toString(), style = T.Strong.copy(color = C.bg, fontSize = 14.sp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = T.Heading)
            Text(sub, style = T.Small)
        }
    }
}

/** One typed line of the specimen card: «Brand: ______». */
@Composable
private fun TypedLine(label: String, fixed: Boolean = false, onRemove: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.Body.copy(color = if (fixed) C.muted else C.ink), maxLines = 1, modifier = Modifier.weight(1f))
        if (onRemove != null) Icon(Icons.Rounded.Close, null, tint = C.muted, modifier = Modifier.size(20.dp).clickable(onClick = onRemove))
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
            Box(Modifier.fillMaxWidth().height(150.dp).background(C.hall(2)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(C.surface), contentAlignment = Alignment.Center) {
                    Icon(kindIcon(preset.key), null, tint = C.ink, modifier = Modifier.size(34.dp))
                }
                // The ribbon: two halves falling apart.
                for (side in listOf(-1f, 1f)) Box(Modifier.align(Alignment.Center).fillMaxWidth(.5f).height(12.dp)
                    .graphicsLayer {
                        translationX = side * (size.width / 2) + side * cut.value * 60f
                        translationY = cut.value * 80f
                        rotationZ = side * cut.value * 25f
                        alpha = 1f - cut.value
                    }.background(C.red))
            }
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.hall_opened_caps), style = T.Label.copy(color = C.accent))
                Text(stringResource(R.string.hall_opened, name), style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.hall_opened_b), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Btn(stringResource(R.string.hall_enter), onEnter, Modifier.padding(top = 18.dp).fillMaxWidth(), Kind.LIME)
            }
        }
    }
}

