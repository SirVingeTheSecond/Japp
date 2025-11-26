package com.japp.repositories.implementations

import com.japp.database.tables.Settlements
import com.japp.models.SettlementStatus
import com.japp.models.domain.Settlement
import com.japp.services.interfaces.ISettlementRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SettlementRepository : ISettlementRepository {

    init {
        transaction {
            SchemaUtils.create(Settlements)
        }
    }

    override fun create(
        groupId: Int,
        fromUserId: Int,
        toUserId: Int,
        amount: Double
    ): Settlement {
        val timestamp = System.currentTimeMillis().toString()

        val settlementId = Settlements.insert {
            it[Settlements.groupId] = groupId
            it[Settlements.fromUserId] = fromUserId
            it[Settlements.toUserId] = toUserId
            it[Settlements.amount] = amount
            it[status] = SettlementStatus.PENDING.value
            it[createdAt] = timestamp
            it[completedAt] = null
        }[Settlements.id]

        return findById(settlementId)!!
    }

    override fun findById(settlementId: Int): Settlement? {
        return Settlements.selectAll()
            .where { Settlements.id eq settlementId }
            .map { rowToSettlement(it) }
            .singleOrNull()
    }

    override fun findByGroupId(groupId: Int): List<Settlement> {
        return Settlements.selectAll()
            .where { Settlements.groupId eq groupId }
            .orderBy(Settlements.createdAt to SortOrder.DESC)
            .map { rowToSettlement(it) }
    }

    override fun findPendingByGroupId(groupId: Int): List<Settlement> {
        return Settlements.selectAll()
            .where {
                (Settlements.groupId eq groupId) and
                        (Settlements.status eq SettlementStatus.PENDING.value)
            }
            .orderBy(Settlements.createdAt to SortOrder.DESC)
            .map { rowToSettlement(it) }
    }

    override fun markAsCompleted(settlementId: Int): Settlement? {
        val timestamp = System.currentTimeMillis().toString()

        Settlements.update({ Settlements.id eq settlementId }) {
            it[status] = SettlementStatus.COMPLETED.value
            it[completedAt] = timestamp
        }

        return findById(settlementId)
    }

    override fun delete(settlementId: Int): Boolean {
        val deleted = Settlements.deleteWhere { Settlements.id eq settlementId }
        return deleted > 0
    }

    private fun rowToSettlement(row: ResultRow) = Settlement(
        id = row[Settlements.id],
        groupId = row[Settlements.groupId],
        fromUserId = row[Settlements.fromUserId],
        toUserId = row[Settlements.toUserId],
        amount = row[Settlements.amount],
        status = SettlementStatus.fromString(row[Settlements.status]) ?: SettlementStatus.PENDING,
        createdAt = row[Settlements.createdAt],
        completedAt = row[Settlements.completedAt]
    )
}