package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A71 (ADR-057 decision 7, v16): one answer of the conversational onboarding, written as it is given, so a closed app
 * reopens the chat at the same step. [step]: the [app.fibrai.android.domain.OnboardingStep] name; [value]: the answer
 * as JSON ([app.fibrai.android.domain.OnboardingAnswer]). Emptied on success, on reset and on reinstall.
 */
@Entity(tableName = "onboarding_answer")
data class OnboardingAnswerEntity(
    @PrimaryKey val step: String,
    val value: String,
    val createdAtEpochMs: Long,
)

@Dao
interface OnboardingAnswerDao {
    @Query("SELECT * FROM onboarding_answer ORDER BY createdAtEpochMs, step")
    fun observeAll(): Flow<List<OnboardingAnswerEntity>>

    @Query("SELECT * FROM onboarding_answer ORDER BY createdAtEpochMs, step")
    suspend fun getAll(): List<OnboardingAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: OnboardingAnswerEntity)

    @Query("DELETE FROM onboarding_answer WHERE step = :step")
    suspend fun delete(step: String)

    @Query("DELETE FROM onboarding_answer")
    suspend fun clear()
}
