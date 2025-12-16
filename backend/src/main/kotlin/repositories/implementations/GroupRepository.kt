package com.japp.repositories.implementations

import com.japp.database.tables.ActivityLogs
import com.japp.database.tables.Attachments
import com.japp.database.tables.DebtHistory
import com.japp.database.tables.ExpenseSplits
import com.japp.database.tables.Expenses
import com.japp.database.tables.GroupMembers
import com.japp.database.tables.Groups
import com.japp.database.tables.MessageReadStatus
import com.japp.database.tables.Messages
import com.japp.database.tables.Settlements
import com.japp.database.tables.Users
import com.japp.models.domain.Group
import com.japp.models.domain.GroupMemberInfo
import com.japp.models.domain.User
import com.japp.services.interfaces.IGroupRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import java.util.UUID

class GroupRepository : IGroupRepository {

    override fun create(name: String, description: String?, createdBy: Int): Group {
        val inviteCode = generateInviteCode()
        val timestamp = System.currentTimeMillis().toString()

        val groupId = Groups.insert {
            it[Groups.name] = name
            it[Groups.description] = description
            it[Groups.inviteCode] = inviteCode
            it[Groups.createdBy] = createdBy
            it[memberCount] = 1
            it[totalExpenses] = 0.0
            it[createdAt] = timestamp
            it[updatedAt] = timestamp
        }[Groups.id]

        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = createdBy
            it[joinedAt] = timestamp
        }

        return findById(groupId)!!
    }

    override fun findById(id: Int): Group? {
        return Groups.selectAll()
            .where { Groups.id eq id }
            .map { rowToGroup(it) }
            .singleOrNull()
    }

    override fun findByInviteCode(code: String): Group? {
        return Groups.selectAll()
            .where { Groups.inviteCode eq code }
            .map { rowToGroup(it) }
            .singleOrNull()
    }

    override fun findByUserId(userId: Int): List<Group> {
        return (Groups innerJoin GroupMembers)
            .selectAll()
            .where { GroupMembers.userId eq userId }
            .map { rowToGroup(it) }
    }

    override fun getMembers(groupId: Int): List<Int> {
        return GroupMembers.selectAll()
            .where { GroupMembers.groupId eq groupId }
            .map { it[GroupMembers.userId] }
    }

    override fun getMembersWithDetails(groupId: Int): List<GroupMemberInfo> {
        return (GroupMembers innerJoin Users)
            .selectAll()
            .where { GroupMembers.groupId eq groupId }
            .map { row ->
                GroupMemberInfo(
                    user = User(
                        id = row[Users.id],
                        username = row[Users.username],
                        firstname = row[Users.firstname],
                        lastname = row[Users.lastname],
                        email = row[Users.email],
                        passwordHash = row[Users.passwordHash],
                        phone = row[Users.phone],
                        profilePicture = row[Users.profilePicture],
                        fcmToken = row[Users.fcmToken],
                        createdAt = row[Users.createdAt]
                    ),
                    joinedAt = row[GroupMembers.joinedAt]
                )
            }
    }

    override fun getMembersSortedByJoinDate(groupId: Int): List<Int> {
        return GroupMembers.selectAll()
            .where { GroupMembers.groupId eq groupId }
            .orderBy(GroupMembers.joinedAt to SortOrder.ASC)
            .map { it[GroupMembers.userId] }
    }

    override fun isMember(groupId: Int, userId: Int): Boolean {
        return GroupMembers.selectAll()
            .where { (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId) }
            .count() > 0
    }

    override fun isOwner(groupId: Int, userId: Int): Boolean {
        return Groups.selectAll()
            .where { (Groups.id eq groupId) and (Groups.createdBy eq userId) }
            .count() > 0
    }

    override fun addMember(groupId: Int, userId: Int): Boolean {
        if (isMember(groupId, userId)) {
            return false
        }

        val timestamp = System.currentTimeMillis().toString()

        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = userId
            it[joinedAt] = timestamp
        }

        Groups.update({ Groups.id eq groupId }) {
            it[memberCount] = memberCount + 1
            it[updatedAt] = timestamp
        }

        return true
    }

    override fun removeMember(groupId: Int, userId: Int): Boolean {
        val deleted = GroupMembers.deleteWhere {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }

        if (deleted > 0) {
            val timestamp = System.currentTimeMillis().toString()
            Groups.update({ Groups.id eq groupId }) {
                it[memberCount] = memberCount - 1
                it[updatedAt] = timestamp
            }
            return true
        }
        return false
    }

    override fun transferOwnership(groupId: Int, newOwnerId: Int) {
        val timestamp = System.currentTimeMillis().toString()
        Groups.update({ Groups.id eq groupId }) {
            it[createdBy] = newOwnerId
            it[updatedAt] = timestamp
        }
    }

    override fun updateTotalExpenses(groupId: Int, amount: Double) {
        val timestamp = System.currentTimeMillis().toString()
        Groups.update({ Groups.id eq groupId }) {
            it[totalExpenses] = totalExpenses + amount
            it[updatedAt] = timestamp
        }
    }

    override fun delete(groupId: Int) {
        // Order obviously matters due to foreign key constraints...

        // Records of users who left with outstanding debts (TRAITORS)
        DebtHistory.deleteWhere { DebtHistory.groupId eq groupId }

        // Message read receipts reference messages
        val messageIds = Messages.select(Messages.id).where { Messages.groupId eq groupId }
        MessageReadStatus.deleteWhere { messageId inSubQuery messageIds }

        Messages.deleteWhere { Messages.groupId eq groupId }

        // Attachments reference expenses
        val expenseIds = Expenses.select(Expenses.id).where { Expenses.groupId eq groupId }
        Attachments.deleteWhere { expenseId inSubQuery expenseIds }

        ExpenseSplits.deleteWhere { expenseId inSubQuery expenseIds }

        Expenses.deleteWhere { Expenses.groupId eq groupId }

        Settlements.deleteWhere { Settlements.groupId eq groupId }

        ActivityLogs.deleteWhere { ActivityLogs.groupId eq groupId }

        GroupMembers.deleteWhere { GroupMembers.groupId eq groupId }

        Groups.deleteWhere { Groups.id eq groupId }
    }

    override fun hasNotificationEnabled(groupId: Int, userId: Int): Boolean {
        return GroupMembers.selectAll().where() {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }.map {it[GroupMembers.notificationEnabled]}.singleOrNull() ?: true;
    }

    override fun setNotificationEnabled(groupId: Int, userId: Int, enabled: Boolean) {
        GroupMembers.update({ (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId) }) {
            it[GroupMembers.notificationEnabled] = enabled
        }
    }

    private fun generateInviteCode(): String {
        return UUID.randomUUID().toString().take(6).uppercase()
    }

    private fun rowToGroup(row: ResultRow) = Group(
        id = row[Groups.id],
        name = row[Groups.name],
        description = row[Groups.description],
        inviteCode = row[Groups.inviteCode],
        createdBy = row[Groups.createdBy],
        memberCount = row[Groups.memberCount],
        totalExpenses = row[Groups.totalExpenses],
        createdAt = row[Groups.createdAt],
        updatedAt = row[Groups.updatedAt]
    )
}
