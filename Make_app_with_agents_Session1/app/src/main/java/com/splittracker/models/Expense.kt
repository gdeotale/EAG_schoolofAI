package com.splittracker.models

import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitAmong: List<String> = emptyList()
)
