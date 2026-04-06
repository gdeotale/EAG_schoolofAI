package com.splittracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onCreate: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Group") },
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
                "Groups help you organize expenses among a specific set of friends.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name (e.g., Goa Trip)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Group")
            }
        }
    }
}
