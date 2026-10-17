package com.freellm.superpro.data.remote

import com.freellm.superpro.domain.model.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<SearchSource> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<SearchSource>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FreeLLMSuperPro/1.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                val abstractText = json.optString("AbstractText", "")
                val abstractUrl = json.optString("AbstractURL", "")
                val abstractSource = json.optString("AbstractSource", "Web")

                if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                    val domain = runCatching { URI(abstractUrl).host ?: abstractSource }.getOrDefault(abstractSource)
                    sources.add(
                        SearchSource(
                            index = sources.size + 1,
                            title = abstractSource,
                            snippet = abstractText,
                            url = abstractUrl,
                            domain = domain
                        )
                    )
                }

                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until relatedTopics.length()) {
                        if (sources.size >= 5) break
                        val topic = relatedTopics.optJSONObject(i) ?: continue
                        val text = topic.optString("Text", "")
                        val firstUrl = topic.optString("FirstURL", "")
                        if (text.isNotBlank() && firstUrl.isNotBlank()) {
                            val domain = runCatching { URI(firstUrl).host ?: "web" }.getOrDefault("web")
                            sources.add(
                                SearchSource(
                                    index = sources.size + 1,
                                    title = text.take(60),
                                    snippet = text,
                                    url = firstUrl,
                                    domain = domain
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sources
    }

    fun buildInjectedContext(sources: List<SearchSource>): String {
        if (sources.isEmpty()) return ""
        val sb = StringBuilder("\n\n[LIVE WEB SEARCH CONTEXT RESULTS]:\n")
        sources.forEach { s ->
            sb.append("[${s.index}] ${s.title} (${s.domain})\nURL: ${s.url}\nExcerpt: ${s.snippet}\n\n")
        }
        sb.append("Directives: Cite these sources inline using footnote numbers like [1], [2] when referencing this real-time data.\n\n")
        return sb.toString()
    }
}
