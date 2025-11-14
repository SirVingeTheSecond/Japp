package com.japp.config

import io.ktor.server.application.*

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationDays: Int
)

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long
)

data class AppConfig(
    val environment: String,
    val version: String
)

fun Application.loadJwtConfig(): JwtConfig {
    return JwtConfig(
        secret = environment.config.property("jwt.secret").getString(),
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        realm = environment.config.property("jwt.realm").getString(),
        expirationDays = environment.config.property("jwt.expirationDays").getString().toInt()
    )
}

fun Application.loadDatabaseConfig(): DatabaseConfig {
    return DatabaseConfig(
        url = environment.config.property("database.url").getString(),
        driver = environment.config.property("database.driver").getString(),
        user = environment.config.property("database.user").getString(),
        password = environment.config.property("database.password").getString(),
        maximumPoolSize = environment.config.property("database.pool.maximumPoolSize").getString().toInt(),
        minimumIdle = environment.config.property("database.pool.minimumIdle").getString().toInt(),
        connectionTimeout = environment.config.property("database.pool.connectionTimeout").getString().toLong(),
        idleTimeout = environment.config.property("database.pool.idleTimeout").getString().toLong(),
        maxLifetime = environment.config.property("database.pool.maxLifetime").getString().toLong()
    )
}