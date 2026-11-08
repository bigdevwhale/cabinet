package app.cabinet.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A hall is one collection: cans, crocodiles. [fields] are its own card fields, one per line. */
@Entity
data class Hall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Two letters before the inventory number: «КР» for crocodiles. */
    val code: String,
    val fields: String = "",
    val sort: Int = 0,
    /** The one thing under its own spotlight; 0 = none. */
    val pearl: Long = 0,
    val created: Long = System.currentTimeMillis(),
) {
    val fieldList: List<String> get() = fields.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

/** A cabinet inside a hall: ceramics, toys, keyrings. Its things stand on its shelves. */
@Entity(indices = [Index("hall")])
data class Cabinet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hall: Long,
    val name: String,
    val sort: Int = 0,
    /** How the shelves are ordered: 0 by hand, 1 by colour, 2 by arrival, 3 by name. */
    val order: Int = 0,
)

enum class Status { OWNED, WISH }

/**
 * One thing. Its photos are files named after [id]: the original, the cut-out and a small thumbnail.
 * [copies] > 1 means doubles, which go to the trade fund.
 */
@Entity(indices = [Index("hall"), Index("cabinet"), Index("series")])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hall: Long,
    val cabinet: Long,
    /** The inventory number, given once and never reused. */
    val inv: Int,
    val title: String = "",
    val status: Status = Status.OWNED,
    val copies: Int = 1,
    /** Epoch day it came to the collection; null = unknown. */
    val acquired: Long? = null,
    /** How exactly [acquired] is known (#2): 0 the day, 1 only the month (the 1st), 2 only the year (1 January). */
    val acquiredLevel: Int = 0,
    /** Where it came from: a shop, a trip, a person. */
    val place: String = "",
    val price: String = "",
    /** 0 unknown, 1 worn … 4 mint. */
    val condition: Int = 0,
    val story: String = "",
    /** Where it physically lives: «box 3, top shelf». */
    val storage: String = "",
    /** The hall's own fields: «name=value» lines. */
    val fields: String = "",
    val tags: String = "",
    val series: Long = 0,
    /** Place on the shelf when the cabinet is ordered by hand. */
    val sort: Int = 0,
    /** Number of photos; photo 0 is the face. */
    val photos: Int = 0,
    /** The face photo is cut out of its background. */
    val cut: Boolean = false,
    /** Main colour: hue 0..359, -1 grey or unknown. */
    val hue: Int = -1,
    /** Width / height of the face picture, for laying out shelves without loading it. */
    val aspect: Float = 1f,
    /** The day it was last found in its place at an inventory check. */
    val checked: Long = 0,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    /** What the lens sees: a normalised vector, so likeness is a dot product. */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val look: ByteArray? = null,
) {
    val owned get() = status == Status.OWNED
    fun field(name: String): String = fields.lines().firstOrNull { it.substringBefore('=') == name }?.substringAfter('=') ?: ""
    fun withField(name: String, value: String): Item {
        val rest = fields.lines().filter { it.isNotBlank() && it.substringBefore('=') != name }
        val v = value.replace('\n', ' ').trim()
        return copy(fields = (if (v.isEmpty()) rest else rest + "$name=$v").joinToString("\n"))
    }
}

/** A set with a known number of pieces: empty places show what is still missing. */
@Entity
data class Series(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hall: Long,
    val name: String,
    val size: Int,
)

/** A line in the visitors' book. */
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val item: Long,
    val text: String,
)

/** A temporary exhibition: a theme of the week and the things chosen for it. */
@Entity
data class Show(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Monday's epoch day. */
    val week: Long,
    val theme: String,
    val title: String,
    /** Item ids, comma separated, in hanging order. */
    val items: String,
    val opened: Long = System.currentTimeMillis(),
) {
    val ids: List<Long> get() = items.split(',').mapNotNull { it.trim().toLongOrNull() }
}

/** An inventory check of one hall in one season: the wax seal. */
@Entity(primaryKeys = ["hall", "season"])
data class Seal(
    val hall: Long,
    /** «2026-4»: year and quarter. */
    val season: String,
    val day: Long,
    val found: Int,
    val missing: Int,
)

data class Count(val hall: Long, val n: Int)

@Dao
interface MuseumDao {
    @Query("SELECT * FROM Hall ORDER BY sort, id") fun halls(): Flow<List<Hall>>
    @Query("SELECT * FROM Hall ORDER BY sort, id") suspend fun hallsNow(): List<Hall>
    @Query("SELECT * FROM Hall WHERE id = :id") suspend fun hall(id: Long): Hall?
    @Insert suspend fun add(h: Hall): Long
    @Update suspend fun save(h: Hall)
    @Delete suspend fun drop(h: Hall)

