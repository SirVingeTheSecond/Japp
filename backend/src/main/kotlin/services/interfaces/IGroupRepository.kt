package com.japp.services.interfaces

import com.japp.models.domain.Group
import com.japp.models.domain.GroupMemberInfo

/**
 * Repository interface for accessing group data
 */
interface IGroupRepository {
    fun create(name: String, description: String?, createdBy: Int): Group
    fun findById(id: Int): Group?
    fun findByInviteCode(code: String): Group?
    fun findByUserId(userId: Int): List<Group>
    fun getMembers(groupId: Int): List<Int>
    fun getMembersWithDetails(groupId: Int): List<GroupMemberInfo>
    fun getMembersSortedByJoinDate(groupId: Int): List<Int>
    fun isMember(groupId: Int, userId: Int): Boolean
    fun isOwner(groupId: Int, userId: Int): Boolean
    fun addMember(groupId: Int, userId: Int): Boolean
    fun removeMember(groupId: Int, userId: Int): Boolean
    fun transferOwnership(groupId: Int, newOwnerId: Int)
    fun updateTotalExpenses(groupId: Int, amount: Double)
    fun delete(groupId: Int)
}