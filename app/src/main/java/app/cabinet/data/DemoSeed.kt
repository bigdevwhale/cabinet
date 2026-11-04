package app.cabinet.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.cabinet.AppContainer
import app.cabinet.domain.Museum
import java.time.LocalDate

/**
 * The demo museum: real records, coins and books, photographed by Wikimedia Commons authors (see tools/demo/credits.md),
 * cut out of their backgrounds as the app would. Some are described, some still wait for their story,
 * a few are on the wish list. The pictures live in the demo build's assets only.
 */
object DemoSeed {
    /** One exhibit: picture, cabinet, title, place, story, the hall's fields in order, and how it is kept. */
    private class T(
        val key: String, val cab: Int, val ru: String, val en: String,
        val placeRu: String = "", val placeEn: String = "", val storyRu: String = "", val storyEn: String = "",
        val fields: List<String> = emptyList(), val days: Int = 200, val wish: Boolean = false, val copies: Int = 1,
        val cond: Int = 0, val storageRu: String = "", val storageEn: String = "", val also: String? = null, val series: Int = 0,
    )

    private class HallDef(val ru: String, val en: String, val code: String, val cabsRu: List<String>, val cabsEn: List<String>,
                          val fieldsRu: List<String>, val fieldsEn: List<String>, val things: List<T>, val pearl: String,
                          val series: List<Triple<String, String, Int>> = emptyList())

