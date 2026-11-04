package app.cabinet.ui.item

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Item
import app.cabinet.data.Status
import app.cabinet.domain.Museum
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.BrassLabel
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.PaperCard
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

/** A thing on its own plinth, with its inventory card typed underneath. */
@Composable
fun ItemScreen(id: Long, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val item = h.item(id) ?: run { nav.close(); return }
    val hall = h.hallOf[item.hall]
    val cab = h.cabinetOf[item.cabinet]
    var viewing by remember { mutableStateOf<Int?>(null) }
    val isPearl = hall?.pearl == item.id

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // The plinth under a lamp.
        Box(Modifier.fillMaxWidth().height(340.dp).background(Brush.verticalGradient(listOf(C.back, C.backLight, C.back)))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x44FFF3C8), Color.Transparent), radius = 700f)))
            Face(item, Modifier.align(Alignment.BottomCenter).padding(bottom = 46.dp, top = 70.dp, start = 40.dp, end = 40.dp).fillMaxSize().clickable { if (item.photos > 0) viewing = 0 },
                big = true, dust = if (Museum.dusty(item)) 0.55f - Museum.fullness(item) * 0.4f else 0f)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp).background(Brush.verticalGradient(listOf(C.woodLight, C.wood, C.woodDark))))
            BrassLabel(h.inv(item), Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            Row(Modifier.statusBarsPadding().padding(6.dp).fillMaxWidth()) {
                IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, nav::close, tint = Color.White, bg = Color(0x33000000))
                Box(Modifier.weight(1f))
                if (item.owned && hall != null) IconBtn(if (isPearl) Icons.Rounded.Star else Icons.Rounded.StarBorder, {
                    c.write { dao.save(hall.copy(pearl = if (isPearl) 0 else item.id)) }
                }, tint = if (isPearl) C.accent else Color.White, bg = Color(0x33000000), desc = stringResource(R.string.pearl))
                IconBtn(Icons.Outlined.Edit, { nav.open(Overlay.Edit(item.id)) }, tint = Color.White, bg = Color(0x33000000))
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(listOfNotNull(hall?.name, cab?.name).joinToString(" › ").uppercase(), style = T.Caps, modifier = Modifier.padding(top = 16.dp))
            Text(item.title.ifBlank { stringResource(R.string.untitled) }, style = T.Title.copy(color = if (item.title.isBlank()) C.muted else C.ink), modifier = Modifier.padding(top = 2.dp))
            if (isPearl) Text(stringResource(R.string.pearl_of, hall?.name ?: ""), style = T.Italic.copy(color = C.accent))
        }

        if (!item.owned) Card(Modifier.padding(16.dp).fillMaxWidth(), color = C.accentSoft) {
            Text(stringResource(R.string.wish_card_t), style = T.Strong)
            Text(stringResource(R.string.wish_card_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            Btn(stringResource(R.string.wish_got), {
                c.write { dao.save(item.copy(status = Status.OWNED, acquired = Museum.today(), updated = System.currentTimeMillis())); celebrate() }
                nav.open(Overlay.Edit(item.id))
            }, Modifier.padding(top = 12.dp).fillMaxWidth(), Kind.ACCENT)
        } else if (Museum.dusty(item)) Card(Modifier.padding(16.dp).fillMaxWidth(), color = C.surface2, onClick = { nav.open(Overlay.Edit(item.id)) }) {
            Text(stringResource(R.string.dusty_card_t), style = T.Strong)
            Text(stringResource(R.string.dusty_card_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        }

        // The inventory card.
        PaperCard(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(stringResource(R.string.card_head, h.inv(item)), style = T.TypeSmall)
            Gap(6.dp)
            @Composable
            fun line(label: String, value: String) {
                if (value.isBlank()) return
                Row { Text("$label: ", style = T.Type.copy(color = C.paperInk.copy(alpha = .55f))); Text(value, style = T.Type) }
            }
            line(stringResource(R.string.f_acquired), item.acquired?.let { Fmt.date(it, ru) } ?: "")
            line(stringResource(R.string.f_place), item.place)
            line(stringResource(R.string.f_price), item.price)
            if (item.condition > 0) line(stringResource(R.string.f_condition), stringArrayResource(R.array.conditions)[item.condition])
            line(stringResource(R.string.f_storage), item.storage)
            for (f in hall?.fieldList ?: emptyList()) line(f, item.field(f))
            h.series.firstOrNull { it.id == item.series }?.let { line(stringResource(R.string.f_series), it.name) }
            if (item.copies > 1) line(stringResource(R.string.f_copies), pluralStringResource(R.plurals.copies_spare, item.copies - 1, item.copies, item.copies - 1))
            line(stringResource(R.string.f_tags), item.tags)
            if (item.checked > 0) line(stringResource(R.string.f_checked), Fmt.date(item.checked, ru))
            if (item.story.isNotBlank()) {
                Text(stringResource(R.string.f_story) + ":", style = T.Type.copy(color = C.paperInk.copy(alpha = .55f)), modifier = Modifier.padding(top = 6.dp))
                Text(item.story, style = T.Type)
            }
            if (item.title.isBlank() && item.place.isBlank() && item.story.isBlank()) Text(stringResource(R.string.card_empty), style = T.Type.copy(color = C.paperInk.copy(alpha = .45f)))
        }

        Btn(stringResource(if (Museum.dusty(item)) R.string.card_fill else R.string.card_edit), { nav.open(Overlay.Edit(item.id)) },
            Modifier.padding(horizontal = 16.dp).fillMaxWidth(), if (Museum.dusty(item)) Kind.ACCENT else Kind.GHOST, icon = Icons.Outlined.Edit)
        Gap(12.dp)
        if (item.photos > 1) {
            Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in 0 until item.photos) Original(item, n, Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).clickable { viewing = n })
            }
        }

        val notes = h.notes.filter { it.item == item.id }
        if (notes.isNotEmpty()) {
            Text(stringResource(R.string.visitors_caps).uppercase(), style = T.Caps, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
            for (n in notes.take(5)) Text("«${n.text}»", style = T.Italic, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        Gap(32.dp)
    }

    viewing?.let { n ->
        Scrim({ viewing = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Original(item, n, Modifier.fillMaxWidth().height(480.dp), fit = true, px = 1600)
                if (item.photos > 1) Text("${n + 1} / ${item.photos}", style = T.Label.copy(color = Color.White), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp).clickable { viewing = (n + 1) % item.photos }.padding(8.dp))
            }
        }
    }
}

/** One of the photos as taken (not the shelf face). */
@Composable
fun Original(item: Item, n: Int, modifier: Modifier, fit: Boolean = false, px: Int = 400) {
    val c = LocalContext.current.container
    var bmp by remember(item.id, n, item.updated) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.id, n, item.updated) { bmp = c.photos.loadOriginal(item.id, n, px)?.asImageBitmap() }
    Box(modifier.background(if (fit) Color.Transparent else C.surface2)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = if (fit) ContentScale.Fit else ContentScale.Crop) }
    }
}
