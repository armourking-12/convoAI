package com.duddleTech.convoAI

import androidx.room.Entity
import androidx.room.PrimaryKey

// Database Entity (For Room Database)
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val localImageUri: String? = null // Saves the path to the image on phone
)