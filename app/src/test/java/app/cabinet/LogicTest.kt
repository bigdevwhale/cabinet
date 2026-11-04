package app.cabinet

import app.cabinet.data.Item
import app.cabinet.data.Lens
import app.cabinet.data.Status
import app.cabinet.domain.Museum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private fun item(id: Long, title: String = "", place: String = "", story: String = "", hue: Int = -1, status: Status = Status.OWNED, acquired: Long? = null, storage: String = "") =
        Item(id = id, hall = 1, cabinet = 1, inv = id.toInt(), title = title, place = place, story = story, hue = hue, status = status, acquired = acquired, storage = storage, photos = 1)

    @Test fun stagesAndRanks() {
        assertEquals(0, Museum.stage(0)); assertEquals(0, Museum.stage(9)); assertEquals(1, Museum.stage(10))
        assertEquals(4, Museum.stage(100)); assertEquals(7, Museum.stage(5000))
        assertEquals(0, Museum.rank(9)); assertEquals(1, Museum.rank(10)); assertEquals(5, Museum.rank(300))
    }

    @Test fun dustCoverComesOffWithPlaceOrStory() {
        assertTrue(Museum.dusty(item(1)))
        assertTrue(Museum.dusty(item(1, title = "Croc")))
        assertFalse(Museum.dusty(item(1, title = "Croc", place = "Riga")))
        assertFalse(Museum.dusty(item(1, title = "Croc", story = "A gift")))
        assertFalse(Museum.dusty(item(1, status = Status.WISH)))
    }

    @Test fun roundAsksTheMostBasicThingFirstAndIsStableAllDay() {
        val items = listOf(item(1, title = "A", place = "x", story = "s", acquired = 1, storage = "b"), item(2, title = "", place = "x"), item(3, title = "C"))
        val q = Museum.question(items, 100)!!
        assertEquals(Museum.Ask.TITLE, q.ask); assertEquals(2L, q.item.id)
        assertEquals(q, Museum.question(items, 100))
        assertNull(Museum.question(listOf(items[0]), 100))
    }

    @Test fun fieldsRoundTrip() {
        val i = item(1).withField("Brand", "Kumo").withField("Year", "2024").withField("Brand", "Sol")
        assertEquals("Sol", i.field("Brand")); assertEquals("2024", i.field("Year"))
        assertEquals("", i.withField("Year", "").field("Year"))
    }

    @Test fun coloursMakeThemes() {
        val items = listOf(item(1, hue = 5), item(2, hue = 120), item(3, hue = 125), item(4, hue = 130), item(5, hue = 200))
        assertEquals(3, Museum.suits(Museum.Theme.GREEN, items).size)
        assertEquals(1, Museum.suits(Museum.Theme.RED, items).size)
        assertTrue(Museum.themeOfWeek(items, 42) in setOf(Museum.Theme.GREEN, Museum.Theme.OLDEST, Museum.Theme.NEWEST))
    }

    @Test fun lensVectorsPackAndCompare() {
        val a = floatArrayOf(0.6f, 0.8f, 0f)
        val b = Lens.unpack(Lens.pack(a))
        assertEquals(1f, Lens.likeness(a, b), 1e-6f)
        assertEquals(0f, Lens.likeness(a, floatArrayOf(1f, 0f)), 0f)
    }

    @Test fun inventoryNumbers() {
        assertEquals("КР-0147", Museum.inv("КР", 147))
        assertEquals("БА", Museum.code("Банки 0,33"))
        assertEquals(java.time.LocalDate.of(2026, 11, 2).toEpochDay(), Museum.week(java.time.LocalDate.of(2026, 11, 4).toEpochDay()))
    }
}
