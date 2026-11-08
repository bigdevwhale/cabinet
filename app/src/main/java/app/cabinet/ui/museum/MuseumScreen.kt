package app.cabinet.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Hall
import app.cabinet.domain.Museum
import app.cabinet.ui.BAR_SPACE
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.Tab
import app.cabinet.ui.add.LocalAdder
import app.cabinet.ui.catalog.Catalog
import app.cabinet.ui.catalog.Filter
import app.cabinet.ui.components.Bar
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.R16
import app.cabinet.ui.components.R22
import app.cabinet.ui.components.Ring
import app.cabinet.ui.components.Rule
import app.cabinet.ui.components.SectionTitle
import app.cabinet.ui.components.lift
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

@Composable
fun MuseumScreen(h: Home, nav: Nav) {
    val adder = LocalAdder.current
    val owned = h.owned
    val described = owned.count { Museum.described(it) }
    val stage = Museum.stage(owned.size)
    val today = Museum.today()
    val name = h.s.name.ifBlank { stringResource(R.string.app_name) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.museum_eyebrow), style = T.Small)
                Text(name, style = T.Hero, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(C.ink).clickable { nav.open(Overlay.Settings) }, contentAlignment = Alignment.Center) {
                Text(name.first().uppercase(), style = T.Strong.copy(color = C.bg))
            }
        }

        if (h.items.isEmpty()) {
            Card(Modifier.padding(16.dp).fillMaxWidth(), pad = 20.dp) {
                Text(stringResource(R.string.empty_t), style = T.Heading)
                Text(stringResource(R.string.empty_b), style = T.Body, modifier = Modifier.padding(top = 6.dp))
                Btn(stringResource(R.string.add_first), { adder.start() }, Modifier.padding(top = 16.dp).fillMaxWidth(), Kind.LIME, icon = Icons.Rounded.Add)
            }
        } else {
            // How much is described, and how far to the next level of the museum.
            Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth(), onClick = { Catalog.preset = Filter.DUSTY; nav.tab = Tab.CATALOG }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(if (owned.isEmpty()) 0f else described.toFloat() / owned.size)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(stringResource(R.string.progress_t, described, owned.size), style = T.Strong)
                        val left = owned.size - described
                        Text(if (left > 0) pluralStringResource(R.plurals.progress_b, left, left) else stringResource(R.string.progress_all), style = T.Small)
                    }
                }
                Rule(Modifier.padding(vertical = 14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.level), style = T.Label)
                        Text(stringArrayResource(R.array.stages)[stage], style = T.Strong)
                    }
                    Text(stringArrayResource(R.array.ranks)[Museum.rank(described)], style = T.Label.copy(color = C.accent),
                        modifier = Modifier.clip(R16).background(C.accentSoft).padding(horizontal = 10.dp, vertical = 5.dp))
                }
                if (stage < Museum.STAGES.lastIndex) {
                    val next = Museum.STAGES[stage + 1]; val prev = Museum.STAGES[stage]
                    Bar((owned.size - prev).toFloat() / (next - prev), Modifier.padding(top = 10.dp))
                    Text(pluralStringResource(R.plurals.stage_next, next - owned.size, next - owned.size, stringArrayResource(R.array.stages)[stage + 1]),
                        style = T.Small, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        SectionTitle(stringResource(R.string.halls_title), action = stringResource(R.string.all), onAction = { nav.tab = Tab.HALLS })
        HallGrid(h, nav)

        // «Do I have it?» — the collector's question at a flea market.
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().lift().clip(R22).background(C.accentSoft).clickable { nav.open(Overlay.Check()) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(R16).background(C.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PhotoCamera, null, tint = C.onAccent, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.check_title), style = T.Strong)
                Text(stringResource(R.string.check_card_b), style = T.Small)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
        }

        Round(h)
        WeekShow(h, nav)

        // Visitors and the day's memories.
        val visitors = Museum.visitors(h.items, today)
        if (visitors > 0) {
            SectionTitle(stringResource(R.string.visitors_caps), action = stringResource(R.string.book_open), onAction = { nav.tab = Tab.BOOK })
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.visitors_today, visitors, visitors), style = T.Strong)
                h.notes.firstOrNull()?.let { n ->
                    Row(Modifier.padding(top = 10.dp).clickable { nav.open(Overlay.Item(n.item)) }, verticalAlignment = Alignment.Top) {
                        h.item(n.item)?.let { Box(Modifier.size(44.dp).clip(R16).background(h.tint(it.hall))) { Face(it, Modifier.fillMaxSize().padding(4.dp)) } }
                        Text("«${n.text}»", style = T.Quote, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
        val otd = Museum.onThisDay(h.items, today)
        if (otd.isNotEmpty()) {
            SectionTitle(stringResource(R.string.otd_caps))
            for (i in otd.take(2)) Card(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth(), pad = 12.dp, onClick = { nav.open(Overlay.Item(i.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(R16).background(h.tint(i.hall))) { Face(i, Modifier.fillMaxSize().padding(5.dp)) }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(i.title.ifBlank { h.inv(i) }, style = T.Strong)
                        val years = java.time.LocalDate.now().year - java.time.LocalDate.ofEpochDay(i.acquired!!).year
                        Text(pluralStringResource(R.plurals.otd_years, years, years), style = T.Small)
                    }
                }
            }
        }

        // The wish list and the spares.
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), onClick = { Catalog.preset = Filter.WISH; nav.tab = Tab.CATALOG }) {
                Text(h.items.count { !it.owned }.toString(), style = T.Number)
                Text(stringResource(R.string.fund_wish), style = T.Small)
            }
            Card(Modifier.weight(1f), onClick = { Catalog.preset = Filter.TRADE; nav.tab = Tab.CATALOG }) {
                Text(owned.sumOf { (it.copies - 1).coerceAtLeast(0) }.toString(), style = T.Number)
                Text(stringResource(R.string.fund_trade), style = T.Small)
            }
        }
        Gap(BAR_SPACE)
    }
}

