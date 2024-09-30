package com.mariankh.mailydaily

import android.util.Log
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class EmailFunctionality {

    // TODO - Need to move it to a secure location
    private val apiKey = "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"

    // Function to send data to HuggingFace API for summarization
    fun sendToModel(
        prompt: String,
        additionalPrompt: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiUrl = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val requestBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "$prompt $additionalPrompt")
            }))
            put("max_tokens", 500)
            put("stream", false)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Failed to communicate with the model: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    val summary = parseSummaryFromResponse(responseBody)
                    onResult(summary)
                } else {
                    onError("Error: ${response.message}")
                }
            }
        })
    }

    // Parse the response from HuggingFace to extract a summary
    private fun parseSummaryFromResponse(responseBody: String): String {
        val jsonObject = JSONObject(responseBody)
        val choicesArray = jsonObject.optJSONArray("choices") ?: return "No summary available."
        val message = choicesArray.getJSONObject(0).optJSONObject("message")
        return message?.optString("content", "No content available") ?: "No summary available."
    }

    // Function to extract recommended actions and summary from email content using HuggingFace API
    suspend fun extractRecommendedActions(emailContent: String): Pair<String, List<ActionItem>> = withContext(Dispatchers.IO) {
        Log.d("EXTRACT_ACTIONS", "Starting request with email content: ${emailContent.take(500)}...")

        val apiUrl = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val truncatedContent = emailContent.take(15000) // Limit the email content to avoid exceeding token limit

        // Create JSON body for HuggingFace API request
        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "You are my mail assistant. Read this email, and tell me a **Summary** in short and friendly way and recommended actions. " + truncatedContent)
            }))
            put("max_tokens", 500)
            put("stream", false)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(apiUrl)
            .post(jsonBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            Log.d("EXTRACT_ACTIONS", "Sending request to API")
            val response = client.newCall(request).execute()

            Log.d("EXTRACT_ACTIONS", "Response received with code: ${response.code}")
            if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                Log.d("RESPONSE", "Response body: $responseBody")

                return@withContext parseResponse(responseBody)
            } else {
                Log.e("ERROR", "Request failed with code ${response.code}")
                return@withContext Pair("Error fetching actions", emptyList())
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Exception during API call", e)
            return@withContext Pair("Error fetching actions", emptyList())
        }
    }

    // Parse HuggingFace API response to extract summary and actions
    private fun parseResponse(responseBody: String): Pair<String, List<ActionItem>> {
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
                    summary = extractSummaryFromText(lines)
                    actions.addAll(extractActionsFromText(lines))
                }
            }
        }

        return Pair(summary, actions)
    }

    // Extract summary from API response
    private fun extractSummaryFromText(lines: List<String>): String {
        val summaryIndex = lines.indexOfFirst { it.startsWith("**Summary:**") }
        return if (summaryIndex != -1) {
            lines[summaryIndex].removePrefix("**Summary:**").trim()
        } else {
            "No summary available"
        }
    }

    // Extract recommended actions from API response
    private fun extractActionsFromText(lines: List<String>): List<ActionItem> {
        val actions = mutableListOf<ActionItem>()
        val actionsIndex = lines.indexOfFirst { it.startsWith("**Recommended Actions:**") }
        if (actionsIndex != -1) {
            val actionLines = lines.drop(actionsIndex + 1)
            actionLines.forEach { line ->
                val linkRegex = """\[(.*?)\]\((.*?)\)""".toRegex()
                val matchResult = linkRegex.find(line)
                if (matchResult != null) {
                    val (text, linkUrl) = matchResult.destructured
                    actions.add(ActionItem(text, linkUrl))
                } else {
                    actions.add(ActionItem(line, null))
                }
            }
        }
        return actions
    }

    // Function to extract email content such as sender, subject, and full body
    fun extractEmailContent(message: Message): EmailContent {
        val messageId = message.id ?: "No ID"
        val emailDate = message.internalDate?.let { Date(it) } ?: Date()
        val senderEmail = getSenderEmail(message)
        val subjectEmail = getSubject(message)
        val emailFullText = extractEmailBody(message.payload)

        return EmailContent(
            id = messageId,
            date = emailDate.toString(),
            sender = senderEmail,
            subject = subjectEmail,
            fullText = emailFullText,
            actions = emptyList()
        )
    }

    // Helper function to get the sender's email address
    private fun getSenderEmail(message: Message): String {
        val headers = message.payload?.headers
        val fromHeader = headers?.find { it.name == "From" }
        return fromHeader?.value ?: "Unknown sender"
    }

    // Helper function to get the subject of the email
    private fun getSubject(message: Message): String {
        val headers = message.payload?.headers
        val subjectHeader = headers?.find { it.name == "Subject" }
        return subjectHeader?.value ?: "No subject"
    }

    // Extract email body from MessagePart
    private fun extractEmailBody(payload: MessagePart?): String {
        if (payload == null) return "No content available"

        fun decodeBase64(encoded: String?): ByteArray? {
            return try {
                if (encoded == null) null else Base64.getUrlDecoder().decode(encoded)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        payload.body?.let {
            val bodyData = decodeBase64(it.data)
            if (bodyData != null) return String(bodyData)
        }

        return "No content available"
    }
}
