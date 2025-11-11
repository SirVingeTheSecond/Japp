package com.japp.plugins

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
    val config = environment.config

    // Get database configuration (the idea is: env vars > config file)
    val dbUrl = System.getenv("POSTGRES_URL")
        ?: config.propertyOrNull("postgres.url")?.getString()

    val dbUser = System.getenv("POSTGRES_USER")
        ?: config.propertyOrNull("postgres.user")?.getString()

    val dbPassword = System.getenv("POSTGRES_PASS")
        ?: config.propertyOrNull("postgres.password")?.getString()

    log.info("Configuring database connection...")
    log.info("Database URL: $dbUrl")
    log.info("Database User: $dbUser")

    // Configure HikariCP
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        driverClassName = "org.postgresql.Driver"
        username = dbUser
        password = dbPassword

        // Connection pool
        maximumPoolSize = 10           // Max connections in pool
        minimumIdle = 2                // Minimum idle connections
        idleTimeout = 600000           // 10 minutes
        connectionTimeout = 30000      // 30 seconds
        maxLifetime = 1800000          // 30 minutes

        // A simple health check
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000 // ms

        // Some performance optimizations
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