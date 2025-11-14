package com.japp.plugins

import com.japp.config.loadDatabaseConfig
import com.japp.database.DatabaseSchema
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Configure database connection using HikariCP and Exposed ORM
 *
 * What this does:
 * 1. Reads database configuration
 * 2. Sets up HikariCP connection
 * 3. Connects Exposed to the database
 * 4. Creates tables automatically
 */
fun Application.configureDatabases() {
    val dbConfig = loadDatabaseConfig()

    log.info("Configuring database connection...")
    log.info("Database URL: ${dbConfig.url}")
    log.info("Database User: ${dbConfig.user}")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbConfig.url
        driverClassName = dbConfig.driver
        username = dbConfig.user
        password = dbConfig.password

        maximumPoolSize = dbConfig.maximumPoolSize
        minimumIdle = dbConfig.minimumIdle
        idleTimeout = dbConfig.idleTimeout
        connectionTimeout = dbConfig.connectionTimeout
        maxLifetime = dbConfig.maxLifetime

        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000

        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    }

    try {
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
        log.info("Database connection successful")

        DatabaseSchema.createTables()
        log.info("Database schema initialized")

    } catch (e: Exception) {
        log.error("Failed to initialize database", e)
        throw RuntimeException("Database initialization failed: ${e.message}", e)
    }
}