package com.japp.models.domain

import com.japp.models.SettlementStatus

data class Settlement(
    val id: Int,
    val groupId: Int,
    val fromUserId: Int,
    val toUserId: Int,
    val amount: Double,
    val status: SettlementStatus,
    val createdAt: String,
    val completedAt: String?
)