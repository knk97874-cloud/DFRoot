package com.freellm.superpro.domain.model

data class SearchSource(
    val index: Int,
    val title: String,
    val snippet: String,
    val url: String,
    val domain: String,
    val faviconUrl: String = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
)
