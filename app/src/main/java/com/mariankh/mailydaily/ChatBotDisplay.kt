package com.mariankh.mailydaily

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Sealed class to represent chat messages from both user and bot
sealed class Message {
    data class UserMessage(val text: String) : Message()
    data class BotMessage(val text: String) : Message()
}

@Composable
fun ChatBotDisplay(
    userAccount: GoogleSignInAccount,
    isLoading: Boolean,
    emailContentList: List<EmailContent>
) {
    var messageList by remember { mutableStateOf(listOf<Message>()) }
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var allemailSummary by remember { mutableStateOf("") }
    val emailFunctionality = remember { EmailFunctionality() }

    // Function to send a message and scroll to the latest message
    fun sendMessage(message: Message) {
        messageList = messageList + message
        coroutineScope.launch {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    // Summarize emails once loaded and available
    LaunchedEffect(isLoading, emailContentList) {
        if (!isLoading && emailContentList.isNotEmpty() && allemailSummary.isEmpty()) {
            val emails = emailContentList.map { "${it.sender} ${it.subject}" }
            coroutineScope.launch(Dispatchers.IO) {
                val (summary, actions) = emailFunctionality.extractRecommendedActions(emails.joinToString(separator = "\n"))
                coroutineScope.launch(Dispatchers.Main) {
                    allemailSummary = summary
                }
            }
        }
    }

    // Send summary message after fetching and summarizing emails
    LaunchedEffect(isLoading, allemailSummary) {
        if (!isLoading && allemailSummary.isNotEmpty()) {
            sendMessage(Message.BotMessage(allemailSummary))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Header with greeting and profile image
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Hello, ${userAccount.displayName}!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            userAccount.photoUrl?.let { photoUrl ->
                Image(
                    painter = rememberImagePainter(photoUrl),
                    contentDescription = "User Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Chat bot interface (scrollable)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display messages
            items(messageList) { message ->
                when (message) {
                    is Message.UserMessage -> UserMessageBubble(message.text)
                    is Message.BotMessage -> BotMessageBubble(message.text)
                }
            }
        }

        // User input box and send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { newValue -> userInput = newValue },
                placeholder = { Text(text = "Type your message...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (userInput.text.isNotEmpty()) {
                    val userMessage = Message.UserMessage(userInput.text)
                    sendMessage(userMessage)
                    userInput = TextFieldValue("")

                    // Simulate bot response
                    coroutineScope.launch(Dispatchers.IO) {
                        val (summary, actions) = emailFunctionality.extractRecommendedActions(userMessage.text)
                        coroutineScope.launch(Dispatchers.Main) {
                            sendMessage(Message.BotMessage(summary))
                        }
                    }
                }
            }) {
                Text(text = "Send")
            }
        }
    }
}

// Composable function to display a bot message
@Composable
fun BotMessageBubble(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.Start)
            .padding(8.dp)
    ) {
        Text(
            text = message,
            style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                .padding(12.dp)
        )
    }
}

// Composable function to display a user message
@Composable
fun UserMessageBubble(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.End)
            .padding(8.dp)
    ) {
        Text(
            text = message,
            style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(12.dp)
        )
    }
}
