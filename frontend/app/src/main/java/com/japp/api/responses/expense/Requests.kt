package com.japp.api.responses.expense

import com.japp.api.responses.SplitType
import com.japp.api.responses.Currency
data class CreateExpenseRequest(
    val groupId: Int,
    val amount: Double,
    val description: String,
    val category: String? = null,
    val currency: Currency,
    val splitType: SplitType,
    val splits: List<ExpenseSplitRequest>? = null
)

data class ExpenseSplitRequest(
    val userId: Int,
    val shareAmount: Double? = null,
    val sharePercentage: Double? = null
)