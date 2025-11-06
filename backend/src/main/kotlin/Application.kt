package com.japp

import io.ktor.server.application.*

fun main(args: Array<String>) {
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
