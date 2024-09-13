package com.mariankh.mailydaily

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var userAccount: GoogleSignInAccount? by mutableStateOf(null)
    private var emailContentList: List<String> by mutableStateOf(emptyList())
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                    val emailContent = extractEmailContent(msg)
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

    private fun extractEmailContent(message: Message): String {
        // Extract email content from the Message object
        return message.snippet ?: "No content"
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
    fun UserInfoDisplay(userAccount: GoogleSignInAccount, emailContentList: List<String>, isLoading: Boolean) {
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
                Text(text = "Here are your unread emails:", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                emailContentList.forEach { emailContent ->
                    Text(text = emailContent, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
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
