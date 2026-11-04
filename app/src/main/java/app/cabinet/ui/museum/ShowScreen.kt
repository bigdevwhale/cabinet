package app.cabinet.ui.museum

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Show
import app.cabinet.domain.Museum
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.art.Shelves
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Caps
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch

/** Temporary exhibitions: a theme a week, a handful of things chosen for it, a poster to send. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val week = Museum.week()
    val current = Museum.showOfWeek(h.shows, week)
    var making by remember { mutableStateOf(current == null) }
    val themes = stringArrayResource(R.array.themes)
    val hints = stringArrayResource(R.array.themes_hint)
    var theme by remember { mutableStateOf(current?.theme?.let { runCatching { Museum.Theme.valueOf(it) }.getOrNull() } ?: Museum.themeOfWeek(h.items, week)) }
    var title by remember { mutableStateOf(current?.title ?: themes[theme.ordinal]) }
    val chosen = remember { mutableStateListOf<Long>().apply { current?.ids?.let { addAll(it) } } }
    val museum = h.s.name.ifBlank { stringResource(R.string.app_name) }
    val past = h.shows.filter { it.week != week }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.show_title), stringResource(R.string.show_week, Fmt.short(week, ru)), nav::close)
        if (!making && current != null) {
            val items = current.ids.mapNotNull { h.item(it) }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                Text(themes[runCatching { Museum.Theme.valueOf(current.theme).ordinal }.getOrDefault(0)].uppercase(), style = T.Caps, modifier = Modifier.padding(top = 8.dp))
                Text(current.title, style = T.Hero, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                Shelves(items, "", tall = 96.dp, emptyRows = 0, onTap = { nav.open(Overlay.Item(it.id)) })
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(stringResource(R.string.show_share), { scope.launch { Poster.share(ctx, current.title, themes[runCatching { Museum.Theme.valueOf(current.theme).ordinal }.getOrDefault(0)], museum, items) } }, Modifier.weight(1f), Kind.ACCENT)
                    Btn(stringResource(R.string.show_redo), { making = true }, Modifier.weight(1f), Kind.GHOST)
                }
                if (past.isNotEmpty()) {
                    Caps(stringResource(R.string.show_past), Modifier.padding(horizontal = 0.dp))
                    for (s in past) PastShow(s, h, ru)
                }
            }
            return@Column
        }
        val pool = Museum.suits(theme, h.items)
        LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text(stringResource(R.string.show_pick_theme), style = T.Caps, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (t in Museum.Theme.entries) {
                            val n = Museum.suits(t, h.items).size
                            if (n == 0 && t != theme) continue
                            Chip(themes[t.ordinal], t == theme) {
                                if (title == themes[theme.ordinal] || title.isBlank()) title = themes[t.ordinal]
                                theme = t
                            }
                        }
                    }
                    Text(hints[theme.ordinal], style = T.Small, modifier = Modifier.padding(top = 10.dp))
                    Field(title, { title = it }, stringResource(R.string.show_name), Modifier.padding(top = 10.dp))
                    Text(stringResource(R.string.show_pick, chosen.size), style = T.Caps, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
            }
            items(pool, key = { it.id }) { i ->
                val on = i.id in chosen
                Box(Modifier.aspectRatio(0.8f).clip(RoundedCornerShape(14.dp)).background(if (on) C.accentSoft else C.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) C.accent else C.line, RoundedCornerShape(14.dp))
                    .clickable { if (on) chosen.remove(i.id) else if (chosen.size < 12) chosen.add(i.id) }.padding(8.dp)) {
                    Face(i, Modifier.fillMaxSize().padding(bottom = 18.dp))
                    Text(i.title.ifBlank { h.inv(i) }, style = T.Label, maxLines = 1, modifier = Modifier.align(Alignment.BottomCenter))
                    if (on) Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(C.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = C.onAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().background(C.surface).navigationBarsPadding().padding(16.dp)) {
            Btn(stringResource(R.string.show_open), {
                val ids = chosen.toList()
                c.write {
                    current?.let { dao.drop(it) }
                    dao.add(Show(week = week, theme = theme.name, title = title.ifBlank { themes[theme.ordinal] }, items = ids.joinToString(",")))
                }
                making = false
            }, Modifier.fillMaxWidth(), Kind.ACCENT, enabled = chosen.size >= 2)
        }
    }
}

@Composable
private fun PastShow(s: Show, h: Home, ru: Boolean) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), pad = 14.dp) {
        Text(s.title, style = T.Strong)
        Text(Fmt.short(s.week, ru), style = T.Small)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in s.ids.mapNotNull { h.item(it) }.take(6)) Face(i, Modifier.size(44.dp))
        }
    }
}

