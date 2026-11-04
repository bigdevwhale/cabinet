package app.cabinet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import app.cabinet.ui.theme.C as app_C
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.cabinet.data.DemoSeed
import app.cabinet.data.Settings
import app.cabinet.ui.CabinetRoot
import app.cabinet.ui.Overlay
import app.cabinet.ui.Tab
import app.cabinet.ui.onboarding.Onboarding
import app.cabinet.ui.theme.CabinetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The main screens over the demo museum, drawn to PNGs (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val app get() = ApplicationProvider.getApplicationContext<CabinetApp>()

    private fun seed(): Settings = runBlocking {
        val c = app.container
        // The demo build seeds itself on start; wait for it rather than seeding twice.
        c.seeding.first { !it }
        if (c.dao.hallsNow().isEmpty()) DemoSeed.seedIfEmpty(c)
        c.seeding.value = false
        c.settings.update { it.copy(tipsOff = true) }
        c.settings.now()
    }

    private fun save(name: String) {
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun settle(waitFor: String?) {
        rule.waitUntil(30_000) { rule.mainClock.advanceTimeBy(100); waitFor == null || rule.onAllNodesWithText(waitFor, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        Thread.sleep(900)
        rule.mainClock.advanceTimeBy(800)
    }

    private fun s(id: Int) = app.getString(id)

    /** One composition; each screen is a fresh root keyed by its name. */
    @Test fun screens() {
        val st = seed()
        val c = app.container
        val crocs = runBlocking { c.dao.hallsNow()[0].id }
        val cans = runBlocking { c.dao.cabinetsNow().first().id }
        val gena = runBlocking { c.dao.itemsNow().first { it.title.contains("1924") }.id }
        val list = listOf(
            Triple("1-museum", Tab.MUSEUM, null),
            Triple("2-hall", Tab.HALLS, Overlay.Hall(crocs)),
            Triple("3-cabinet", Tab.HALLS, Overlay.Cabinet(cans)),
            Triple("4-item", Tab.HALLS, Overlay.Item(gena)),
            Triple("5-check", Tab.MUSEUM, Overlay.Check(autostart = false)),
            Triple("6-catalog", Tab.CATALOG, null),
            Triple("7-book", Tab.BOOK, null),
            Triple("8-show", Tab.MUSEUM, Overlay.Show),
            Triple("9-halls", Tab.HALLS, null),
            Triple("a-hall-new", Tab.HALLS, Overlay.HallEdit(0)),
            Triple("b-edit", Tab.HALLS, Overlay.Edit(gena)),
        )
        var which by mutableStateOf(list[0])
        rule.mainClock.autoAdvance = false
        rule.setContent { CabinetTheme { key(which.first) { CabinetRoot(st, which.second, which.third) } } }
        for (sc in list) {
            rule.runOnUiThread { which = sc }
            settle(s(R.string.tab_halls))
            save(sc.first)
        }
    }

    @Test fun welcome() {
        rule.mainClock.autoAdvance = false
        rule.setContent { CabinetTheme { Onboarding() } }
        settle(null)
        save("0-welcome")
    }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-night-xxhdpi")
class RenderScreensRuDark : ScreensBase() { override val lang = "ru-dark" }
