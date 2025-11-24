package com.japp.services.interfaces

import com.japp.models.domain.Settlement

interface ISettlementRepository {
    fun create(
        groupId: Int,
        fromUserId: Int,
        toUserId: Int,
        amount: Double
    ): Settlement

    fun findById(settlementId: Int): Settlement?
    fun findByGroupId(groupId: Int): List<Settlement>
    fun findPendingByGroupId(groupId: Int): List<Settlement>
    fun markAsCompleted(settlementId: Int): Settlement?
    fun delete(settlementId: Int): Boolean
}