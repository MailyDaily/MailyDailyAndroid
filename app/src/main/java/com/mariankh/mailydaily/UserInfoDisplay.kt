package com.mariankh.mailydaily

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

@Composable
fun UserInfoDisplay(
    userAccount: GoogleSignInAccount,
    emailContentList: List<EmailContent>,
    isLoading: Boolean,
    navController: NavController
) {
    var allemailSummary by remember { mutableStateOf("") }
    var actions by remember { mutableStateOf(emptyList<ActionItem>()) }

    // Create EmailFunctionality instance
    val emailFunctionality = EmailFunctionality()
    val context = LocalContext.current

    // Summarize emails once they have been fetched
    if (!isLoading && emailContentList.isNotEmpty() && allemailSummary.isEmpty()) {
        val emails = emailContentList.joinToString("\n") { "${it.sender}: ${it.subject}" }
        LaunchedEffect(emails) {
            emailFunctionality.sendToModel(
                prompt = "Summarize these emails",
                additionalPrompt = emails,
                onResult = { summary ->
                    allemailSummary = summary
                },
                onError = { error ->
                    Toast.makeText(context, "Error summarizing emails: $error", Toast.LENGTH_LONG).show()
                }
            )
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
                            navController.navigate("logout")
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

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
                items(emailContentList) { emailContent ->
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
                    Text(
                        text = AnnotatedString(actionItem.text),
                        modifier = Modifier.clickable {
                            handleLinkClick(actionItem.url, context)
                        },
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                } else {
                    Text(
                        text = actionItem.text,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.None
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Helper function to handle link clicks (opening URL in the browser)
fun handleLinkClick(url: String, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open link: $url", Toast.LENGTH_SHORT).show()
    }
}

// Data class for an action item with a text and optional URL
data class ActionItem(val text: String, val url: String?)

// Composable for displaying an email in a card
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "From: ${emailContent.sender}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Date: ${emailContent.date}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = emailContent.fullText, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            emailContent.actions.forEach { action ->
                Button(
                    onClick = { handleAction(action) },
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

// Function to handle action clicks (for future expansion)
fun handleAction(action: ActionItem) {
    Log.d("Action", "Performing action: ${action.text} with URL: ${action.url}")
}
