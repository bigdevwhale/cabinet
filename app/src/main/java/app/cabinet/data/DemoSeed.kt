package app.cabinet.data

import android.graphics.Bitmap
import app.cabinet.AppContainer
import app.cabinet.art.Specimens
import app.cabinet.art.Specimens.Can
import app.cabinet.art.Specimens.Croc
import app.cabinet.art.Specimens.Crocodile
import app.cabinet.art.Specimens.Pattern
import app.cabinet.domain.Museum
import app.cabinet.domain.Presets
import java.time.LocalDate

/** The demo museum: a hall of 0.33 cans and a hall of crocodiles, some described, some still under covers. */
object DemoSeed {
    private class Thing(
        val cab: Int, val face: () -> Bitmap, val ru: String, val en: String,
        val placeRu: String = "", val placeEn: String = "", val storyRu: String = "", val storyEn: String = "",
        val fields: List<String> = emptyList(), val daysAgo: Int = 100, val wish: Boolean = false, val copies: Int = 1,
        val condition: Int = 0, val storage: String = "", val series: Boolean = false,
    )

    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.hallsNow().isNotEmpty()) return
        val ru = c.ru
        val today = Museum.today()
        val cans = c.addHall(Presets.ALL.first { it.key == "cans" }, if (ru) "Банки 0,33" else "0.33 cans")
        val crocs = c.addHall(Presets.ALL.first { it.key == "figures" }, if (ru) "Крокодилы" else "Crocodiles")
        val cabs = c.dao.cabinetsNow()
        val canCabs = cabs.filter { it.hall == cans }
        val crocCabs = cabs.filter { it.hall == crocs }
        val kumo = c.dao.add(Series(hall = cans, name = if (ru) "Kumo, летняя серия" else "Kumo summer series", size = 6))

        val GREEN = 0xFF2F6B3A.toInt(); val CREAM = 0xFFEDE3C4.toInt()
        val canThings = listOf(
            Thing(0, { Specimens.can(Can(0xFF1F3A5F.toInt(), 0xFFF2C14E.toInt(), 0xFFFFFFFF.toInt(), "NORDLYS", Pattern.WAVES, seed = 1)) }, "Nordlys Pils", "Nordlys Pils",
                "Берген, вокзальный ларёк", "Bergen, a station kiosk", "Купил за пять минут до поезда, пил уже в вагоне. Банку вёз в рукаве куртки.", "Bought five minutes before the train and drank on board. Carried the can home in a jacket sleeve.",
                listOf("Бренд=Nordlys", "Страна=Норвегия", "Объём=0,33 л", "Год=2024"), 520, condition = 4, storage = "Коробка 1"),
            Thing(0, { Specimens.can(Can(0xFFB8322A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), "BODEGA", Pattern.STRIPES, seed = 2)) }, "Bodega Lager", "Bodega Lager",
                "Лиссабон", "Lisbon", "", "", listOf("Бренд=Bodega", "Страна=Португалия", "Объём=0,33 л"), 410, copies = 2, condition = 3),
            Thing(0, { Specimens.can(Can(0xFF111111.toInt(), 0xFFC9A646.toInt(), 0xFFC9A646.toInt(), "OBSIDIAN", Pattern.DIAGONAL, seed = 3)) }, "Obsidian Stout", "Obsidian Stout",
                "Подарок Лёши", "A gift from Alex", "Лёша привёз из Дублина и сказал: «Только не пей, она для полки». Я и не стал.", "Alex brought it from Dublin and said: «Don't drink it, it's for the shelf.» I didn't.",
                listOf("Бренд=Obsidian", "Страна=Ирландия", "Объём=0,33 л"), 300, condition = 4),
            Thing(0, { Specimens.can(Can(0xFFE6B422.toInt(), 0xFF7A4B12.toInt(), 0xFF3B220A.toInt(), "SÓL", Pattern.SUN, seed = 4)) }, "Sól Blond", "Sól Blond", daysAgo = 40),
            Thing(1, { Specimens.can(Can(0xFFF08A24.toInt(), 0xFFFFE08A.toInt(), 0xFFFFFFFF.toInt(), "VENTO", Pattern.DOTS, slim = true, seed = 5)) }, "Vento Arancia", "Vento Arancia",
                "Сицилия, заправка у Катании", "Sicily, a petrol station near Catania", "Жара под сорок, и эта оранжевая банка была самой холодной вещью на острове.", "Nearly forty degrees, and this orange can was the coldest thing on the island.",
                listOf("Бренд=Vento", "Страна=Италия", "Объём=0,33 л"), 700, condition = 3),
            Thing(1, { Specimens.can(Can(0xFF7FC8A9.toInt(), 0xFFFFFFFF.toInt(), 0xFF1E4D3B.toInt(), "LAGOM", Pattern.CHECK, slim = true, seed = 6)) }, "Lagom Fläder", "Lagom Fläder",
                "Стокгольм", "Stockholm", "", "", listOf("Бренд=Lagom", "Страна=Швеция", "Объём=0,33 л"), 260, condition = 4),
            Thing(1, { Specimens.can(Can(0xFFE84A7F.toInt(), 0xFFFFD1E0.toInt(), 0xFFFFFFFF.toInt(), "MIRABEL", Pattern.WAVES, slim = true, seed = 7)) }, "", "", daysAgo = 3),
            Thing(1, { Specimens.can(Can(0xFF2A9D8F.toInt(), 0xFFE9C46A.toInt(), 0xFFFFFFFF.toInt(), "ONDINA", Pattern.STRIPES, seed = 8)) }, "Ondina Limone", "Ondina Limone",
                "Генуя", "Genoa", "", "", listOf("Бренд=Ondina", "Страна=Италия"), 1100, condition = 2),
            Thing(2, { Specimens.can(Can(0xFF5B3A29.toInt(), 0xFFE8D5B5.toInt(), 0xFFE8D5B5.toInt(), "KAFFA", Pattern.PLAIN, slim = true, seed = 9)) }, "Kaffa Cold Brew", "Kaffa Cold Brew",
                "Аэропорт Хельсинки", "Helsinki airport", "Пересадка девять часов. Кофе в банке оказался лучше, чем в кофейне напротив.", "A nine-hour layover. The canned coffee beat the café across the hall.",
                listOf("Бренд=Kaffa", "Страна=Финляндия"), 150, condition = 4),
            Thing(2, { Specimens.can(Can(0xFF8DB596.toInt(), 0xFFF4F1DE.toInt(), 0xFF22313F.toInt(), "HANAMI", Pattern.DOTS, slim = true, seed = 10)) }, "Hanami Matcha", "Hanami Matcha",
                "Подарок от сестры", "A gift from my sister", "", "", listOf("Бренд=Hanami", "Страна=Япония"), 60, condition = 4),
            Thing(3, { Specimens.can(Can(0xFF6A4C93.toInt(), 0xFFFFCA3A.toInt(), 0xFFFFFFFF.toInt(), "KUMO", Pattern.SUN, seed = 11)) }, "Kumo № 1, «Закат»", "Kumo No. 1, «Sunset»",
                "Токио, автомат у храма", "Tokyo, a vending machine by a shrine", "Первая из серии. С неё и началось: увидел, что у банки есть номер, и понял, что будут ещё пять.", "The first of the series. It all started here: I saw the can had a number and knew five more would follow.",
                listOf("Бренд=Kumo", "Страна=Япония", "Серия=Летняя"), 800, condition = 4, series = true),
            Thing(3, { Specimens.can(Can(0xFF1982C4.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), "KUMO", Pattern.WAVES, seed = 12)) }, "Kumo № 2, «Волна»", "Kumo No. 2, «Wave»",
                "Токио", "Tokyo", "", "", listOf("Бренд=Kumo", "Серия=Летняя"), 790, condition = 4, series = true),
            Thing(3, { Specimens.can(Can(0xFF8AC926.toInt(), 0xFF1B4332.toInt(), 0xFF1B4332.toInt(), "KUMO", Pattern.CHECK, seed = 13)) }, "Kumo № 4, «Сад»", "Kumo No. 4, «Garden»",
                "Обмен с Мариной из Казани", "A swap with Marina from Kazan", "Отдал за неё дубль Bodega и магнит из Риги. Честный обмен.", "Swapped a spare Bodega and a Riga magnet for it. A fair trade.",
                listOf("Бренд=Kumo", "Серия=Летняя"), 200, condition = 3, series = true),
            Thing(3, { Specimens.can(Can(0xFFFF595E.toInt(), 0xFFFFCA3A.toInt(), 0xFFFFFFFF.toInt(), "KUMO", Pattern.DIAGONAL, seed = 14)) }, "Kumo № 5, «Фонарики»", "Kumo No. 5, «Lanterns»",
                "", "", "", "", listOf("Бренд=Kumo", "Серия=Летняя"), 20, series = true),
            Thing(3, { Specimens.can(Can(0xFF0B3954.toInt(), 0xFFBFD7EA.toInt(), 0xFFFFFFFF.toInt(), "KUMO", Pattern.DOTS, seed = 15)) }, "Kumo № 6, «Звёзды»", "Kumo No. 6, «Stars»",
                wish = true, series = true),
            Thing(0, { Specimens.can(Can(0xFF3D405B.toInt(), 0xFFE07A5F.toInt(), 0xFFF4F1DE.toInt(), "TUNDRA", Pattern.STRIPES, seed = 16)) }, "Tundra IPA", "Tundra IPA", wish = true),
        )
        val crocThings = listOf(
            Thing(0, { Specimens.crocodile(Crocodile(Croc.SITTING, 0xFF4C9A52.toInt(), CREAM, 1, 0xFFE04F5F.toInt())) }, "Крокодил с бантом", "Crocodile with a bow",
                "Блошиный рынок на Удельной", "A flea market in St Petersburg", "Продавец хотел за него двести рублей, отдал за сто пятьдесят, потому что «у него улыбка грустная». Улыбка обычная.", "The seller wanted two hundred roubles, then let him go for one fifty because «his smile is sad». It isn't.",
                listOf("Материал=Керамика, глазурь", "Размер=9 см"), 1300, condition = 3, storage = "Полка в гостиной"),
            Thing(0, { Specimens.crocodile(Crocodile(Croc.CLAY, 0xFF2E7D6B.toInt(), 0xFFD8C690.toInt(), 2)) }, "Изразцовый крокодил", "Tile crocodile",
                "Гжель, мастерская", "A Gzhel workshop", "", "", listOf("Материал=Керамика"), 900, condition = 4),
            Thing(0, { Specimens.crocodile(Crocodile(Croc.CLAY, 0xFF6C9A2E.toInt(), 0xFFF0E6A8.toInt(), 3)) }, "", "", daysAgo = 5),
            Thing(1, { Specimens.crocodile(Crocodile(Croc.PLUSH, 0xFF5DAA68.toInt(), 0xFFF3E9C6.toInt(), 4, 0xFFE63946.toInt())) }, "Гена", "Gena",
                "Из детства", "From childhood", "Самый первый. Спал со мной до восьми лет, потом переехал на шкаф и следил оттуда.", "The very first one. Slept with me until I was eight, then moved to the wardrobe and kept watch from there.",
                listOf("Материал=Плюш", "Размер=40 см"), 9000, condition = 2, storage = "Спальня, шкаф"),
            Thing(1, { Specimens.crocodile(Crocodile(Croc.PLUSH, 0xFF8E7DBE.toInt(), 0xFFF1E3F3.toInt(), 5, 0xFFFFD166.toInt())) }, "Сиреневый", "The lilac one",
                "Автомат с игрушками в ТЦ", "A claw machine at a mall", "", "", listOf("Материал=Плюш"), 450, copies = 2),
            Thing(1, { Specimens.crocodile(Crocodile(Croc.PLUSH, 0xFF3FA7D6.toInt(), 0xFFFFF3B0.toInt(), 6, 0xFFEE6C4D.toInt())) }, "", "", daysAgo = 2),
            Thing(2, { Specimens.crocodile(Crocodile(Croc.KEYRING, 0xFFB5892E.toInt(), 0xFFE2C46B.toInt(), 7)) }, "Латунный брелок", "Brass keyring",
                "Стамбул, Гранд-базар", "Istanbul, the Grand Bazaar", "Торговался полчаса. Он сдался, когда я сказал, что коллекционирую крокодилов: «Тогда бери, для дела».", "Haggled for half an hour. He gave in when I said I collect crocodiles: «Then take it, it's for a cause.»",
                listOf("Материал=Латунь", "Размер=5 см"), 600, condition = 3),
            Thing(2, { Specimens.crocodile(Crocodile(Croc.KEYRING, 0xFF2A9D3F.toInt(), 0xFFB7E4A0.toInt(), 8)) }, "Резиновый брелок", "Rubber keyring",
                "", "", "", "", listOf("Материал=Резина"), 30),
            Thing(3, { Specimens.crocodile(Crocodile(Croc.WOOD, 0xFFB07A43.toInt(), 0xFFD9B27C.toInt(), 9)) }, "Деревянный на шарнирах", "Jointed wooden croc",
                "Бали, Убуд", "Ubud, Bali", "Если взять за хвост, он извивается как живой. Гости всегда так и делают.", "Hold it by the tail and it wriggles like a live one. Guests always do.",
                listOf("Материал=Дерево", "Размер=30 см"), 1500, condition = 4),
            Thing(3, { Specimens.crocodile(Crocodile(Croc.GLASS, 0xFF4FB3BF.toInt(), 0xFFBDE7EC.toInt(), 10)) }, "Стеклянный из Мурано", "Murano glass croc",
                "Венеция", "Venice", "", "", listOf("Материал=Стекло"), 1000, condition = 4),
            Thing(3, { Specimens.crocodile(Crocodile(Croc.BRASS, 0xFF9C6B30.toInt(), 0xFFC8A15A.toInt(), 11)) }, "Бронзовый пресс-папье", "Bronze paperweight", wish = true),
        )

        val all = mutableListOf<Item>()
        suspend fun put(hall: Long, cabsOf: List<Cabinet>, t: Thing) {
            val face = t.face()
            val cab = cabsOf[t.cab.coerceAtMost(cabsOf.lastIndex)].id
            val id = c.dao.add(Item(
                hall = hall, cabinet = cab, inv = c.dao.lastInv() + 1, title = if (ru) t.ru else t.en,
                status = if (t.wish) Status.WISH else Status.OWNED, copies = t.copies,
                acquired = if (t.wish) null else today - t.daysAgo, place = if (ru) t.placeRu else t.placeEn,
                story = if (ru) t.storyRu else t.storyEn, storage = if (ru) t.storage else "", condition = t.condition,
                fields = if (ru) t.fields.joinToString("\n") else "", series = if (t.series) kumo else 0,
                sort = c.dao.lastSort(cab) + 1, photos = 1, cut = true, hue = Photos.hue(face), aspect = face.width.toFloat() / face.height,
                created = System.currentTimeMillis() - t.daysAgo * 86_400_000L,
            ))
            c.photos.keep(id, 0, Photos.onGround(face, 900))
            c.photos.writeFace(id, face)
            all += c.dao.item(id)!!
        }
        for (t in canThings) put(cans, canCabs, t)
        for (t in crocThings) put(crocs, crocCabs, t)

        // One crocodile came on this very day a few years ago.
        all.firstOrNull { it.title.startsWith("Гена") || it.title.startsWith("Gena") }?.let {
            c.dao.save(it.copy(acquired = LocalDate.now().minusYears(24).toEpochDay()))
        }
        // Pearls of the halls.
        c.dao.hall(cans)?.let { h -> all.firstOrNull { it.title.contains("Закат") || it.title.contains("Sunset") }?.let { c.dao.save(h.copy(pearl = it.id)) } }
        c.dao.hall(crocs)?.let { h -> all.firstOrNull { it.title.startsWith("Гена") || it.title.startsWith("Gena") }?.let { c.dao.save(h.copy(pearl = it.id)) } }

        // The visitors' book of the last days.
        for (d in 1..4) c.visitorsWrite(today - 5 + d)
        // Last season's inventory of the cans.
        c.dao.add(Seal(cans, Museum.season(today - 95), today - 95, 12, 0))
        c.settings.update { it.copy(onboarded = true, stage = Museum.stage(all.count { i -> i.owned })) }
    }
}
