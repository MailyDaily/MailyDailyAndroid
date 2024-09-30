package com.mariankh.mailydaily

import okhttp3.*
import org.json.JSONObject
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

// Define the EmailContent class (or import if defined elsewhere)
data class EmailContent(
    val id: String,
    val date: String,
    val sender: String,
    val subject: String,
    var fullText: String,
    var actions: List<ActionItem>
)

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var userAccount: GoogleSignInAccount? by mutableStateOf(null)
    private var emailContentList: List<EmailContent> by mutableStateOf(emptyList())
    private var isLoading by mutableStateOf(false)

    // Define a constant for handling authorization result
    private val REQUEST_AUTHORIZATION = 1001

    @RequiresApi(26)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In with Gmail Read-Only scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Register sign-in result handler
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
        }

        setContent {
            MailyDailyTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        // Check if the user is already signed in and display appropriate screen
                        if (userAccount != null) {
                            UserInfoDisplay(userAccount!!, emailContentList, isLoading, navController)
                        } else {
                            Greeting("Sign In") {
                                initiateSignIn()
                            }
                        }
                    }
                    composable("logout") {
                        LogoutScreen {
                            // Handle logout logic here
                            userAccount = null // Clear the user account
                            navController.navigate("home") // Navigate back to home
                        }
                    }
                }
            }
        }
    }

    // Function to initiate Google Sign-In process
    private fun initiateSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // Handle the result from Google Sign-In
    @RequiresApi(26)
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let {
                userAccount = account // Store the signed-in account
                fetchEmails(it) // Start fetching emails once signed in
            }
        } catch (e: ApiException) {
            Log.e("SIGN_IN", "Sign-in failed: ${e.statusCode} - ${e.message}", e)
        }
    }

    // Fetch emails from Gmail API using the signed-in account
    @RequiresApi(26)
    fun fetchEmails(account: GoogleSignInAccount) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EMAIL_FETCH", "Fetching emails...")
                isLoading = true
                // Configure the Gmail API credentials with the signed-in account
                val credential = GoogleAccountCredential.usingOAuth2(
                    this@MainActivity, listOf("https://www.googleapis.com/auth/gmail.readonly")
                ).apply {
                    selectedAccount = account.account
                }

                val emailFunctionality = EmailFunctionality()
                val transport: HttpTransport = NetHttpTransport()
                val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

                // Create the Gmail service object
                val service = Gmail.Builder(transport, jsonFactory, credential)
                    .setApplicationName("MailyDaily")
                    .build()

                // Fetch the latest emails
                val response: ListMessagesResponse = service.users().messages().list("me").apply {
                    q = "newer_than:1d" // Fetch emails from the last day
                    maxResults = 3    // Limit to 3 emails
                }.execute()

                val messages = response.messages ?: emptyList()
                val emailContents = mutableListOf<EmailContent>()

                // Process emails in parallel using coroutines
                val jobs = messages.map { message ->
                    async {
                        val msg: Message = service.users().messages().get("me", message.id).execute()
                        val emailContent = emailFunctionality.extractEmailContent(msg)

                        // Classify and extract actions for the email content
                        val actionsDeferred = async {
                            emailFunctionality.extractRecommendedActions(
                                "FROM:" + emailContent.sender + " DATE: " + emailContent.date + " " + emailContent.fullText
                            )
                        }

                        val (summary, actions) = actionsDeferred.await()

                        // Update the email content with the fetched summary and actions
                        emailContent.fullText = summary
                        emailContent.actions = actions

                        emailContents.add(emailContent)
                    }
                }

                // Wait for all jobs to complete
                jobs.awaitAll()

                // Update UI state on the main thread
                withContext(Dispatchers.Main) {
                    emailContentList = emailContents
                    isLoading = false
                }

                Log.d("EMAIL_FETCH", "Emails fetched successfully")
            } catch (e: UserRecoverableAuthIOException) {
                // If authorization is needed, request consent from the user
                withContext(Dispatchers.Main) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }
            } catch (e: Exception) {
                Log.e("EMAIL_FETCH", "Error fetching emails", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    // Handle the result of the authorization request
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                // Retry fetching emails after user grants authorization
                userAccount?.let {
                    fetchEmails(it)
                }
            } else {
                Log.e("AUTH", "User denied or failed to give consent")
            }
        }
    }
}