    private val RECORDS = HallDef("Пластинки", "Records", "ПЛ",
        listOf("Мелодия", "Довоенные", "Зарубежные"), listOf("Melodiya", "Pre-war", "Foreign"),
        listOf("Исполнитель", "Год", "Лейбл"), listOf("Artist", "Year", "Label"),
        listOf(
            T("rec_bears", 0, "«Песенка о медведях»", "«Song of the Bears»", "Блошиный рынок на Удельной", "A flea market in St Petersburg",
                "Песня из «Кавказской пленницы». Купил коробку пластинок за сто рублей, а нужна была только эта.",
                "The song from «Kidnapping, Caucasian Style». Bought a whole box of records for a hundred roubles just for this one.",
                listOf("Аида Ведищева", "1967", "Мелодия"), 420, cond = 3, storageRu = "Стеллаж у проигрывателя", storageEn = "Shelf by the turntable"),
            T("rec_vysotsky", 0, "«Последний жулик»", "«The Last Swindler»", "Досталась от дяди", "From my uncle",
                "Дядя крутил её на каждом дне рождения. Теперь она у меня, вместе с его конвертом с пометками карандашом.",
                "My uncle played it at every birthday. Now it's mine, with his sleeve and his pencil notes on it.",
                listOf("Владимир Высоцкий, Микаэл Таривердиев", "", "Мелодия"), 900, cond = 2),
            T("rec_happiness", 0, "«Иду за счастьем»", "«Going for Happiness»", fields = listOf("Анатолий Горохов", "", "Мелодия"), days = 2400),
            T("rec_beatles", 0, "The Beatles на «Мелодии»", "The Beatles on Melodiya", "Обмен с Мариной из Казани", "A swap with Marina from Kazan",
                "Маленькая пластинка-миньон. Те самые Битлз, только с советской этикеткой и без названий песен на английском.",
                "A little seven-inch. The very same Beatles, only with a Soviet label and no English song titles.",
                listOf("The Beatles", "", "Мелодия"), 300, cond = 3),
            T("rec_robeson", 0, "Поль Робсон. Песни", "Paul Robeson. Songs", "Букинист на Литейном", "A second-hand shop on Liteyny",
                fields = listOf("Поль Робсон", "", "Министерство культуры СССР"), days = 640, copies = 2, cond = 2),
            T("rec_muztrest", 1, "Пластинка «Музтреста»", "A Muztrest record", "Чердак дачи в Комарово", "The attic of a dacha in Komarovo",
                "Нашлась в коробке из-под обуви вместе с патефонными иголками. Шеллак, семьдесят восемь оборотов.",
                "Turned up in a shoebox with gramophone needles. Shellac, seventy-eight turns a minute.",
                listOf("", "1929", "Музтрест"), 1200, cond = 2, storageRu = "Коробка 3", storageEn = "Box 3"),
            T("rec_umayun", 1, "«Умаюн», персидская мелодия", "«Umayun», a Persian melody", "Тбилиси, Сухой мост", "Tbilisi, the Dry Bridge market",
                fields = listOf("Саша Оганезашвили, кеманча", "", "Грампласттрест"), days = 700, cond = 3),
            T("rec_dilijan", 1, "«Дилижан»", "«Dilijan»", fields = listOf("Бала Меликян, Саша Оганезашвили", "", "Грампласттрест"), days = 2600),
            T("rec_anker", 1, "Йозеф Вайс, 1911", "Josef Weiss, 1911", "Берлин, антикварная лавка", "Berlin, an antique shop",
                "Вариации на «Весёлого странника» в исполнении пианиста Йозефа Вайса. Этикетке больше ста лет, а она как новая.",
                "Variations on «The Merry Wanderer» played by Josef Weiss. The label is over a hundred years old and looks new.",
                listOf("Йозеф Вайс", "1911", "Anker"), 1500, cond = 4),
            T("rec_vox", 1, "Кипрас Петраускас, 1922", "Kipras Petrauskas, 1922", "Вильнюс", "Vilnius",
                fields = listOf("Кипрас Петраускас", "1922", "Vox"), days = 980, cond = 2),
            T("rec_elvis", 2, "«I Forgot to Remember to Forget»", "«I Forgot to Remember to Forget»", "Мемфис, магазин у Сан-студии", "Memphis, a shop near Sun Studio",
                "Семьдесят восемь оборотов, хрупкий шеллак. Летела домой в ручной клади, завёрнутая в свитер.",
                "Seventy-eight turns, fragile shellac. Flew home as hand luggage, wrapped in a jumper.",
                listOf("Elvis Presley", "1955", "RCA Victor"), 520, cond = 4, storageRu = "Рамка на стене", storageEn = "A frame on the wall"),
            T("rec_parlophone", 2, "«Barnyard Hop»", "«Barnyard Hop»", fields = listOf("", "", "Parlophone"), days = 2800),
            T("rec_imperial", 2, "Конверт Imperial «Hot Jazz»", "An Imperial «Hot Jazz» sleeve", "Лондон, Портобелло-роуд", "London, Portobello Road",
                fields = listOf("", "", "Imperial"), days = 810, cond = 2),
            T("rec_tounsia", 2, "«Ала баб дарек», 1945", "«Ala Bab Darek», 1945", "Тунис, рынок в медине", "Tunis, a market in the medina",
                "Продавец долго не хотел отдавать: говорил, что это его бабушка слушала по вечерам.",
                "The seller didn't want to part with it: he said his grandmother listened to it every evening.",
                listOf("Луиза Тунсия", "1945", ""), 560, cond = 3),
            T("rec_letter", 2, "Говорящее письмо Voice-o-Graph", "A Voice-o-Graph talking letter", wish = true),
        ), pearl = "rec_elvis")

