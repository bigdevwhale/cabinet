package app.cabinet.ui.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Status
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.Sheet
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch
import java.io.File

/** Where a new thing goes and how it arrives. One per app, shown by [AdderHost] on top of everything. */
class Adder {
    internal var sheet by mutableStateOf<Status?>(null)
    internal var hall by mutableStateOf<Long?>(null)
    internal var cabinet by mutableStateOf<Long?>(null)

    /** Opens the «new thing» sheet; with [hall]/[cabinet] given they are already chosen. */
    fun start(hall: Long? = null, cabinet: Long? = null, status: Status = Status.OWNED) {
        this.hall = hall; this.cabinet = cabinet; sheet = status
    }
}

val LocalAdder = staticCompositionLocalOf<Adder> { error("No Adder provided") }

/**
 * The «new thing» sheet and its launchers: the phone's own camera for one thing, the gallery for many at once
 * (they arrive under dust covers, to be described later), or a wish with no photo yet.
 * Must be composed after the screens so the sheet lies above them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdderHost(a: Adder, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var shot by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf(Status.OWNED) }

    fun place(): Pair<Long, Long>? {
        val hl = a.hall ?: h.halls.firstOrNull()?.id ?: return null
        val cb = a.cabinet?.takeIf { h.cabinetOf[it]?.hall == hl } ?: h.cabinets.firstOrNull { it.hall == hl }?.id ?: return null
        return hl to cb
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = shot
        val (hl, cb) = place() ?: return@rememberLauncherForActivityResult
        if (ok && uri != null) c.scope.launch {
            val id = c.addThing(hl, cb, uri, status)
            if (id != null) nav.open(Overlay.Edit(id, fresh = true))
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(60)) { uris ->
        val (hl, cb) = place() ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        c.scope.launch {
            val ids = uris.mapNotNull { c.addThing(hl, cb, it, status) }
            if (ids.size == 1) nav.open(Overlay.Edit(ids[0], fresh = true))
            else Toast.makeText(ctx, ctx.resources.getQuantityString(R.plurals.added_many, ids.size, ids.size), Toast.LENGTH_LONG).show()
        }
    }

    a.sheet?.let { st ->
        Scrim({ a.sheet = null }, bottom = true) {
            Sheet {
                Text(stringResource(if (st == Status.WISH) R.string.add_wish_t else R.string.add_t), style = T.Heading)
                if (h.halls.size > 1) {
                    Text(stringResource(R.string.add_hall), style = T.Caps, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (hl in h.halls) Chip(hl.name, place()?.first == hl.id) { a.hall = hl.id; a.cabinet = null }
                    }
                }
                val cabs = h.cabinets.filter { it.hall == place()?.first }
                if (cabs.size > 1) {
                    Text(stringResource(R.string.add_cabinet), style = T.Caps, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (cb in cabs) Chip(cb.name, place()?.second == cb.id) { a.cabinet = cb.id }
                    }
                }
                Column(Modifier.padding(top = 14.dp).navigationBarsPadding()) {
                    Option(Icons.Outlined.PhotoCamera, stringResource(R.string.add_camera), stringResource(R.string.add_camera_sub)) {
                        a.sheet = null; status = st
                        val f = File(c.photos.inbox, "shot_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                        shot = uri
                        runCatching { camera.launch(uri) }.onFailure { Toast.makeText(ctx, R.string.no_camera, Toast.LENGTH_LONG).show() }
                    }
                    Option(Icons.Outlined.PhotoLibrary, stringResource(R.string.add_gallery), stringResource(R.string.add_gallery_sub)) {
                        a.sheet = null; status = st
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (st == Status.WISH) Option(Icons.Outlined.AutoAwesome, stringResource(R.string.add_no_photo), stringResource(R.string.add_no_photo_sub)) {
                        a.sheet = null
                        val (hl, cb) = place() ?: return@Option
                        c.scope.launch { nav.open(Overlay.Edit(c.addThing(hl, cb, null, Status.WISH), fresh = true)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Option(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.accent, modifier = Modifier.size(26.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = T.Strong)
            Text(sub, style = T.Small)
        }
    }
}
