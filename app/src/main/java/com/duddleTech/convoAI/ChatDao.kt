package com.duddleTech.convoAI

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Entity
import androidx.room.PrimaryKey

// ✅ ChatSession Entity defined here for completeness
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val lastActive: Long = System.currentTimeMillis()
)
// ✅ ChatMessage Entity (Assuming this structure based on usage)
@Dao
interface ChatDao {
    // --- SESSIONS ---
    @Query("SELECT * FROM chat_sessions ORDER BY lastActive DESC")
    suspend fun getAllSessions(): List<ChatSession>

    // 🛠️ CRITICAL FIX: Added onConflict = OnConflictStrategy.REPLACE
    // This prevents the crash by updating the existing row instead of trying to create a duplicate.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET lastActive = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTime(sessionId: Long, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionReturnId(session: ChatSession): Long

    // --- MESSAGES ---
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessage>

    @Insert
    suspend fun insertMessage(message: ChatMessage)
}