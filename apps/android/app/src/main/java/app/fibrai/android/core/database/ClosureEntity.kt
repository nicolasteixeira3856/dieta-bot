package app.fibrai.android.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A day or week closure (A60 part B, ADR-044, v12). One row per [key] (`day:{date}` or `week:{monday}`), so a closure
 * already produced is never produced again. [numbers]: the app's numbers as sent ([app.fibrai.android.domain.ClosureNumbers]
 * JSON); [text]: the server text, null while there is none.
 */
@Entity(tableName = "closure")
data class ClosureEntity(
    @PrimaryKey
    val key: String,
    /** "day" | "week". */
    val period: String,
    /** The closed day, or the Monday of the closed week (ISO). */
    val date: String,
    val numbers: String,
    val text: String? = null,
    /** "text" | "fallback" | "offline" | "empty" (a day without any record: no text is asked). */
    val status: String,
    val createdAtEpochMs: Long,
    /** The one retry of the text at the next app start was spent. */
    @ColumnInfo(defaultValue = "0")
    val retried: Boolean = false,
)

@Dao
interface ClosureDao {
    @Query("SELECT * FROM closure WHERE `key` = :key")
    suspend fun get(key: String): ClosureEntity?

    @Query("SELECT * FROM closure WHERE date >= :from ORDER BY date DESC")
    fun observeSince(from: String): Flow<List<ClosureEntity>>

    @Query("SELECT * FROM closure WHERE status = 'offline' AND retried = 0")
    suspend fun getOffline(): List<ClosureEntity>

    /** Inserted only once per key. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: ClosureEntity): Long

    /** A72: a record in a past day updates that day's numbers; the text stays. */
    @Query("UPDATE closure SET numbers = :numbers WHERE `key` = :key")
    suspend fun setNumbers(key: String, numbers: String)

    @Query("UPDATE closure SET text = :text, status = :status, retried = :retried WHERE `key` = :key")
    suspend fun setText(key: String, text: String?, status: String, retried: Boolean)
}
