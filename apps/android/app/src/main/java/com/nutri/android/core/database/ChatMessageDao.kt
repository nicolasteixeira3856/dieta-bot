package com.nutri.android.core.database

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

    /** Assistant rows still waiting for a tap (A34): Registrar or the inline Substituir. */
    @Query(
        "SELECT * FROM chat_message WHERE role = 'assistant' AND " +
            "(recordState = 'pending_replace' OR (recordMode = 'ask' AND recordState IS NULL AND estimateKcal IS NOT NULL))",
    )
    suspend fun getOpenRecords(): List<ChatMessageEntity>

    @Query("UPDATE chat_message SET recordState = :state, undoData = :undoData WHERE id = :id")
    suspend fun setRecordState(id: Long, state: String?, undoData: String?)

    @Query("UPDATE chat_message SET undoData = :undoData, memoryUpdated = :memoryUpdated WHERE id = :id")
    suspend fun setReceiptUndo(id: Long, undoData: String, memoryUpdated: Boolean)

    @Query("UPDATE chat_message SET receiptState = :state WHERE id = :id")
    suspend fun setReceiptState(id: Long, state: String)
}
