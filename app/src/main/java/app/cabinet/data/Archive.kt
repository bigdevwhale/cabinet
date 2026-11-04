package app.cabinet.data

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import app.cabinet.AppContainer
import app.cabinet.domain.Museum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** The whole museum in one file (cards and photos), a CSV for spreadsheets and a printable PDF catalogue. */
object Archive {
    private const val VERSION = 1

    suspend fun export(c: AppContainer, out: OutputStream) = withContext(Dispatchers.IO) {
        val d = c.dao
        val json = JSONObject()
            .put("version", VERSION)
            .put("halls", JSONArray(d.hallsNow().map { JSONObject().put("id", it.id).put("name", it.name).put("code", it.code).put("fields", it.fields).put("sort", it.sort).put("pearl", it.pearl).put("created", it.created) }))
            .put("cabinets", JSONArray(d.cabinetsNow().map { JSONObject().put("id", it.id).put("hall", it.hall).put("name", it.name).put("sort", it.sort).put("order", it.order) }))
            .put("items", JSONArray(d.itemsNow().map { i ->
                JSONObject().put("id", i.id).put("hall", i.hall).put("cabinet", i.cabinet).put("inv", i.inv).put("title", i.title).put("status", i.status.name)
                    .put("copies", i.copies).put("acquired", i.acquired ?: JSONObject.NULL).put("place", i.place).put("price", i.price).put("condition", i.condition)
                    .put("story", i.story).put("storage", i.storage).put("fields", i.fields).put("tags", i.tags).put("series", i.series).put("sort", i.sort)
                    .put("photos", i.photos).put("cut", i.cut).put("hue", i.hue).put("aspect", i.aspect.toDouble()).put("checked", i.checked).put("created", i.created).put("updated", i.updated)
            }))
            .put("series", JSONArray(d.seriesNow().map { JSONObject().put("id", it.id).put("hall", it.hall).put("name", it.name).put("size", it.size) }))
            .put("notes", JSONArray(d.notesNow().map { JSONObject().put("day", it.day).put("item", it.item).put("text", it.text) }))
            .put("shows", JSONArray(d.showsNow().map { JSONObject().put("week", it.week).put("theme", it.theme).put("title", it.title).put("items", it.items).put("opened", it.opened) }))
            .put("seals", JSONArray(d.sealsNow().map { JSONObject().put("hall", it.hall).put("season", it.season).put("day", it.day).put("found", it.found).put("missing", it.missing) }))
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("museum.json")); z.write(json.toString(1).toByteArray()); z.closeEntry()
            c.photos.dir.listFiles()?.forEach { f ->
                z.putNextEntry(ZipEntry("photos/" + f.name)); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
    }

