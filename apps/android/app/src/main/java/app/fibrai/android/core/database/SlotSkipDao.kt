package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SlotSkipDao {
    @Query("SELECT * FROM slot_skip WHERE date = :date")
    fun observeByDate(date: String): Flow<List<SlotSkipEntity>>

    @Query("SELECT * FROM slot_skip WHERE date = :date")
    suspend fun getByDate(date: String): List<SlotSkipEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: SlotSkipEntity)

    @Query("DELETE FROM slot_skip WHERE date = :date AND slotId = :slotId")
    suspend fun delete(date: String, slotId: Long)

    @Query("DELETE FROM slot_skip WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
