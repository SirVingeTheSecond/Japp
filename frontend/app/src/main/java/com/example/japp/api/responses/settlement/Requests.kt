package com.example.japp.api.responses.settlement

data class CreateSettlementRequest(
    val groupId: Int,
    val toUserId: Int,
    val amount: Double
)