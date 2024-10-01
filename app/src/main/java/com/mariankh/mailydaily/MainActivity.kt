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
import com.mariankh.mailydaily.mail.authenticateWithIMAP
import com.mariankh.mailydaily.ui.theme.IMAPLoginScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

object EmailStore {
    var emailHistory: MutableList<EmailContent> = mutableListOf()
}

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var userAccount: GoogleSignInAccount? by mutableStateOf(null)
    private var imapAccount: ImapAccount? by mutableStateOf(null)
    private var emailContentList: List<EmailContent> by mutableStateOf(emptyList())
    private var isLoading by mutableStateOf(false) // Ensure isLoading is tracked by Compose

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
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val data = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    handleSignInResult(task)
                } else {
                    Log.e("SIGN_IN", "Sign-in canceled or failed")
                }
            }

        setContent {

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        if (userAccount != null) {
                            ChatBotDisplay(
                                userAccount!!,
                                imapAccount =null,
                                isLoading,
                                emailContentList,
                                navController
                            )
                        } else if (imapAccount != null) {
                            ChatBotDisplay(
                                userAccount = null, // No Google Account
                                imapAccount = imapAccount,
                                isLoading = isLoading,
                                emailContentList = emailContentList,
                                navController = navController
                            )
                        }
                        else  {

                        Greeting(
                            userType = "User",
                            onGoogleSignInClick = { initiateGoogleSignIn() },
                            onIMAPSignInClick = { navController.navigate("imapLogin") }
                        )

                        }
                    }
                    composable("imapLogin") {
                        IMAPLoginScreen { username, password, imapServer, imapPort, onError ->
                            // Call a function to authenticate using IMAP and fetch emails
                            Log.d("IMAPLoginScreen", "trying to login")
                            authenticateWithIMAP(username, password, imapServer, imapPort, navController, { imapAccount ->
                                // Handle successful authentication
                                this@MainActivity.imapAccount = imapAccount
                               // fetchEmailsUsingIMAP(imapAccount) // Fetch emails if needed
                            }, onError) // Pass the error callback
                        }
                    }


                    composable("logout") {
                        LogoutScreen {
                            userAccount = null // Clear the user account
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                }


        }
    }

    private fun initiateGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        Log.d("SIGN_IN", "Initiating sign-in")
        signInLauncher.launch(signInIntent)
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
    fun fetchEmails(account: GoogleSignInAccount) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EMAIL_FETCH", "Fetching emails")
                isLoading = true // Start loading

                val credential = GoogleAccountCredential.usingOAuth2(
                    this@MainActivity, listOf("https://www.googleapis.com/auth/gmail.readonly")
                ).apply {
                    selectedAccount = account.account
                }

                val emailFunctionality = EmailFunctionality()
                val transport: HttpTransport = NetHttpTransport()
                val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

                val service = Gmail.Builder(
                    transport, jsonFactory, credential
                ).setApplicationName("MailyDaily").build()

                val response: ListMessagesResponse = service.users().messages().list("me").apply {
                    q = "newer_than:1d" // Fetch emails from the last day
                    maxResults = 3   // Limit to 10 emails
                }.execute()

                val messages = response.messages ?: emptyList()
                val emailContents = mutableListOf<EmailContent>()

                // Process emails in parallel using coroutines
                val jobs = messages.map { message ->
                    async {
                        val msg: Message = service.users().messages().get("me", message.id).execute()
                        val emailContent = emailFunctionality.extractEmailContent(msg, service)

                        val actionsDeferred = async {
                            emailFunctionality.extractRecommendedActions(
                                "FROM:" + emailContent.sender + " DATE: " + emailContent.date + " " + emailContent.fullText
                            )
                        }

                        val (summary, actions) = actionsDeferred.await()
                        emailContent.fullText = summary
                        // emailContent.actions = actions

                        emailContents.add(emailContent)
                    }
                }

                // Await all jobs to complete
                jobs.awaitAll()

                // Update UI state on the main thread
                withContext(Dispatchers.Main) {
                    emailContentList = emailContents
                    EmailStore.emailHistory.addAll(emailContents)
                    isLoading = false // Stop loading
                }

                Log.d("EMAIL_FETCH", "Emails fetched successfully")
            } catch (e: Exception) {
                Log.e("EMAIL_FETCH", "Error fetching emails", e)
                withContext(Dispatchers.Main) {
                    isLoading = false // Stop loading in case of error
                }
            }
        }
    }
}
