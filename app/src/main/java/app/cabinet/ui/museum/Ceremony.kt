package app.cabinet.ui.museum

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cabinet.R
import app.cabinet.domain.Museum
import app.cabinet.ui.Home
import app.cabinet.ui.art.Facade
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.theme.T

/** The house has grown: the new facade rises, with a line about what was built. */
@Composable
fun CeremonyView(stage: Int, h: Home, onDone: () -> Unit) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(stage) { rise.animateTo(1f, tween(1200)) }
    Scrim(onDone) {
        Card(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.cer_caps).uppercase(), style = T.Caps)
                Facade(stage, 0, Modifier.fillMaxWidth().height(180.dp).padding(top = 10.dp).graphicsLayer { alpha = rise.value; translationY = (1 - rise.value) * 40f })
                Text(stringArrayResource(R.array.stages)[stage], style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text(stringResource(R.string.cer_body, Museum.STAGES[stage]), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Btn(stringResource(R.string.cer_ok), onDone, Modifier.padding(top = 16.dp).fillMaxWidth(), Kind.ACCENT)
            }
        }
    }
}
