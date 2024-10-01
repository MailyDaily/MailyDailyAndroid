package com.mariankh.mailydaily.mail

import android.util.Log
import androidx.navigation.NavController
import com.mariankh.mailydaily.EmailContent
import com.mariankh.mailydaily.EmailStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.mail.*
import javax.mail.internet.MimeMessage
import java.util.Properties

fun authenticateWithIMAP(
    username: String,
    password: String,
    imapServer: String,
    imapPort: String,
    navController: NavController,
    onError: (String) -> Unit
) {
    // Run the network request in the background
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imap.host", imapServer)
                put("mail.imap.port", imapPort)
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.connectiontimeout", "10000") // Set timeout
                put("mail.imap.timeout", "10000") // Set timeout
            }

            Log.d("IMAPLoginScreen","Trying to login...")
            val session = Session.getInstance(props, null)
            val store = session.getStore("imaps")
            store.connect(imapServer, username, password)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)

            val messages: Array<Message> = inbox.messages.takeLast(1).toTypedArray()

            // Fetch email content
            val emailList = messages.map { message ->
                val mimeMessage = message as MimeMessage
                EmailContent(
                    id = mimeMessage.messageID,
                    sender = mimeMessage.from.firstOrNull().toString(),
                    subject = mimeMessage.subject ?: "No subject",
                    date = mimeMessage.sentDate.toString(),
                    fullText = mimeMessage.content.toString(),
                    actions = null,
                    category = " ",
                    snippet = mimeMessage.content.toString(),
                )
            }

            // Navigate back to home and display emails
            withContext(Dispatchers.Main) {
                EmailStore.emailHistory.addAll(emailList)
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }

        } catch (e: Exception) {
            Log.e("IMAP_AUTH", "IMAP authentication failed: ${e.message}", e)
            // Handle authentication failure by passing the error message to the UI
            withContext(Dispatchers.Main) {
                onError("Authentication failed: ${e.message}")
            }
        }
    }
}
