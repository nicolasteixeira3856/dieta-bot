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
}
