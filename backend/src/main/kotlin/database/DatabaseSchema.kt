package com.japp.database

import com.japp.database.tables.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object DatabaseSchema {

    /**
     * Initialize all database tables
     */
    fun createTables() {
        transaction {
            SchemaUtils.create(
                Users,
                Groups,
                GroupMembers,
                DebtHistory,
                Expenses,
                ExpenseSplits,
                Settlements,
                ActivityLogs,
                Messages,
                MessageReadStatus,
                Attachments
            )
        }
    }

    /**
     * Drop all tables (for testing)
     */
    fun dropTables() {
        transaction {
            SchemaUtils.drop(
                Attachments,
                MessageReadStatus,
                Messages,
                ActivityLogs,
                Settlements,
                ExpenseSplits,
                Expenses,
                GroupMembers,
                DebtHistory,
                Groups,
                Users
            )
        }
    }
}