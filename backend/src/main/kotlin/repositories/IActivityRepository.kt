package com.japp.repositories

import com.japp.models.ActivityType
import com.japp.models.domain.ActivityLog

interface IActivityRepository {
    fun create(
        groupId: Int,
        userId: Int,
        actionType: ActivityType,
        description: String,
        relatedExpenseId: Int? = null,
        relatedSettlementId: Int? = null,
        metadata: String = "{}"
    ): ActivityLog

    fun findByGroupId(groupId: Int, limit: Int = 50): List<ActivityLog>
    fun findById(activityId: Int): ActivityLog?
}