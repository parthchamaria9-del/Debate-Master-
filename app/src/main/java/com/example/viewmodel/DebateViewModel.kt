package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.GeminiService
import com.example.ui.Localization
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DebateViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DebateViewModel"
    private val repository: DebateRepository

    // Screen navigation state
    var currentScreen by mutableStateOf("dashboard")
        private set

    // Current app wide language state (synced with Localization.currentLanguage)
    var currentLanguage by mutableStateOf("English")
        private set

    // Room Database Observables
    val userProfile: StateFlow<UserProfile?>
    val speechPractices: StateFlow<List<SpeechPractice>>
    val debateBattles: StateFlow<List<DebateBattle>>

    // --- Speech Practice UI States ---
    var selectedAgeGroup by mutableStateOf("Teen (13-17)")
    var selectedTone by mutableStateOf("Persuasive")
    var selectedLength by mutableStateOf("Medium")
    var enteredTopic by mutableStateOf("Why protecting wild forests matters")
    var generatedSpeechText by mutableStateOf("")
    var userSpeechInput by mutableStateOf("")
    
    var isSpeechGenerating by mutableStateOf(false)
    var isAnalyzingSpeech by mutableStateOf(false)
    var speechAnalysisResultJson by mutableStateOf<String?>(null)

    // --- Live Debate Battle UI States ---
    var debateTopic by mutableStateOf("Is artificial intelligence better than human teachers?")
    var debateSide by mutableStateOf("FOR / Pro-Topic")
    var debateDifficulty by mutableStateOf("Intermediate")
    var isBattleStarted by mutableStateOf(false)
    var isOpponentSpeaking by mutableStateOf(false)
    var isEvaluatingBattle by mutableStateOf(false)
    
    // Chat logs format: List of Map with keys "sender" (user, opponent, referee) and "message"
    var debateChatList by mutableStateOf<List<Map<String, String>>>(emptyList())
    var currentCoachHint by mutableStateOf("")
    var battleAdjudicationJson by mutableStateOf<String?>(null)

    // --- Topic Generator UI States ---
    var selectedCategory by mutableStateOf("Tech & AI")
    var selectedLevel by mutableStateOf("Intermediate")
    var generatedTopicsList by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var isGeneratingTopics by mutableStateOf(false)

    init {
        val database = DebateDatabase.getDatabase(application)
        val dao = database.debateDao()
        repository = DebateRepository(dao)

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        speechPractices = repository.allSpeechPractices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        debateBattles = repository.allDebateBattles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate default user profile if database is empty
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                if (profile == null) {
                    val defaultProfile = UserProfile(
                        name = "Junior Speaker",
                        ageGroup = "Teen (13-17)",
                        language = "English",
                        level = 1,
                        points = 10,
                        badgesJson = "[]",
                        streak = 1,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                    repository.insertUserProfile(defaultProfile)
                } else {
                    currentLanguage = profile.language
                    Localization.currentLanguage = profile.language
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        currentScreen = screen
    }

    fun toggleLanguage() {
        val nextLang = if (currentLanguage == "English") "Hindi" else "English"
        currentLanguage = nextLang
        Localization.currentLanguage = nextLang
        
        // Update user profile representation
        viewModelScope.launch {
            val oldProfile = userProfile.value
            if (oldProfile != null) {
                repository.insertUserProfile(oldProfile.copy(language = nextLang))
            }
        }
    }

    /**
     * Speach Coach: Generates draft script from Coach Arya
     */
    fun startGenerateSpeech() {
        if (enteredTopic.isBlank()) return
        isSpeechGenerating = true
        viewModelScope.launch {
            try {
                val script = GeminiService.generateSpeech(
                    topic = enteredTopic,
                    ageGroup = selectedAgeGroup,
                    tone = selectedTone,
                    length = selectedLength,
                    language = currentLanguage
                )
                generatedSpeechText = script
                userSpeechInput = script // Pre-populate as default so the user can rehearse it or edit it
            } catch (e: Exception) {
                Log.e(TAG, "Speech generation failed", e)
            } finally {
                isSpeechGenerating = false
            }
        }
    }

    /**
     * Speech Coach: Evaluates typed or recorded transcript
     */
    fun startEvaluateSpeech() {
        if (userSpeechInput.isBlank()) return
        isAnalyzingSpeech = true
        speechAnalysisResultJson = null
        viewModelScope.launch {
            try {
                val evaluation = GeminiService.evaluateSpeech(
                    topic = enteredTopic,
                    transcript = userSpeechInput,
                    ageGroup = selectedAgeGroup,
                    language = currentLanguage
                )
                speechAnalysisResultJson = evaluation
                
                // Parse metrics to award XP points
                try {
                    val jsonObj = JSONObject(evaluation)
                    val overallScore = jsonObj.optInt("score", 70)
                    
                    // Save practice history to Room database
                    val item = SpeechPractice(
                        topic = enteredTopic,
                        ageGroup = selectedAgeGroup,
                        tone = selectedTone,
                        generatedSpeech = generatedSpeechText,
                        userSpeech = userSpeechInput,
                        score = overallScore,
                        feedbackJson = evaluation
                    )
                    repository.insertSpeechPractice(item)
                    
                    // Reward base XP
                    rewardPointsAndCheckBadges(
                        pointsGained = overallScore / 2 + 20, 
                        isSpeechCategory = true, 
                        scoreObj = jsonObj
                    )
                } catch (pe: Exception) {
                    Log.e(TAG, "Error logging speech history save", pe)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Speech evaluation failed", e)
            } finally {
                isAnalyzingSpeech = false
            }
        }
    }

    fun loadPresetText(type: String) {
        val isHindi = currentLanguage == "Hindi"
        when (type) {
            "vivekananda" -> {
                enteredTopic = if (isHindi) "स्वामी विवेकानंद का शिकागो भाषण" else "Swami Vivekananda Chicago Oratory"
                userSpeechInput = if (isHindi) {
                    "अमेरिका के मेरे भाइयों और बहनों, आपने जिस सौहार्द और स्नेह के साथ हमारा स्वागत किया है, उससे मेरा दिल भर आया है। मैं आपको दुनिया की सबसे प्राचीन संन्यासियों की परंपरा की ओर से धन्यवाद देता हूँ। मैं आपको सभी धर्मों की जननी की ओर से धन्यवाद देता हूँ और सभी जातियों और संप्रदायों के लाखों-करोड़ों हिंदुओं की ओर से धन्यवाद देता हूँ। धन्यवाद!"
                } else {
                    "Sisters and Brothers of America, It fills my heart with joy unspeakable to rise in response to the warm and cordial welcome which you have given us. I thank you in the name of the most ancient order of monks in the world; I thank you in the name of the mother of religions, and I thank you in the name of millions and millions of Hindu people of all classes and sects!"
                }
            }
            "homework" -> {
                enteredTopic = if (isHindi) "क्या बच्चों को होमवर्क मिलना चाहिए?" else "Should children get homework?"
                userSpeechInput = if (isHindi) {
                    "आदरणीय अध्यक्ष जी और मेरे प्यारे दोस्तों। आज मैं इस बात के पक्ष में बोलने आया हूँ कि स्कूली बच्चों को बहुत अधिक होमवर्क नहीं मिलना चाहिए। बच्चों को खेलने, कला सीखने और अपने माता-पिता के साथ समय बिताने की भी जरूरत होती है। होमवर्क उन्हें थका देता है और उनके बचपन की रचनात्मकता को दबा देता है। धन्यवाद!"
                } else {
                    "Respected judges, teachers, and friends. Today I want to argue that school children should not be overloaded with homework. Children need time to play outdoors, learn sports, and bond with parents. Excessive homework creates massive pressure and takes the joy out of learning."
                }
            }
            "nature" -> {
                enteredTopic = if (isHindi) "पर्यावरण की रक्षा हमारा कर्तव्य है" else "Duty to protect the environment"
                userSpeechInput = if (isHindi) {
                    "नमस्ते। पृथ्वी हमारा घर है और पेड़ इसके रखवाले हैं। आज हम ग्लोबल वार्मिंग और प्लास्टिक प्रदूषण के संकट का सामना कर रहे हैं। यदि हम आज पेड़ नहीं लगाएंगे और पानी नहीं बचाएंगे, तो हमारी आने वाली पीढ़ियों के पास जीने के लिए सुरक्षित धरती नहीं होगी। आइये हम सब मिलकर संकल्प लें कि प्लास्टिक का कम से कम उपयोग करेंगे।"
                } else {
                    "Hello everyone. The Earth is our only home, and nature provides us with water, air, and soil. Today, plastics and carbon emissions are choking our oceans and raising global temperatures. We must pledge to plant trees, refuse single-use plastics, and preserve rainwater. It is our collective duty to cure our planet."
                }
            }
        }
    }

    // --- Live Debate Battle Logic ---
    fun startDebateBattle() {
        isBattleStarted = true
        isOpponentSpeaking = false
        battleAdjudicationJson = null
        currentCoachHint = ""
        
        // Setup initial introductory messages from Coach Arya (Referee)
        val initialRefereeMsg = mapOf(
            "sender" to "referee",
            "message" to if (currentLanguage == "Hindi") {
                "🏆 वाद-विवाद प्रतियोगिता शुरू हो चुकी है! विषय: '$debateTopic'\nआप '$debateSide' का समर्थन कर रहे हैं। आपके विरोधी भी तैयार हैं!"
            } else {
                "🏆 The Debate Ring is ready! Topic: '$debateTopic'\nYou are arguing '$debateSide'. Your AI opponent is loaded and waiting for your argument!"
            }
        )
        debateChatList = listOf(initialRefereeMsg)
        
        // If user is AGAINST, the AI Opponent (FOR) starts first!
        val isUserFor = debateSide.contains("FOR") || debateSide.contains("पक्ष")
        if (!isUserFor) {
            triggerOpponentTurn()
        }
    }

    fun submitUserArgument(argumentText: String) {
        if (argumentText.isBlank() || isOpponentSpeaking) return
        
        // Add user statement to list
        val userMsg = mapOf("sender" to "user", "message" to argumentText)
        debateChatList = debateChatList + userMsg
        
        // Trigger opponent response
        triggerOpponentTurn()
    }

    private fun triggerOpponentTurn() {
        isOpponentSpeaking = true
        viewModelScope.launch {
            try {
                // Compile past history as JSON for Gemini context
                val historyArr = JSONArray()
                debateChatList.filter { it["sender"] != "referee" }.forEach {
                    val obj = JSONObject()
                    obj.put("sender", it["sender"])
                    obj.put("message", it["message"])
                    historyArr.put(obj)
                }

                val aiSide = if (debateSide.contains("FOR") || debateSide.contains("पक्ष")) "AGAINST" else "FOR"
                val responseJson = GeminiService.getDebateOpponentResponse(
                    topic = debateTopic,
                    side = debateSide,
                    difficulty = debateDifficulty,
                    language = currentLanguage,
                    chatHistoryJson = historyArr.toString()
                )

                try {
                    val responseObj = JSONObject(responseJson)
                    val opponentText = responseObj.optString("response", "I challenge your premise.")
                    val coachTip = responseObj.optString("hint", "Try targeting their assumptions.")
                    
                    Log.d(TAG, "Opponent statement response: $opponentText")
                    Log.d(TAG, "Arya Hint statement: $coachTip")

                    debateChatList = debateChatList + mapOf("sender" to "opponent", "message" to opponentText)
                    currentCoachHint = coachTip
                } catch (je: Exception) {
                    // Fallback to plain text response
                    debateChatList = debateChatList + mapOf("sender" to "opponent", "message" to responseJson)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed requesting opponent response", e)
            } finally {
                isOpponentSpeaking = false
            }
        }
    }

    fun concludeAndJudgeBattle() {
        if (!isBattleStarted) return
        isEvaluatingBattle = true
        battleAdjudicationJson = null
        viewModelScope.launch {
            try {
                val historyArr = JSONArray()
                debateChatList.filter { it["sender"] != "referee" }.forEach {
                    val obj = JSONObject()
                    obj.put("sender", it["sender"])
                    obj.put("message", it["message"])
                    historyArr.put(obj)
                }

                val result = GeminiService.judgeDebateBattle(
                    topic = debateTopic,
                    side = debateSide,
                    language = currentLanguage,
                    chatHistoryJson = historyArr.toString()
                )
                battleAdjudicationJson = result

                try {
                    val jsonObj = JSONObject(result)
                    val score = jsonObj.optInt("score", 70)
                    
                    // Persistent logging to database
                    val battleLog = DebateBattle(
                        topic = debateTopic,
                        difficulty = debateDifficulty,
                        language = currentLanguage,
                        side = debateSide,
                        chatHistoryJson = historyArr.toString(),
                        score = score,
                        reportJson = result
                    )
                    repository.insertDebateBattle(battleLog)

                    // Reward battle points
                    val wonMultiplier = if (jsonObj.optString("winner", "User") == "User") 40 else 10
                    rewardPointsAndCheckBadges(
                        pointsGained = (score / 2) + wonMultiplier,
                        isSpeechCategory = false,
                        scoreObj = jsonObj
                    )
                } catch (pe: Exception) {
                    Log.e(TAG, "Error inserting debate history", pe)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failing to evaluate debate match", e)
            } finally {
                isEvaluatingBattle = false
            }
        }
    }

    fun resetDebateState() {
        isBattleStarted = false
        debateChatList = emptyList()
        currentCoachHint = ""
        battleAdjudicationJson = null
    }

    // --- Topic Generator Logic ---
    fun startGenerateTopics() {
        isGeneratingTopics = true
        viewModelScope.launch {
            try {
                val responseJson = GeminiService.generateTopics(
                    category = selectedCategory,
                    level = selectedLevel,
                    language = currentLanguage
                )

                val arr = JSONArray(responseJson)
                val newList = mutableListOf<Map<String, Any>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val map = mutableMapOf<String, Any>()
                    map["title"] = obj.optString("title")
                    map["description"] = obj.optString("description")
                    
                    val prosArr = obj.optJSONArray("pros")
                    val prosList = mutableListOf<String>()
                    if (prosArr != null) {
                        for (p in 0 until prosArr.length()) {
                            prosList.add(prosArr.getString(p))
                        }
                    }
                    map["pros"] = prosList

                    val consArr = obj.optJSONArray("cons")
                    val consList = mutableListOf<String>()
                    if (consArr != null) {
                        for (c in 0 until consArr.length()) {
                            consList.add(consArr.getString(c))
                        }
                    }
                    map["cons"] = consList
                    
                    newList.add(map)
                }
                generatedTopicsList = newList
            } catch (e: Exception) {
                Log.e(TAG, "Topic generator failed query", e)
            } finally {
                isGeneratingTopics = false
            }
        }
    }

    fun useTopicInSpeechOrDebate(title: String, mode: String) {
        if (mode == "speech") {
            enteredTopic = title
            generatedSpeechText = ""
            userSpeechInput = ""
            speechAnalysisResultJson = null
            navigateTo("coach")
        } else {
            debateTopic = title
            resetDebateState()
            navigateTo("battle")
        }
    }

    // --- Daily Challenge trigger ---
    fun loadDailyChallenge() {
        val isHindi = currentLanguage == "Hindi"
        debateTopic = if (isHindi) "क्या स्कूल में गृहकार्य को पूरी तरह बैन कर देना चाहिए?" else "Should homework be banned for school pupils?"
        debateSide = if (isHindi) "पक्ष में (FOR)" else "FOR / Pro-Topic"
        resetDebateState()
        navigateTo("battle")
    }

    // --- Level Up, Point Rewarding & Badging ---
    private suspend fun rewardPointsAndCheckBadges(
        pointsGained: Int,
        isSpeechCategory: Boolean,
        scoreObj: JSONObject
    ) {
        val currentProfile = userProfile.value ?: return

        val newPoints = currentProfile.points + pointsGained
        // Every 250 points levels the user up!
        val newLevel = (newPoints / 250) + 1

        val currentBadgesList = JSONArray(currentProfile.badgesJson)
        val earnedBadgesSet = mutableSetOf<String>()
        for (i in 0 until currentBadgesList.length()) {
            earnedBadgesSet.add(currentBadgesList.getString(i))
        }

        // Evaluate badge requirements:
        // 1. Ice Breaker
        if (!earnedBadgesSet.contains("Ice Breaker")) {
            earnedBadgesSet.add("Ice Breaker")
        }
        // 2. Logic Guru
        val logicVal = scoreObj.optInt("logicScore", scoreObj.optInt("logic", 0))
        if (logicVal > 85 && !earnedBadgesSet.contains("Logic Guru")) {
            earnedBadgesSet.add("Logic Guru")
        }
        // 3. Persuasion Master
        val persuasionVal = scoreObj.optInt("persuasionScore", scoreObj.optInt("persuasion", 0))
        if (persuasionVal > 88 && !earnedBadgesSet.contains("Persuasion Master")) {
            earnedBadgesSet.add("Persuasion Master")
        }
        // 4. Hindi Orator
        if (currentLanguage == "Hindi" && scoreObj.optInt("score", 0) > 80 && !earnedBadgesSet.contains("Hindi Orator")) {
            earnedBadgesSet.add("Hindi Orator")
        }
        // 5. Debate Champion
        if (!isSpeechCategory && scoreObj.optString("winner") == "User" && debateDifficulty == "Advanced" && !earnedBadgesSet.contains("Debate Champion")) {
            earnedBadgesSet.add("Debate Champion")
        }
        // 6. Silver Tongue
        if (newPoints >= 500 && !earnedBadgesSet.contains("Silver Tongue")) {
            earnedBadgesSet.add("Silver Tongue")
        }

        // Updated profile representation
        val updatedProfile = currentProfile.copy(
            points = newPoints,
            level = newLevel,
            badgesJson = JSONArray(earnedBadgesSet.toList()).toString(),
            streak = if (System.currentTimeMillis() - currentProfile.lastActiveTimestamp < 30 * 60 * 60 * 1000) {
                currentProfile.streak // within same day boundaries approximately
            } else {
                currentProfile.streak + 1
            },
            lastActiveTimestamp = System.currentTimeMillis()
        )

        repository.insertUserProfile(updatedProfile)
    }
}
