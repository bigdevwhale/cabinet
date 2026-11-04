package app.cabinet.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.cabinet.BuildConfig
import app.cabinet.R
import app.cabinet.container
import app.cabinet.data.Archive
import app.cabinet.data.Settings
import app.cabinet.ru
import app.cabinet.ui.Home
import app.cabinet.ui.Nav
import app.cabinet.ui.components.Btn
import app.cabinet.ui.components.Card
import app.cabinet.ui.components.Chip
import app.cabinet.ui.components.Field
import app.cabinet.ui.components.Kind
import app.cabinet.ui.components.Scrim
import app.cabinet.ui.components.TopBar
import app.cabinet.ui.openUrl
import app.cabinet.ui.theme.C
import app.cabinet.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "cabinet"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

@Composable
private fun Section(title: String, hint: String? = null) {
    Text(title.uppercase(), style = T.Caps, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
    if (hint != null) Text(hint, style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val s = h.s
    val scope = rememberCoroutineScope()
    fun put(f: (Settings) -> Settings) = c.write { settings.update(f) }
    var name by remember { mutableStateOf(s.name) }
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }
    val stamp = LocalDate.now().toString()
    val museumName = s.name.ifBlank { ctx.getString(R.string.app_name) }

    fun done(ok: Boolean) = Toast.makeText(ctx, if (ok) R.string.saved_file else R.string.failed_file, Toast.LENGTH_LONG).show()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u ->
        if (u != null) scope.launch { done(runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.export(c, it) } }.isSuccess) }
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) scope.launch { done(runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.csv(c, it, ru) } }.isSuccess) }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { u ->
        if (u != null) scope.launch { done(runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.pdf(c, it, museumName, ru) } }.isSuccess) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> restoring = u }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings), null, {
            if (name != s.name) put { it.copy(name = name.trim()) }
            nav.close()
        })
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Section(stringResource(R.string.set_name), stringResource(R.string.set_name_hint))
                Field(name, { name = it; put { st -> st.copy(name = it.trim()) } }, stringResource(R.string.name), hint = stringResource(R.string.app_name))

                Section(stringResource(R.string.set_cut), stringResource(R.string.set_cut_hint))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(stringResource(R.string.on), s.cut) { put { it.copy(cut = true) } }
                    Chip(stringResource(R.string.off), !s.cut) { put { it.copy(cut = false) } }
                }

                Section(stringResource(R.string.set_theme))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((m, l) in listOf(0 to R.string.theme_system, 1 to R.string.theme_light, 2 to R.string.theme_dark))
                        Chip(stringResource(l), s.theme == m) { put { it.copy(theme = m) } }
                }

                Btn(stringResource(R.string.tips_again), {
                    put { it.copy(tips = emptySet(), tipsOff = false) }
                    Toast.makeText(ctx, R.string.tips_again_done, Toast.LENGTH_SHORT).show()
                }, Modifier.padding(top = 18.dp), Kind.GHOST)

                Section(stringResource(R.string.set_lang))
                val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                        Chip(stringResource(l), cur == tag) { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
                }
            }

            Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Section(stringResource(R.string.set_files), stringResource(R.string.set_files_hint))
                for ((label, sub, go) in listOf<Triple<Int, Int, () -> Unit>>(
                    Triple(R.string.backup, R.string.backup_sub) { backup.launch("cabinet-$stamp.zip") },
                    Triple(R.string.restore, R.string.restore_sub) { restore.launch(arrayOf("application/zip", "application/octet-stream")) },
                    Triple(R.string.export_pdf, R.string.export_pdf_sub) { pdf.launch("catalogue-$stamp.pdf") },
                    Triple(R.string.export_csv, R.string.export_csv_sub) { csv.launch("cabinet-$stamp.csv") },
                )) Row(Modifier.fillMaxWidth().clickable(onClick = go).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
                    Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
                }
                Section(stringResource(R.string.privacy), stringResource(R.string.privacy_text))
            }

            Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Section(stringResource(R.string.community), stringResource(R.string.community_text))
                for ((label, sub, url) in listOf(
                    Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
                    Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
                    Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
                    Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
                )) Row(Modifier.fillMaxWidth().clickable { openUrl(ctx, url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
                    Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
                }
                Section(stringResource(R.string.about))
                Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small)
                Text(stringResource(R.string.font_credits), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                Btn(stringResource(R.string.source), { openUrl(ctx, "https://github.com/bigdevwhale/cabinet") }, Modifier.padding(top = 10.dp), Kind.GHOST)
            }
        }
    }

    restoring?.let { u ->
        Scrim({ restoring = null }) {
            Card(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_t), style = T.Heading)
                Text(stringResource(R.string.restore_b), style = T.Body, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(stringResource(R.string.cancel), { restoring = null }, Modifier.weight(1f), Kind.GHOST)
                    Btn(stringResource(R.string.restore), {
                        restoring = null
                        scope.launch {
                            val ok = runCatching { ctx.contentResolver.openInputStream(u)!!.use { Archive.import(c, it) } }.getOrDefault(false)
                            Toast.makeText(ctx, if (ok) R.string.restored else R.string.failed_file, Toast.LENGTH_LONG).show()
                            if (ok) { c.relook(); nav.stack.clear() }
                        }
                    }, Modifier.weight(1f), Kind.RED)
                }
            }
        }
    }
}
