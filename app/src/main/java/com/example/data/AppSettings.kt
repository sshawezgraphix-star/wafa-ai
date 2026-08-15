package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AssistantVoice(
    val id: String,
    val name: String,
    val gender: String = "Female",
    val description: String
)

data class SavedNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("maya_ai_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_VOICE_NAME = "selected_voice_name"
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        private const val KEY_SYSTEM_INSTRUCTION = "custom_system_instruction"
        private const val KEY_SAVED_NOTES = "saved_notes_json"

        val AVAILABLE_FEMALE_VOICES = listOf(
            AssistantVoice("Aoede", "Aoede", "Female", "Melodic, Deep & Expressive (Default)"),
            AssistantVoice("Kore", "Kore", "Female", "Calm, Crisp & Professional"),
            AssistantVoice("Leda", "Leda", "Female", "Warm, Friendly & Conversational"),
            AssistantVoice("Fenrir", "Fenrir", "Male", "Authoritative & Strong"),
            AssistantVoice("Puck", "Puck", "Male", "Playful & Energetic")
        )
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _voiceFlow = MutableStateFlow(getSelectedVoice())
    val voiceFlow: StateFlow<String> = _voiceFlow.asStateFlow()

    private val _assistantNameFlow = MutableStateFlow(getAssistantName())
    val assistantNameFlow: StateFlow<String> = _assistantNameFlow.asStateFlow()

    private val _notesFlow = MutableStateFlow(getNotes())
    val notesFlow: StateFlow<List<SavedNote>> = _notesFlow.asStateFlow()

    fun getApiKey(): String {
        val storedKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (storedKey.isNotBlank()) return storedKey
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        _apiKeyFlow.value = apiKey.trim()
    }

    fun getSelectedVoice(): String {
        return prefs.getString(KEY_VOICE_NAME, "Aoede") ?: "Aoede"
    }

    fun setSelectedVoice(voiceName: String) {
        prefs.edit().putString(KEY_VOICE_NAME, voiceName).apply()
        _voiceFlow.value = voiceName
    }

    fun getAssistantName(): String {
        return prefs.getString(KEY_ASSISTANT_NAME, "Maya") ?: "Maya"
    }

    fun setAssistantName(name: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, name).apply()
        _assistantNameFlow.value = name
    }

    fun getNotes(): List<SavedNote> {
        val jsonStr = prefs.getString(KEY_SAVED_NOTES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<SavedNote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedNote(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addNote(text: String): SavedNote {
        val newNote = SavedNote(text = text.trim())
        val updated = listOf(newNote) + getNotes()
        saveNotesInternal(updated)
        _notesFlow.value = updated
        return newNote
    }

    fun deleteNote(noteId: String) {
        val updated = getNotes().filterNot { it.id == noteId }
        saveNotesInternal(updated)
        _notesFlow.value = updated
    }

    private fun saveNotesInternal(notes: List<SavedNote>) {
        val jsonArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("text", n.text)
                put("timestamp", n.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_NOTES, jsonArray.toString()).apply()
    }
}
