package com.mariankh.mailydaily.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun IMAPLoginScreen(
    onLoginClick: (username: String, password: String, imapServer: String, imapPort: String, onError: (String) -> Unit) -> Unit
) {
    var username by remember { mutableStateOf("m.terzi@girlguides.org.cy") }
    var password by remember { mutableStateOf("Mia!20589") }
    var imapServer by remember { mutableStateOf("girlguides.org.cy") }
    var imapPort by remember { mutableStateOf("143") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IMAP Login",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // IMAP Server Field
        OutlinedTextField(
            value = imapServer,
            onValueChange = { imapServer = it },
            label = { Text("IMAP Server") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // IMAP Port Field
        OutlinedTextField(
            value = imapPort,
            onValueChange = { imapPort = it },
            label = { Text("IMAP Port") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show error message if any
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Show loading spinner if loading
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Login Button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null

                    // Call the login callback and handle errors
                    onLoginClick(username, password, imapServer, imapPort) { error ->
                        errorMessage = error
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login with IMAP")
            }
        }
    }
}

