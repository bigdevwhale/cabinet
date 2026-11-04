package app.cabinet.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.Overlay
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Face
import app.cabinet.ui.components.Fmt
import app.cabinet.ui.components.PaperCard
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import java.time.LocalDate

/** The museum's books: arrivals in the order they came, and what visitors wrote. */
@Composable
fun BookScreen(h: Home, nav: Nav) {
    val ru = LocalContext.current.ru
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Text(stringResource(R.string.book_title), style = T.Title)
            Row(Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(stringResource(R.string.book_arrivals), tab == 0) { tab = 0 }
                Chip(stringResource(R.string.book_visitors), tab == 1) { tab = 1 }
            }
        }
        if (tab == 0) {
            val arrivals = h.owned.sortedWith(compareByDescending<app.cabinet.data.Item> { it.acquired ?: (it.created / 86_400_000L) }.thenByDescending { it.inv })
            val byYear = arrivals.groupBy { LocalDate.ofEpochDay(it.acquired ?: (it.created / 86_400_000L)).year }
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                for ((year, list) in byYear) {
                    item(key = "y$year") {
                        Row(Modifier.padding(top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                            Text(year.toString(), style = T.Heading)
                            Text("  " + stringResource(R.string.book_year_n, list.size), style = T.Small)
                        }
                    }
                    item(key = "p$year") {
                        PaperCard(Modifier.fillMaxWidth()) {
                            for (i in list) Row(Modifier.fillMaxWidth().clickable { nav.open(Overlay.Item(i.id)) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(i.acquired?.let { Fmt.short(it, ru).substringBeforeLast(' ') } ?: "—", style = T.TypeSmall, modifier = Modifier.width(64.dp))
                                Face(i, Modifier.size(28.dp))
                                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                                    Text(i.title.ifBlank { stringResource(R.string.untitled) }, style = T.Type, maxLines = 1)
                                    if (i.place.isNotBlank()) Text(i.place, style = T.TypeSmall, maxLines = 1)
                                }
                                Text(h.inv(i), style = T.TypeSmall)
                            }
                        }
                    }
                }
                if (arrivals.isEmpty()) item { Text(stringResource(R.string.book_empty), style = T.Body.copy(color = C.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                item { Text(stringResource(R.string.visitors_hint), style = T.Small, modifier = Modifier.padding(bottom = 10.dp)) }
                items(h.notes, key = { it.id }) { n ->
                    val i = h.item(n.item)
                    Row(Modifier.fillMaxWidth().clickable(enabled = i != null) { nav.open(Overlay.Item(n.item)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        if (i != null) Face(i, Modifier.size(44.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("«${n.text}»", style = T.Italic)
                            Text(Fmt.date(n.day, ru), style = T.Small)
                        }
                    }
                }
                if (h.notes.isEmpty()) item { Text(stringResource(R.string.book_no_visitors), style = T.Body.copy(color = C.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            }
        }
    }
}