    @Query("SELECT * FROM Cabinet ORDER BY hall, sort, id") fun cabinets(): Flow<List<Cabinet>>
    @Query("SELECT * FROM Cabinet ORDER BY hall, sort, id") suspend fun cabinetsNow(): List<Cabinet>
    @Insert suspend fun add(c: Cabinet): Long
    @Update suspend fun save(c: Cabinet)
    @Delete suspend fun drop(c: Cabinet)

    @Query("SELECT * FROM Item ORDER BY hall, cabinet, sort, id") fun items(): Flow<List<Item>>
    @Query("SELECT * FROM Item ORDER BY hall, cabinet, sort, id") suspend fun itemsNow(): List<Item>
    @Query("SELECT * FROM Item WHERE id = :id") suspend fun item(id: Long): Item?
    @Query("SELECT COALESCE(MAX(inv), 0) FROM Item") suspend fun lastInv(): Int
    @Query("SELECT COALESCE(MAX(sort), 0) FROM Item WHERE cabinet = :cabinet") suspend fun lastSort(cabinet: Long): Int
    @Insert suspend fun add(i: Item): Long
    @Update suspend fun save(i: Item)
    @Update suspend fun save(i: List<Item>)
    @Delete suspend fun drop(i: Item)
    @Query("UPDATE Item SET cabinet = :to WHERE cabinet = :from") suspend fun moveAll(from: Long, to: Long)
    @Query("UPDATE Item SET series = 0 WHERE series = :series") suspend fun unseries(series: Long)

    @Query("SELECT * FROM Series ORDER BY hall, id") fun series(): Flow<List<Series>>
    @Insert suspend fun add(s: Series): Long
    @Update suspend fun save(s: Series)
    @Delete suspend fun drop(s: Series)

    @Query("SELECT * FROM Note ORDER BY day DESC, id DESC LIMIT 200") fun notes(): Flow<List<Note>>
    @Query("SELECT COALESCE(MAX(day), 0) FROM Note") suspend fun lastNoteDay(): Long
    @Insert suspend fun add(n: Note): Long

    @Query("SELECT * FROM Show ORDER BY week DESC, id DESC") fun shows(): Flow<List<Show>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(s: Show): Long
    @Delete suspend fun drop(s: Show)

    @Query("SELECT * FROM Seal ORDER BY day DESC") fun seals(): Flow<List<Seal>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(s: Seal)

    @Query("DELETE FROM Hall") suspend fun clearHalls()
    @Query("DELETE FROM Cabinet") suspend fun clearCabinets()
    @Query("DELETE FROM Item") suspend fun clearItems()
    @Query("DELETE FROM Series") suspend fun clearSeries()
    @Query("DELETE FROM Note") suspend fun clearNotes()
    @Query("DELETE FROM Show") suspend fun clearShows()
    @Query("DELETE FROM Seal") suspend fun clearSeals()
    @Query("SELECT * FROM Series") suspend fun seriesNow(): List<Series>
    @Query("SELECT * FROM Note") suspend fun notesNow(): List<Note>
    @Query("SELECT * FROM Show") suspend fun showsNow(): List<Show>
    @Query("SELECT * FROM Seal") suspend fun sealsNow(): List<Seal>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addRaw(h: Hall)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addRaw(c: Cabinet)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addRaw(i: Item)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addRaw(s: Series)

    @Transaction
    suspend fun wipe() { clearHalls(); clearCabinets(); clearItems(); clearSeries(); clearNotes(); clearShows(); clearSeals() }
}

@Database(entities = [Hall::class, Cabinet::class, Item::class, Series::class, Note::class, Show::class, Seal::class], version = 2, exportSchema = true)
@androidx.room.TypeConverters(Converters::class)
abstract class MuseumDb : RoomDatabase() {
    abstract fun dao(): MuseumDao

    companion object {
        fun create(context: Context): MuseumDb = Room.databaseBuilder(context, MuseumDb::class.java, "museum.db")
            .addMigrations(DATE_LEVEL)
            .build()

        /** A date known only to the month or the year (#2). */
        private val DATE_LEVEL = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Item ADD COLUMN acquiredLevel INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter fun status(s: Status): String = s.name
    @androidx.room.TypeConverter fun status(s: String): Status = runCatching { Status.valueOf(s) }.getOrDefault(Status.OWNED)
}
