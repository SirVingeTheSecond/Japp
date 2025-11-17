package com.japp.models.domain

data class DebtHistory(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val amountOwed: Double,
    val leftAt: String
)