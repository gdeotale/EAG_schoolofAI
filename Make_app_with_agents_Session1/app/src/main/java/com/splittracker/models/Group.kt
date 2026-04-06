package com.splittracker.models

import java.util.UUID

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    var members: MutableList<String> = mutableListOf()
)
