package com.japp.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database


object GroupMembers : Table() {
    val id = integer("id").autoIncrement()
    val member = reference("member_id", Users.id)
    val group = reference("group_id", Groups.id)

    override val primaryKey = PrimaryKey(id)
}

class GroupMemberRepository(private val database: Database) {

}