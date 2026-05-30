package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebateDao {
    @Query("SELECT * FROM speech_practices ORDER BY timestamp DESC")
    fun getAllSpeechPractices(): Flow<List<SpeechPractice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeechPractice(practice: SpeechPractice)

    @Query("SELECT * FROM debate_battles ORDER BY timestamp DESC")
    fun getAllDebateBattles(): Flow<List<DebateBattle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebateBattle(battle: DebateBattle)

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)
}
