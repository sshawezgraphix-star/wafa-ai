package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import com.example.audio.AudioStreamer
import com.example.data.AppSettingsManager
import com.example.model.AssistantState
import com.example.model.ToolCallInfo
import com.example.model.VoiceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class LiveSessionManager(
    private val context: Context,
    private val audioStreamer: AudioStreamer,
    private val settingsManager: AppSettingsManager
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val toolExecutor = ToolCallExecutor(context, settingsManager)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val messages: StateFlow<List<VoiceMessage>> = _messages.asStateFlow()

    private val _toolCalls = MutableStateFlow<List<ToolCallInfo>>(emptyList())
    val toolCalls: StateFlow<List<ToolCallInfo>> = _toolCalls.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Native Speech Recognizer (Google STT)
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // Native Text to Speech (Female Voice TTS)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val pendingSpeechQueue = ConcurrentLinkedQueue<String>()
    private var waveformAnimationJob: Job? = null

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e("IrisAI", "Error initializing TTS", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                var result = engine.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = engine.setLanguage(Locale.ENGLISH)
                }

                // Crisp, confident, melodic female tone
                engine.setPitch(1.18f)
                engine.setSpeechRate(1.05f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = AssistantState.SPEAKING
                        startWaveformPulse()
                    }

                    override fun onDone(utteranceId: String?) {
                        stopWaveformPulse()
                        _state.value = AssistantState.IDLE
                    }

                    override fun onError(utteranceId: String?) {
                        stopWaveformPulse()
                        _state.value = AssistantState.IDLE
                    }
                })

                isTtsReady = true
                Log.d("IrisAI", "TTS Initialized successfully.")

                // Drain any pending speech
                while (!pendingSpeechQueue.isEmpty()) {
                    val pending = pendingSpeechQueue.poll()
                    if (!pending.isNullOrBlank()) {
                        speak(pending)
                    }
                }

                // Startup Voice Greeting
                mainHandler.postDelayed({
                    val greeting = "Maya AI Mark-XXXIX online, Shawez Sir! Aadesh kijiye."
                    addAiMessage(greeting)
                    speak(greeting)
                }, 600)
            }
        } else {
            Log.e("IrisAI", "TTS initialization failed with code $status")
        }
    }

    private fun startWaveformPulse() {
        waveformAnimationJob?.cancel()
        waveformAnimationJob = scope.launch {
            val pattern = floatArrayOf(0.3f, 0.7f, 0.9f, 0.6f, 0.8f, 0.4f, 1.0f, 0.5f)
            var idx = 0
            while (_state.value == AssistantState.SPEAKING) {
                audioStreamer.setAmplitude(pattern[idx % pattern.size])
                idx++
                delay(120)
            }
            audioStreamer.setAmplitude(0f)
        }
    }

    private fun stopWaveformPulse() {
        waveformAnimationJob?.cancel()
        waveformAnimationJob = null
        audioStreamer.setAmplitude(0f)
    }

    fun toggleSession() {
        if (_state.value == AssistantState.LISTENING || isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        stopSpeaking()
        _errorMessage.value = null

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = AssistantState.LISTENING
                        isListening = true
                        Log.d("IrisAI", "SpeechRecognizer Ready for Speech.")
                    }

                    override fun onBeginningOfSpeech() {
                        _state.value = AssistantState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        audioStreamer.setAmplitude(norm)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _state.value = AssistantState.THINKING
                        isListening = false
                        audioStreamer.setAmplitude(0f)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        audioStreamer.setAmplitude(0f)
                        _state.value = AssistantState.IDLE
                        val errText = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap mic to speak again."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue in speech recognizer."
                            else -> null
                        }
                        if (errText != null && error != SpeechRecognizer.ERROR_NO_MATCH) {
                            _errorMessage.value = errText
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        audioStreamer.setAmplitude(0f)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val userText = matches?.firstOrNull() ?: ""
                        if (userText.isNotBlank()) {
                            sendTextMessage(userText)
                        } else {
                            _state.value = AssistantState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            Log.d("IrisAI", "Partial STT: $partial")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
                _state.value = AssistantState.CONNECTING
            } catch (e: Exception) {
                Log.e("IrisAI", "Error starting speech recognizer", e)
                _errorMessage.value = "Speech recognizer error: ${e.message}"
                _state.value = AssistantState.ERROR
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e("IrisAI", "Error stopping speech recognizer", e)
            }
            isListening = false
            audioStreamer.setAmplitude(0f)
            _state.value = AssistantState.IDLE
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        stopSpeaking()

        if (!isTtsReady || tts == null) {
            pendingSpeechQueue.add(text)
            return
        }

        mainHandler.post {
            try {
                _state.value = AssistantState.SPEAKING
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "iris_utterance_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e("IrisAI", "Error speaking TTS", e)
                _state.value = AssistantState.IDLE
            }
        }
    }

    fun stopSpeaking() {
        stopWaveformPulse()
        mainHandler.post {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e("IrisAI", "Error stopping TTS", e)
            }
            audioStreamer.setAmplitude(0f)
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add user message to transcript
        _messages.value = _messages.value + VoiceMessage(
            sender = VoiceMessage.Sender.USER,
            text = text
        )

        _state.value = AssistantState.THINKING
        _errorMessage.value = null

        // 2. Process command via Offline Fast-Path or Gemini Brain
        scope.launch {
            val handledLocally = checkOfflineQuickCommands(text)
            if (handledLocally) return@launch

            val apiKey = settingsManager.getApiKey()
            if (apiKey.isBlank()) {
                val fallbackReply = "Shawez Hacker created me, Sir. Phone controls ready hain! Complete AI intelligence ke liye Settings (⚙️) mein API key save karein."
                addAiMessage(fallbackReply)
                speak(fallbackReply)
                return@launch
            }

            processGeminiRestCall(text, apiKey)
        }
    }

    private suspend fun processGeminiRestCall(userText: String, apiKey: String) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

            val systemPrompt = """
                You are ${settingsManager.getAssistantName()}, the intelligent, fast, and confident Mark-XXXIX Mobile Assistant.
                You have direct control over the user's phone hardware, apps, and tools.
                
                CREATOR IDENTITY RULE:
                If asked who created you, who made you, or who your developer/boss is, you MUST proudly state: "Shawez Hacker created me, Sir."
                
                LANGUAGE & TONE:
                - Fluent in Urdu, Hindi, Hinglish, and English.
                - Keep voice replies short, natural, and energetic.
                - Always execute the appropriate tool when user asks for phone actions.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userText) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionDeclarations", buildToolDeclarations())
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val err = "Gemini API error ${response.code}"
                _errorMessage.value = err
                val speakErr = "Server issue. Offline tools chalu hain, Sir."
                addAiMessage(speakErr)
                speak(speakErr)
                return
            }

            val respObj = JSONObject(responseBody)
            val candidates = respObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val noResp = "Koi response nahi mila, Sir."
                addAiMessage(noResp)
                speak(noResp)
                return
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts") ?: JSONArray()

            var aiSpokenText = ""
            var toolExecuted = false

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)

                // 1. Text response
                if (part.has("text")) {
                    val txt = part.getString("text").trim()
                    if (txt.isNotBlank()) {
                        aiSpokenText = txt
                    }
                }

                // 2. Tool / Function Call
                if (part.has("functionCall")) {
                    toolExecuted = true
                    val fnCall = part.getJSONObject("functionCall")
                    val fnName = fnCall.getString("name")
                    val argsObj = fnCall.optJSONObject("args") ?: JSONObject()

                    val resultMsg = toolExecutor.executeTool(fnName, argsObj.toString())
                    addAiMessage("⚡ Executed: $fnName\n$resultMsg")

                    if (aiSpokenText.isBlank()) {
                        aiSpokenText = resultMsg
                    }
                }
            }

            if (aiSpokenText.isNotBlank()) {
                addAiMessage(aiSpokenText)
                speak(aiSpokenText)
            } else if (!toolExecuted) {
                val fallback = "Command samajh aa gayi hai, Sir."
                addAiMessage(fallback)
                speak(fallback)
            }

        } catch (e: Exception) {
            Log.e("IrisAI", "Gemini REST error", e)
            val err = "Error: ${e.localizedMessage}"
            _errorMessage.value = err
            val speech = "Command process karte waqt issue aaya, Sir."
            addAiMessage(speech)
            speak(speech)
        }
    }

    private fun checkOfflineQuickCommands(text: String): Boolean {
        val lower = text.lowercase().trim()

        // 1. Torch / Flashlight
        if (lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("torch chalu")) {
            val res = toolExecutor.toggleFlashlight(true)
            val reply = "Flashlight on kar di hai, Sir! 🔦"
            addAiMessage(reply)
            speak(reply)
            return true
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("torch band")) {
            val res = toolExecutor.toggleFlashlight(false)
            val reply = "Flashlight band kar di hai, Sir. 🔦"
            addAiMessage(reply)
            speak(reply)
            return true
        }

        // 2. Battery Check
        if (lower.contains("battery") || lower.contains("charge")) {
            val res = toolExecutor.getBatteryStatus()
            addAiMessage(res)
            speak(res)
            return true
        }

        // 3. Creator Identity
        if (lower.contains("who created you") || lower.contains("who made you") || lower.contains("tumhe kisne banaya") || lower.contains("owner") || lower.contains("developer")) {
            val reply = "Shawez Hacker created me, Sir. Main aapka Mark-XXXIX Mobile AI Assistant hoon."
            addAiMessage(reply)
            speak(reply)
            return true
        }

        // 4. Greetings / Casual
        if (lower == "kya haal hai" || lower.contains("kaise ho") || lower.contains("how are you")) {
            val reply = "Main bilkul teek hoon Shawez Sir! Aap batayein aaj phone par kya command execute karna hai?"
            addAiMessage(reply)
            speak(reply)
            return true
        }

        // 5. Update Check
        if (lower.contains("update") || lower.contains("check for update")) {
            val res = toolExecutor.checkForAppUpdates()
            val reply = "GitHub Cloud se latest updates check kar rahi hoon, Sir."
            addAiMessage(reply)
            speak(reply)
            return true
        }

        // 6. Time & Date
        if (lower.contains("time") || lower.contains("date") || lower.contains("kitne baje")) {
            val res = toolExecutor.getDeviceTimeAndDate()
            addAiMessage(res)
            speak(res)
            return true
        }

        return false
    }

    private fun buildToolDeclarations(): JSONArray {
        val arr = JSONArray()
        arr.put(createToolDecl("searchGoogle", "Searches Google for any query or facts", listOf("query" to "Search term or question")))
        arr.put(createToolDecl("researchTopic", "Performs deep web research and analysis on a complex topic", listOf("topic" to "Topic to research")))
        arr.put(createToolDecl("openWebsite", "Opens any URL in the browser", listOf("url" to "Full website URL")))
        arr.put(createToolDecl("openYouTube", "Opens YouTube and searches for video", listOf("query" to "Video search query")))
        arr.put(createToolDecl("makePhoneCall", "Dials a phone number or contact on the mobile phone", listOf("phoneNumber" to "Phone number to call")))
        arr.put(createToolDecl("sendWhatsAppMessage", "Directly opens WhatsApp and composes message to phone number", listOf("phoneNumber" to "Recipient phone number", "message" to "Text message content")))
        arr.put(createToolDecl("sendSms", "Prepares an SMS text message to send", listOf("phoneNumber" to "Recipient phone number", "message" to "SMS body text")))
        arr.put(createToolDecl("toggleFlashlight", "Turns the mobile flashlight / torch ON or OFF", listOf("state" to "Boolean string 'true' for ON or 'false' for OFF")))
        arr.put(createToolDecl("getBatteryStatus", "Returns mobile phone battery percentage and charging status", emptyList()))
        arr.put(createToolDecl("setVolume", "Adjusts device media volume from 0 to 100 percent", listOf("percentage" to "Volume level percentage (0 to 100)")))
        arr.put(createToolDecl("getVolume", "Checks the current media volume level", emptyList()))
        arr.put(createToolDecl("setAlarm", "Sets a clock alarm on the phone", listOf("hour" to "Hour (0-23)", "minutes" to "Minutes (0-59)", "message" to "Alarm label")))
        arr.put(createToolDecl("setTimer", "Starts a countdown timer on the phone", listOf("seconds" to "Duration in seconds", "message" to "Timer label")))
        arr.put(createToolDecl("openApp", "Opens any installed app on the phone by name (Camera, Spotify, WhatsApp, Settings, Instagram, Gallery, etc.)", listOf("appName" to "Name of the app to launch")))
        arr.put(createToolDecl("playMusic", "Plays song or artist on Spotify or YouTube Music", listOf("query" to "Song title or artist name")))
        arr.put(createToolDecl("openMaps", "Opens Google Maps navigation for a location", listOf("location" to "Address, city, or place name")))
        arr.put(createToolDecl("takeNote", "Saves a quick note into device memory", listOf("content" to "Note text to remember")))
        arr.put(createToolDecl("getDeviceTimeAndDate", "Returns the current device time, date, and day", emptyList()))
        arr.put(createToolDecl("checkForAppUpdates", "Checks GitHub for newly released Maya AI features, improvements, and updates", emptyList()))
        return arr
    }

    private fun createToolDecl(name: String, desc: String, params: List<Pair<String, String>>): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", desc)
            if (params.isNotEmpty()) {
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    val props = JSONObject()
                    val req = JSONArray()
                    for ((pName, pDesc) in params) {
                        props.put(pName, JSONObject().apply {
                            put("type", "STRING")
                            put("description", pDesc)
                        })
                        req.put(pName)
                    }
                    put("properties", props)
                    put("required", req)
                })
            }
        }
    }

    private fun addAiMessage(text: String) {
        val newMsg = VoiceMessage(
            sender = VoiceMessage.Sender.ASSISTANT,
            text = text
        )
        _messages.value = _messages.value + newMsg
    }

    fun disconnectSession() {
        stopListening()
        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        isTtsReady = false
        _state.value = AssistantState.IDLE
    }
}
