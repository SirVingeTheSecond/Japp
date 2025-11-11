package com.japp.database.tables

import org.jetbrains.exposed.v1.core.Table

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val phone = varchar("phone", 255).nullable()
    val profilePicture = varchar("profile_picture", 700).nullable()
    val createdAt = varchar("created_at", 255)

    override val primaryKey = PrimaryKey(id)
}