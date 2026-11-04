package app.cabinet

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import app.cabinet.data.Cabinet
import app.cabinet.data.DemoSeed
import app.cabinet.data.Hall
import app.cabinet.data.Item
import app.cabinet.data.Lens
import app.cabinet.data.MuseumDb
import app.cabinet.data.Note
import app.cabinet.data.Photos
import app.cabinet.data.SettingsStore
import app.cabinet.data.Status
import app.cabinet.domain.Museum
import app.cabinet.domain.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = MuseumDb.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val photos = Photos(app)
    val lens = Lens(app)
    /** On the main thread: Room and DataStore are main-safe; heavy work switches dispatchers inside. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** True while the demo build fills its museum, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)
    /** Things whose photos are being processed right now (cut out, looked at). */
    val busy = MutableStateFlow<Set<Long>>(emptySet())
    /** A new stage of the house was just built: show the ceremony. */
    val ceremony = MutableStateFlow<Int?>(null)

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    val ru: Boolean get() = app.localized().resources.configuration.locales[0].language == "ru"

    suspend fun addHall(p: Preset, name: String? = null): Long {
        val n = p.names(ru)
        val title = name?.takeIf { it.isNotBlank() } ?: n.hall
        val sort = dao.hallsNow().size
        val id = dao.add(Hall(name = title, code = Museum.code(title), fields = n.fields.joinToString("\n"), sort = sort))
        n.cabinets.forEachIndexed { i, c -> dao.add(Cabinet(hall = id, name = c, sort = i)) }
        return id
    }

    /** A new thing from a photo: kept at once with its picture, cut out and looked at in the background. */
    suspend fun addThing(hall: Long, cabinet: Long, photo: Bitmap?, status: Status = Status.OWNED, title: String = ""): Long {
        val inv = dao.lastInv() + 1
        val sort = dao.lastSort(cabinet) + 1
        val id = dao.add(Item(hall = hall, cabinet = cabinet, inv = inv, sort = sort, status = status, title = title,
            acquired = if (status == Status.OWNED) Museum.today() else null, photos = if (photo != null) 1 else 0))
        if (photo != null) {
            photos.keep(id, 0, photo)
            process(id, photo)
        }
        celebrate()
        return id
    }

    suspend fun addThing(hall: Long, cabinet: Long, uri: Uri, status: Status = Status.OWNED): Long? {
        val bmp = photos.read(uri) ?: return null
        return addThing(hall, cabinet, bmp, status)
    }

    /** Cuts the face out and records its colour and look. */
    fun process(id: Long, photo: Bitmap, cut: Boolean? = null) {
        busy.value = busy.value + id
        scope.launch {
            try {
                val wantCut = cut ?: settings.now().cut
                val face = photos.makeFace(id, photo, wantCut)
                val look = lens.look(face.picture)
                dao.item(id)?.let { dao.save(it.copy(cut = face.cut, hue = face.hue, aspect = face.aspect, look = Lens.pack(look), updated = System.currentTimeMillis())) }
            } finally { busy.value = busy.value - id }
        }
    }

    /** Redoes the face from photo 0, e.g. after the first photo changed or to cut it out again. */
    suspend fun reface(id: Long, cut: Boolean) {
        val bmp = photos.loadOriginal(id, 0) ?: return
        process(id, bmp, cut)
    }

    suspend fun addPhoto(item: Item, uri: Uri) {
        val bmp = photos.read(uri) ?: return
        photos.keep(item.id, item.photos, bmp)
        dao.save(item.copy(photos = item.photos + 1, updated = System.currentTimeMillis()))
        if (item.photos == 0) process(item.id, bmp)
    }

    suspend fun delete(item: Item) {
        dao.drop(item)
        photos.deleteAll(item.id)
        dao.hallsNow().firstOrNull { it.pearl == item.id }?.let { dao.save(it.copy(pearl = 0)) }
    }

    /** Things whose look is missing or of the other kind (the model came or went) get looked at again. */
    fun relook() = scope.launch {
        lens.look(Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)) // loads the model, sets lens.smart
        val dim = if (lens.smart) Lens.DIM else Lens.PLAIN
        for (i in dao.itemsNow()) {
            if (i.photos == 0 || i.look?.size == dim * 4) continue
            val face = photos.load(i.id, true, i.updated) ?: continue
            val look = lens.look(face)
            dao.item(i.id)?.let { dao.save(it.copy(look = Lens.pack(look))) }
        }
    }

    /** A new stage of the house when the collection reached it. */
    suspend fun celebrate() {
        val owned = dao.itemsNow().count { it.owned }
        val stage = Museum.stage(owned)
        val s = settings.now()
        if (stage > s.stage) {
            settings.update { it.copy(stage = stage) }
            if (s.onboarded) ceremony.value = stage
        }
    }

    /** Today's lines in the visitors' book, written once a day. */
    suspend fun visitorsWrite(day: Long = Museum.today()) {
        if (dao.lastNoteDay() >= day) return
        val items = dao.itemsNow()
        val fav = Museum.favourites(items, day, 2)
        if (fav.isEmpty()) return
        val res = app.localized().resources
        val r = Random(day)
        val halls = dao.hallsNow().associateBy { it.id }
        for (i in fav.take(if (Museum.visitors(items, day) > 6) 2 else 1)) {
            val pool = when {
                i.story.isNotBlank() && r.nextBoolean() -> R.array.visit_story
                i.place.isNotBlank() && r.nextBoolean() -> R.array.visit_place
                else -> R.array.visit_any
            }
            val lines = res.getStringArray(pool)
            val title = i.title.ifBlank { Museum.inv(halls[i.hall]?.code ?: "", i.inv) }
            val text = lines[r.nextInt(lines.size)].format(title, halls[i.hall]?.name ?: "", i.place)
            dao.add(Note(day = day, item = i.id, text = text))
        }
    }
}

class CabinetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.LANG.isNotEmpty()) AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.LANG))
        container = AppContainer(this)
        shortcut()
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.seeding.value = false
            container.visitorsWrite()
            container.relook()
        }
    }
}

/** A long press on the icon offers «Do I have it?» straight away. */
private fun Context.shortcut() = runCatching {
    val intent = android.content.Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_CHECK)
    val label = localized().getString(R.string.shortcut_check)
    androidx.core.content.pm.ShortcutManagerCompat.setDynamicShortcuts(this, listOf(
        androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "check").setShortLabel(label).setLongLabel(label)
            .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher)).setIntent(intent).build()))
}

val Context.container: AppContainer get() = (applicationContext as CabinetApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

/** A context in the app's own language, for text made outside an activity. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
