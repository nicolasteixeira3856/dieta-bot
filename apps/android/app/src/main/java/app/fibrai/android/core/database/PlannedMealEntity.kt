package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A plan reserved for one meal of one day (A60 part D, ADR-046, v12): nothing eaten, one row per date and slot. A record
 * or a skip on the slot replaces it (it is part of the slot state, see [app.fibrai.android.domain.SlotState.planned]); a
 * new day has none.
 */
@Entity(tableName = "planned_meal", primaryKeys = ["date", "slotId"])
data class PlannedMealEntity(
    val date: String,
    val slotId: Long,
    val text: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    /** The plan bubble it came from; null when the bubble is gone. */
    val sourceMessageId: Long? = null,
)

@Dao
interface PlannedMealDao {
    @Query("SELECT * FROM planned_meal WHERE date = :date")
    fun observeByDate(date: String): Flow<List<PlannedMealEntity>>

    @Query("SELECT * FROM planned_meal WHERE date = :date")
    suspend fun getByDate(date: String): List<PlannedMealEntity>

    @Query("SELECT * FROM planned_meal WHERE date = :date AND slotId = :slotId")
    suspend fun get(date: String, slotId: Long): PlannedMealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: PlannedMealEntity)

    @Query("DELETE FROM planned_meal WHERE date = :date AND slotId = :slotId")
    suspend fun delete(date: String, slotId: Long)

    @Query("DELETE FROM planned_meal WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
