package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DayEntity?>

    @Query("SELECT * FROM day WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DayEntity)
}
