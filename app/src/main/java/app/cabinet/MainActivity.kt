package app.cabinet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cabinet.data.Settings
import app.cabinet.ui.CabinetRoot
import app.cabinet.ui.Overlay
import app.cabinet.ui.theme.CabinetTheme

class MainActivity : AppCompatActivity() {
    /** Bumped when the «Do I have this?» shortcut opens the app, so the root starts over on the check. */
    private var checks by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.wantsCheck()) checks++
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings so the welcome doesn't flash for someone already here.
            settings?.takeIf { !seeding }?.let { s ->
                CabinetTheme(s.theme) {
                    key(checks) { CabinetRoot(s, overlay = if (checks > 0) Overlay.Check() else null) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.wantsCheck()) checks++
    }

    override fun onResume() {
        super.onResume()
        container.write { visitorsWrite() }
    }

    private fun Intent.wantsCheck() = action == ACTION_CHECK

    companion object {
        const val ACTION_CHECK = "app.cabinet.CHECK"
    }
}
