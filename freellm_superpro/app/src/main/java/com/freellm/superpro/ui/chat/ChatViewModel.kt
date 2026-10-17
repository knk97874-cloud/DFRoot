package com.freellm.superpro.ui.chat

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freellm.superpro.data.local.AppDatabase
import com.freellm.superpro.data.local.entity.ConversationEntity
import com.freellm.superpro.data.local.entity.MessageEntity
import com.freellm.superpro.data.preferences.AppPreferences
import com.freellm.superpro.data.remote.ChatStreamUpdate
import com.freellm.superpro.data.remote.FreeLLMClient
import com.freellm.superpro.data.remote.WebSearchEngine
import com.freellm.superpro.domain.model.Message
import com.freellm.superpro.domain.model.Role
import com.freellm.superpro.domain.model.SearchSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getInstance(application)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val preferences = AppPreferences(application)
    private val freeLLMClient = FreeLLMClient()
    private val webSearchEngine = WebSearchEngine()
    private val gson = Gson()

    private var tts: TextToSpeech? = null

    // UI States
    val conversations = conversationDao.getAllConversations().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    val deepThinkEnabled = preferences.deepThinkEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val webSearchEnabled = preferences.webSearchEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val currentModel = preferences.modelId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "auto"
    )

    private val _selectedCitation = MutableStateFlow<SearchSource?>(null)
    val selectedCitation = _selectedCitation.asStateFlow()

    private val _canvasHtml = MutableStateFlow<String?>(null)
    val canvasHtml = _canvasHtml.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
        viewModelScope.launch {
            conversations.collect { list ->
                if (_activeConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun onInputChange(newText: String) {
        _inputText.value = newText
    }

    private var messageObserverJob: kotlinx.coroutines.Job? = null

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            messageDao.getMessagesForConversation(id).collect { entities ->
                val domainMessages = entities.map { e ->
                    val sourcesType = object : TypeToken<List<SearchSource>>() {}.type
                    val sources: List<SearchSource> = runCatching {
                        gson.fromJson<List<SearchSource>>(e.searchSourcesJson, sourcesType)
                    }.getOrDefault(emptyList())

                    Message(
                        id = e.id,
                        conversationId = e.conversationId,
                        role = if (e.role == "user") Role.USER else Role.ASSISTANT,
                        content = e.content,
                        reasoningContent = e.reasoningContent,
                        reasoningDurationSeconds = e.reasoningDurationSeconds,
                        searchSources = sources,
                        timestamp = e.timestamp
                    )
                }
                // Preserve in-flight streaming message if Room DB emits while generating
                val currentStreaming = _messages.value.find { it.isStreaming }
                if (currentStreaming != null && domainMessages.none { it.id == currentStreaming.id }) {
                    _messages.value = domainMessages + currentStreaming
                } else {
                    _messages.value = domainMessages
                }
            }
        }
    }

    fun startNewConversation() {
        val newId = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            conversationDao.insertOrUpdate(
                ConversationEntity(
                    id = newId,
                    title = "New Chat",
                    modelId = currentModel.value
                )
            )
            _activeConversationId.value = newId
            _messages.value = emptyList()
        }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty() || _isStreaming.value) return

        _inputText.value = ""
        val convId = _activeConversationId.value ?: run {
            val newId = java.util.UUID.randomUUID().toString()
            _activeConversationId.value = newId
            newId
        }

        val userMessage = Message(
            conversationId = convId,
            role = Role.USER,
            content = prompt
        )

        val updatedList = _messages.value + userMessage
        _messages.value = updatedList

        viewModelScope.launch {
            // Save user message to database
            messageDao.insertMessage(
                MessageEntity(
                    id = userMessage.id,
                    conversationId = convId,
                    role = "user",
                    content = userMessage.content,
                    reasoningContent = "",
                    reasoningDurationSeconds = 0.0,
                    searchSourcesJson = "[]"
                )
            )

            // Auto-update conversation title from first prompt
            if (_messages.value.size <= 1) {
                conversationDao.updateTitle(convId, prompt.take(30))
            }

            _isStreaming.value = true

            // 1. Web Search Phase (if enabled)
            var liveSources = emptyList<SearchSource>()
            var searchInjectedPrompt = prompt

            if (webSearchEnabled.value) {
                liveSources = webSearchEngine.search(prompt)
                if (liveSources.isNotEmpty()) {
                    searchInjectedPrompt = prompt + webSearchEngine.buildInjectedContext(liveSources)
                }
            }

            // Create initial placeholder Assistant message
            val assistantMessageId = java.util.UUID.randomUUID().toString()
            var currentAssistantMsg = Message(
                id = assistantMessageId,
                conversationId = convId,
                role = Role.ASSISTANT,
                content = "",
                reasoningContent = "",
                isReasoningActive = deepThinkEnabled.value,
                searchSources = liveSources,
                isStreaming = true
            )

            _messages.value = _messages.value + currentAssistantMsg

            val baseUrl = preferences.apiUrl.first()
            val apiKey = preferences.apiKey.first()
            val model = preferences.modelId.first()
            val sysPrompt = preferences.systemPrompt.first()
            val temp = preferences.temperature.first()

            val streamContent = StringBuilder()
            val streamReasoning = StringBuilder()
            var duration = 0.0

            freeLLMClient.streamChat(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                systemPrompt = sysPrompt,
                history = updatedList,
                newPrompt = searchInjectedPrompt,
                deepThinkEnabled = deepThinkEnabled.value,
                temperature = temp
            ).collect { update ->
                when (update) {
                    is ChatStreamUpdate.ReasoningChunk -> {
                        streamReasoning.append(update.text)
                        duration = update.elapsedSeconds
                        currentAssistantMsg = currentAssistantMsg.copy(
                            reasoningContent = streamReasoning.toString(),
                            isReasoningActive = true,
                            reasoningDurationSeconds = duration
                        )
                        updateLastAssistantMessage(currentAssistantMsg)
                    }
                    is ChatStreamUpdate.ContentChunk -> {
                        streamContent.append(update.text)
                        currentAssistantMsg = currentAssistantMsg.copy(
                            content = streamContent.toString(),
                            isReasoningActive = false
                        )
                        updateLastAssistantMessage(currentAssistantMsg)
                    }
                    is ChatStreamUpdate.Complete -> {
                        _isStreaming.value = false
                        currentAssistantMsg = currentAssistantMsg.copy(
                            content = update.finalContent.ifEmpty { streamContent.toString() },
                            reasoningContent = update.finalReasoning.ifEmpty { streamReasoning.toString() },
                            reasoningDurationSeconds = update.durationSeconds,
                            isReasoningActive = false,
                            isStreaming = false
                        )
                        updateLastAssistantMessage(currentAssistantMsg)

                        // Save assistant message to Room DB
                        messageDao.insertMessage(
                            MessageEntity(
                                id = currentAssistantMsg.id,
                                conversationId = convId,
                                role = "assistant",
                                content = currentAssistantMsg.content,
                                reasoningContent = currentAssistantMsg.reasoningContent,
                                reasoningDurationSeconds = currentAssistantMsg.reasoningDurationSeconds,
                                searchSourcesJson = gson.toJson(currentAssistantMsg.searchSources)
                            )
                        )
                    }
                    is ChatStreamUpdate.Error -> {
                        _isStreaming.value = false
                        currentAssistantMsg = currentAssistantMsg.copy(
                            content = "⚠️ ${update.message}",
                            isReasoningActive = false,
                            isStreaming = false
                        )
                        updateLastAssistantMessage(currentAssistantMsg)

                        // Save error message to Room DB so it remains permanently visible
                        messageDao.insertMessage(
                            MessageEntity(
                                id = currentAssistantMsg.id,
                                conversationId = convId,
                                role = "assistant",
                                content = currentAssistantMsg.content,
                                reasoningContent = currentAssistantMsg.reasoningContent,
                                reasoningDurationSeconds = currentAssistantMsg.reasoningDurationSeconds,
                                searchSourcesJson = gson.toJson(currentAssistantMsg.searchSources)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateLastAssistantMessage(updated: Message) {
        val current = _messages.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        _messages.value = current
    }

    fun toggleDeepThink() {
        viewModelScope.launch {
            preferences.setDeepThinkEnabled(!deepThinkEnabled.value)
        }
    }

    fun toggleWebSearch() {
        viewModelScope.launch {
            preferences.setWebSearchEnabled(!webSearchEnabled.value)
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            preferences.setModelId(modelId)
        }
    }

    fun pinConversation(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            conversationDao.setPinned(id, isPinned)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.deleteConversation(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun selectCitation(source: SearchSource?) {
        _selectedCitation.value = source
    }

    fun openCanvas(html: String?) {
        _canvasHtml.value = html
    }

    fun speak(text: String) {
        tts?.speak(text.take(500), TextToSpeech.QUEUE_FLUSH, null, "msg_tts")
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
