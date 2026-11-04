package app.cabinet.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Cabinet
import app.cabinet.data.Hall
import app.cabinet.data.Item
import app.cabinet.data.Note
import app.cabinet.data.Seal
import app.cabinet.data.Series
import app.cabinet.data.Settings
import app.cabinet.data.Show
import app.cabinet.domain.Museum
import app.cabinet.ui.book.BookScreen
import app.cabinet.ui.catalog.CatalogScreen
import app.cabinet.ui.check.CheckScreen
import app.cabinet.ui.components.room
import app.cabinet.ui.halls.CabinetScreen
import app.cabinet.ui.halls.HallEditScreen
import app.cabinet.ui.halls.HallScreen
import app.cabinet.ui.halls.HallsScreen
import app.cabinet.ui.halls.InventoryScreen
import app.cabinet.ui.item.EditScreen
import app.cabinet.ui.item.ItemScreen
import app.cabinet.ui.museum.CeremonyView
import app.cabinet.ui.museum.MuseumScreen
import app.cabinet.ui.museum.ShowScreen
import app.cabinet.ui.onboarding.Onboarding
import app.cabinet.ui.settings.SettingsPage
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

enum class Tab { MUSEUM, HALLS, CATALOG, BOOK }

sealed interface Overlay {
    data class Hall(val id: Long) : Overlay
    data class Cabinet(val id: Long) : Overlay
    data class Item(val id: Long) : Overlay
    /** Editing a card; [fresh] when it was just made, so Back keeps it rather than asking. */
    data class Edit(val id: Long, val fresh: Boolean = false) : Overlay
    /** A new hall (id 0) or an existing one's settings. */
    data class HallEdit(val id: Long) : Overlay
    data class Inventory(val hall: Long) : Overlay
    data class Check(val autostart: Boolean = false, val file: String? = null) : Overlay
    data object Show : Overlay
    data object Settings : Overlay
}

/** Everything the screens read, collected once at the root. */
class Home(
    val s: Settings,
    val halls: List<Hall>,
    val cabinets: List<Cabinet>,
    val items: List<Item>,
    val series: List<Series>,
    val notes: List<Note>,
    val shows: List<Show>,
    val seals: List<Seal>,
) {
    val owned = items.filter { it.owned }
    val hallOf = halls.associateBy { it.id }
    val cabinetOf = cabinets.associateBy { it.id }
    fun code(i: Item) = hallOf[i.hall]?.code ?: ""
    fun inv(i: Item) = Museum.inv(code(i), i.inv)
    fun item(id: Long) = items.firstOrNull { it.id == id }
}

class Nav(tab: Tab, first: Overlay?) {
    var tab by mutableStateOf(tab)
    val stack = mutableStateListOf<Overlay>().apply { if (first != null) add(first) }
    fun open(o: Overlay) { stack.add(o) }
    fun close() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(o: Overlay) { close(); open(o) }
}

@Composable
fun CabinetRoot(s: Settings, tab: Tab = Tab.MUSEUM, overlay: Overlay? = null) {
    val c = LocalContext.current.container
    if (!s.onboarded) { Onboarding(); return }

    val halls by remember { c.dao.halls() }.collectAsState(null)
    val cabinets by remember { c.dao.cabinets() }.collectAsState(null)
    val items by remember { c.dao.items() }.collectAsState(null)
    val series by remember { c.dao.series() }.collectAsState(emptyList())
    val notes by remember { c.dao.notes() }.collectAsState(emptyList())
    val shows by remember { c.dao.shows() }.collectAsState(emptyList())
    val seals by remember { c.dao.seals() }.collectAsState(emptyList())
    val ceremony by c.ceremony.collectAsState()
    val nav = remember { Nav(tab, overlay) }

    val h = Home(s, halls ?: return, cabinets ?: return, items ?: return, series, notes, shows, seals)

    Box(Modifier.fillMaxSize().room()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (nav.tab) {
                    Tab.MUSEUM -> MuseumScreen(h, nav)
                    Tab.HALLS -> HallsScreen(h, nav)
                    Tab.CATALOG -> CatalogScreen(h, nav)
                    Tab.BOOK -> BookScreen(h, nav)
                }
                // Lists scroll under the status bar: keep it readable. The museum fades its own in over the facade.
                if (nav.tab != Tab.MUSEUM) StatusScrim(1f)
            }
            TabBar(nav)
        }
        if (nav.stack.isNotEmpty()) BackHandler { nav.close() }
        for (o in nav.stack) Box(Modifier.fillMaxSize().room()) {
            when (o) {
                is Overlay.Hall -> HallScreen(o.id, h, nav)
                is Overlay.Cabinet -> CabinetScreen(o.id, h, nav)
                is Overlay.Item -> ItemScreen(o.id, h, nav)
                is Overlay.Edit -> EditScreen(o.id, o.fresh, h, nav)
                is Overlay.HallEdit -> HallEditScreen(o.id, h, nav)
                is Overlay.Inventory -> InventoryScreen(o.hall, h, nav)
                is Overlay.Check -> CheckScreen(o.autostart, h, nav, o.file)
                Overlay.Show -> ShowScreen(h, nav)
                Overlay.Settings -> SettingsPage(h, nav)
            }
        }
        ceremony?.let { st -> CeremonyView(st, h) { c.ceremony.value = null } }
    }
}

@Composable
private fun TabBar(nav: Nav) {
    Column(Modifier.fillMaxWidth().background(C.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            @Composable
            fun item(t: Tab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int) {
                val on = nav.tab == t && nav.stack.isEmpty()
                val col = if (on) C.ink else C.muted
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { nav.tab = t; nav.stack.clear() }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) C.surface2 else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp)) {
                        Icon(icon, null, tint = col, modifier = Modifier.size(24.dp))
                    }
                    Text(stringResource(label), style = T.Label.copy(color = col, fontSize = 12.sp), modifier = Modifier.padding(top = 2.dp))
                }
            }
            item(Tab.MUSEUM, Icons.Outlined.AccountBalance, R.string.tab_museum)
            item(Tab.HALLS, Icons.Outlined.TableRows, R.string.tab_halls)
            // «Do I have this?» sits in the middle: the one button a collector needs at a flea market.
            Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { nav.open(Overlay.Check()) },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.offset(y = (-4).dp).shadow(6.dp, CircleShape).size(50.dp).clip(CircleShape).background(C.accent).border(2.dp, C.surface, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ImageSearch, null, tint = C.onAccent, modifier = Modifier.size(26.dp))
                }
                Text(stringResource(R.string.tab_check), style = T.Label.copy(color = C.ink, fontSize = 12.sp), modifier = Modifier.offset(y = (-3).dp))
            }
            item(Tab.CATALOG, Icons.Outlined.ManageSearch, R.string.tab_catalog)
            item(Tab.BOOK, Icons.Outlined.MenuBook, R.string.tab_book)
        }
    }
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** A strip behind the status bar in the room's colour; [alpha] 0 leaves what is under it visible. */
@Composable
fun StatusScrim(alpha: Float) {
    if (alpha <= 0f) return
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars).background(C.bg.copy(alpha = alpha.coerceIn(0f, 1f))))
}
