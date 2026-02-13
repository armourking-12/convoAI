package com.duddleTech.convoAI

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ Add 'ChatMessage' to the entities list!
@Database(entities = [ChatSession::class, ChatMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration() // ✅ This deletes old data if schema changes (prevents crashes)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}