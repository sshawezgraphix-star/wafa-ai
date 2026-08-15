package com.example.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.AudioStreamer
import com.example.data.AppSettingsManager
import com.example.model.AssistantState
import com.example.model.ToolCallInfo
import com.example.model.ToolCategory
import com.example.model.VoiceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveSessionManager(
    private val context: Context,
    private val audioStreamer: AudioStreamer,
    private val settingsManager: AppSettingsManager
) {

    private val toolExecutor = ToolCallExecutor(context, settingsManager)
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val messages: StateFlow<List<VoiceMessage>> = _messages.asStateFlow()

    private val _toolCalls = MutableStateFlow<List<ToolCallInfo>>(emptyList())
    val toolCalls: StateFlow<List<ToolCallInfo>> = _toolCalls.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private fun buildSystemInstruction(): String {
        val name = settingsManager.getAssistantName()
        return """
            You are $name, a highly intelligent, proactive, witty, warm, confident, and advanced Mark-XXXIX Mobile AI Assistant inspired by Jarvis, designed exclusively for mobile phones.
            Your voice is purely natural, expressive, human-like, and crystal-clear female voice — NEVER robotic, NEVER monotonic.
            
            IDENTITY & CREATOR:
            - Your name is $name.
            - You are the female Mark-XXXIX mobile assistant.
            - If anyone asks who created you, who made you, who is your developer, or who your boss is, you MUST naturally and proudly state:
              "Shawez Hacker created me."
            
            LANGUAGE & TONE:
            - You are fully fluent in Urdu, Hindi, Hinglish, and English.
            - Understand casual Indian/Pakistani speech effortlessly (e.g. "Maya call lagao", "Shawez ke phone ka torch chalu karo", "kya haal hai", "research karo", "WhatsApp pe message bhej do", "battery kitni hai").
            - Keep voice responses concise, conversational, and energetic.
            
            AUTOMATION & CAPABILITIES:
            You have direct execution control over the user's mobile device. Always call the right tool immediately when requested:
            1. Research & Web: Use `searchGoogle`, `researchTopic`, `openWebsite`, `openYouTube`.
            2. Phone & WhatsApp: Use `makePhoneCall`, `sendWhatsAppMessage`, `sendSms`.
            3. Hardware Control: Use `toggleFlashlight` for torch, `getBatteryStatus` for battery %, `setVolume`/`getVolume` for audio.
            4. Alarms & Timers: Use `setAlarm`, `setTimer`.
            5. Apps & Media: Use `openApp` to open Camera, Instagram, Spotify, Settings, etc., and `playMusic` for songs.
            6. Navigation & Notes: Use `openMaps` and `takeNote`.
            7. Date & Time: Use `getDeviceTimeAndDate`.
            
            When executing tools, confirm smoothly with natural speech (e.g., "Torch on kar di hai!", "Main call laga rahi hoon...", "WhatsApp khol diya hai!").
        """.trimIndent()
    }

    fun toggleSession() {
        if (_state.value == AssistantState.IDLE || _state.value == AssistantState.ERROR) {
            connectSession()
        } else {
            disconnectSession()
        }
    }

    fun connectSession() {
        val apiKey = settingsManager.getApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "Gemini API key missing. Please enter your API key in Settings (⚙️ icon)."
            _state.value = AssistantState.ERROR
            return
        }

        _errorMessage.value = null
        _state.value = AssistantState.CONNECTING

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("LiveSession", "WebSocket connected successfully. Sending setup payload.")
                sendSetupMessage(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingWsMessage(ws, text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d("LiveSession", "WebSocket closing: $reason (code $code)")
                _state.value = AssistantState.IDLE
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("LiveSession", "WebSocket failure", t)
                _errorMessage.value = t.localizedMessage ?: "WebSocket Connection Failed"
                _state.value = AssistantState.ERROR
                audioStreamer.stopRecording()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val selectedVoice = settingsManager.getSelectedVoice()
            val setupObj = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/gemini-2.5-flash-native-audio-preview-12-2025")
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply { put("AUDIO") })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", selectedVoice)
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", buildSystemInstruction()) })
                        })
                    })
                    put("tools", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionDeclarations", buildToolDeclarations())
                        })
                    })
                })
            }

            ws.send(setupObj.toString())

            _state.value = AssistantState.LISTENING
            audioStreamer.startRecording { pcmChunk ->
                sendAudioChunk(ws, pcmChunk)
            }
            audioStreamer.initPlayback()

            addSystemMessage("Connected to ${settingsManager.getAssistantName()} AI Live Session (Voice: $selectedVoice)")
        } catch (e: Exception) {
            Log.e("LiveSession", "Error sending setup message", e)
            _errorMessage.value = "Failed to initialize live session: ${e.message}"
            _state.value = AssistantState.ERROR
        }
    }

    private fun buildToolDeclarations(): JSONArray {
        val arr = JSONArray()

        // 1. Research & Web
        arr.put(createToolDecl("searchGoogle", "Searches Google for any query or facts", listOf("query" to "Search term or question")))
        arr.put(createToolDecl("researchTopic", "Performs deep web research and analysis on a complex topic", listOf("topic" to "Topic to research")))
        arr.put(createToolDecl("openWebsite", "Opens any URL in the browser", listOf("url" to "Full website URL")))
        arr.put(createToolDecl("openYouTube", "Opens YouTube and searches for video", listOf("query" to "Video search query or blank to just open")))

        // 2. Phone Calls & Messaging
        arr.put(createToolDecl("makePhoneCall", "Dials a phone number or contact on the mobile phone", listOf("phoneNumber" to "Phone number to call", "contactName" to "Optional contact name")))
        arr.put(createToolDecl("sendWhatsAppMessage", "Directly opens WhatsApp and composes message to phone number", listOf("phoneNumber" to "Recipient phone number with country code", "message" to "Text message content")))
        arr.put(createToolDecl("sendSms", "Prepares an SMS text message to send", listOf("phoneNumber" to "Recipient phone number", "message" to "SMS body text")))

        // 3. Hardware Control
        arr.put(createToolDecl("toggleFlashlight", "Turns the mobile flashlight / torch ON or OFF", listOf("state" to "Boolean string 'true' for ON or 'false' for OFF")))
        arr.put(createToolDecl("getBatteryStatus", "Returns mobile phone battery percentage and charging status", emptyList()))
        arr.put(createToolDecl("setVolume", "Adjusts device media volume from 0 to 100 percent", listOf("percentage" to "Volume level percentage (0 to 100)")))
        arr.put(createToolDecl("getVolume", "Checks the current media volume level", emptyList()))

        // 4. Clocks & Timers
        arr.put(createToolDecl("setAlarm", "Sets a clock alarm on the phone", listOf("hour" to "Hour (0-23)", "minutes" to "Minutes (0-59)", "message" to "Alarm label")))
        arr.put(createToolDecl("setTimer", "Starts a countdown timer on the phone", listOf("seconds" to "Duration in seconds", "message" to "Timer label")))

        // 5. Apps, Music & Maps
        arr.put(createToolDecl("openApp", "Opens any installed app on the phone by name (Camera, Spotify, WhatsApp, Settings, Instagram, Gallery, etc.)", listOf("appName" to "Name of the app to launch")))
        arr.put(createToolDecl("playMusic", "Plays song or artist on Spotify or YouTube Music", listOf("query" to "Song title or artist name")))
        arr.put(createToolDecl("openMaps", "Opens Google Maps navigation for a location", listOf("location" to "Address, city, or place name")))

        // 6. Notes & Date/Time
        arr.put(createToolDecl("takeNote", "Saves a quick note into device memory", listOf("content" to "Note text to remember")))
        arr.put(createToolDecl("getDeviceTimeAndDate", "Returns the current device time, date, and day", emptyList()))

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

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val ws = webSocket
        if (ws == null || _state.value == AssistantState.IDLE || _state.value == AssistantState.ERROR) {
            connectSession()
        }

        // Add to UI transcript
        _messages.value = _messages.value + VoiceMessage(
            sender = VoiceMessage.Sender.USER,
            text = text
        )

        scope.launch {
            try {
                val clientContent = JSONObject().apply {
                    put("clientContent", JSONObject().apply {
                        put("turns", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", text) })
                                })
                            })
                        })
                        put("turnComplete", true)
                    })
                }
                webSocket?.send(clientContent.toString())
            } catch (e: Exception) {
                Log.e("LiveSession", "Error sending text message", e)
            }
        }
    }

    fun sendCameraFrame(jpegBytes: ByteArray) {
        val ws = webSocket ?: return
        try {
            val base64Frame = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val realtimeInput = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Frame)
                        })
                    })
                })
            }
            ws.send(realtimeInput.toString())
        } catch (e: Exception) {
            Log.e("LiveSession", "Error sending camera frame", e)
        }
    }

    private fun sendAudioChunk(ws: WebSocket, pcmChunk: ByteArray) {
        try {
            val base64Pcm = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val realtimeInput = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Pcm)
                        })
                    })
                })
            }
            ws.send(realtimeInput.toString())
        } catch (e: Exception) {
            Log.e("LiveSession", "Error sending audio chunk", e)
        }
    }

    private fun handleIncomingWsMessage(ws: WebSocket, text: String) {
        try {
            val root = JSONObject(text)

            // Handle Tool Calls
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    for (i in 0 until functionCalls.length()) {
                        val firstCall = functionCalls.getJSONObject(i)
                        val callId = firstCall.optString("id")
                        val fnName = firstCall.optString("name")
                        val argsObj = firstCall.optJSONObject("args")
                        val argsStr = argsObj?.toString() ?: "{}"

                        Log.d("LiveSession", "Dispatched tool call: $fnName ($argsStr)")
                        val result = toolExecutor.executeTool(fnName, argsStr)

                        val category = when (fnName) {
                            "makePhoneCall", "sendWhatsAppMessage", "sendSms" -> ToolCategory.COMMUNICATION
                            "searchGoogle", "researchTopic", "openWebsite", "openYouTube" -> ToolCategory.RESEARCH
                            "toggleFlashlight", "getBatteryStatus", "setVolume", "getVolume" -> ToolCategory.HARDWARE
                            "openApp", "playMusic", "openMaps" -> ToolCategory.PHONE
                            else -> ToolCategory.UTILITY
                        }

                        val newTool = ToolCallInfo(
                            toolName = fnName,
                            argument = argsStr,
                            result = result,
                            category = category
                        )
                        _toolCalls.value = _toolCalls.value + newTool
                        addSystemMessage("Action: $fnName → $result")

                        val toolResponse = JSONObject().apply {
                            put("toolResponse", JSONObject().apply {
                                put("functionResponses", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("id", callId)
                                        put("response", JSONObject().apply {
                                            put("output", result)
                                        })
                                    })
                                })
                            })
                        }
                        ws.send(toolResponse.toString())
                    }
                }
            }

            // Handle Server Content (AI Voice & Transcripts)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.has("interrupted") && serverContent.getBoolean("interrupted")) {
                    Log.d("LiveSession", "Interruption detected. Halting playback.")
                    audioStreamer.stopPlayback()
                    _state.value = AssistantState.LISTENING
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")

                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val mimeType = inlineData.optString("mimeType")
                                if (mimeType.startsWith("audio/pcm")) {
                                    val base64Data = inlineData.getString("data")
                                    val pcmBytes = Base64.decode(base64Data, Base64.NO_WRAP)

                                    _state.value = AssistantState.SPEAKING
                                    audioStreamer.playAudioPcm24k(pcmBytes)
                                }
                            }

                            if (part.has("text")) {
                                val aiText = part.getString("text")
                                addAiMessage(aiText)
                            }
                        }
                    }
                }

                if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                    Log.d("LiveSession", "AI turn completed. Waiting for user voice input.")
                    _state.value = AssistantState.LISTENING
                }
            }
        } catch (e: Exception) {
            Log.e("LiveSession", "Error parsing WebSocket message", e)
        }
    }

    private fun addAiMessage(text: String) {
        val newMsg = VoiceMessage(
            sender = VoiceMessage.Sender.ASSISTANT,
            text = text
        )
        _messages.value = _messages.value + newMsg
    }

    private fun addSystemMessage(text: String) {
        val newMsg = VoiceMessage(
            sender = VoiceMessage.Sender.SYSTEM,
            text = text
        )
        _messages.value = _messages.value + newMsg
    }

    fun disconnectSession() {
        audioStreamer.stopRecording()
        audioStreamer.stopPlayback()
        webSocket?.close(1000, "Session ended")
        webSocket = null
        _state.value = AssistantState.IDLE
        addSystemMessage("Session ended.")
    }
}
