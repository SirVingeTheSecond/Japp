package com.japp.database

import com.japp.database.tables.ActivityLogs
import com.japp.database.tables.Expenses
import com.japp.database.tables.GroupMembers
import com.japp.database.tables.Groups
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
                ActivityLogs
            )
        }
    }

    /**
     * Drop all tables (for testing)
     */
    fun dropTables() {
        transaction {
            SchemaUtils.drop(
                Users,
                Groups,
                GroupMembers,
                Expenses,
                ActivityLogs
            )
        }
    }
}