/** The halls as soft coloured tiles, two to a row, with a few of their things. */
@Composable
fun HallGrid(h: Home, nav: Nav) {
    val tiles: List<Hall?> = h.halls + listOf(null)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in tiles.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (hall in row) {
                if (hall == null) {
                    Column(Modifier.weight(1f).height(150.dp).clip(R22).background(C.surface2).clickable { nav.open(Overlay.HallEdit(0)) }.padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.size(36.dp).clip(R16).background(C.surface), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = C.ink) }
                        Text(stringResource(R.string.hall_new), style = T.Strong)
                    }
                } else {
                    val mine = h.items.filter { it.hall == hall.id && it.owned && it.photos > 0 }.sortedByDescending { it.created }
                    Column(Modifier.weight(1f).height(150.dp).clip(R22).background(h.tint(hall.id)).clickable { nav.open(Overlay.Hall(hall.id)) }.padding(12.dp)) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                            for (i in mine.take(3)) Face(i, Modifier.weight(1f).fillMaxSize(), dim = false)
                        }
                        Text(hall.name, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        val n = h.items.count { it.hall == hall.id && it.owned }
                        Text(pluralStringResource(R.plurals.items_n, n, n), style = T.Small.copy(color = C.ink2.copy(alpha = .7f)))
                    }
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/** The evening round: one question about one thing, once a day. */
@Composable
private fun Round(h: Home) {
    val c = LocalContext.current.container
    val today = Museum.today()
    val q = remember(h.items.size, today) { Museum.question(h.items, today) }
    SectionTitle(stringResource(R.string.round_caps), action = if (h.s.rounds > 0) pluralStringResource(R.plurals.rounds_done, h.s.rounds, h.s.rounds) else null)
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        if (h.s.round >= today || q == null) {
            Text(stringResource(if (q == null) R.string.round_all else R.string.round_done), style = T.Body)
            return@Card
        }
        val item = h.item(q.item.id) ?: q.item
        var answer by remember(item.id, q.ask) { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(R16).background(h.tint(item.hall))) { Face(item, Modifier.fillMaxSize().padding(6.dp), dim = false) }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringArrayResource(R.array.round_q)[q.ask.ordinal].format(item.title.ifBlank { h.inv(item) }.removeSurrounding("«", "»")), style = T.Strong.copy(fontSize = 16.sp, lineHeight = 21.sp))
                Text(h.inv(item) + " · " + (h.hallOf[item.hall]?.name ?: ""), style = T.Small, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
        fun save(next: app.cabinet.data.Item) = c.write { dao.save(next.copy(updated = System.currentTimeMillis())); settings.update { it.copy(round = today, rounds = it.rounds + 1) } }
        if (q.ask == Museum.Ask.WHEN) {
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, back) in listOf(R.string.when_year to 0L, R.string.when_older to 1L, R.string.when_long to 5L)) {
                    Text(stringResource(label), style = T.Label.copy(color = C.ink, fontSize = 13.sp),
                        modifier = Modifier.clip(R16).background(C.surface2).clickable {
                            save(item.copy(acquired = java.time.LocalDate.now().minusYears(back).withDayOfYear(1).toEpochDay(), acquiredLevel = 2))
                        }.padding(horizontal = 12.dp, vertical = 9.dp))
                }
            }
        } else {
            BasicTextField(answer, { answer = it }, textStyle = T.Body.copy(color = C.ink), minLines = if (q.ask == Museum.Ask.STORY) 3 else 1,
                cursorBrush = SolidColor(C.accent), modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().clip(R16).background(C.surface2).padding(14.dp)) {
                        if (answer.isEmpty()) Text(stringArrayResource(R.array.round_hint)[q.ask.ordinal], style = T.Body.copy(color = C.muted))
                        inner()
                    }
                })
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.round_skip), style = T.Label.copy(fontSize = 13.sp),
                modifier = Modifier.clickable { c.write { settings.update { it.copy(round = today) } } }.padding(vertical = 8.dp))
            Box(Modifier.weight(1f))
            if (q.ask != Museum.Ask.WHEN) Btn(stringResource(R.string.round_write), {
                val a = answer.trim()
                save(when (q.ask) {
                    Museum.Ask.TITLE -> item.copy(title = a)
                    Museum.Ask.PLACE -> item.copy(place = a)
                    Museum.Ask.STORY -> item.copy(story = a)
                    Museum.Ask.STORAGE -> item.copy(storage = a)
                    Museum.Ask.WHEN -> item
                })
            }, kind = Kind.PRIMARY, enabled = answer.isNotBlank())
        }
    }
}

/** This week's exhibition: the theme, or the show already hung. */
@Composable
private fun WeekShow(h: Home, nav: Nav) {
    val themes = stringArrayResource(R.array.themes)
    val theme = Museum.themeOfWeek(h.items)
    val show = Museum.showOfWeek(h.shows)
    SectionTitle(stringResource(R.string.show_caps), action = stringResource(R.string.show_all), onAction = { nav.open(Overlay.Show) })
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(R22).background(C.hall(5)).clickable { nav.open(Overlay.Show) }.padding(16.dp)) {
        val items = show?.ids?.mapNotNull { h.item(it) } ?: Museum.suits(theme, h.items).take(4)
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in items.take(4)) Face(i, Modifier.weight(1f).fillMaxSize(), dim = false)
        }
        Text(show?.title ?: themes[theme.ordinal], style = T.Heading, modifier = Modifier.padding(top = 10.dp))
        Text(if (show != null) pluralStringResource(R.plurals.show_items, show.ids.size, show.ids.size) else stringResource(R.string.show_make_hint),
            style = T.Small.copy(color = C.ink2.copy(alpha = .75f)))
    }
}
