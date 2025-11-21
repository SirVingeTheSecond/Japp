package com.japp.api.responses.expense

import com.japp.api.responses.Currency
import com.japp.api.responses.ExpenseCategory
import com.japp.api.responses.SplitType

data class CreateExpenseRequest(
    val groupId: Int,
    val amount: Double,
    val description: String,
    val category: ExpenseCategory? = null,
    val currency: Currency = Currency.DKK,
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<ExpenseSplitRequest>? = null
)

data class ExpenseSplitRequest(
    val userId: Int,
    val shareAmount: Double? = null,
    val sharePercentage: Double? = null
)