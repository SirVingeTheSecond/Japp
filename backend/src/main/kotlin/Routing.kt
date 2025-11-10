package com.japp

import com.japp.routes.authRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.days

fun Application.configureRouting() {

    // CORS config
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        anyHost() // TODO: Restrict to specific hosts in production
        allowCredentials = true
        maxAgeDuration = 1.days
    }

    // Request/Response logging
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val uri = call.request.uri
            val userAgent = call.request.headers["User-Agent"]
            "[$status] $httpMethod $uri - $userAgent"
        }
    }

    // Error handling
    install(StatusPages) {

        // 400 Bad Request - Validation errors (e.g., malformed JSON)
        exception<IllegalArgumentException> { call, cause ->
            call.application.log.warn("Validation error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "ValidationError",
                    "message" to (cause.message ?: "Invalid request"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        // 404 Not Found
        exception<NoSuchElementException> { call, cause ->
            call.application.log.warn("Resource not found: ${cause.message}")
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to "NotFound",
                    "message" to (cause.message ?: "Resource not found"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        // 500 Internal Server Error - Catch-all for unexpected errors
        exception<Exception> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "InternalServerError",
                    "message" to "An unexpected error occurred",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        // 404 for undefined routes
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                mapOf(
                    "error" to "NotFound",
                    "message" to "Endpoint not found: ${call.request.uri}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }

    // API Routes
    routing {

        // Health check endpoint (public)
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis(),
                    "version" to "1.0.0"
                )
            )
        }

        route("/api") {
            authRoutes()

            // Test endpoint
            get("/test") {
                call.respond(
                    mapOf(
                        "message" to "Japp API is running",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        }
    }

//    post("/users") {
//        val user = call.receive<User>()
//        val id = userService.create(user)
//        call.respond(HttpStatusCode.Created, id)
//    }
//
//    get("/users/{id}") {
//        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//        try {
//            val user = userService.read(id)
//            call.respond(HttpStatusCode.OK, user)
//        } catch (e: Exception) {
//            call.respond(HttpStatusCode.NotFound)
//        }
//    }

}