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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.domain.Museum
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.Tab
import app.cabinet.ui.add.rememberAdder
import app.cabinet.ui.art.Facade
import app.cabinet.ui.art.ShelfStrip
import app.cabinet.ui.catalog.Filter
import app.cabinet.ui.catalog.Catalog
import app.cabinet.ui.components.Bar
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Caps
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Gap
import app.cabinet.ui.components.IconBtn
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.Stats
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

@Composable
fun MuseumScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val adder = rememberAdder(h, nav)
    val owned = h.owned
    val described = owned.count { Museum.described(it) }
    val stage = Museum.stage(owned.size)
    val today = Museum.today()
    val visitors = Museum.visitors(h.items, today)

    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        // The house itself, edge to edge under the status bar, with the museum's name over the sky.
        Box(Modifier.fillMaxWidth()) {
            Facade(stage, visitors, seed = today)
            Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to androidx.compose.ui.graphics.Color.Black.copy(alpha = if (C.dark) .55f else .38f), 0.45f to androidx.compose.ui.graphics.Color.Transparent)))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.museum_eyebrow).uppercase(), style = T.Caps.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f)))
                    Text(h.s.name.ifBlank { stringResource(R.string.app_name) }, style = T.Title.copy(color = androidx.compose.ui.graphics.Color.White,
                        shadow = androidx.compose.ui.graphics.Shadow(androidx.compose.ui.graphics.Color.Black.copy(alpha = .35f), blurRadius = 12f)))
                }
                IconBtn(Icons.Outlined.Settings, { nav.open(Overlay.Settings) }, tint = androidx.compose.ui.graphics.Color.White, bg = androidx.compose.ui.graphics.Color.Black.copy(alpha = .18f))
            }
        }
        // What is being built next.
        Row(Modifier.fillMaxWidth().background(C.surface).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringArrayResource(R.array.stages)[stage], style = T.Italic.copy(color = C.ink))
                if (stage < Museum.STAGES.lastIndex) {
                    val next = Museum.STAGES[stage + 1]; val prev = Museum.STAGES[stage]
                    Bar((owned.size - prev).toFloat() / (next - prev), Modifier.padding(top = 6.dp))
                    Text(pluralStringResource(R.plurals.stage_next, next - owned.size, next - owned.size, stringArrayResource(R.array.stages)[stage + 1]),
                        style = T.Small, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))

        if (owned.isEmpty() && h.items.isEmpty()) {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Text(stringResource(R.string.empty_t), style = T.Heading)
                Text(stringResource(R.string.empty_b), style = T.Body, modifier = Modifier.padding(top = 6.dp))
                Btn(stringResource(R.string.add_first), { adder.start() }, Modifier.padding(top = 14.dp).fillMaxWidth(), Kind.ACCENT)
            }
            return@Column
        }

        Round(h)

        Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth(), pad = 14.dp) {
            Stats(listOf(
                Fmt.n(owned.size) to stringResource(R.string.st_items),
                Fmt.n(h.halls.size) to stringResource(R.string.st_halls),
                Fmt.n(described) to stringResource(R.string.st_described),
            ))
            val rank = Museum.rank(described)
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(C.line))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.rank), style = T.Caps)
                    Text(stringArrayResource(R.array.ranks)[rank], style = T.Heading)
                }
            }
            if (rank < Museum.RANKS.lastIndex) {
                val left = Museum.RANKS[rank + 1] - described
                Text(pluralStringResource(R.plurals.rank_next, left, left, stringArrayResource(R.array.ranks)[rank + 1]), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            }
        }


        val dusty = owned.count { Museum.dusty(it) }
        if (dusty > 0) Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth(), onClick = { Catalog.preset = Filter.DUSTY; nav.tab = Tab.CATALOG }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pluralStringResource(R.plurals.dusty_t, dusty, dusty), style = T.Strong)
                    Text(stringResource(R.string.dusty_b), style = T.Small, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = C.muted)
            }
            ShelfStrip(owned.filter { Museum.dusty(it) }.take(8), Modifier.padding(top = 10.dp), tall = 52.dp)
        }

        // The exhibition of the week.
        val theme = Museum.themeOfWeek(h.items)
        val show = Museum.showOfWeek(h.shows)
        Caps(stringResource(R.string.show_caps), right = stringResource(R.string.show_all), onRight = { nav.open(Overlay.Show) })
        Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = { nav.open(Overlay.Show) }) {
            if (show != null) {
                Text(show.title, style = T.Heading)
                Text(stringArrayResource(R.array.themes)[runCatching { Museum.Theme.valueOf(show.theme).ordinal }.getOrDefault(0)], style = T.Small)
                ShelfStrip(show.ids.mapNotNull { h.item(it) }, Modifier.padding(top = 10.dp), tall = 64.dp)
            } else {
                Text(stringResource(R.string.show_theme), style = T.Caps)
                Text(stringArrayResource(R.array.themes)[theme.ordinal], style = T.Heading, modifier = Modifier.padding(top = 2.dp))
                Text(stringArrayResource(R.array.themes_hint)[theme.ordinal], style = T.Small, modifier = Modifier.padding(top = 4.dp))
                Btn(stringResource(R.string.show_make), { nav.open(Overlay.Show) }, Modifier.padding(top = 12.dp), Kind.GHOST)
            }
        }

        // Inventory due.
        val season = Museum.season(today)
        val due = h.halls.filter { hl -> h.owned.any { it.hall == hl.id } && h.seals.none { it.hall == hl.id && it.season == season } }
        if (due.isNotEmpty()) Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth(), onClick = { nav.open(Overlay.Inventory(due.first().id)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.inv_due_t, due.first().name), style = T.Strong)
                    Text(stringResource(R.string.inv_due_b), style = T.Small, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = C.muted)
            }
        }

        // Visitors.
        Caps(stringResource(R.string.visitors_caps), right = stringResource(R.string.book_open), onRight = { nav.tab = Tab.BOOK })
        Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Text(pluralStringResource(R.plurals.visitors_today, visitors, visitors), style = T.Strong)
            for (n in h.notes.take(2)) Text("«${n.text}»", style = T.Italic, modifier = Modifier.padding(top = 8.dp).clickable { nav.open(Overlay.Item(n.item)) })
            Text(stringResource(R.string.visitors_hint), style = T.Small, modifier = Modifier.padding(top = 10.dp))
        }

        // On this day.
        val otd = Museum.onThisDay(h.items, today)
        if (otd.isNotEmpty()) {
            Caps(stringResource(R.string.otd_caps))
            for (i in otd.take(3)) Card(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth(), onClick = { nav.open(Overlay.Item(i.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Face(i, Modifier.size(56.dp))
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(i.title.ifBlank { h.inv(i) }, style = T.Strong)
                        val years = java.time.LocalDate.now().year - java.time.LocalDate.ofEpochDay(i.acquired!!).year
                        Text(pluralStringResource(R.plurals.otd_years, years, years), style = T.Small)
                    }
                }
            }
        }

        // Funds.
        val doubles = owned.filter { it.copies > 1 }
        val wishes = h.items.count { !it.owned }
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), pad = 14.dp, onClick = { Catalog.preset = Filter.WISH; nav.tab = Tab.CATALOG }) {
                Text(Fmt.n(wishes), style = T.Number)
                Text(stringResource(R.string.fund_wish), style = T.Label)
            }
            Card(Modifier.weight(1f), pad = 14.dp, onClick = { Catalog.preset = Filter.TRADE; nav.tab = Tab.CATALOG }) {
                Text(Fmt.n(doubles.sumOf { it.copies - 1 }), style = T.Number)
                Text(stringResource(R.string.fund_trade), style = T.Label)
            }
        }
        Gap(12.dp)
    }
    app.cabinet.ui.StatusScrim(((scroll.value - 300) / 200f))
    }
}

