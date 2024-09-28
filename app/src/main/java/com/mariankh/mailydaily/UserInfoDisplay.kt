package com.mariankh.mailydaily

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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

@Composable
fun UserInfoDisplay(
    userAccount: GoogleSignInAccount,
    emailContentList: List<EmailContent>,
    isLoading: Boolean,
    navController: NavController
) {
    var allemailSummary by remember { mutableStateOf("") }
    var actions by remember { mutableStateOf(emptyList<ActionItem>()) }

    // Summarize emails once they have been fetched
    if (!isLoading && emailContentList.isNotEmpty() && allemailSummary.isEmpty()) {
        val emails = emailContentList.map {  it.sender + "  " +it.subject }
        LaunchedEffect(emails) {
            summarizeAllEmails(emails, { summary ->
                allemailSummary = summary
            }, { error ->
                allemailSummary = "Error summarizing emails: $error"
            })
        }
    }

    // Extract actions from email content
    LaunchedEffect(emailContentList) {
        val allActions = emailContentList.flatMap { it.actions }
        actions = allActions
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
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            userAccount.photoUrl?.let { photoUrl ->
                Image(
                    painter = rememberImagePainter(photoUrl),
                    contentDescription = "User Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            // Navigate to the logout screen when the profile picture is clicked
                            navController.navigate("logout")
                        }
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
                text = "Here is an overview of your unread emails",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = allemailSummary,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Here are your unread emails:",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recommended Actions:",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            actions.forEach { actionItem ->
                if (actionItem.url != null) {
                    ClickableText(
                        text = AnnotatedString(actionItem.text),
                        onClick = { offset ->
                            val annotatedString = AnnotatedString(actionItem.text)
                            annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                                val uri = Uri.parse(annotation.item)
                                //  LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        },
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                } else {
                    Text(
                        text = actionItem.text,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
fun summarizeAllEmails(emails: List<String>, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val apiKey ="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" // Replace with your actual API key
    val url = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407/v1/chat/completions"

    val client = OkHttpClient()
    val truncatedContent = emails.take(25000) // Truncate content to fit within token limit
    val jsonBody = JSONObject().apply {
        put("model", "mistralai/Mistral-Nemo-Instruct-2407")
        put("messages", JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("content", "Hello, tell me \"X sent you an email about Y, and Y about X in a paragraph. be short and polite.  Say that you are my email AI assistant and that you are here to help me handle these effortlesly" + truncatedContent)
        }))
        put("max_tokens", 8000)
        put("temperature", 0.5)
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

data class ActionItem(val text: String, val url: String?)


@Composable
fun EmailCard(emailContent: EmailContent) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(text = "From: ${emailContent.sender}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Date: ${emailContent.date}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = emailContent.fullText, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            emailContent.actions.forEach { action ->
                Button(
                    onClick = { /* Handle action */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(action.text)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

