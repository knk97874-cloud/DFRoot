package com.freellm.superpro.data.remote

import org.json.JSONObject

sealed class StreamEvent {
    data class ContentToken(val token: String) : StreamEvent()
    data class ReasoningToken(val token: String) : StreamEvent()
    data object StreamFinished : StreamEvent()
    data class StreamError(val error: String) : StreamEvent()
}

class SSEStreamParser {

    fun parseLine(line: String): StreamEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith(":")) return null

        if (trimmed.startsWith("data:")) {
            val data = trimmed.removePrefix("data:").trim()
            if (data == "[DONE]") {
                return StreamEvent.StreamFinished
            }

            try {
                val json = JSONObject(data)
                val choices = json.optJSONArray("choices") ?: return null
                if (choices.length() == 0) return null

                val firstChoice = choices.getJSONObject(0)
                val delta = firstChoice.optJSONObject("delta") ?: return null

                // 1. Direct reasoning_content (DeepSeek / OpenAI reasoning format)
                val reasoning = delta.optString("reasoning_content", "")
                if (reasoning.isNotEmpty()) {
                    return StreamEvent.ReasoningToken(reasoning)
                }

                // 2. Regular content
                val content = delta.optString("content", "")
                if (content.isNotEmpty()) {
                    return StreamEvent.ContentToken(content)
                }
            } catch (e: Exception) {
                // Ignore parse errors on partial heartbeats
            }
        }
        return null
    }
}
