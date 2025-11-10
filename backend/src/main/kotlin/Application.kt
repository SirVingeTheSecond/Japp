package com.japp

import com.japp.plugins.configureDatabases
import com.japp.plugins.configureFrameworks
import com.japp.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.japp.plugins.configureSecurity
import com.japp.plugins.configureSerialization
import com.japp.plugins.configureSockets

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureSockets()
    configureRouting()
}
