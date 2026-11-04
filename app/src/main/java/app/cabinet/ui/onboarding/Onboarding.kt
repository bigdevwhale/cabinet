package app.cabinet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.container
import app.cabinet.domain.Presets
import app.cabinet.ru
import app.cabinet.ui.art.Facade
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.room
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch

/** Two pages: what this is, then the museum's name and its first hall. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var museum by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf(Presets.ALL.first()) }
    var hall by remember { mutableStateOf(preset.names(ru).hall) }

    Column(Modifier.fillMaxSize().room().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 24.dp)) {
            if (page == 0) {
                Facade(0, 0, Modifier.padding(top = 24.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)))
                Text(stringResource(R.string.app_name), style = T.Hero, modifier = Modifier.padding(top = 18.dp))
                Text(stringResource(R.string.ob_lead), style = T.Italic, modifier = Modifier.padding(top = 8.dp))
                for ((t, b) in listOf(R.string.ob_1t to R.string.ob_1b, R.string.ob_2t to R.string.ob_2b, R.string.ob_3t to R.string.ob_3b, R.string.ob_4t to R.string.ob_4b)) {
                    Row(Modifier.padding(top = 16.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(C.accent))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(stringResource(t), style = T.Strong)
                            Text(stringResource(b), style = T.Small, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.ob_name_t), style = T.Title, modifier = Modifier.padding(top = 32.dp))
                Text(stringResource(R.string.ob_name_b), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                Field(museum, { museum = it }, stringResource(R.string.name), Modifier.padding(top = 12.dp), hint = stringResource(R.string.ob_name_hint))

                Text(stringResource(R.string.ob_hall_t), style = T.Title, modifier = Modifier.padding(top = 32.dp))
                Text(stringResource(R.string.ob_hall_b), style = T.Small, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (p in Presets.ALL) Chip(p.names(ru).hall, p == preset) {
                        if (hall == preset.names(ru).hall || hall.isBlank()) hall = p.names(ru).hall
                        preset = p
                    }
                }
                Field(hall, { hall = it }, stringResource(R.string.hall_name), Modifier.padding(top = 14.dp))
                Text(stringResource(R.string.hall_kind_hint, preset.names(ru).cabinets.joinToString(", ")), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.ob_more_halls), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            if (page == 0) Btn(stringResource(R.string.ob_go), { page = 1 }, Modifier.fillMaxWidth(), Kind.ACCENT)
            else Btn(stringResource(R.string.ob_open), {
                scope.launch {
                    if (c.dao.hallsNow().isEmpty()) c.addHall(preset, hall.trim())
                    c.settings.update { it.copy(onboarded = true, name = museum.trim()) }
                }
            }, Modifier.fillMaxWidth(), Kind.ACCENT, enabled = hall.isNotBlank())
        }
    }
}
