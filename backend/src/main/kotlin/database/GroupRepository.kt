package com.japp.database

import org.jetbrains.exposed.v1.core.Table


object Groups : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val invite_code = varchar("invite_code", 255)
    val owner = reference("owner_id", Users.id)
}