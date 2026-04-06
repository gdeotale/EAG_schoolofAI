package com.splittracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendScreen(
    groupId: String,
    groupName: String,
    senderName: String,
    onBack: () -> Unit,
    onInvite: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Friend") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Users log into Split Tracker securely via Google. Please enter their Google email address below.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        val trimmedEmail = email.trim()
                        onInvite(trimmedEmail) // Adds them silently to group database

                        // Automatically composes the external email intent using ACTION_SENDTO with URI parameters
                        val subject = android.net.Uri.encode("You're invited to $groupName!")
                        val body = android.net.Uri.encode("Hi you have been invited to $groupName by $senderName\n\nDownload the app from the Play Store here: https://play.google.com/store/apps/details?id=com.splittracker\n\nOnce installed, click this link to instantly join the group: splittracker://join?groupId=$groupId")
                        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:$trimmedEmail?subject=$subject&body=$body")
                        }
                        
                        try {
                            context.startActivity(Intent.createChooser(mailIntent, "Send Invite Email via..."))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No email app found!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Invite Link")
            }
        }
    }
}
