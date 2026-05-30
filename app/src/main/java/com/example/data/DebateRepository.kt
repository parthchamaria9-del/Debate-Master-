package com.example.data

import kotlinx.coroutines.flow.Flow

class DebateRepository(private val debateDao: DebateDao) {
    val allSpeechPractices: Flow<List<SpeechPractice>> = debateDao.getAllSpeechPractices()
    val allDebateBattles: Flow<List<DebateBattle>> = debateDao.getAllDebateBattles()
    val userProfile: Flow<UserProfile?> = debateDao.getUserProfile()

    suspend fun insertSpeechPractice(practice: SpeechPractice) {
        debateDao.insertSpeechPractice(practice)
    }

    suspend fun insertDebateBattle(battle: DebateBattle) {
        debateDao.insertDebateBattle(battle)
    }

    suspend fun insertUserProfile(profile: UserProfile) {
        debateDao.insertUserProfile(profile)
    }
}
