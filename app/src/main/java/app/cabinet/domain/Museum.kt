package app.cabinet.domain

import app.cabinet.data.Item
import app.cabinet.data.Show
import java.time.LocalDate
import java.time.temporal.IsoFields
import kotlin.random.Random

/** The museum's rules, kept apart from storage and screens. */
object Museum {
    /** Things it takes to build each stage of the house, from a shelf in the hall to a museum with a crest. */
    val STAGES = intArrayOf(0, 10, 25, 50, 100, 250, 500, 1000)

    fun stage(owned: Int): Int = STAGES.indexOfLast { owned >= it }.coerceAtLeast(0)

    /** Described things it takes for each rank. */
    val RANKS = intArrayOf(0, 10, 30, 75, 150, 300)

    fun rank(described: Int): Int = RANKS.indexOfLast { described >= it }.coerceAtLeast(0)

    /** A card is described when the thing has a name and either where it's from or its story. Until then it stands under a dust cover. */
    fun described(i: Item): Boolean = i.title.isNotBlank() && (i.place.isNotBlank() || i.story.isNotBlank())

    fun dusty(i: Item): Boolean = i.owned && !described(i)

    /** How full the card is, 0..1: what the dust cover shows through. */
    fun fullness(i: Item): Float {
        var n = 0
        if (i.title.isNotBlank()) n++
        if (i.place.isNotBlank()) n++
        if (i.story.isNotBlank()) n++
        if (i.acquired != null) n++
        if (i.storage.isNotBlank()) n++
        return n / 5f
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Monday of this week. */
    fun week(day: Long = today()): Long = LocalDate.ofEpochDay(day).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.toEpochDay()

    fun season(day: Long = today()): String = LocalDate.ofEpochDay(day).let { "${it.year}-${it.get(IsoFields.QUARTER_OF_YEAR)}" }

    fun inv(code: String, n: Int): String = "${code.ifBlank { "№" }}-${n.toString().padStart(4, '0')}"

    fun code(name: String): String = name.filter { it.isLetter() }.take(2).uppercase().ifEmpty { "КР" }

    // ——— The evening round: one question about one thing a day ———

    enum class Ask { TITLE, PLACE, STORY, WHEN, STORAGE }

    data class Question(val item: Item, val ask: Ask)

    /** Today's question, or null when every card is complete. The same all day. */
    fun question(items: List<Item>, day: Long = today()): Question? {
        val owned = items.filter { it.owned }
        for (ask in Ask.entries) {
            val open = owned.filter { missing(it, ask) }
            if (open.isNotEmpty()) return Question(open[Random(day * 31 + ask.ordinal).nextInt(open.size)], ask)
        }
        return null
    }

    private fun missing(i: Item, ask: Ask) = when (ask) {
        Ask.TITLE -> i.title.isBlank()
        Ask.PLACE -> i.place.isBlank()
        Ask.STORY -> i.story.isBlank()
        Ask.WHEN -> i.acquired == null
        Ask.STORAGE -> i.storage.isBlank()
    }

    // ——— Temporary exhibitions ———

    enum class Theme { RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET, GREY, OLDEST, NEWEST, STORIES, TRAVEL, PEARLS, FREE }

    /** The things that suit a theme, best first. */
    fun suits(t: Theme, items: List<Item>, now: Long = System.currentTimeMillis()): List<Item> {
        val owned = items.filter { it.owned && it.photos > 0 }
        fun hue(a: Int, b: Int) = owned.filter { it.hue in a until b }
        return when (t) {
            Theme.RED -> owned.filter { it.hue in 0 until 15 || it.hue >= 330 }
            Theme.ORANGE -> hue(15, 45)
            Theme.YELLOW -> hue(45, 70)
            Theme.GREEN -> hue(70, 170)
            Theme.BLUE -> hue(170, 260)
            Theme.VIOLET -> hue(260, 330)
            Theme.GREY -> owned.filter { it.hue < 0 }
            Theme.OLDEST -> owned.sortedBy { it.acquired ?: (it.created / 86_400_000L) }
            Theme.NEWEST -> owned.filter { now - it.created < 120L * 86_400_000L }.sortedByDescending { it.created }
            Theme.STORIES -> owned.filter { it.story.isNotBlank() }.sortedByDescending { it.story.length }
            Theme.TRAVEL -> owned.filter { it.place.isNotBlank() }
            Theme.PEARLS -> owned.filter { it.condition == 4 }
            Theme.FREE -> owned
        }
    }

    /** The theme of the week: one with enough things for a show, different each week. */
    fun themeOfWeek(items: List<Item>, week: Long = week()): Theme {
        val ok = Theme.entries.filter { it != Theme.FREE && suits(it, items).size >= 3 }
        if (ok.isEmpty()) return Theme.FREE
        return ok[Random(week).nextInt(ok.size)]
    }

    fun showOfWeek(shows: List<Show>, week: Long = week()): Show? = shows.firstOrNull { it.week == week }

    // ——— Visitors ———

    /** How many visitors came today: more for a fuller, better described museum. */
    fun visitors(items: List<Item>, day: Long = today()): Int {
        val owned = items.count { it.owned }
        if (owned == 0) return 0
        val described = items.count { it.owned && described(it) }
        val base = 1 + owned / 8 + described / 3
        return (base * (0.7 + Random(day).nextDouble() * 0.6)).toInt().coerceAtLeast(1)
    }

    /** Where visitors linger: the best described things. */
    fun favourites(items: List<Item>, day: Long = today(), n: Int = 3): List<Item> {
        val r = Random(day * 7 + 3)
        // Score once per thing: a comparator that rolls dice breaks the sort.
        return items.filter { it.owned && it.photos > 0 }
            .map { it to fullness(it) * 2 + (it.story.length.coerceAtMost(300) / 300f) + r.nextFloat() }
            .sortedByDescending { it.second }.map { it.first }
            .take(n)
    }

    /** Things that came to the collection on this day in an earlier year. */
    fun onThisDay(items: List<Item>, day: Long = today()): List<Item> {
        val d = LocalDate.ofEpochDay(day)
        return items.filter { i ->
            i.owned && i.acquiredLevel == 0 && i.acquired?.let { LocalDate.ofEpochDay(it).let { a -> a.monthValue == d.monthValue && a.dayOfMonth == d.dayOfMonth && a.year < d.year } } == true
        }
    }
}
