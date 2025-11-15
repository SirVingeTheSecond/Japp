package com.japp.database

import com.japp.database.tables.ActivityLogs
import com.japp.database.tables.ExpenseSplits
import com.japp.database.tables.Expenses
import com.japp.database.tables.GroupMembers
import com.japp.database.tables.Groups
import com.japp.database.tables.MessageReadStatus
import com.japp.database.tables.Messages
import com.japp.database.tables.Settlements
import com.japp.database.tables.Users
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
                Groups,
                Users
            )
        }
    }
}