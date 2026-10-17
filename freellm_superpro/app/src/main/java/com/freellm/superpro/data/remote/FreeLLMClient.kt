package com.freellm.superpro.data.remote

import com.freellm.superpro.domain.engine.DeepThinkEngine
import com.freellm.superpro.domain.model.Message
import com.freellm.superpro.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class ChatStreamUpdate {
    data class ReasoningChunk(val text: String, val elapsedSeconds: Double) : ChatStreamUpdate()
    data class ContentChunk(val text: String) : ChatStreamUpdate()
    data class Complete(val finalContent: String, val finalReasoning: String, val durationSeconds: Double) : ChatStreamUpdate()
    data class Error(val message: String) : ChatStreamUpdate()
}

class FreeLLMClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sseParser = SSEStreamParser()
    private val thinkEngine = DeepThinkEngine()

    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<Message>,
        newPrompt: String,
        deepThinkEnabled: Boolean,
        temperature: Float = 0.7f
    ): Flow<ChatStreamUpdate> = flow {
        thinkEngine.reset()
        val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "${baseUrl.removeSuffix("/")}/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("temperature", temperature.toDouble())

            val messagesArray = JSONArray()

            // System message
            var sys = systemPrompt
            if (deepThinkEnabled) {
                sys += "\n\n[INSTRUCTION]: Provide detailed, step-by-step reasoning inside <think>...</think> tags before delivering your final answer."
            }
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", sys)
            })

            // Past conversation history (sliding window)
            history.takeLast(10).forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", when (msg.role) {
                        Role.USER -> "user"
                        Role.ASSISTANT -> "assistant"
                        Role.SYSTEM -> "system"
                    })
                    put("content", msg.content)
                })
            }

            // New User message
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", newPrompt)
            })

            put("messages", messagesArray)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: response.message
                emit(ChatStreamUpdate.Error("API Error ${response.code}: $errBody"))
                return@flow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                emit(ChatStreamUpdate.Error("Response body is empty"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()

            var fullContent = StringBuilder()
            var fullReasoning = StringBuilder()

            while (line != null) {
                val event = sseParser.parseLine(line)
                when (event) {
                    is StreamEvent.ContentToken -> {
                        val processed = thinkEngine.processToken(event.token)
                        if (processed.thoughtToken.isNotEmpty()) {
                            fullReasoning.append(processed.thoughtToken)
                            emit(ChatStreamUpdate.ReasoningChunk(processed.thoughtToken, thinkEngine.getElapsedTimeSeconds()))
                        }
                        if (processed.answerToken.isNotEmpty()) {
                            fullContent.append(processed.answerToken)
                            emit(ChatStreamUpdate.ContentChunk(processed.answerToken))
                        }
                    }
                    is StreamEvent.ReasoningToken -> {
                        fullReasoning.append(event.token)
                        emit(ChatStreamUpdate.ReasoningChunk(event.token, thinkEngine.getElapsedTimeSeconds()))
                    }
                    is StreamEvent.StreamFinished -> {
                        break
                    }
                    is StreamEvent.StreamError -> {
                        emit(ChatStreamUpdate.Error(event.error))
                        break
                    }
                    null -> {}
                }
                line = reader.readLine()
            }

            reader.close()
            emit(ChatStreamUpdate.Complete(
                finalContent = fullContent.toString().ifEmpty { thinkEngine.getCompleteContent() },
                finalReasoning = fullReasoning.toString().ifEmpty { thinkEngine.getCompleteReasoning() },
                durationSeconds = thinkEngine.getElapsedTimeSeconds()
            ))

        } catch (e: Exception) {
            emit(ChatStreamUpdate.Error("Network failure: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)
}
