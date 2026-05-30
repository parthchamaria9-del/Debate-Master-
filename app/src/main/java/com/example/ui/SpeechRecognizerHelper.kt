package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun startListening(languageCode: String = "en-US") {
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("SpeechHelper", "Ready for speech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d("SpeechHelper", "Speech beginning")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            onRmsChanged(rmsdB)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched. Try speaking closer to mic."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy or already recording."
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
                                else -> "Microphone error. You can type your input!"
                            }
                            onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onResult(matches[0])
                            } else {
                                onError("Could not recognize speech.")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (languageCode == "Hindi") "hi-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (languageCode == "Hindi") "hi-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, if (languageCode == "Hindi") "hi-IN" else "en-US")
            }

            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Failed starting speech recognizer", e)
            onError("System speech not available. Standard keyboard entry is fully enabled!")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error stopping helper", e)
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
