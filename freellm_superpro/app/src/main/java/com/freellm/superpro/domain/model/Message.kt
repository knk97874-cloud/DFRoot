package com.freellm.superpro.domain.model

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val role: Role,
    val content: String = "",
    val reasoningContent: String = "",
    val isReasoningActive: Boolean = false,
    val reasoningDurationSeconds: Double = 0.0,
    val searchSources: List<SearchSource> = emptyList(),
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}
