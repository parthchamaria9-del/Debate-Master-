package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Common method to call Google Gemini API
     */
    private suspend fun callGemini(
        prompt: String,
        systemInstruction: String? = null,
        isJsonOutput: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder!")
            return@withContext null
        }

        try {
            // Build request json
            val root = JSONObject()
            
            // Contents
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArr.put(partObj)
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            root.put("contents", contentsArr)

            // System Instructions
            if (systemInstruction != null) {
                val systemInstructionObj = JSONObject()
                val systemPartsArr = JSONArray()
                val systemPartObj = JSONObject()
                systemPartObj.put("text", systemInstruction)
                systemPartsArr.put(systemPartObj)
                systemInstructionObj.put("parts", systemPartsArr)
                root.put("systemInstruction", systemInstructionObj)
            }

            // Generation config
            val generationConfig = JSONObject()
            if (isJsonOutput) {
                generationConfig.put("responseMimeType", "application/json")
            }
            generationConfig.put("temperature", 0.7)
            root.put("generationConfig", generationConfig)

            val requestBodyStr = root.toString()
            Log.d(TAG, "Request payload: $requestBodyStr")

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = requestBodyStr.toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string()
                Log.d(TAG, "Response string code: ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(TAG, "Response failed: $responseStr")
                    return@withContext null
                }

                if (responseStr != null) {
                    val responseJson = JSONObject(responseStr)
                    val candidates = responseJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val responseContent = firstCandidate?.optJSONObject("content")
                    val parts = responseContent?.optJSONArray("parts")
                    val textResult = parts?.optJSONObject(0)?.optString("text")
                    return@withContext textResult
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini: ", e)
        }
        return@withContext null
    }

    /**
     * 1. Speech Generator
     */
    suspend fun generateSpeech(
        topic: String,
        ageGroup: String,
        tone: String,
        length: String,
        language: String
    ): String {
        val systemInstruction = """
            You are a highly expressive public speaking speech generator and elocution coach.
            Generate a full speech on the requested topic in the specified language (English or Hindi. If Hindi, use beautiful Devnagari script).
            Tailor the complexity and length according to:
            - Age Group: $ageGroup (Child 8-12 should be simple and cheerful; Teen 13-17 should be engaging and moderate; Adult 18+ should be deep and highly persuasive).
            - Tone: $tone (e.g. Persuasive, Informative, Humorous, Inspiring).
            - Length: $length (Short: ~1-2 mins, Medium: ~3 mins, Long: ~5 mins).
            Provide only the final speech script text directly. Do not add metadata, introduction paragraphs, or outer tags unless it is part of the spoken speech (like "Dear teachers and friends..." or "आदरणीय निर्णायक मंडल और मेरे प्रिय दोस्तों..."). Use paragraphs and clear transitions.
        """.trimIndent()

        val prompt = "Generate a $length speech in $language on the topic '$topic' with an $tone tone for $ageGroup."
        return callGemini(prompt, systemInstruction, isJsonOutput = false) 
            ?: "Failed to generate speech. Please check your internet connection."
    }

    /**
     * 2. Speech Evaluator / Coach (Text/Voice speech input)
     */
    suspend fun evaluateSpeech(
        topic: String,
        transcript: String,
        ageGroup: String,
        language: String
    ): String {
        val systemInstruction = """
            You are an expert speech evaluator and public speaking master coach.
            Analyze the user's speech transcript on the given topic. Provide a comprehensive critique and numeric evaluations in JSON output.
            Rate each category on a scale of 0 to 100 based on the user's target age group ($ageGroup) and language ($language).
            Return EXACTLY and ONLY a JSON object with this exact structure:
            {
               "score": 85, // Overall composite score
               "vocabularyScore": 80,
               "logicScore": 75,
               "grammarScore": 90,
               "persuasionScore": 82,
               "clarityScore": 88,
               "critique": "Overall encouraging summary of their speech performance.",
               "strongPoints": ["Point 1 detail", "Point 2 detail"],
               "improvementPoints": ["Suggestion 1 detail", "Suggestion 2 detail"],
               "exercises": ["Exercise 1 description to practice this specific weakness", "Exercise 2"]
            }
            
            Note: All textual explanations in 'critique', 'strongPoints', 'improvementPoints', and 'exercises' should be written in the specified language: $language (If Hindi, write in neat Hindi Devnagari script).
        """.trimIndent()

        val prompt = "Topic: $topic\nUser's Speech Transcript: $transcript"
        return callGemini(prompt, systemInstruction, isJsonOutput = true) 
            ?: "{}"
    }

    /**
     * 3. Topic Generator with Pros & Cons
     */
    suspend fun generateTopics(
        category: String,
        level: String,
        language: String
    ): String {
        val systemInstruction = """
            You are an expert debate adjudicator and topic curator.
            Generate exactly 4 distinct debate and public speaking topics for category: $category and difficulty level: $level.
            For each topic, provide the title, a brief educational description, 2 strong arguments in favor (pros), and 2 strong arguments against (cons).
            Return EXACTLY and ONLY a JSON array of objects with the structure:
            [
              {
                "title": "Title of topic",
                "description": "Short explanation of why this topic is relevant today.",
                "pros": ["Pro argument 1", "Pro argument 2"],
                "cons": ["Con argument 1", "Con argument 2"]
              }
            ]
            IMPORTANT: Ensure that if the language is 'Hindi', all the keys remain standard but the values (title, description, pros, cons) are written in beautiful Devnagari Hindi.
        """.trimIndent()

        val prompt = "Generate 4 topics on $category at $level level in $language language."
        return callGemini(prompt, systemInstruction, isJsonOutput = true) 
            ?: "[]"
    }

    /**
     * 4. Live Debate Battle Opponent Turn
     */
    suspend fun getDebateOpponentResponse(
        topic: String,
        side: String,
        difficulty: String,
        language: String,
        chatHistoryJson: String
    ): String {
        val systemInstruction = """
            You are the ultimate Live Debate Battle opponent.
            The user is debating you on the topic: "$topic".
            The user is standing: "$side" (FOR or AGAINST the topic).
            You MUST argue the OPPOSITE side with conviction and intelligence!
            Adapt your difficulty to: $difficulty (Beginner: friendly, simple arguments, helpful; Intermediate: standard debate level, clear rebuttals; Advanced: master logic, points out fallacies, cross-examines fiercely).
            Language of debate: $language (If Hindi, write in fluent, natural Hindi Devnagari script).
            
            Review the provided back-and-forth chat history. Respond with your next turn. Keep your speech length reasonable (100-200 words) so it feels like a live conversation.
            Also, provide a confidential coaching Tip/Hint to help the user counter your point.
            
            Return ONLY a valid JSON object matching this structure:
            {
               "response": "Your spoken debate argument including rebuttal to user, logical point, and critical question.",
               "hint": "A short, actionable hint/clue to help the user understand how to build their next rebuttal (e.g. 'Tip: Point out that safety features are cheaper than accidents' or Hindi equivalent if language is Hindi)."
            }
        """.trimIndent()

        val prompt = "Chat history logs so far:\n$chatHistoryJson\n\nGenerate the next opponent response."
        return callGemini(prompt, systemInstruction, isJsonOutput = true) 
            ?: "{}"
    }

    /**
     * 5. Live Debate Battle Adjudication / Rating Report
     */
    suspend fun judgeDebateBattle(
        topic: String,
        side: String,
        language: String,
        chatHistoryJson: String
    ): String {
        val systemInstruction = """
            You are a chief debate referee and master communication evaluator.
            The debate has completed. Read the entire chat history of the debate battle:
            Topic: "$topic"
            User argued: "$side"
            
            Examine the user's argument content, rebuttal efficiency, logic, persuasion, and formatting. Provide a fair score out of 100 and output a detailed analytical report.
            Return EXACTLY and ONLY a JSON object matching this structure:
            {
               "winner": "User" or "AI Opponent", // Decide based on quality of responses
               "score": 85, // Overall user score (0-100)
               "argumentsScore": 80,
               "logicScore": 75,
               "persuasionScore": 90,
               "rebuttalsScore": 82,
               "strengths": ["Clear opening thesis", "Refuted opponent's cost argument well"],
               "weaknesses": ["Missed opportunity to counter point X", "Used generalization fallacy in turn 3"],
               "missedOpportunities": ["Could have cited the environmental footprint", "Failed to attack the core premise of regulation"],
               "exercises": ["Check out logical fallacies exercise", "Practice spontaneous rebuttals for 2 minutes"],
               "badgeEarned": "Logic Guru" // Optional name of badge if earned, choose from: "Ice Breaker", "Logic Guru", "Persuasion Master", "Hindi Orator", "Debate Champion", "Silver Tongue"
            }
            
            Ensure the values are in $language ( Hindi or English ).
        """.trimIndent()

        val prompt = "Debate Chat Logs:\n$chatHistoryJson"
        return callGemini(prompt, systemInstruction, isJsonOutput = true) 
            ?: "{}"
    }
}
