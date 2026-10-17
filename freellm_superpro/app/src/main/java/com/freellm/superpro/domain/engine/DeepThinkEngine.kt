package com.freellm.superpro.domain.engine

class DeepThinkEngine {

    private var isInThinkingPhase = false
    private var thinkingStartTime = 0L
    private val reasoningBuffer = StringBuilder()
    private val contentBuffer = StringBuilder()

    fun reset() {
        isInThinkingPhase = false
        thinkingStartTime = 0L
        reasoningBuffer.clear()
        contentBuffer.clear()
    }

    fun startThinking(currentTime: Long = System.currentTimeMillis()) {
        isInThinkingPhase = true
        thinkingStartTime = currentTime
    }

    fun stopThinking() {
        isInThinkingPhase = false
    }

    fun isThinking(): Boolean = isInThinkingPhase

    fun getElapsedTimeSeconds(currentTime: Long = System.currentTimeMillis()): Double {
        if (thinkingStartTime == 0L) return 0.0
        return ((currentTime - thinkingStartTime) / 100) / 10.0
    }

    /**
     * Ingests a new token and determines if it belongs to reasoning or answer.
     * Handles <think> and </think> delimiters dynamically.
     */
    fun processToken(token: String): ProcessedToken {
        var processed = token
        var isReasoning = isInThinkingPhase

        if (processed.contains("<think>")) {
            startThinking()
            isReasoning = true
            processed = processed.replace("<think>", "")
        }

        if (processed.contains("</think>")) {
            stopThinking()
            val parts = processed.split("</think>", limit = 2)
            val thoughtPart = parts[0]
            val answerPart = if (parts.size > 1) parts[1] else ""

            reasoningBuffer.append(thoughtPart)
            contentBuffer.append(answerPart)

            return ProcessedToken(
                thoughtToken = thoughtPart,
                answerToken = answerPart,
                isThinkingNow = false
            )
        }

        if (isInThinkingPhase) {
            reasoningBuffer.append(processed)
            return ProcessedToken(
                thoughtToken = processed,
                answerToken = "",
                isThinkingNow = true
            )
        } else {
            contentBuffer.append(processed)
            return ProcessedToken(
                thoughtToken = "",
                answerToken = processed,
                isThinkingNow = false
            )
        }
    }

    fun getCompleteReasoning(): String = reasoningBuffer.toString()
    fun getCompleteContent(): String = contentBuffer.toString()
}

data class ProcessedToken(
    val thoughtToken: String,
    val answerToken: String,
    val isThinkingNow: Boolean
)