    private val COINS = HallDef("Монеты", "Coins", "МН",
        listOf("СССР", "Юбилейные", "Российская империя"), listOf("USSR", "Commemorative", "Russian Empire"),
        listOf("Страна", "Год", "Номинал", "Металл"), listOf("Country", "Year", "Denomination", "Metal"),
        listOf(
            T("coin_rub1924", 0, "Рубль 1924 года", "A 1924 rouble", "Досталась от деда", "From my grandfather",
                "Рабочий и крестьянин на фоне восходящего солнца. Дед хранил его в спичечном коробке и никому не показывал.",
                "A worker and a peasant against the rising sun. Grandfather kept it in a matchbox and showed it to no one.",
                listOf("СССР", "1924", "1 рубль", "серебро"), 9000, cond = 3, storageRu = "Шкатулка в спальне", storageEn = "The box in the bedroom"),
            T("coin_2k1973", 0, "2 копейки 1973 года", "2 kopecks, 1973", "Копилка", "The piggy bank",
                fields = listOf("СССР", "1973", "2 копейки", "латунь"), days = 1800, copies = 3, cond = 2),
            T("coin_20k1967", 1, "20 копеек 1967 года", "20 kopecks, 1967", "Нумизматический клуб", "The coin club",
                "Из серии к пятидесятилетию Советской власти. На реверсе крейсер «Аврора».",
                "From the series for fifty years of Soviet power. The cruiser Aurora on the reverse.",
                listOf("СССР", "1967", "20 копеек", "мельхиор"), 260, cond = 4, also = "coin_20k1967b", series = 1),
            T("coin_15k1967", 1, "15 копеек 1967 года", "15 kopecks, 1967", "Нумизматический клуб", "The coin club",
                fields = listOf("СССР", "1967", "15 копеек", "мельхиор"), days = 255, cond = 4, series = 1),
            T("coin_50k1967", 1, "50 копеек 1967 года", "50 kopecks, 1967", fields = listOf("СССР", "1967", "50 копеек", "мельхиор"), days = 2500, series = 1),
            T("coin_rub1985", 1, "Рубль к 40-летию Победы", "Rouble for 40 years of Victory", "Подарок отца", "A present from my father",
                "Папа принёс его девятого мая восемьдесят пятого и сказал: «Береги». Берегу.",
                "Dad brought it home on the ninth of May 1985 and said: «Look after it.» I do.",
                listOf("СССР", "1985", "1 рубль", "медно-никелевый сплав"), 14000, cond = 3, also = "coin_rub1985b"),
            T("coin_olymp1", 1, "Олимпиада-80, бег", "Moscow 1980 Olympics, running", "Аукцион", "An auction",
                fields = listOf("СССР", "1978", "", "серебро"), days = 380, cond = 4),
            T("coin_olymp2", 1, "Олимпиада-80, эмблема", "Moscow 1980 Olympics, the emblem", fields = listOf("СССР", "1977", "", "серебро"), days = 2700),
            T("coin_5k1765", 2, "Пятак 1765 года", "5 kopecks, 1765", "Огород под Тверью", "A vegetable garden near Tver",
                "Почти полсотни граммов меди, с вензелем Екатерины Второй. Нашли металлоискателем, когда копали под картошку.",
                "Nearly fifty grams of copper with Catherine the Great's monogram. Found with a metal detector while digging for potatoes.",
                listOf("Российская империя", "1765", "5 копеек", "медь"), 700, cond = 2),
            T("coin_2k1797", 2, "2 копейки 1797 года", "2 kopecks, 1797", "Ярмарка в Суздале", "A fair in Suzdal",
                fields = listOf("Российская империя", "1797", "2 копейки", "медь"), days = 1100, cond = 2),
            T("coin_5k1833", 2, "5 копеек 1833 года", "5 kopecks, 1833", "Обмен в клубе", "A swap at the club",
                fields = listOf("Российская империя", "1833", "5 копеек", "медь"), days = 640, cond = 3),
            T("coin_5k1881", 2, "5 копеек 1881 года", "5 kopecks, 1881", fields = listOf("Российская империя", "1881", "5 копеек", "медь"), days = 2900),
            T("coin_diamond", 1, "3 рубля «Алмазный фонд России»", "3 roubles «Diamond Fund of Russia»", wish = true),
        ), pearl = "coin_rub1924",
        series = listOf(Triple("50 лет Советской власти", "Fifty years of Soviet power", 5)))

