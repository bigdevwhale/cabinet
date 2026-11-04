package app.cabinet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Settings
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T

/** The screens that explain themselves on the first visit: an icon on a soft colour, and the text (title, then points). */
enum class TipOf(val icon: ImageVector, val tint: Int, val text: Int) {
    MUSEUM(Icons.Outlined.Home, 0, R.array.tip_museum),
    HALLS(Icons.Outlined.GridView, 1, R.array.tip_halls),
    HALL(Icons.Outlined.Collections, 2, R.array.tip_hall),
    CABINET(Icons.Outlined.Collections, 2, R.array.tip_cabinet),
    ITEM(Icons.Outlined.Info, 3, R.array.tip_item),
    EDIT(Icons.Outlined.EditNote, 4, R.array.tip_edit),
    CHECK(Icons.Outlined.PhotoCamera, 5, R.array.tip_check),
    CATALOG(Icons.Outlined.Search, 6, R.array.tip_catalog),
    BOOK(Icons.Outlined.AutoStories, 7, R.array.tip_book),
    SHOW(Icons.Outlined.Celebration, 0, R.array.tip_show),
    INVENTORY(Icons.Outlined.FactCheck, 1, R.array.tip_inventory),
    HALL_NEW(Icons.Outlined.AddBusiness, 2, R.array.tip_hall_new),
}

/**
 * The first time a screen opens, a card rises from the bottom: a painting, what this place is for, and two to
 * four things to do here. Shown once per screen; «Don't show tips» turns them all off (Settings brings them back).
 */
@Composable
fun Tip(tip: TipOf, s: Settings) {
    val c = LocalContext.current.container
    var gone by remember { mutableStateOf(false) }
    if (gone || s.tipsOff || tip.name in s.tips) return
    val lines = stringArrayResource(tip.text)
    fun close(all: Boolean) {
        gone = true
        c.write { settings.update { it.copy(tips = it.tips + tip.name, tipsOff = it.tipsOff || all) } }
    }
    Scrim({ close(false) }, bottom = true) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.surface)) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(C.hall(tip.tint)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(C.surface), contentAlignment = Alignment.Center) {
                    Icon(tip.icon, null, tint = C.ink, modifier = Modifier.size(30.dp))
                }
            }
            Column(Modifier.navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp)) {
                Text(lines[0], style = T.Title)
                lines.drop(1).forEachIndexed { k, line ->
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.surface2), contentAlignment = Alignment.Center) {
                            Text((k + 1).toString(), style = T.Strong.copy(fontSize = 13.sp))
                        }
                        Text(line, style = T.Body, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    }
                }
                Btn(stringResource(R.string.tip_ok), { close(false) }, Modifier.padding(top = 20.dp).fillMaxWidth(), Kind.PRIMARY)
                Text(stringResource(R.string.tip_off), style = T.Label, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 12.dp))
            }
        }
    }
}

