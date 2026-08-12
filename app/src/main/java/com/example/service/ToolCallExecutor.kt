package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class ToolCallExecutor(private val context: Context) {

    fun executeTool(functionName: String, argsJson: String): String {
        Log.d("ToolCallExecutor", "Executing tool $functionName with args $argsJson")
        return when (functionName) {
            "openWebsite" -> {
                val url = extractArg(argsJson, "url") ?: "https://google.com"
                openUrl(url)
                "Successfully opened website $url"
            }
            "searchGoogle" -> {
                val query = extractArg(argsJson, "query") ?: "Firdous AI"
                val url = "https://www.google.com/search?q=${Uri.encode(query)}"
                openUrl(url)
                "Searched Google for '$query'"
            }
            "openYouTube" -> {
                val query = extractArg(argsJson, "query")
                val url = if (query.isNullOrBlank()) {
                    "https://www.youtube.com"
                } else {
                    "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                }
                openUrl(url)
                "Opened YouTube for search '$query'"
            }
            else -> "Unknown function tool called: $functionName"
        }
    }

    private fun openUrl(url: String) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ToolCallExecutor", "Failed to open URL: $url", e)
        }
    }

    private fun extractArg(jsonStr: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(jsonStr)
        return match?.groupValues?.get(1)
    }
}
