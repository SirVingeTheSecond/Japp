package com.japp.repositories

import com.japp.database.tables.ActivityLogs
import com.japp.models.domain.ActivityLog
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ActivityRepository : IActivityRepository {

    init {
        transaction {
            SchemaUtils.create(ActivityLogs)
        }
    }

    override fun create(
        groupId: Int,
        userId: Int,
        actionType: String,
        description: String,
        relatedExpenseId: Int?,
        relatedSettlementId: Int?,
        metadata: String
    ): ActivityLog {
        val timestamp = System.currentTimeMillis().toString()

        val activityId = ActivityLogs.insert {
            it[ActivityLogs.groupId] = groupId
            it[ActivityLogs.userId] = userId
            it[ActivityLogs.actionType] = actionType
            it[ActivityLogs.description] = description
            it[ActivityLogs.relatedExpenseId] = relatedExpenseId
            it[ActivityLogs.relatedSettlementId] = relatedSettlementId
            it[ActivityLogs.metadata] = metadata
            it[createdAt] = timestamp
        }[ActivityLogs.id]

        return findById(activityId)!!
    }

    override fun findByGroupId(groupId: Int, limit: Int): List<ActivityLog> {
        return ActivityLogs.selectAll()
            .where { ActivityLogs.groupId eq groupId }
            .orderBy(ActivityLogs.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { rowToActivityLog(it) }
    }

    override fun findById(activityId: Int): ActivityLog? {
        return ActivityLogs.selectAll()
            .where { ActivityLogs.id eq activityId }
            .map { rowToActivityLog(it) }
            .singleOrNull()
    }

    private fun rowToActivityLog(row: ResultRow) = ActivityLog(
        id = row[ActivityLogs.id],
        groupId = row[ActivityLogs.groupId],
        userId = row[ActivityLogs.userId],
        actionType = row[ActivityLogs.actionType],
        description = row[ActivityLogs.description],
        relatedExpenseId = row[ActivityLogs.relatedExpenseId],
        relatedSettlementId = row[ActivityLogs.relatedSettlementId],
        metadata = row[ActivityLogs.metadata],
        createdAt = row[ActivityLogs.createdAt]
    )
}