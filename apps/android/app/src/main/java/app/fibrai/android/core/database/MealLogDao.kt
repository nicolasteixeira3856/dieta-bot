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

    /** A72: the extra whose key is [extraId] on [date]. */
    @Query("SELECT * FROM meal_log WHERE date = :date AND extraId = :extraId ORDER BY id ASC")
    suspend fun getByExtra(date: String, extraId: Long): List<MealLogEntity>

    @Query("DELETE FROM meal_log WHERE date = :date AND extraId = :extraId")
    suspend fun deleteByExtra(date: String, extraId: Long)

    /** A72 (the Home strip): kcal per day between two inclusive ISO dates; a day without records is absent. */
    @Query("SELECT date AS date, SUM(kcal) AS kcal FROM meal_log WHERE date >= :from AND date <= :to GROUP BY date")
    fun observeDayKcal(from: String, to: String): Flow<List<DayKcal>>

    @Insert
    suspend fun insert(row: MealLogEntity)

    @Query("DELETE FROM meal_log WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM meal_log WHERE date = :date AND slotId = :slotId")
    suspend fun deleteBySlot(date: String, slotId: Long)
}
