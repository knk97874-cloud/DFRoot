package com.freellm.superpro.domain.model

data class ModelOption(
    val id: String,
    val displayName: String,
    val providerBadge: String,
    val description: String,
    val supportsVision: Boolean = false,
    val supportsReasoning: Boolean = false
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            ModelOption(
                id = "auto",
                displayName = "Auto (Smart Routing)",
                providerBadge = "⚡ Auto",
                description = "Automatically routes to the fastest, smartest model for the task",
                supportsVision = true,
                supportsReasoning = true
            ),
            ModelOption(
                id = "claude-3-5-sonnet",
                displayName = "Claude 3.5 Sonnet",
                providerBadge = "Anthropic",
                description = "Top coding, nuance, and complex analytical reasoning",
                supportsVision = true,
                supportsReasoning = false
            ),
            ModelOption(
                id = "gpt-4o",
                displayName = "GPT-4o Omni",
                providerBadge = "OpenAI",
                description = "Multimodal flagship model with high speed and precision",
                supportsVision = true,
                supportsReasoning = false
            ),
            ModelOption(
                id = "deepseek-r1",
                displayName = "DeepSeek R1 (Reasoning)",
                providerBadge = "DeepSeek",
                description = "Superhuman reasoning, math proofs, and transparent CoT thoughts",
                supportsVision = false,
                supportsReasoning = true
            ),
            ModelOption(
                id = "llama-3.3-70b",
                displayName = "Llama 3.3 70B",
                providerBadge = "Meta",
                description = "Ultra-fast open weights state-of-the-art intelligence",
                supportsVision = false,
                supportsReasoning = false
            )
        )
    }
}
