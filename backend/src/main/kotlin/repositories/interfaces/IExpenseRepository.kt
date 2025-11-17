package com.japp.repositories.interfaces

import com.japp.models.Currency
import com.japp.models.ExpenseCategory
import com.japp.models.SplitType
import com.japp.models.domain.Expense
import com.japp.models.domain.ExpenseSplit

interface IExpenseRepository {
    fun create(
        groupId: Int,
        paidBy: Int,
        amount: Double,
        currency: Currency,
        description: String,
        category: ExpenseCategory?,
        splitType: SplitType
    ): Expense

    fun createSplit(
        expenseId: Int,
        userId: Int,
        shareAmount: Double?,
        sharePercentage: Double?
    ): ExpenseSplit

    fun findById(expenseId: Int): Expense?
    fun findByGroupId(groupId: Int): List<Expense>
    fun getSplits(expenseId: Int): List<ExpenseSplit>
    fun delete(expenseId: Int): Boolean
    fun calculateGroupBalances(groupId: Int): Map<Int, Double>
}