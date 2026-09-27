package com.nutri.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DayDigestDao {
    @Query("SELECT * FROM day_digest WHERE date = :date ORDER BY seq ASC")
    suspend fun getByDate(date: String): List<DayDigestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DayDigestEntity)

    @Query("DELETE FROM day_digest WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
