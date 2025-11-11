package com.japp.models.domain

data class Settlement(
    val id: Int,
    val groupId: Int,
    val fromUserId: Int,
    val toUserId: Int,
    val amount: Double,
    val completed: Boolean,
    val createdAt: String,
    val completedAt: String?
)