package app.cabinet.ui.item

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Item
import app.cabinet.data.Status
import app.cabinet.domain.Museum
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.R12
import app.cabinet.ui.components.R16
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.lift
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

/** The card of a thing being filled in. Leaving the page keeps what was typed. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(id: Long, fresh: Boolean, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val item = h.item(id) ?: run { nav.close(); return }
    var title by remember { mutableStateOf(item.title) }
    var hall by remember { mutableStateOf(item.hall) }
    var cabinet by remember { mutableStateOf(item.cabinet) }
    var status by remember { mutableStateOf(item.status) }
    var acquired by remember { mutableStateOf(item.acquired) }
    var level by remember { mutableStateOf(item.acquiredLevel) }
    var place by remember { mutableStateOf(item.place) }
    var price by remember { mutableStateOf(item.price) }
    var condition by remember { mutableStateOf(item.condition) }
    var storage by remember { mutableStateOf(item.storage) }
    var story by remember { mutableStateOf(item.story) }
    var tags by remember { mutableStateOf(item.tags) }
    var series by remember { mutableStateOf(item.series) }
    var copies by remember { mutableStateOf(item.copies) }
    val fields = remember { mutableStateMapOf<String, String>().apply { item.fields.lines().filter { '=' in it }.forEach { put(it.substringBefore('='), it.substringAfter('=')) } } }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var shot by remember { mutableStateOf<Uri?>(null) }

    fun result(base: Item): Item {
        var out = base.copy(
            title = title.trim(), hall = hall, cabinet = cabinet, status = status, acquired = acquired, acquiredLevel = if (acquired == null) 0 else level, place = place.trim(), price = price.trim(),
            condition = condition, storage = storage.trim(), story = story.trim(), tags = tags.trim(), series = series, copies = copies,
            updated = System.currentTimeMillis(),
        )
        out = out.copy(fields = "")
        for ((k, v) in fields) out = out.withField(k, v)
        return out
    }
    fun save() {
        val now = h.item(id) ?: return
        val next = result(now)
        if (next.copy(updated = 0, look = null) != now.copy(updated = 0, look = null) || next.fields != now.fields) c.write {
            if (now.cabinet != next.cabinet) dao.save(next.copy(sort = dao.lastSort(next.cabinet) + 1)) else dao.save(next)
            if (now.status != next.status) celebrate()
        }
        nav.close()
    }
    BackHandler { save() }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> val u = shot; if (ok && u != null) scope.launch { h.item(id)?.let { c.addPhoto(it, u) } } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) scope.launch { h.item(id)?.let { c.addPhoto(it, u) } } }

    val hallObj = h.hallOf[hall]
    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // The photo on its hall's colour, with what can be done to it.
            Box(Modifier.fillMaxWidth().height(300.dp).background(h.tint(hall))) {
                Face(item, Modifier.fillMaxSize().statusBarsPadding().padding(start = 40.dp, end = 40.dp, top = 60.dp, bottom = 70.dp), big = true, dim = false)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                    IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, ::save)
                    Box(Modifier.weight(1f))
                    Text(h.inv(item), style = T.Label.copy(color = C.ink2), modifier = Modifier.clip(R12).background(C.surface).padding(horizontal = 10.dp, vertical = 8.dp))
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    for (n in 1 until item.photos) Box(Modifier.size(46.dp)) {
                        Original(item, n, Modifier.fillMaxSize().clip(R12).clickable { scope.launch { c.photos.toFront(item.id, n); c.reface(item.id, h.s.cut) } })
                        Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).size(16.dp).background(Color(0x99000000), CircleShape).padding(2.dp)
                            .clickable { scope.launch { c.photos.remove(item.id, n, item.photos); c.dao.save(item.copy(photos = item.photos - 1, updated = System.currentTimeMillis())) } })
                    }
                    IconBtn(Icons.Outlined.PhotoCamera, {
                        val f = File(c.photos.inbox, "shot_" + System.currentTimeMillis() + ".jpg")
                        val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f); shot = u
                        runCatching { camera.launch(u) }
                    }, desc = stringResource(R.string.add_camera))
                    IconBtn(Icons.Outlined.PhotoLibrary, { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, desc = stringResource(R.string.add_gallery))
                    if (item.photos > 0) IconBtn(Icons.Outlined.ContentCut, { scope.launch { c.reface(item.id, !item.cut) } },
                        bg = if (item.cut) C.ink else C.surface, tint = if (item.cut) C.bg else C.ink, desc = stringResource(R.string.cut))
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                // The name, large.
                Box(Modifier.padding(top = 18.dp, start = 4.dp, end = 4.dp)) {
                    if (title.isEmpty()) Text(stringResource(R.string.f_title_plate), style = T.Title.copy(color = C.muted))
                    BasicTextField(title, { title = it }, textStyle = T.Title, cursorBrush = SolidColor(C.accent), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                }
                if (item.photos > 0) Text(stringResource(if (item.cut) R.string.cut_on else R.string.cut_off), style = T.Small, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

                // In the collection or still wished for.
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(R16).background(C.surface2).padding(4.dp)) {
                    for ((st, label) in listOf(Status.OWNED to R.string.status_owned, Status.WISH to R.string.status_wish)) {
                        val on = status == st
                        Text(stringResource(label), style = T.Button.copy(color = if (on) C.ink else C.muted, fontSize = 14.sp), textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(R12).background(if (on) C.surface else Color.Transparent)
                                .clickable { status = st; if (st == Status.OWNED && acquired == null) acquired = Museum.today() }.padding(vertical = 11.dp))
                    }
                }

                Group(stringResource(R.string.e_where)) {
                    if (h.halls.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (hl in h.halls) Chip(hl.name, hl.id == hall) { hall = hl.id; cabinet = h.cabinets.firstOrNull { it.hall == hl.id }?.id ?: cabinet; series = 0 }
                    }
                    FlowRow(Modifier.padding(top = if (h.halls.size > 1) 10.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (cb in h.cabinets.filter { it.hall == hall }) Chip(cb.name, cb.id == cabinet) { cabinet = cb.id }
                    }
                }

                Group(stringResource(R.string.e_card)) {
                    Field(place, { place = it }, stringResource(R.string.f_place), hint = stringResource(R.string.f_place_hint))
                    Field(story, { story = it }, stringResource(R.string.f_story), Modifier.padding(top = 12.dp), lines = 4, hint = stringResource(R.string.f_story_hint))
                    if (status == Status.OWNED) {
                        Text(stringResource(R.string.f_acquired), style = T.Label, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
                        // The day, or only the month, or only the year: many remember no more than that (#2).
                        FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((k, label) in listOf(0 to R.string.date_day, 1 to R.string.date_month, 2 to R.string.date_year)) Chip(stringResource(label), level == k) {
                                level = k
                                acquired = acquired?.let { Fmt.trim(it, k) }
                            }
                        }
                        if (level == 0) Row(Modifier.fillMaxWidth().clip(R16).background(C.surface2).clickable { picking = true }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = C.muted, modifier = Modifier.size(18.dp))
                            Text(acquired?.let { Fmt.date(it, ru) } ?: stringResource(R.string.unknown), style = T.Body.copy(color = C.ink), modifier = Modifier.padding(start = 10.dp))
                        } else {
                            val top = Fmt.trim(Museum.today(), level)
                            val d = java.time.LocalDate.ofEpochDay(acquired ?: top)
                            Row(Modifier.fillMaxWidth().clip(R16).background(C.surface2).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconBtn(Icons.Rounded.Remove, { acquired = d.minusYears(1).toEpochDay() }, bg = C.surface2)
                                Text(acquired?.let { Fmt.date(it, level, ru) } ?: stringResource(R.string.unknown), style = T.Body.copy(color = C.ink), textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f).clickable { if (acquired == null) acquired = d.toEpochDay() })
                                IconBtn(Icons.Rounded.Add, { acquired = d.plusYears(1).toEpochDay().coerceAtMost(top) }, bg = C.surface2)
                            }
                            if (level == 1) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val loc = if (ru) java.util.Locale.forLanguageTag("ru") else java.util.Locale.ENGLISH
                                for (m in 1..12) Chip(java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, loc), acquired != null && d.monthValue == m) {
                                    acquired = d.withMonth(m).toEpochDay()
                                }
                            }
                        }
                    }
                    for (f in hallObj?.fieldList ?: emptyList()) Field(fields[f] ?: "", { fields[f] = it }, f, Modifier.padding(top = 12.dp))
                    Field(storage, { storage = it }, stringResource(R.string.f_storage), Modifier.padding(top = 12.dp), hint = stringResource(R.string.f_storage_hint))
                    Field(price, { price = it }, stringResource(R.string.f_price), Modifier.padding(top = 12.dp))
                    Field(tags, { tags = it }, stringResource(R.string.f_tags), Modifier.padding(top = 12.dp), hint = stringResource(R.string.f_tags_hint))
                }

                Group(stringResource(R.string.f_condition)) {
                    val conds = stringArrayResource(R.array.conditions)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (k in 1..4) Text(conds[k], style = T.Label.copy(color = if (k == condition) C.bg else C.ink, fontSize = 12.5.sp), textAlign = TextAlign.Center, maxLines = 1,
                            modifier = Modifier.weight(1f).clip(R12).background(if (k == condition) C.ink else C.surface2).clickable { condition = if (condition == k) 0 else k }.padding(vertical = 10.dp))
                    }
                    if (status == Status.OWNED) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.f_copies), style = T.Strong)
                            Text(stringResource(R.string.copies_hint), style = T.Small)
                        }
                        IconBtn(Icons.Rounded.Remove, { copies = (copies - 1).coerceAtLeast(1) }, bg = C.surface2)
                        Text(copies.toString(), style = T.Heading, modifier = Modifier.padding(horizontal = 14.dp))
                        IconBtn(Icons.Rounded.Add, { copies = (copies + 1).coerceAtMost(99) }, bg = C.surface2)
                    }
                    val ss = h.series.filter { it.hall == hall }
                    if (ss.isNotEmpty()) {
                        Text(stringResource(R.string.f_series), style = T.Label, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(stringResource(R.string.series_none), series == 0L) { series = 0 }
                            for (s in ss) Chip(s.name, series == s.id) { series = s.id }
                        }
                    }
                }
                Btn(stringResource(R.string.delete_thing), { confirmDelete = true }, Modifier.padding(top = 20.dp).fillMaxWidth(), Kind.SOFT, icon = Icons.Outlined.Delete)
                Gap(110.dp)
            }
        }
        Btn(stringResource(R.string.done), ::save, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp).fillMaxWidth().lift(R16), Kind.PRIMARY)
    }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (acquired ?: Museum.today()) * 86_400_000L)
        DatePickerDialog(onDismissRequest = { picking = false },
            confirmButton = { TextButton({ acquired = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }; picking = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton({ acquired = null; picking = false }) { Text(stringResource(R.string.unknown)) } }) {
            DatePicker(state)
        }
    }
    if (confirmDelete) Scrim({ confirmDelete = false }) {
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_thing_t), style = T.Heading)
            Text(stringResource(R.string.delete_thing_b), style = T.Body, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(stringResource(R.string.cancel), { confirmDelete = false }, Modifier.weight(1f), Kind.SOFT)
                Btn(stringResource(R.string.delete), { c.write { delete(item) }; nav.stack.removeAll { it is app.cabinet.ui.Overlay.Item && it.id == id || it is app.cabinet.ui.Overlay.Edit && it.id == id } }, Modifier.weight(1f), Kind.RED)
            }
        }
    }
}

/** A titled white card of fields. */
@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = T.Heading, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
    Card(Modifier.fillMaxWidth(), content = content)
}
