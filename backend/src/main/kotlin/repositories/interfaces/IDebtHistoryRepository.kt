package com.japp.repositories.interfaces

import com.japp.models.domain.DebtHistory

interface IDebtHistoryRepository {
    fun create(groupId: Int, userId: Int, amountOwed: Double): DebtHistory
    fun findByGroupId(groupId: Int): List<DebtHistory>
    fun findByUserId(userId: Int): List<DebtHistory>
    fun hasDebtHistory(groupId: Int, userId: Int): Boolean
}