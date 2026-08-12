package com.example.model

enum class AssistantState(val label: String) {
    IDLE("Tap to Speak"),
    CONNECTING("Connecting to Firdous..."),
    LISTENING("Firdous is listening..."),
    SPEAKING("Firdous is speaking..."),
    ERROR("Connection Error")
}

data class VoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Sender { USER, FIRDOUS, SYSTEM }
}

data class ToolCallInfo(
    val toolName: String,
    val argument: String,
    val timestamp: Long = System.currentTimeMillis()
)
