package com.example.myapplication

import java.time.LocalDateTime

data class Task(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val dateTime: LocalDateTime,
    val priority: Int // 0=Important, 1=Critical, 2=Flexible
)
