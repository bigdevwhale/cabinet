package app.cabinet.ui.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.R12
import app.cabinet.ui.components.R16
import app.cabinet.ui.components.Rule
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.lift
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

/** A thing: its photo on its hall's colour, and under it a sheet with everything known about it. */
@OptIn(ExperimentalLayoutApi::class)
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(380.dp).background(h.tint(item.hall)).clickable(enabled = item.photos > 0) { viewing = 0 }) {
                Face(item, Modifier.fillMaxSize().statusBarsPadding().padding(start = 36.dp, end = 36.dp, top = 64.dp, bottom = 46.dp), big = true, dim = false)
            }
            Column(Modifier.offset(y = (-26).dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg).padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(50)).background(C.line))
                Text(item.title.ifBlank { stringResource(R.string.untitled) }, style = T.Title.copy(color = if (item.title.isBlank()) C.muted else C.ink), modifier = Modifier.padding(top = 14.dp))
                Text(listOfNotNull(h.inv(item), hall?.name, cab?.name).joinToString(" · "), style = T.Small, modifier = Modifier.padding(top = 2.dp))
                if (isPearl) Text(stringResource(R.string.pearl_of, hall?.name ?: ""), style = T.Label.copy(color = C.accent), modifier = Modifier.padding(top = 6.dp))

                // The essentials as soft pills.
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.place.isNotBlank()) Pill(Icons.Outlined.Place, item.place)
                    item.acquired?.let { Pill(Icons.Outlined.CalendarMonth, Fmt.date(it, ru)) }
                    if (item.storage.isNotBlank()) Pill(Icons.Outlined.Inventory2, item.storage)
                    if (item.condition > 0) Pill(Icons.Outlined.Verified, stringArrayResource(R.array.conditions)[item.condition])
                }

                if (!item.owned) Card(Modifier.padding(top = 16.dp).fillMaxWidth(), color = C.accentSoft) {
                    Text(stringResource(R.string.wish_card_t), style = T.Strong)
                    Text(stringResource(R.string.wish_card_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                    Btn(stringResource(R.string.wish_got), {
                        c.write { dao.save(item.copy(status = Status.OWNED, acquired = Museum.today(), updated = System.currentTimeMillis())); celebrate() }
                        nav.open(Overlay.Edit(item.id))
                    }, Modifier.padding(top = 12.dp).fillMaxWidth(), Kind.LIME)
                } else if (Museum.dusty(item)) Card(Modifier.padding(top = 16.dp).fillMaxWidth(), color = C.surface2, onClick = { nav.open(Overlay.Edit(item.id)) }) {
                    Text(stringResource(R.string.no_story_t), style = T.Strong)
                    Text(stringResource(R.string.no_story_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                }

                if (item.story.isNotBlank()) {
                    Text(stringResource(R.string.f_story), style = T.Heading, modifier = Modifier.padding(top = 22.dp))
                    Text(item.story, style = T.Body, modifier = Modifier.padding(top = 6.dp))
                }

                // The hall's own fields and the rest of the card, as a quiet table.
                val rows = buildList {
                    for (f in hall?.fieldList ?: emptyList()) item.field(f).takeIf { it.isNotBlank() }?.let { add(f to it) }
                    if (item.price.isNotBlank()) add(stringResource(R.string.f_price) to item.price)
                    h.series.firstOrNull { it.id == item.series }?.let { add(stringResource(R.string.f_series) to it.name) }
                    if (item.copies > 1) add(stringResource(R.string.f_copies) to pluralStringResource(R.plurals.copies_spare, item.copies - 1, item.copies, item.copies - 1))
                    if (item.tags.isNotBlank()) add(stringResource(R.string.f_tags) to item.tags)
                    if (item.checked > 0) add(stringResource(R.string.f_checked) to Fmt.date(item.checked, ru))
                }
                if (rows.isNotEmpty()) Card(Modifier.padding(top = 20.dp).fillMaxWidth(), pad = 0.dp) {
                    rows.forEachIndexed { k, (label, value) ->
                        if (k > 0) Rule(Modifier.padding(horizontal = 16.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
                            Text(label, style = T.Small, modifier = Modifier.weight(.42f))
                            Text(value, style = T.Body.copy(color = C.ink), modifier = Modifier.weight(.58f), textAlign = TextAlign.End)
                        }
                    }
                }

                if (item.photos > 1) {
                    Text(stringResource(R.string.photos), style = T.Heading, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (n in 0 until item.photos) Original(item, n, Modifier.size(88.dp).clip(R16).clickable { viewing = n })
                    }
                }

                val notes = h.notes.filter { it.item == item.id }
                if (notes.isNotEmpty()) {
                    Text(stringResource(R.string.visitors_caps), style = T.Heading, modifier = Modifier.padding(top = 22.dp))
                    for (n in notes.take(4)) Text("«${n.text}»", style = T.Quote, modifier = Modifier.padding(top = 8.dp))
                }
                Gap(110.dp)
            }
        }
        // Back, pearl and edit float over the photo.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, nav::close)
            Box(Modifier.weight(1f))
            if (item.owned && hall != null) IconBtn(if (isPearl) Icons.Rounded.Star else Icons.Rounded.StarBorder, {
                c.write { dao.save(hall.copy(pearl = if (isPearl) 0 else item.id)) }
            }, tint = if (isPearl) C.accent else C.ink, desc = stringResource(R.string.pearl))
        }
        Btn(stringResource(if (Museum.dusty(item)) R.string.card_fill else R.string.card_edit), { nav.open(Overlay.Edit(item.id)) },
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp).fillMaxWidth().lift(R16), Kind.PRIMARY, icon = Icons.Rounded.Edit)
    }

    viewing?.let { n ->
        Scrim({ viewing = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Original(item, n, Modifier.fillMaxWidth().height(480.dp), fit = true, px = 1600)
                if (item.photos > 1) Text("${n + 1} / ${item.photos}", style = T.Label.copy(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.padding(top = 10.dp).clickable { viewing = (n + 1) % item.photos }.padding(8.dp))
            }
        }
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String) {
    Row(Modifier.lift(R12).clip(R12).background(C.surface).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.muted, modifier = Modifier.size(15.dp))
        Text(text, style = T.Label.copy(color = C.ink, fontSize = 12.5.sp), modifier = Modifier.padding(start = 5.dp))
    }
}

/** One of the photos as taken (not the cut-out). */
@Composable
fun Original(item: Item, n: Int, modifier: Modifier, fit: Boolean = false, px: Int = 400) {
    val c = LocalContext.current.container
    var bmp by remember(item.id, n, item.updated) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.id, n, item.updated) { bmp = c.photos.loadOriginal(item.id, n, px)?.asImageBitmap() }
    Box(modifier.background(if (fit) Color.Transparent else C.surface2)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = if (fit) ContentScale.Fit else ContentScale.Crop) }
    }
}
