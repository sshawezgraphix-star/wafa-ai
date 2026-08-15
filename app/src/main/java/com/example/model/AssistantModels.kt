package com.example.model

enum class AssistantState(val label: String) {
    IDLE("Tap Arc Reactor to Start"),
    CONNECTING("Connecting to Maya AI..."),
    LISTENING("Maya is listening..."),
    THINKING("Maya is analyzing..."),
    SPEAKING("Maya is speaking..."),
    ERROR("Connection Error")
}

data class VoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String? = null
) {
    enum class Sender {
        USER,
        ASSISTANT,
        SYSTEM,
        RESEARCH,
        TOOL
    }
}

enum class ToolCategory {
    PHONE,
    COMMUNICATION,
    RESEARCH,
    HARDWARE,
    UTILITY
}

data class ToolCallInfo(
    val toolName: String,
    val argument: String,
    val result: String? = null,
    val category: ToolCategory = ToolCategory.UTILITY,
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuickActionItem(
    val id: String,
    val label: String,
    val prompt: String,
    val iconKey: String
)
