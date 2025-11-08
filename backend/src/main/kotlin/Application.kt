package com.japp

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

fun main(args: Array<String>) {
    Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureFrameworks()
    configureDatabases()
    configureSockets()
    configureRouting()
}
