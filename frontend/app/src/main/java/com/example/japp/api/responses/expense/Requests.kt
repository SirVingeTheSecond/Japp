package com.example.japp.api.responses.expense

data class CreateExpenseRequest(
    val groupId: Int,
    val amount: Double,
    val description: String,
    val category: String? = null,
    val splitType: String = "equal",
    val splits: List<ExpenseSplitRequest>? = null
)

data class ExpenseSplitRequest(
    val userId: Int,
    val shareAmount: Double? = null,
    val sharePercentage: Double? = null
)