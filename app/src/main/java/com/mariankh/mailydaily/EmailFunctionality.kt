package com.mariankh.mailydaily

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.Date

class EmailFunctionality {


    suspend fun extractRecommendedActions(emailContent: String): Pair<String, List<ActionItem>> = withContext(
        Dispatchers.IO) {
        Log.d("EXTRACT ACTIONS", "Starting request with email content: ${emailContent.take(500)}...")

        val apiKey = "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"
        val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val truncatedContent = emailContent.take(15000) // Truncate content to fit within token limit

        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "You are my friendly mail assistant. Read this email, and tell me a who is sending and for what reason and if there is anything I shall do. Offer recommended actions that I can do from the email, like reply or visit a url, or delete. Keep it short " + truncatedContent)
            }))
            put("max_tokens", 500)
            put("stream", false)
        }.toString()

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            Log.d("EXTRACT ACTIONS", "Sending request to API")
            val response = client.newCall(request).execute()

            Log.d("EXTRACT ACTIONS", "Response received with code: ${response.code}")
            if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                Log.d("RESPONSE", "Response body: $responseBody")

                val jsonObject = JSONObject(responseBody)
                val choicesArray = jsonObject.optJSONArray("choices")
                var summary = ""
                val actions = mutableListOf<ActionItem>()

                choicesArray?.let { array ->
                    if (array.length() > 0) {
                        val item = array.getJSONObject(0)
                        val message = item.optJSONObject("message")
                        val generatedText = message?.optString("content", "No text available")
                        if (!generatedText.isNullOrBlank()) {
                            val lines = generatedText.split("\n").map { it.trim() }
                            val summaryIndex = lines.indexOfFirst { it.startsWith("**Summary:**") }
                            if (summaryIndex != -1) {
                                summary = lines[summaryIndex].removePrefix("**Summary:**").trim()
                            }
                            val actionsIndex = lines.indexOfFirst { it.startsWith("**Recommended Actions:**") }
                            if (actionsIndex != -1) {
                                val actionLines = lines.drop(actionsIndex + 1)
                                actionLines.forEach { line ->
                                    val linkRegex = """\[(.*?)\]\((.*?)\)""".toRegex()
                                    val matchResult = linkRegex.find(line)
                                    if (matchResult != null) {
                                        val (text, url) = matchResult.destructured
                                        actions.add(ActionItem(text, url))
                                    } else {
                                        actions.add(ActionItem(line, null))
                                    }
                                }
                            }
                        }
                    }
                }

                Pair(summary, actions)
            } else {
                Log.e("ERROR", "Request failed with code ${response.code}")
                Log.e("ERROR RESPONSE", response.body?.string().orEmpty())
                Pair("Error fetching actions", emptyList())
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Exception during API call", e)
            Pair("Error fetching actions", emptyList())
        }
    }




    fun extractEmailContent(message: Message, service: Gmail): EmailContent {
        val messageId = message.id ?: "No ID"
        val emailDate = message.internalDate?.let { Date(it) } ?: Date()
        val emailSnippet = message.snippet ?: "No snippet"
        val senderEmail = getSenderEmail(message)
        val emailFullText = extractEmailBody(message.payload)

        return EmailContent(
            id = messageId,
            date = emailDate.toString(),
            sender = senderEmail,
            snippet = emailSnippet,
            fullText = emailFullText,
            category = "Unknown", // Placeholder, will be updated
            actions = emptyList() // Placeholder, will be updated
        )
    }

    private fun getSenderEmail(message: Message): String {
        val headers: List<MessagePartHeader>? = message.payload?.headers
        val fromHeader = headers?.find { it.name == "From" }
        return fromHeader?.value ?: "Unknown sender"
    }

    private fun extractEmailBody(payload: MessagePart?): String {
        if (payload == null) return "No content available"

        fun decodeBase64(encoded: String?): ByteArray? {
            return try {
                if (encoded == null) null
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getUrlDecoder().decode(encoded)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        payload.body?.let {
            val bodyData = decodeBase64(it.data)
            if (bodyData != null) return String(bodyData)
        }

        if (payload.parts != null && payload.parts.isNotEmpty()) {
            for (part in payload.parts) {
                val mimeType = part.mimeType ?: continue
                val partData = decodeBase64(part.body?.data)
                if (partData != null && (mimeType == "text/plain" || mimeType == "text/html")) {
                    return String(partData)
                }
            }
        }

        return "No content available"
    }
}