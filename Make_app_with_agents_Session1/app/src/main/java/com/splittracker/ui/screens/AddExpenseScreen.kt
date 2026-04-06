package com.splittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    groupId: String,
    members: List<String>,
    userMap: Map<String, String>,
    currentUserEmail: String,
    onBack: () -> Unit,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    // Who paid state
    var paidByExpanded by remember { mutableStateOf(false) }
    var paidBy by remember { mutableStateOf(currentUserEmail) } // Default to current user

    // Split among state (multi select)
    val splitAmong = remember { mutableStateListOf<String>().apply { addAll(members) } }

    fun displayFor(email: String) = userMap[email] ?: email

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
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
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What was this for?") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (Rs)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            // Paid By Dropdown
            ExposedDropdownMenuBox(
                expanded = paidByExpanded,
                onExpandedChange = { paidByExpanded = !paidByExpanded }
            ) {
                OutlinedTextField(
                    value = displayFor(paidBy),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Who Paid?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = paidByExpanded,
                    onDismissRequest = { paidByExpanded = false }
                ) {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(displayFor(member)) },
                            onClick = {
                                paidBy = member
                                paidByExpanded = false
                            }
                        )
                    }
                }
            }
            
            Text("Split equally among:", style = MaterialTheme.typography.titleMedium)
            
            // Checklist for splitting
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (splitAmong.contains(member)) {
                                    splitAmong.remove(member)
                                } else {
                                    splitAmong.add(member)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = splitAmong.contains(member),
                            onCheckedChange = null // Handled by Row click
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayFor(member))
                    }
                }
            }

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amountDouble > 0 && splitAmong.isNotEmpty()) {
                        onSave(description, amountDouble, paidBy, splitAmong.toList())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = splitAmong.isNotEmpty()
            ) {
                Text("Save Expense")
            }
        }
    }
}