    private val BOOKS = HallDef("Книги", "Books", "КН",
        listOf("Переплёты", "Старинные", "Детские"), listOf("Bindings", "Antique", "Children's"),
        listOf("Автор", "Год издания", "Издательство", "Переплёт"), listOf("Author", "Year", "Publisher", "Binding"),
        listOf(
            T("book_browning", 0, "Сочинения Роберта Браунинга", "The Works of Robert Browning", "Лондон, книжный на Сесил-корт", "London, a bookshop in Cecil Court",
                "Переплёт «Косуэй» с миниатюрой на эмали под стеклом. Самая дорогая книга в доме, и её никто не читает.",
                "A Cosway binding with an enamel miniature under glass. The most expensive book in the house, and nobody reads it.",
                listOf("Роберт Браунинг", "", "", "сафьян, миниатюра"), 600, cond = 4, storageRu = "Стеклянный шкаф", storageEn = "The glass cabinet"),
            T("book_alpine", 0, "«Alpine Ascents and Adventures»", "«Alpine Ascents and Adventures»", "Женева, букинист", "Geneva, a second-hand bookshop",
                "Купил после первого похода в горы. Издательский переплёт с ёлками и вершиной, тиснение почти не стёрлось.",
                "Bought after my first trip to the mountains. A publisher's binding with fir trees and a peak, the stamping barely worn.",
                listOf("Г. Шютц Уилсон", "1878", "Sampson Low", "коленкор"), 430, cond = 3),
            T("book_painters", 0, "«Representative Painters of the XIXth Century»", "«Representative Painters of the XIXth Century»", "Эдинбург", "Edinburgh",
                fields = listOf("", "1899", "", "коленкор, золото"), days = 300, cond = 3),
            T("book_fireflies", 0, "«Shadows and Fireflies»", "«Shadows and Fireflies»", fields = listOf("", "", "", "коленкор"), days = 2800),
            T("book_worcester", 0, "Резьба Вустерского собора", "Carvings of Worcester Cathedral", fields = listOf("", "", "", ""), days = 3000),
            T("book_prayer", 0, "Молитвослов в переплёте Чиверса", "A prayer book bound by Chivers", "Бат", "Bath",
                fields = listOf("", "", "", "Чиверс, Бат"), days = 820, cond = 3),
            T("book_portraits", 1, "«Русские портреты»", "«Russian Portraits»", "Букинист на Арбате", "A second-hand bookshop on the Arbat",
                "Издание великого князя Николая Михайловича. Пять томов, у меня пока только первый.",
                "Grand Duke Nicholas Mikhailovich's edition. Five volumes; so far I have only the first.",
                listOf("вел. кн. Николай Михайлович", "1905", "", "картон"), 1300, cond = 2),
            T("book_pantokrator", 1, "Оклад с Пантократором", "A cover with Christ Pantocrator", "Аукцион в Хельсинки", "An auction in Helsinki",
                fields = listOf("", "", "", "металл, финифть"), days = 2100, cond = 3),
            T("book_embroidered", 1, "Вышитый переплёт", "An embroidered binding", "Подарок бабушки", "A present from grandmother",
                "Бабушка вышила этот переплёт для своего молитвенника, когда ей было шестнадцать.",
                "Grandmother embroidered this binding for her prayer book when she was sixteen.",
                listOf("", "", "", "шёлк, вышивка"), 12000, cond = 2),
            T("book_saint", 1, "Молитвослов с образом святителя", "A prayer book with a saint", fields = listOf("", "", "", "живопись на коже"), days = 2600),
            T("book_flower", 1, "Альбом с цветочной миниатюрой", "An album with a flower miniature", "Блошиный рынок в Риге", "A flea market in Riga",
                fields = listOf("", "", "", "бархат, фарфор"), days = 500, cond = 3),
            T("book_toasting", 2, "«The Enchanted Toasting-Fork»", "«The Enchanted Toasting-Fork»", "Кембридж", "Cambridge",
                "Сказка про волшебную вилку для тостов. Читал её дочке на ночь по-английски, медленно и с переводом.",
                "A fairy tale about a magic toasting fork. I read it to my daughter at bedtime in English, slowly, translating as I went.",
                listOf("", "1880", "", "коленкор"), 760, cond = 3),
            T("book_roslin", 1, "Евангелие Тороса Рослина", "The T'oros Roslin Gospels", wish = true),
        ), pearl = "book_browning")

