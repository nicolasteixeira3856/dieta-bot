package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    /** fromDate inclusive, ISO yyyy-MM-dd. */
    @Query("SELECT * FROM chat_message WHERE date >= :fromDate ORDER BY createdAtEpochMs ASC, id ASC")
    fun observeSince(fromDate: String): Flow<List<ChatMessageEntity>>

    /** Newest first: the Chat window (A32). fromDate inclusive; at most [limit] rows. */
    @Query("SELECT * FROM chat_message WHERE date >= :fromDate ORDER BY createdAtEpochMs DESC, id DESC LIMIT :limit")
    fun observeLatest(fromDate: String, limit: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message WHERE date = :date ORDER BY createdAtEpochMs ASC, id ASC")
    suspend fun getByDate(date: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_message WHERE id = :id")
    suspend fun getById(id: Long): ChatMessageEntity?

    @Insert
    suspend fun insert(row: ChatMessageEntity): Long

    /** Id of the latest row of [role] on [date] (A38: the wipe marker a compaction started after). */
    @Query("SELECT MAX(id) FROM chat_message WHERE date = :date AND role = :role")
    suspend fun latestIdOf(date: String, role: String): Long?

    /** Receipts of the 60 days (A34): which one is the latest of its slot. */
    @Query("SELECT * FROM chat_message WHERE date >= :fromDate AND role IN (:roles) ORDER BY createdAtEpochMs ASC, id ASC")
    suspend fun getReceiptsSince(fromDate: String, roles: List<String>): List<ChatMessageEntity>

    /** Assistant rows still waiting for a tap (A34): Registrar, the inline Substituir, Adicionar or Atualizar (A47). */
    @Query(
        "SELECT * FROM chat_message WHERE role = 'assistant' AND " +
            "(recordState IN ('pending_replace', 'pending_add', 'pending_revise') OR " +
            "(recordMode = 'ask' AND recordState IS NULL AND estimateKcal IS NOT NULL))",
    )
    suspend fun getOpenRecords(): List<ChatMessageEntity>

    /** A47: an open row (nothing decided or a pending confirmation) becomes [state]; a decided row stays. Rows changed. */
    @Query(
        "UPDATE chat_message SET recordState = :state WHERE id = :id AND " +
            "(recordState IS NULL OR recordState IN ('pending_replace', 'pending_add', 'pending_revise'))",
    )
    suspend fun closeOpenRecord(id: Long, state: String): Int

    /** A47: an open row's record state and proposal (a destination picked for an addition). Rows changed. */
    @Query(
        "UPDATE chat_message SET recordState = :state, mealChange = :mealChange WHERE id = :id AND " +
            "(recordState IS NULL OR recordState IN ('pending_add'))",
    )
    suspend fun setMealChange(id: Long, state: String?, mealChange: String): Int

    @Query("SELECT recordState FROM chat_message WHERE id = :id")
    suspend fun recordStateOf(id: Long): String?

    @Query("UPDATE chat_message SET recordState = :state, undoData = :undoData WHERE id = :id")
    suspend fun setRecordState(id: Long, state: String?, undoData: String?)

    @Query("UPDATE chat_message SET undoData = :undoData, memoryUpdated = :memoryUpdated WHERE id = :id")
    suspend fun setReceiptUndo(id: Long, undoData: String, memoryUpdated: Boolean)

    @Query("UPDATE chat_message SET receiptState = :state WHERE id = :id")
    suspend fun setReceiptState(id: Long, state: String)

    /** A60 part A: the plan budget of an answer and its local choice. */
    @Query("UPDATE chat_message SET planBudget = :planBudget WHERE id = :id")
    suspend fun setPlanBudget(id: Long, planBudget: String)

    /** A59: the skips of an answer. */
    @Query("SELECT skipOutcomes FROM chat_message WHERE id = :id")
    suspend fun skipOutcomesOf(id: Long): String?

    @Query("UPDATE chat_message SET skipOutcomes = :skipOutcomes WHERE id = :id")
    suspend fun setSkipOutcomes(id: Long, skipOutcomes: String)

    /** A59: answers with a delete proposal still waiting for Excluir e pular or Manter registro, any day. */
    @Query("SELECT * FROM chat_message WHERE role = 'assistant' AND skipOutcomes LIKE '%\"pending_delete\"%'")
    suspend fun getOpenSkips(): List<ChatMessageEntity>
}
