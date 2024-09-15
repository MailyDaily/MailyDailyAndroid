package com.mariankh.mailydaily

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.mariankh.mailydaily.ui.theme.MailyDailyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.compose.rememberImagePainter
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import java.util.Base64
import java.util.Date

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var userAccount: GoogleSignInAccount? by mutableStateOf(null)
    private var emailContentList: List<EmailContent> by mutableStateOf(emptyList())
    private var isLoading by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    handleSignInResult(task)
                }
            }

        setContent {
            MailyDailyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userAccount != null) {
                        UserInfoDisplay(userAccount!!, emailContentList, isLoading)
                    } else {
                        Greeting("Android") {
                            initiateSignIn()
                        }
                    }
                }
            }
        }
    }

    private fun initiateSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        Log.d("SIGN_IN", "Sign-in ok?  1")
        signInLauncher.launch(signInIntent)
        Log.d("SIGN_IN", "Sign-in ok? 2 ")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            Log.d("SIGN_IN", "Handling sign-in result")
            val account = task.getResult(ApiException::class.java)
            account?.let {
                userAccount = account
                Log.d("SIGN_IN", "Sign-in successful: ${userAccount?.displayName}")
                fetchEmails(it)
            }
        } catch (e: ApiException) {
            Log.e("SIGN_IN", "Sign-in failed: ${e.statusCode} - ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchEmails(account: GoogleSignInAccount) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EMAIL_FETCH", "Fetching emails")
                isLoading = true
                val credential = GoogleAccountCredential.usingOAuth2(
                    this@MainActivity, listOf("https://www.googleapis.com/auth/gmail.readonly")
                ).apply {
                    selectedAccount = account.account
                }

                val transport: HttpTransport = NetHttpTransport()
                val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

                val service = Gmail.Builder(
                    transport,
                    jsonFactory,
                    credential
                ).setApplicationName("MailyDaily")
                    .build()

                val response: ListMessagesResponse = service.users().messages().list("me").apply {
                    q = "newer_than:1d" // Fetch emails from the last day
                }.execute()

                val messages = response.messages ?: emptyList()
                val emailContents = mutableListOf<EmailContent>()

                // Process emails in parallel using coroutines
                val jobs = messages.map { message ->
                    async {
                        val msg: Message = service.users().messages().get("me", message.id).execute()
                        val emailContent = extractEmailContent(msg, service)

                        // Classify the email and extract actions
                       // val categoryDeferred = async { classifyEmailCategory(emailContent.fullText) }

                        val actionsDeferred = async { extractRecommendedActions(emailContent.fullText) }

                        val category = "" //categoryDeferred.await()
                        val actions = actionsDeferred.await()

                        emailContent.category = category
                        emailContent.actions = actions
                        emailContents.add(emailContent)
                    }
                }

                // Await all jobs to complete
                jobs.awaitAll()

                // Update UI state on the main thread
                withContext(Dispatchers.Main) {
                    emailContentList = emailContents
                    isLoading = false
                }

                Log.d("EMAIL_FETCH", "Emails fetched successfully")
            } catch (e: Exception) {
                Log.e("EMAIL_FETCH", "Error fetching emails", e)
                // Ensure UI is updated to stop loading spinner if an error occurred
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }


    /***
     * curl 'https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions' \
     * -H "Authorization: Bearer hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
     * -H 'Content-Type: application/json' \
     * -d '{
     * 	"model": "mistralai/Mistral-Nemo-Instruct-2407",
     * 	"messages": [{"role": "user", "content": "What is the capital of France?"}],
     * 	"max_tokens": 500,
     * 	"stream": false
     * }'
     *
     */

    suspend fun classifyEmailCategory(emailContent: String): String = withContext(Dispatchers.IO) {
        val apiKey = "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"
        val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "Give me a one word category for this email" + emailContent)
            }))
            put("max_tokens", 500)
            put("stream", false)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val jsonObject = JSONObject(response.body?.string().orEmpty())
            jsonObject.optString("category", "Unknown")
        } else {
            "Error: ${response.message}"
        }
    }

    suspend fun extractRecommendedActions(emailContent: String): List<String> = withContext(Dispatchers.IO) {
        Log.d("EXTRACT ACTIONS", "Starting request with email content: ${emailContent.take(500)}...")

        val apiKey = "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"
        val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val truncatedContent = emailContent.take(15000) // Truncate content to fit within token limit

        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "You are my mail assistant. Read this email, and tell me a summary in short and friendly way and recommended actions. " + truncatedContent)
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

            Log.d("EXTRACT ACTIONS", "Response received")
            Log.d("RESPONSE CODE", response.code.toString())

            if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                Log.d("RESPONSE", "Response body: $responseBody")

                val jsonObject = JSONObject(responseBody)
                val choicesArray = jsonObject.optJSONArray("choices")
                val actions = mutableListOf<String>()

                choicesArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val message = item.optJSONObject("message")
                        val generatedText = message?.optString("content", "No text available")
                        if (!generatedText.isNullOrBlank()) {
                            actions.add(generatedText)
                        }
                    }
                }

                actions
            } else {
                Log.e("ERROR", "Request failed with code ${response.code}")
                Log.e("ERROR RESPONSE", response.body?.string().orEmpty())
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Exception during API call", e)
            emptyList()
        }
    }



    private fun extractEmailContent(message: Message, service: Gmail): EmailContent {
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

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier, onSignInClick: () -> Unit) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello $name!",
                modifier = modifier.clickable(onClick = onSignInClick),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }

    @Composable
    fun UserInfoDisplay(
        userAccount: GoogleSignInAccount,
        emailContentList: List<EmailContent>,
        isLoading: Boolean
    ) {
        var emailSummary by remember { mutableStateOf("") }

        // Summarize emails once the emails have been fetched
        if (!isLoading && emailContentList.isNotEmpty() && emailSummary.isEmpty()) {
            val emails = emailContentList.map { it.fullText }
            LaunchedEffect(emails) {
                summarizeEmails(emails, { summary ->
                    emailSummary = summary
                }, { error ->
                    emailSummary = "Error summarizing emails: $error"
                })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hello, ${userAccount.displayName}!",
                    style = MaterialTheme.typography.titleLarge
                )
                userAccount.photoUrl?.let { photoUrl ->
                    Image(
                        painter = rememberImagePainter(photoUrl),
                        contentDescription = "User Profile Picture",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Display the summary at the top
                Text(
                    text = "Summary of your emails:",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = emailSummary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                // List the emails below the summary
                Text(
                    text = "Here are your unread emails:",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emailContentList.size) { index ->
                        val emailContent = emailContentList[index]
                        EmailCard(emailContent)
                    }
                }
            }
        }
    }



    @Composable
    fun EmailCard(emailContent: EmailContent) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(text = "From: ${emailContent.sender}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Date: ${emailContent.date}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Category: ${emailContent.category}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = emailContent.snippet, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                emailContent.actions.forEach { action ->
                    Button(onClick = { /* Handle action */ }) {
                        Text(action)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    fun summarizeEmails(emails: List<String>, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val apiKey = "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" // Replace with your actual API key
        val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("model", "mistralai/Mistral-Nemo-Instruct-2407")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "ou are my mail assistant. Read this email, and tell me a summary in short and friendly way and recommended actions." + emails)
            }))
            put("max_tokens", 500)
            put("stream", false)
        }.toString()

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody.toString()
        )

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
                response.body?.string()?.let { responseBody ->
                    // Parse the summary from Hugging Face
                    val jsonResponse = JSONObject(responseBody)
                    val summary = jsonResponse.optJSONArray("summary_text")?.getString(0)
                    summary?.let { onResult(it) }
                } ?: onError("Empty response")
            }
        })
    }

    fun classifyEmailCategory(emailContent: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api-inference.huggingface.co/models/facebook/bart-large-mnli"

        val client = OkHttpClient()
        val requestBody = JSONObject().apply {
            put("inputs", emailContent)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val result = JSONObject(responseBody).optJSONArray("labels")?.getString(0) ?: "Unknown"
                    onResult(result)
                } else {
                    onError("Error: ${response.message}")
                }
            }
        })
    }
}

data class EmailContent(
    val id: String,
    val date: String,
    val sender: String,
    val snippet: String,
    val fullText: String,
    var category: String,
    var actions: List<String>
)