    private fun picture(c: AppContainer, key: String): Bitmap? =
        runCatching { c.app.assets.open("things/$key.webp").use { BitmapFactory.decodeStream(it) } }.getOrNull()

    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.hallsNow().isNotEmpty()) return
        val ru = c.ru
        val today = Museum.today()
        for ((n, def) in listOf(RECORDS, COINS, BOOKS).withIndex()) {
            val hall = c.dao.add(Hall(name = if (ru) def.ru else def.en, code = if (ru) def.code else def.en.take(2).uppercase(),
                fields = (if (ru) def.fieldsRu else def.fieldsEn).joinToString("\n"), sort = n))
            val cabs = (if (ru) def.cabsRu else def.cabsEn).mapIndexed { k, name -> c.dao.add(Cabinet(hall = hall, name = name, sort = k)) }
            val series = def.series.map { (sr, se, size) -> c.dao.add(Series(hall = hall, name = if (ru) sr else se, size = size)) }
            val names = if (ru) def.fieldsRu else def.fieldsEn
            var pearl = 0L
            for (t in def.things) {
                val face = picture(c, t.key) ?: continue
                var item = Item(
                    hall = hall, cabinet = cabs[t.cab], inv = c.dao.lastInv() + 1, title = if (ru) t.ru else t.en,
                    status = if (t.wish) Status.WISH else Status.OWNED, copies = t.copies,
                    acquired = if (t.wish) null else today - t.days, place = if (ru) t.placeRu else t.placeEn,
                    story = if (ru) t.storyRu else t.storyEn, storage = if (ru) t.storageRu else t.storageEn, condition = t.cond,
                    series = if (t.series > 0) series.getOrElse(t.series - 1) { 0L } else 0L,
                    photos = if (t.also != null) 2 else 1, cut = face.hasAlpha(), hue = Photos.hue(face), aspect = face.width.toFloat() / face.height,
                    created = System.currentTimeMillis() - t.days * 86_400_000L,
                )
                // The fields are the hall's own; only the Russian demo fills their values.
                if (ru) t.fields.forEachIndexed { k, v -> if (v.isNotBlank() && k < names.size) item = item.withField(names[k], v) }
                val id = c.dao.add(item)
                c.photos.keep(id, 0, if (face.hasAlpha()) Photos.onGround(face, 1000) else face)
                t.also?.let { k -> picture(c, k)?.let { b -> c.photos.keep(id, 1, if (b.hasAlpha()) Photos.onGround(b, 1000) else b) } }
                c.photos.writeFace(id, face)
                if (t.key == def.pearl) pearl = id
            }
            c.dao.hall(hall)?.let { c.dao.save(it.copy(pearl = pearl)) }
        }

        // A record that came on this very day some years ago.
        c.dao.itemsNow().firstOrNull { it.title.contains("Робсон") || it.title.contains("Robeson") }?.let {
            c.dao.save(it.copy(acquired = LocalDate.now().minusYears(12).toEpochDay()))
        }
        for (d in 1..4) c.visitorsWrite(today - 5 + d)
        c.dao.hallsNow().firstOrNull()?.let { c.dao.add(Seal(it.id, Museum.season(today - 95), today - 95, 13, 1)) }
        val owned = c.dao.itemsNow().count { it.owned }
        c.settings.update { it.copy(onboarded = true, stage = Museum.stage(owned)) }
    }
}