/** The evening round: one question about one thing, once a day. */
@Composable
private fun Round(h: Home) {
    val c = LocalContext.current.container
    val today = Museum.today()
    val q = remember(h.items.size, today) { Museum.question(h.items, today) }
    Caps(stringResource(R.string.round_caps), right = if (h.s.rounds > 0) pluralStringResource(R.plurals.rounds_done, h.s.rounds, h.s.rounds) else null)
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = C.paper) {
        if (h.s.round >= today || q == null) {
            Text(stringResource(if (q == null) R.string.round_all else R.string.round_done), style = T.Type)
            return@Card
        }
        val item = h.item(q.item.id) ?: q.item
        var answer by remember(item.id, q.ask) { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).background(C.back), contentAlignment = Alignment.Center) {
                Face(item, Modifier.fillMaxSize().padding(8.dp))
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(h.inv(item) + " · " + (h.hallOf[item.hall]?.name ?: ""), style = T.TypeSmall, maxLines = 1)
                Text(stringArrayResource(R.array.round_q)[q.ask.ordinal].format(item.title.ifBlank { h.inv(item) }),
                    style = T.Italic.copy(color = C.paperInk, fontSize = 19.sp, lineHeight = 24.sp), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (q.ask == Museum.Ask.WHEN) {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, back) in listOf(R.string.when_year to 0L, R.string.when_older to 1L, R.string.when_long to 5L)) {
                    Text(stringResource(label), style = T.Label.copy(color = C.paperInk),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(C.paperLine).clickable {
                            val d = if (back == 0L) java.time.LocalDate.now().withDayOfYear(1).toEpochDay() else java.time.LocalDate.now().minusYears(back).withDayOfYear(1).toEpochDay()
                            c.write { dao.save(item.copy(acquired = d, updated = System.currentTimeMillis())); settings.update { it.copy(round = today, rounds = it.rounds + 1) } }
                        }.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
        } else {
            androidx.compose.foundation.text.BasicTextField(
                answer, { answer = it }, textStyle = T.Type, minLines = if (q.ask == Museum.Ask.STORY) 3 else 1,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.paperInk),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().background(C.paperLine.copy(alpha = .35f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                        if (answer.isEmpty()) Text(stringArrayResource(R.array.round_hint)[q.ask.ordinal], style = T.Type.copy(color = C.paperInk.copy(alpha = .4f)))
                        inner()
                    }
                },
            )
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.round_skip), style = T.Label.copy(color = C.paperInk.copy(alpha = .6f)),
                modifier = Modifier.clickable { c.write { settings.update { it.copy(round = today) } } }.padding(vertical = 8.dp))
            Box(Modifier.weight(1f))
            if (q.ask != Museum.Ask.WHEN) Text(stringResource(R.string.round_write), style = T.Button.copy(color = if (answer.isBlank()) C.paperInk.copy(alpha = .35f) else androidx.compose.ui.graphics.Color.White),
                modifier = Modifier.clickable(enabled = answer.isNotBlank()) {
                    val a = answer.trim()
                    val next = when (q.ask) {
                        Museum.Ask.TITLE -> item.copy(title = a)
                        Museum.Ask.PLACE -> item.copy(place = a)
                        Museum.Ask.STORY -> item.copy(story = a)
                        Museum.Ask.STORAGE -> item.copy(storage = a)
                        Museum.Ask.WHEN -> item
                    }
                    c.write { dao.save(next.copy(updated = System.currentTimeMillis())); settings.update { it.copy(round = today, rounds = it.rounds + 1) } }
                }.clip(RoundedCornerShape(50)).background(if (answer.isBlank()) C.paperLine.copy(alpha = .5f) else C.red).padding(vertical = 9.dp, horizontal = 18.dp), textAlign = TextAlign.End)
        }
    }
}
