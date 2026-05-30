package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SpeechPractice::class, DebateBattle::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
abstract class DebateDatabase : RoomDatabase() {
    abstract fun debateDao(): DebateDao

    companion object {
        @Volatile
        private var INSTANCE: DebateDatabase? = null

        fun getDatabase(context: Context): DebateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DebateDatabase::class.java,
                    "debate_master_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
