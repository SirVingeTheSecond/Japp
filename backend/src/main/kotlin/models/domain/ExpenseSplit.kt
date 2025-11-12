package com.japp.models.domain

data class ExpenseSplit(
    val id: Int,
    val expenseId: Int,
    val userId: Int,
    val shareAmount: Double?,
    val sharePercentage: Double?
)