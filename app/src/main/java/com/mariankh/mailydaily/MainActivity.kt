package com.mariankh.mailydaily

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
import kotlinx.coroutines.CoroutineScope
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartHeader
import java.util.Base64
import java.util.Date

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var userAccount: GoogleSignInAccount? by mutableStateOf(null)
    private var emailContentList: List<String> by mutableStateOf(emptyList())
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
                val emailContents = mutableListOf<String>()
                for (message in messages) {
                    val msg: Message = service.users().messages().get("me", message.id).execute()
                    val emailContent = extractEmailContent(msg, service)
                    emailContents.add(emailContent)
                }

                emailContentList = emailContents
                Log.d("EMAIL_FETCH", "Emails fetched successfully")
            } catch (e: Exception) {
                Log.e("EMAIL_FETCH", "Error fetching emails", e)
            } finally {
                isLoading = false
            }
        }
    }

    private fun extractEmailContent(message: Message, service: Gmail): String {
        // Extract email details from the Message object
        val messageId = message.id ?: "No ID"
        val msg: Message = service.users().messages().get("me", messageId).execute()
        val emailDate = msg.internalDate?.let { Date(it) } ?: Date()
        val emailSnippet = msg.snippet ?: "No snippet"
        val senderEmail = getSenderEmail(msg)
        val emailFullText = "" //extractEmailBody(msg.payload)

        return "Email ID: $messageId\n" +
                "Received at: $emailDate\n" +
                "From: $senderEmail\n" +
                "Snippet: $emailSnippet\n" +
                "Full Text: $emailFullText"
    }

    private fun getSenderEmail(message: Message): String {
        val headers: List<MessagePartHeader>? = message.payload?.headers
        val fromHeader = headers?.find { it.name == "From" }
        return fromHeader?.value ?: "Unknown sender"
    }

    private fun extractEmailBody(payload: MessagePart?): String {
        if (payload == null) return "No content available"

        // Attempt to decode the body from Base64
        @RequiresApi(Build.VERSION_CODES.O)
        fun decodeBase64(encoded: String?): ByteArray? {
            return try {
                if (encoded == null) null
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getUrlDecoder().decode(encoded)
                } else {
                    TODO("VERSION.SDK_INT < O")
                } // URL-safe Base64 decoder
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // Check if there are parts
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
        emailContentList: List<String>,
        isLoading: Boolean
    ) {
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
                Text(
                    text = "Here are your unread emails:",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emailContentList.size) { index -> // Using the index to access each item
                        Card(
                            shape = MaterialTheme.shapes.medium, // Rounded corners
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp), // Padding around each card
                            //elevation = 4
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp) // Padding inside the card
                            ) {
                                Text(
                                    text = emailContentList[index], // Access the email content by index
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        MailyDailyTheme {
            Greeting("Android") {}
        }
    }
}
