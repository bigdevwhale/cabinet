package app.cabinet.ui.item

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

/** The inventory card being filled in. Leaving the page keeps what was typed. */
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
            title = title.trim(), hall = hall, cabinet = cabinet, status = status, acquired = acquired, place = place.trim(), price = price.trim(),
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
    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(stringResource(if (fresh) R.string.edit_new else R.string.edit_title), h.inv(item), ::save)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Photos.
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Face(item, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(C.back), big = true)
                for (n in 1 until item.photos) Box(Modifier.size(72.dp)) {
                    Original(item, n, Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                    Text(stringResource(R.string.photo_first), style = T.Caps.copy(color = androidx.compose.ui.graphics.Color.White),
                        modifier = Modifier.align(Alignment.BottomCenter).background(androidx.compose.ui.graphics.Color(0x88000000), RoundedCornerShape(6.dp))
                            .clickable { scope.launch { c.photos.toFront(item.id, n); c.reface(item.id, h.s.cut) } }.padding(horizontal = 4.dp, vertical = 2.dp))
                    Icon(Icons.Outlined.Delete, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp)
                        .background(androidx.compose.ui.graphics.Color(0x88000000), CircleShape).padding(2.dp)
                        .clickable { scope.launch { c.photos.remove(item.id, n, item.photos); c.dao.save(item.copy(photos = item.photos - 1, updated = System.currentTimeMillis())) } })
                }
                IconBtn(Icons.Outlined.AddAPhoto, {
                    val f = File(c.photos.inbox, "shot_${System.currentTimeMillis()}.jpg")
                    val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f); shot = u
                    runCatching { camera.launch(u) }
                }, bg = C.surface2, desc = stringResource(R.string.add_camera))
                IconBtn(Icons.Outlined.PhotoLibrary, { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, bg = C.surface2, desc = stringResource(R.string.add_gallery))
                if (item.photos > 0) IconBtn(Icons.Outlined.ContentCut, { scope.launch { c.reface(item.id, !item.cut) } }, bg = if (item.cut) C.accentSoft else C.surface2,
                    tint = if (item.cut) C.accent else C.ink, desc = stringResource(R.string.cut))
            }
            if (item.photos > 0) Text(stringResource(if (item.cut) R.string.cut_on else R.string.cut_off), style = T.Small)

            Field(title, { title = it }, stringResource(R.string.f_title), Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(stringResource(R.string.status_owned), status == Status.OWNED) { status = Status.OWNED; if (acquired == null) acquired = Museum.today() }
                Chip(stringResource(R.string.status_wish), status == Status.WISH) { status = Status.WISH }
            }

            Section(stringResource(R.string.e_where))
            if (h.halls.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (hl in h.halls) Chip(hl.name, hl.id == hall) { hall = hl.id; cabinet = h.cabinets.firstOrNull { it.hall == hl.id }?.id ?: cabinet; series = 0 }
            }
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (cb in h.cabinets.filter { it.hall == hall }) Chip(cb.name, cb.id == cabinet) { cabinet = cb.id }
            }

            Section(stringResource(R.string.e_card))
            if (status == Status.OWNED) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, C.line, RoundedCornerShape(12.dp)).clickable { picking = true }.padding(16.dp)) {
                    Text(stringResource(R.string.f_acquired), style = T.Label, modifier = Modifier.weight(1f))
                    Text(acquired?.let { Fmt.date(it, ru) } ?: stringResource(R.string.unknown), style = T.Body)
                }
            }
            Field(place, { place = it }, stringResource(R.string.f_place), Modifier.padding(top = 8.dp), hint = stringResource(R.string.f_place_hint))
            for (f in hallObj?.fieldList ?: emptyList()) Field(fields[f] ?: "", { fields[f] = it }, f, Modifier.padding(top = 8.dp))
            Field(price, { price = it }, stringResource(R.string.f_price), Modifier.padding(top = 8.dp))
            Field(storage, { storage = it }, stringResource(R.string.f_storage), Modifier.padding(top = 8.dp), hint = stringResource(R.string.f_storage_hint))
            Field(story, { story = it }, stringResource(R.string.f_story), Modifier.padding(top = 8.dp), lines = 4, hint = stringResource(R.string.f_story_hint))
            Field(tags, { tags = it }, stringResource(R.string.f_tags), Modifier.padding(top = 8.dp), hint = stringResource(R.string.f_tags_hint))

            Section(stringResource(R.string.f_condition))
            val conds = stringArrayResource(R.array.conditions)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (k in 0..4) Chip(conds[k], condition == k) { condition = k }
            }

            if (status == Status.OWNED) {
                Section(stringResource(R.string.f_copies))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBtn(Icons.Rounded.Remove, { copies = (copies - 1).coerceAtLeast(1) }, bg = C.surface2)
                    Text(copies.toString(), style = T.Heading, modifier = Modifier.padding(horizontal = 18.dp))
                    IconBtn(Icons.Rounded.Add, { copies = (copies + 1).coerceAtMost(99) }, bg = C.surface2)
                    Text(stringResource(R.string.copies_hint), style = T.Small, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
            }

            val ss = h.series.filter { it.hall == hall }
            if (ss.isNotEmpty()) {
                Section(stringResource(R.string.f_series))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(stringResource(R.string.series_none), series == 0L) { series = 0 }
                    for (s in ss) Chip(s.name, series == s.id) { series = s.id }
                }
            }

            Btn(stringResource(R.string.delete_thing), { confirmDelete = true }, Modifier.padding(top = 28.dp).fillMaxWidth(), Kind.GHOST, icon = Icons.Outlined.Delete)
            Gap(24.dp)
        }
        Box(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().padding(16.dp)) {
            Btn(stringResource(R.string.done), ::save, Modifier.fillMaxWidth(), Kind.ACCENT)
        }
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
                Btn(stringResource(R.string.cancel), { confirmDelete = false }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.delete), { c.write { delete(item) }; nav.stack.removeAll { it is app.cabinet.ui.Overlay.Item && it.id == id || it is app.cabinet.ui.Overlay.Edit && it.id == id } }, Modifier.weight(1f), Kind.RED)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), style = T.Caps, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}
