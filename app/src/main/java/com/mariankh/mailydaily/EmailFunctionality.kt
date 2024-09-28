package com.mariankh.mailydaily

import android.os.Build
import android.util.Log
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.Date

class EmailFunctionality {
    val emails = EmailStore.emailHistory
    val apiKey ="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" // Replace with your actual API key
    val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"


    fun sendtoModel(promt :String, additinalpromt: String, username:String,onResult: (String) -> Unit, onError: (String) -> Unit) {

        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "Hello, I am "+username +  promt + "" + emails + " . "+ additinalpromt)
            }))
            put("max_tokens", 8000)
            put("temperature", 0.5)
            put("stream", false)
        }.toString()

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody.toString()
        )

        Log.d("ALLEMAILCONTENT", jsonBody);
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Error occurred")
            }


            override fun onResponse(call: Call, response: Response) {
                response.takeIf { it.isSuccessful }?.body?.string()?.let { responseBody ->
                    try {
                        val content = JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        println("Response Content: $content")
                        content.let { onResult(it) }

                    } catch (e: JSONException) {
                        e.printStackTrace() // Handle JSON parsing errors
                    }
                } ?: println("Request failed with code: ${response.code}")
            }
        })
    }


    suspend fun extractRecommendedActions(emailContent: String): Pair<String, List<ActionItem>> =  withContext(
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
                put("content", "You are my mail assistant. Read this email, and tell me a **Summary** in short and friendly way and recommended actions. " + truncatedContent)            }))
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
        val subjectEmail = getSubject(message)
        val emailFullText = extractEmailBody(message.payload)

        return EmailContent(
            id = messageId,
            date = emailDate.toString(),
            sender = senderEmail,
            subject =subjectEmail,
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

    private fun getSubject(message: Message): String {
        val headers: List<MessagePartHeader>? = message.payload?.headers
        val subjectHeader = headers?.find { it.name == "Subject" }
        return subjectHeader?.value ?: "No subject"
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

data class EmailContent(
    val id: String,
    val date: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    var fullText: String,
    var category: String,
    var actions: List<ActionItem>
)