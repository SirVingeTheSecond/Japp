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
                MessageReadStatus
            )
        }
    }

    /**
     * Drop all tables (for testing)
     */
    fun dropTables() {
        transaction {
            SchemaUtils.drop(
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