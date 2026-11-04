package app.cabinet.ui.check

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Item
import app.cabinet.data.Match
import app.cabinet.data.Photos
import app.cabinet.data.Status
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.catalog.matches
import app.cabinet.ui.components.Bar
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * «Do I have this?» — at a flea market, in a shop, at a swap. Shoot the thing with the phone's camera;
 * the lens compares it with every shelf and says whether it is already at home, and where it lives.
 */
@Composable
fun CheckScreen(autostart: Boolean, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var face by remember { mutableStateOf<Bitmap?>(null) }
    var results by remember { mutableStateOf<List<Match>?>(null) }
    var looking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var shot by remember { mutableStateOf<Uri?>(null) }
    var started by rememberSaveable { mutableStateOf(false) }

    fun look(uri: Uri) {
        looking = true; results = null
        scope.launch {
            val bmp = c.photos.read(uri, 1600)
            photo = bmp
            if (bmp != null) {
                val cut = withContext(Dispatchers.Default) { if (h.s.cut) c.photos.cutOut(Photos.run { bmp.fit(1024) }) else null }
                face = cut ?: bmp
                val look = c.lens.look(face!!)
                results = Match.find(look, h.items)
            }
            looking = false
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> val u = shot; if (ok && u != null) look(u) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) look(u) }
    fun shoot() {
        val f = File(c.photos.inbox, "check.jpg")
        val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f); shot = u
        runCatching { camera.launch(u) }
    }
    LaunchedEffect(Unit) { if (autostart && !started) { started = true; shoot() } }

    fun add(status: Status) {
        val bmp = photo ?: return
        val hall = results?.firstOrNull()?.item?.hall ?: h.halls.firstOrNull()?.id ?: return
        val cab = results?.firstOrNull()?.item?.cabinet ?: h.cabinets.firstOrNull { it.hall == hall }?.id ?: return
        scope.launch { val id = c.addThing(hall, cab, bmp, status); nav.replace(Overlay.Edit(id, fresh = true)) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(stringResource(R.string.check_title), stringResource(R.string.check_sub), nav::close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(stringResource(R.string.check_shoot), ::shoot, Modifier.weight(1f), Kind.ACCENT, icon = Icons.Outlined.PhotoCamera)
                Btn(stringResource(R.string.check_pick), { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.weight(1f), Kind.GHOST, icon = Icons.Outlined.PhotoLibrary)
            }
            face?.let { f ->
                Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(C.back), contentAlignment = Alignment.Center) {
                    Image(f.asImageBitmap(), null, Modifier.fillMaxSize().padding(14.dp), contentScale = ContentScale.Fit)
                }
            }
            if (face == null && !looking) Card(Modifier.padding(top = 14.dp).fillMaxWidth(), color = C.surface2) {
                Text(stringResource(R.string.check_how_t), style = T.Strong)
                Text(stringResource(R.string.check_how_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            }
            if (looking) Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), color = C.accent, strokeWidth = 2.dp)
                Text(stringResource(R.string.check_looking), style = T.Body, modifier = Modifier.padding(start = 12.dp))
            }
            results?.let { rs ->
                val best = rs.firstOrNull()
                val level = best?.level ?: -1
                Card(Modifier.padding(top = 14.dp).fillMaxWidth(), color = when (level) { 2 -> C.redSoft; 1 -> C.accentSoft; else -> C.greenSoft }) {
                    Text(stringResource(when {
                        level == 2 && best?.item?.owned == true -> R.string.check_have
                        level == 2 -> R.string.check_wished
                        level == 1 -> R.string.check_alike
                        else -> R.string.check_new
                    }), style = T.Heading)
                    Text(stringResource(when (level) { 2 -> R.string.check_have_b; 1 -> R.string.check_alike_b; else -> R.string.check_new_b }), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                    if (!c.lens.smart) Text(stringResource(R.string.check_plain), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                }
                for (m in rs.filter { it.level >= 1 || it == rs.first() }.take(5)) MatchRow(m.item, h, nav, m.score)
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(stringResource(R.string.check_add), { add(Status.OWNED) }, Modifier.weight(1f), if (level < 2) Kind.ACCENT else Kind.GHOST)
                    Btn(stringResource(R.string.check_wish), { add(Status.WISH) }, Modifier.weight(1f), Kind.GHOST)
                }
            }

            Text(stringResource(R.string.check_or_name).uppercase(), style = T.Caps, modifier = Modifier.padding(top = 26.dp, bottom = 6.dp))
            Field(query, { query = it }, stringResource(R.string.search_hint))
            if (query.isNotBlank()) {
                val found = h.items.filter { matches(it, query, h) }
                if (found.isEmpty()) Text(stringResource(R.string.check_text_none), style = T.Body.copy(color = C.green), modifier = Modifier.padding(top = 10.dp))
                for (i in found.take(20)) MatchRow(i, h, nav, null)
            }
            Gap(24.dp)
        }
    }
}

@Composable
private fun MatchRow(i: Item, h: Home, nav: Nav, score: Float?) {
    Card(Modifier.padding(top = 8.dp).fillMaxWidth(), pad = 12.dp, onClick = { nav.open(Overlay.Item(i.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Face(i, Modifier.size(64.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(i.title.ifBlank { h.inv(i) }, style = T.Strong, maxLines = 1)
                Text(listOfNotNull(h.inv(i), h.hallOf[i.hall]?.name, h.cabinetOf[i.cabinet]?.name).joinToString(" · "), style = T.Small, maxLines = 1)
                if (!i.owned) Text(stringResource(R.string.status_wish), style = T.Label.copy(color = C.accent))
                else if (i.storage.isNotBlank()) Text(stringResource(R.string.check_lives, i.storage), style = T.Label.copy(color = C.ink2), maxLines = 1)
                if (score != null) Bar(((score - 0.4f) / 0.55f).coerceIn(0.05f, 1f), Modifier.padding(top = 6.dp), color = if (score >= Match.SURE) C.red else C.accent)
            }
        }
    }
}
