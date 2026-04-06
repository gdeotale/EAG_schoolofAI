package com.splittracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.splittracker.R
import com.splittracker.models.Expense
import com.splittracker.models.Group

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    groups: List<Group>,
    expenses: List<Expense>,
    userMap: Map<String, String>,
    currentUserEmail: String,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSignOut: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialogFor by remember { mutableStateOf<Group?>(null) }

    showDeleteDialogFor?.let { group ->
        val groupExpenses = expenses.filter { it.groupId == group.id }
        var isSettledUp = true
        for (member in group.members) {
            var bal = 0.0
            groupExpenses.forEach { exp ->
                val split = exp.amount / exp.splitAmong.size
                if (exp.paidBy == member) {
                    bal += exp.amount - (if (exp.splitAmong.contains(member)) split else 0.0)
                } else if (exp.splitAmong.contains(member)) {
                    bal -= split
                }
            }
            if (kotlin.math.abs(bal) > 0.01) {
                isSettledUp = false
                break
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialogFor = null },
            title = { Text("Delete Group ${group.name}?") },
            text = {
                if (isSettledUp) {
                    Text("Everyone in this group is settled up! Are you sure you want to permanently delete this group?")
                } else {
                    Text("You cannot delete this group yet. Members still have pending unsettled balances with each other.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                if (isSettledUp) {
                    Button(
                        onClick = {
                            onDeleteGroup(group.id)
                            showDeleteDialogFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Group")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogFor = null }) {
                    Text(if (isSettledUp) "Cancel" else "OK")
                }
            }
        )
    }

    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        val currentName = userMap[currentUserEmail] ?: "Loading..."
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Profile Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: $currentName", style = MaterialTheme.typography.bodyLarge)
                    Text("Email: $currentUserEmail", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.groups_bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isDarkTheme) 0.3f else 0.7f
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("My Groups") },
                    actions = {
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile")
                        }
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Sign Out")
                        }
                        IconButton(onClick = onToggleTheme) {
                            if (isDarkTheme) {
                                Icon(Icons.Filled.Build, contentDescription = "Light Mode")
                            } else {
                                Icon(Icons.Filled.Star, contentDescription = "Dark Mode")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateGroup,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create Group") },
                    text = { Text("Create New Group") }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (groups.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No groups yet!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "We are looking for groups containing the email:\n$currentUserEmail\n\nIf your friend invited you, ensure they typed this exact address without any typos or missing dots. Or press the + button to create a new group!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(groups) { group ->
                    val groupExpenses = expenses.filter { it.groupId == group.id }
                    var myBalance = 0.0
                    groupExpenses.forEach { expense ->
                        val splitAmount = expense.amount / expense.splitAmong.size
                        if (expense.paidBy == currentUserEmail) {
                            val portionForOthers = expense.amount - (if (expense.splitAmong.contains(currentUserEmail)) splitAmount else 0.0)
                            myBalance += portionForOthers
                        } else {
                            if (expense.splitAmong.contains(currentUserEmail)) {
                                myBalance -= splitAmount
                            }
                        }
                    }
                    val finalLent = if (myBalance > 0) myBalance else 0.0
                    val finalBorrowed = if (myBalance < 0) -myBalance else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { onNavigateToGroup(group.id) },
                                onLongClick = { 
                                    if (group.name != "Self Expense Tracker") {
                                        showDeleteDialogFor = group 
                                    } else {
                                        android.widget.Toast.makeText(context, "Cannot delete your default expense tracker!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                val palette = listOf(
                                    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), 
                                    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFFFF8A65), 
                                    Color(0xFF4DB6AC), Color(0xFFFFB74D)
                                )
                                val groupColor = palette[kotlin.math.abs(group.id.hashCode()) % palette.size]
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(groupColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = if (group.name.isNotBlank()) group.name.take(1).uppercase() else "?"
                                    Text(initial, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            },
                            headlineContent = { Text(group.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                Column {
                                    Text("${group.members.size} members")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (finalLent > 0) {
                                            Text("Lent: %.2f Rs".format(finalLent), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (finalBorrowed > 0) {
                                            Text("Borrowed: %.2f Rs".format(finalBorrowed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (finalLent == 0.0 && finalBorrowed == 0.0) {
                                            Text("Settled up", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
