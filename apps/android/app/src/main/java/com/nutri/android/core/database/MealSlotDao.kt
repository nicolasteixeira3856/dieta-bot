package com.nutri.android.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MealSlotDao {
    @Query("SELECT * FROM meal_slot ORDER BY sortOrder ASC, minutesFromMidnight ASC")
    fun observeAll(): Flow<List<MealSlotEntity>>

    @Query("SELECT * FROM meal_slot ORDER BY sortOrder ASC, minutesFromMidnight ASC")
    suspend fun getAll(): List<MealSlotEntity>

    /** Upsert never deletes the row, so meal_log.slotId of a kept slot survives. */
    @Upsert
    suspend fun upsert(row: MealSlotEntity): Long

    @Query("DELETE FROM meal_slot WHERE id NOT IN (:keep)")
    suspend fun deleteAllExcept(keep: List<Long>)
}
