package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY id ASC")
    fun observeByDate(date: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY id ASC")
    suspend fun getByDate(date: String): List<MealLogEntity>

    /** Inclusive ISO dates. */
    @Query("SELECT * FROM meal_log WHERE date >= :from AND date <= :to ORDER BY date ASC, id ASC")
    suspend fun getBetween(from: String, to: String): List<MealLogEntity>

    @Query("SELECT * FROM meal_log WHERE date = :date AND slotId = :slotId ORDER BY id ASC")
    suspend fun getBySlot(date: String, slotId: Long): List<MealLogEntity>

    @Insert
    suspend fun insert(row: MealLogEntity)

    @Query("DELETE FROM meal_log WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM meal_log WHERE date = :date AND slotId = :slotId")
    suspend fun deleteBySlot(date: String, slotId: Long)
}