    /** Replaces the museum with the one in the file. False if the file is not a museum. */
    suspend fun import(c: AppContainer, input: InputStream): Boolean = withContext(Dispatchers.IO) {
        val tmp = java.io.File(c.app.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var json: JSONObject? = null
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                when {
                    e.name == "museum.json" -> json = JSONObject(z.readBytes().decodeToString())
                    e.name.startsWith("photos/") && !e.name.contains("..") -> java.io.File(tmp, e.name.removePrefix("photos/")).outputStream().use { z.copyTo(it) }
                }
            }
        }
        val j = json ?: return@withContext false
        val d = c.dao
        d.wipe()
        c.photos.dir.listFiles()?.forEach { it.delete() }
        tmp.listFiles()?.forEach { it.renameTo(java.io.File(c.photos.dir, it.name)) }
        fun arr(k: String) = j.optJSONArray(k) ?: JSONArray()
        fun objs(k: String) = arr(k).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        for (it in objs("halls")) { d.addRaw(Hall(it.getLong("id"), it.getString("name"), it.optString("code"), it.optString("fields"), it.optInt("sort"), it.optLong("pearl"), it.optLong("created"))) }
        for (it in objs("cabinets")) { d.addRaw(Cabinet(it.getLong("id"), it.getLong("hall"), it.getString("name"), it.optInt("sort"), it.optInt("order"))) }
        for (it in objs("items")) {
            d.addRaw(Item(
                id = it.getLong("id"), hall = it.getLong("hall"), cabinet = it.getLong("cabinet"), inv = it.getInt("inv"), title = it.optString("title"),
                status = runCatching { Status.valueOf(it.optString("status")) }.getOrDefault(Status.OWNED), copies = it.optInt("copies", 1),
                acquired = if (it.isNull("acquired")) null else it.optLong("acquired"), place = it.optString("place"), price = it.optString("price"),
                condition = it.optInt("condition"), story = it.optString("story"), storage = it.optString("storage"), fields = it.optString("fields"),
                tags = it.optString("tags"), series = it.optLong("series"), sort = it.optInt("sort"), photos = it.optInt("photos"), cut = it.optBoolean("cut"),
                hue = it.optInt("hue", -1), aspect = it.optDouble("aspect", 1.0).toFloat(), checked = it.optLong("checked"),
                created = it.optLong("created"), updated = System.currentTimeMillis(),
            ))
        }
        for (it in objs("series")) { d.addRaw(Series(it.getLong("id"), it.getLong("hall"), it.getString("name"), it.optInt("size", 2))) }
        for (it in objs("notes")) { d.add(Note(day = it.getLong("day"), item = it.getLong("item"), text = it.getString("text"))) }
        for (it in objs("shows")) { d.add(Show(week = it.getLong("week"), theme = it.optString("theme"), title = it.optString("title"), items = it.optString("items"), opened = it.optLong("opened"))) }
        for (it in objs("seals")) { d.add(Seal(it.getLong("hall"), it.getString("season"), it.getLong("day"), it.optInt("found"), it.optInt("missing"))) }
        tmp.deleteRecursively()
        true
    }

    suspend fun csv(c: AppContainer, out: OutputStream, ru: Boolean) = withContext(Dispatchers.IO) {
        val halls = c.dao.hallsNow().associateBy { it.id }
        val cabs = c.dao.cabinetsNow().associateBy { it.id }
        val series = c.dao.seriesNow().associateBy { it.id }
        val items = c.dao.itemsNow().sortedBy { it.inv }
        val extra = halls.values.flatMap { it.fieldList }.distinct()
        val head = (if (ru) listOf("Номер", "Название", "Зал", "Шкаф", "Статус", "Копий", "Поступление", "Откуда", "Цена", "Состояние", "Хранится", "Серия", "Метки", "История")
            else listOf("Number", "Title", "Hall", "Cabinet", "Status", "Copies", "Acquired", "From", "Price", "Condition", "Kept in", "Series", "Tags", "Story")) + extra
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder().append(0xFEFF.toChar()).append(head.joinToString(",") { q(it) }).append("\r\n")
        for (i in items) {
            val h = halls[i.hall]
            val row = listOf(Museum.inv(h?.code ?: "", i.inv), i.title, h?.name ?: "", cabs[i.cabinet]?.name ?: "",
                if (i.owned) (if (ru) "в коллекции" else "owned") else (if (ru) "хочу" else "wanted"), i.copies.toString(),
                i.acquired?.let { java.time.LocalDate.ofEpochDay(it).toString() } ?: "", i.place, i.price, i.condition.takeIf { it > 0 }?.toString() ?: "",
                i.storage, series[i.series]?.name ?: "", i.tags, i.story) + extra.map { i.field(it) }
            sb.append(row.joinToString(",") { q(it) }).append("\r\n")
        }
        out.write(sb.toString().toByteArray())
    }

    /** A printed catalogue: A4 pages, a photo and the card for every thing, hall by hall. */
    suspend fun pdf(c: AppContainer, out: OutputStream, title: String, ru: Boolean) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val W = 595; val H = 842; val m = 40f
        val ink = 0xFF231C15.toInt(); val muted = 0xFF7A6E5E.toInt()
        val head = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 22f; typeface = android.graphics.Typeface.SERIF }
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 12.5f; isFakeBoldText = true }
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 9.5f }
        val line = Paint().apply { color = 0xFFDDD4C3.toInt(); strokeWidth = 0.7f }
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = m
            page!!.canvas.drawText("$title — $pageNo", m, H - 20f, text)
        }
        newPage()
        page!!.canvas.drawText(title, m, y + 20f, head); y += 44f
        val halls = c.dao.hallsNow()
        val items = c.dao.itemsNow()
        for (h in halls) {
            val mine = items.filter { it.hall == h.id && it.owned }.sortedBy { it.inv }
            if (mine.isEmpty()) continue
            if (y > H - 160) newPage()
            page!!.canvas.drawText(h.name, m, y + 18f, head.apply { textSize = 17f }); y += 32f
            for (i in mine) {
                if (y > H - 110) newPage()
                val cv = page!!.canvas
                val f = c.photos.thumb(i.id).takeIf { it.exists() } ?: c.photos.face(i.id)
                if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { b ->
                    val k = minOf(70f / b.width, 70f / b.height)
                    cv.drawBitmap(b, null, RectF(m + (70 - b.width * k) / 2, y + (70 - b.height * k) / 2, m + (70 + b.width * k) / 2, y + (70 + b.height * k) / 2), null)
                }
                val x = m + 84f
                cv.drawText(Museum.inv(h.code, i.inv) + "   " + i.title.ifBlank { "—" }, x, y + 12f, name)
                val facts = listOfNotNull(
                    i.acquired?.let { java.time.LocalDate.ofEpochDay(it).toString() }, i.place.ifBlank { null }, i.price.ifBlank { null },
                    i.storage.ifBlank { null }?.let { (if (ru) "хранится: " else "kept in: ") + it },
                    i.copies.takeIf { it > 1 }?.let { (if (ru) "копий: " else "copies: ") + it },
                ) + h.fieldList.mapNotNull { k -> i.field(k).ifBlank { null }?.let { "$k: $it" } }
                var ty = y + 27f
                for (l in wrap(facts.joinToString(" · "), text, W - x - m).take(2)) { cv.drawText(l, x, ty, text); ty += 12f }
                for (l in wrap(i.story, text, W - x - m).take(3)) { cv.drawText(l, x, ty, text); ty += 12f }
                y += 80f
                cv.drawLine(m, y - 5f, W - m, y - 5f, line)
            }
            y += 10f
        }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out); doc.close()
    }

    private fun wrap(s: String, p: Paint, max: Float): List<String> {
        if (s.isBlank()) return emptyList()
        val out = mutableListOf<String>(); var cur = ""
        for (w in s.replace('\n', ' ').split(' ')) {
            val t = if (cur.isEmpty()) w else "$cur $w"
            if (p.measureText(t) > max && cur.isNotEmpty()) { out += cur; cur = w } else cur = t
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }
}
