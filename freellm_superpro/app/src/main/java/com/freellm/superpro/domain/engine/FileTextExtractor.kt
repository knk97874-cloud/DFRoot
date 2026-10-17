package com.freellm.superpro.domain.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FileTextExtractor(private val context: Context) {

    suspend fun extractTextFromUri(uri: Uri, maxChars: Int = 30000): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext ""
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            var totalChars = 0

            while (line != null && totalChars < maxChars) {
                sb.append(line).append("\n")
                totalChars += line.length + 1
                line = reader.readLine()
            }
            reader.close()
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting file content: ${e.localizedMessage}"
        }
    }
}
