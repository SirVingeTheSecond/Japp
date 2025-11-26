package com.japp.repositories.implementations

import com.japp.database.tables.DebtHistory
import com.japp.models.domain.DebtHistory as DebtHistoryModel
import com.japp.services.interfaces.IDebtHistoryRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*

class DebtHistoryRepository : IDebtHistoryRepository {

    override fun create(groupId: Int, userId: Int, amountOwed: Double): DebtHistoryModel {
        val timestamp = System.currentTimeMillis().toString()

        val debtId = DebtHistory.insert {
            it[DebtHistory.groupId] = groupId
            it[DebtHistory.userId] = userId
            it[DebtHistory.amountOwed] = amountOwed
            it[leftAt] = timestamp
        }[DebtHistory.id]

        return DebtHistoryModel(
            id = debtId,
            groupId = groupId,
            userId = userId,
            amountOwed = amountOwed,
            leftAt = timestamp
        )
    }

    override fun findByGroupId(groupId: Int): List<DebtHistoryModel> {
        return DebtHistory.selectAll()
            .where { DebtHistory.groupId eq groupId }
            .map { rowToDebtHistory(it) }
    }

    override fun findByUserId(userId: Int): List<DebtHistoryModel> {
        return DebtHistory.selectAll()
            .where { DebtHistory.userId eq userId }
            .map { rowToDebtHistory(it) }
    }

    override fun hasDebtHistory(groupId: Int, userId: Int): Boolean {
        return DebtHistory.selectAll()
            .where { (DebtHistory.groupId eq groupId) and (DebtHistory.userId eq userId) }
            .count() > 0
    }

    private fun rowToDebtHistory(row: ResultRow) = DebtHistoryModel(
        id = row[DebtHistory.id],
        groupId = row[DebtHistory.groupId],
        userId = row[DebtHistory.userId],
        amountOwed = row[DebtHistory.amountOwed],
        leftAt = row[DebtHistory.leftAt]
    )
}