package com.splittracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import com.splittracker.models.Expense
import com.splittracker.models.Group

import androidx.compose.ui.platform.LocalContext
import android.media.RingtoneManager
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailsScreen(
    group: Group,
    expenses: List<Expense>,
    userMap: Map<String, String>,
    currentUserEmail: String,
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onInviteFriend: (String) -> Unit,
    onSettleUp: (Double, String, String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onEditExpense: (String, String, Double) -> Unit
) {
    fun displayFor(email: String) = userMap[email] ?: email

    var expenseActionsFor by remember { mutableStateOf<Expense?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    // Calculate net balance for current user
    var myBalance = 0.0
    expenses.forEach { expense ->
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

    val context = LocalContext.current
    var selectedMemberForBalance by remember { mutableStateOf<String?>(null) }
    var showChart by remember { mutableStateOf(false) }

    expenseActionsFor?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseActionsFor = null },
            title = { Text("Expense Actions") },
            text = { Text("What would you like to do with '${exp.description}'?") },
            confirmButton = {
                Button(onClick = { 
                    expenseToEdit = exp
                    expenseActionsFor = null 
                }) { Text("Edit") }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        onDeleteExpense(exp.id)
                        expenseActionsFor = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        )
    }

    expenseToEdit?.let { exp ->
        var editDesc by remember { mutableStateOf(exp.description) }
        var editAmt by remember { mutableStateOf(exp.amount.toString()) }
        AlertDialog(
            onDismissRequest = { expenseToEdit = null },
            title = { Text("Edit Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") }
                    )
                    OutlinedTextField(
                        value = editAmt,
                        onValueChange = { editAmt = it },
                        label = { Text("Amount (Rs)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amtDouble = editAmt.toDoubleOrNull()
                    if (editDesc.isNotBlank() && amtDouble != null && amtDouble > 0) {
                        onEditExpense(exp.id, editDesc, amtDouble)
                        expenseToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { expenseToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Chart Dialog
    if (showChart) {
        val memberBalances = group.members.filter { it != currentUserEmail }.map { member ->
            var bal = 0.0
            expenses.forEach { expense ->
                val splitAmount = expense.amount / expense.splitAmong.size
                if (expense.paidBy == currentUserEmail && expense.splitAmong.contains(member)) {
                    bal += splitAmount
                } else if (expense.paidBy == member && expense.splitAmong.contains(currentUserEmail)) {
                    bal -= splitAmount
                }
            }
            member to bal
        }

        AlertDialog(
            onDismissRequest = { showChart = false },
            title = { Text("Balance Chart") },
            text = {
                val maxAbs = memberBalances.maxOfOrNull { Math.abs(it.second) } ?: 1.0
                val scale = if (maxAbs == 0.0) 1.0 else maxAbs

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(memberBalances) { (member, balance) ->
                        Column {
                            Text(text = displayFor(member), style = MaterialTheme.typography.labelMedium)
                            Row(modifier = Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    if (balance < 0) {
                                        val fraction = (-balance / scale).toFloat()
                                        if (fraction > 0f) {
                                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color = MaterialTheme.colorScheme.error))
                                        }
                                    }
                                }
                                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (balance > 0) {
                                        val fraction = (balance / scale).toFloat()
                                        if (fraction > 0f) {
                                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color = MaterialTheme.colorScheme.primary))
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if(balance > 0) "+%.2f".format(balance) else if(balance < 0) "%.2f".format(balance) else "0",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(if (balance >= 0) Alignment.End else Alignment.Start)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChart = false }) { Text("Close") }
            }
        )
    }

    selectedMemberForBalance?.let { member ->
        var balanceWithMember = 0.0
        expenses.forEach { expense ->
            val splitAmount = expense.amount / expense.splitAmong.size
            if (expense.paidBy == currentUserEmail && expense.splitAmong.contains(member)) {
                balanceWithMember += splitAmount
            } else if (expense.paidBy == member && expense.splitAmong.contains(currentUserEmail)) {
                balanceWithMember -= splitAmount
            }
        }
        
        var memberTotalBalance = 0.0
        expenses.forEach { expense ->
            val splitAmount = expense.amount / expense.splitAmong.size
            if (expense.paidBy == member) {
                val portionForOthers = expense.amount - (if (expense.splitAmong.contains(member)) splitAmount else 0.0)
                memberTotalBalance += portionForOthers
            } else {
                if (expense.splitAmong.contains(member)) {
                    memberTotalBalance -= splitAmount
                }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedMemberForBalance = null },
            title = { Text("Balance with ${displayFor(member)}") },
            text = { 
                Column {
                    if (balanceWithMember > 0) {
                        Text("${displayFor(member)} owes you %.2f Rs".format(balanceWithMember), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    } else if (balanceWithMember < 0) {
                        Text("You owe ${displayFor(member)} %.2f Rs".format(-balanceWithMember), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text("You and ${displayFor(member)} are settled up!", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    if (memberTotalBalance == 0.0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${displayFor(member)} is fully settled up with everyone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                if (balanceWithMember != 0.0) {
                    Button(onClick = { 
                        try {
                            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val r = RingtoneManager.getRingtone(context, notification)
                            r.play()
                        } catch (e: Exception) {}
                        
                        if (balanceWithMember > 0) {
                            onSettleUp(balanceWithMember, member, currentUserEmail)
                        } else {
                            onSettleUp(-balanceWithMember, currentUserEmail, member)
                        }
                        selectedMemberForBalance = null 
                    }) {
                        Text("Settle Up")
                    }
                } else if (memberTotalBalance == 0.0) {
                    Button(
                        onClick = {
                            onRemoveMember(member)
                            selectedMemberForBalance = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove from Group")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMemberForBalance = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (group.name != "Self Expense Tracker") {
                        IconButton(onClick = { onInviteFriend(group.id) }) {
                            Icon(Icons.Default.Add, contentDescription = "Invite")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (group.name == "Self Expense Tracker" || group.members.size >= 2) {
                ExtendedFloatingActionButton(
                    onClick = { onAddExpense(group.id) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                    text = { Text("Add Expense") }
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (group.name == "Self Expense Tracker") {
                        val totalSpent = expenses.sumOf { it.amount }
                        Text("Total Spent: %.2f Rs".format(totalSpent), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Total Lent: %.2f Rs".format(finalLent), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Total Borrowed: %.2f Rs".format(finalBorrowed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Tap a member to see what you owe them:",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(group.members) { member ->
                        if (member != currentUserEmail) {
                            AssistChip(
                                onClick = { selectedMemberForBalance = member },
                                label = { Text(displayFor(member)) }
                            )
                        }
                    }
                }
                
                if (group.members.size >= 2) {
                    Button(
                        onClick = { showChart = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("View Balance Chart")
                    }
                }
                
                if (myBalance == 0.0 && group.name != "Self Expense Tracker") {
                    Button(
                        onClick = { onRemoveMember(currentUserEmail) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Leave Group (All Settled)")
                    }
                }
                
                if (group.name != "Self Expense Tracker" && group.members.size < 2) {
                    Text(
                        text = "You need at least 2 members to start adding expenses. Tap the '+' in the top right to invite friends!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (expenses.isEmpty() && (group.name == "Self Expense Tracker" || group.members.size >= 2)) {
                item {
                    Text(
                        "No expenses yet. Add one!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(expenses) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    expenseActionsFor = expense
                                }
                            )
                    ) {
                        ListItem(
                            headlineContent = { Text(expense.description) },
                            supportingContent = { Text("${displayFor(expense.paidBy)} paid - split among ${expense.splitAmong.size}") },
                            trailingContent = {
                                Text(
                                    "${expense.amount} Rs",
                                    color = if (expense.paidBy == currentUserEmail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
