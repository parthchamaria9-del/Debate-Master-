package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_practices")
data class SpeechPractice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val ageGroup: String,
    val tone: String,
    val generatedSpeech: String,
    val userSpeech: String,
    val score: Int,
    val feedbackJson: String, // JSON mapping of feedback categories and suggestions
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debate_battles")
data class DebateBattle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val difficulty: String,
    val language: String,
    val side: String, // "FOR / पक्ष" or "AGAINST / विपक्ष"
    val chatHistoryJson: String, // Chat list in JSON
    val score: Int,
    val reportJson: String, // Evaluation report in JSON
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val ageGroup: String, // "Child (8-12)", "Teen (13-17)", "Adult (18+)"
    val language: String, // "English" or "Hindi"
    val level: Int = 1,
    val points: Int = 0,
    val badgesJson: String = "[]", // String list of badges
    val streak: Int = 0,
    val lastActiveTimestamp: Long = 0L
)
