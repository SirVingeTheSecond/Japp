package com.japp.repositories

import com.japp.models.Group
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object Groups : Table("groups") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val inviteCode = varchar("invite_code", 20).uniqueIndex()
    val createdBy = integer("created_by").references(Users.id)
    val memberCount = integer("member_count").default(0)
    val totalExpenses = double("total_expenses").default(0.0)
    val createdAt = varchar("created_at", 255)
    val updatedAt = varchar("updated_at", 255)

    override val primaryKey = PrimaryKey(id)
}

object GroupMembers : Table("group_members") {
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val joinedAt = varchar("joined_at", 255)

    override val primaryKey = PrimaryKey(groupId, userId)
}

class GroupRepository(private val database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(Groups, GroupMembers)
        }
    }

    suspend fun create(name: String, description: String?, createdBy: Int): Group = dbQuery {
        val inviteCode = generateInviteCode()
        val timestamp = System.currentTimeMillis().toString()

        val groupId = Groups.insert {
            it[Groups.name] = name
            it[Groups.description] = description
            it[Groups.inviteCode] = inviteCode
            it[Groups.createdBy] = createdBy
            it[memberCount] = 1 // Creator is obviously the first member...
            it[totalExpenses] = 0.0
            it[createdAt] = timestamp
            it[updatedAt] = timestamp
        }[Groups.id]

        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = createdBy
            it[joinedAt] = timestamp
        }

        findById(groupId)!!
    }

    suspend fun findById(id: Int): Group? = dbQuery {
        Groups.selectAll()
            .where { Groups.id eq id }
            .map { rowToGroup(it) }
            .singleOrNull()
    }

    suspend fun findByInviteCode(code: String): Group? = dbQuery {
        Groups.selectAll()
            .where { Groups.inviteCode eq code }
            .map { rowToGroup(it) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: Int): List<Group> = dbQuery {
        (Groups innerJoin GroupMembers)
            .selectAll()
            .where { GroupMembers.userId eq userId }
            .map { rowToGroup(it) }
    }

    suspend fun getMembers(groupId: Int): List<Int> = dbQuery {
        GroupMembers.selectAll()
            .where { GroupMembers.groupId eq groupId }
            .map { it[GroupMembers.userId] }
    }

    suspend fun isMember(groupId: Int, userId: Int): Boolean = dbQuery {
        GroupMembers.selectAll()
            .where { (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId) }
            .count() > 0
    }

    // I do not know what to currently use this for, but it is here
    suspend fun isOwner(groupId: Int, userId: Int): Boolean = dbQuery {
        Groups.selectAll()
            .where { (Groups.id eq groupId) and (Groups.createdBy eq userId) }
            .count() > 0
    }

    suspend fun addMember(groupId: Int, userId: Int): Boolean = dbQuery {
        if (isMember(groupId, userId)) {
            return@dbQuery false
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

        true
    }

    suspend fun removeMember(groupId: Int, userId: Int): Boolean = dbQuery {
        val deleted = GroupMembers.deleteWhere {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }

        if (deleted > 0) {
            val timestamp = System.currentTimeMillis().toString()
            Groups.update({ Groups.id eq groupId }) {
                it[memberCount] = memberCount - 1
                it[updatedAt] = timestamp
            }
            true
        } else {
            false
        }
    }

    suspend fun updateTotalExpenses(groupId: Int, amount: Double): Unit = dbQuery {
        val timestamp = System.currentTimeMillis().toString()
        Groups.update({ Groups.id eq groupId }) {
            it[totalExpenses] = totalExpenses + amount
            it[updatedAt] = timestamp
        }
    }

    suspend fun delete(groupId: Int): Unit = dbQuery {
        GroupMembers.deleteWhere { GroupMembers.groupId eq groupId }
        Groups.deleteWhere { Groups.id eq groupId }
    }

    // ToDo: Might need to be done in a better way
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

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}