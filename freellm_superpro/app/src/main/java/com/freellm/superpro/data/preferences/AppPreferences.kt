package com.freellm.superpro.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "freellm_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_API_URL = stringPreferencesKey("api_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_MODEL_ID = stringPreferencesKey("model_id")
        val KEY_DEEPTHINK = booleanPreferencesKey("deepthink_enabled")
        val KEY_WEBSEARCH = booleanPreferencesKey("websearch_enabled")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_AMOLED_THEME = booleanPreferencesKey("amoled_theme")

        const val DEFAULT_API_URL = "https://freellm-production-7c31.up.railway.app/v1"
        const val DEFAULT_API_KEY = "freellmapi-41f23cbeb53f3eb3d06230c88885c01258e9c1c2650c8366"
        const val DEFAULT_MODEL = "auto"
        const val DEFAULT_SYSTEM_PROMPT = "You are FreeLLM SuperPro, a world-class AI assistant. You deliver comprehensive, accurate, articulate, and insightful answers with flawless Markdown formatting."
    }

    val apiUrl: Flow<String> = context.dataStore.data.map { it[KEY_API_URL] ?: DEFAULT_API_URL }
    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API_KEY] ?: DEFAULT_API_KEY }
    val modelId: Flow<String> = context.dataStore.data.map { it[KEY_MODEL_ID] ?: DEFAULT_MODEL }
    val deepThinkEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_DEEPTHINK] ?: true }
    val webSearchEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEBSEARCH] ?: false }
    val systemPrompt: Flow<String> = context.dataStore.data.map { it[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT }
    val temperature: Flow<Float> = context.dataStore.data.map { it[KEY_TEMPERATURE] ?: 0.7f }
    val amoledTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_AMOLED_THEME] ?: true }

    suspend fun setApiUrl(url: String) = context.dataStore.edit { it[KEY_API_URL] = url }
    suspend fun setApiKey(key: String) = context.dataStore.edit { it[KEY_API_KEY] = key }
    suspend fun setModelId(model: String) = context.dataStore.edit { it[KEY_MODEL_ID] = model }
    suspend fun setDeepThinkEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_DEEPTHINK] = enabled }
    suspend fun setWebSearchEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_WEBSEARCH] = enabled }
    suspend fun setSystemPrompt(prompt: String) = context.dataStore.edit { it[KEY_SYSTEM_PROMPT] = prompt }
    suspend fun setTemperature(temp: Float) = context.dataStore.edit { it[KEY_TEMPERATURE] = temp }
    suspend fun setAmoledTheme(enabled: Boolean) = context.dataStore.edit { it[KEY_AMOLED_THEME] = enabled }
}
