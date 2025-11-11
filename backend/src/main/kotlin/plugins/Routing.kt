package com.japp.plugins

import com.japp.models.dto.HealthResponse
import com.japp.models.dto.MeResponse
import com.japp.routes.authRoutes
import com.japp.routes.expenseRoutes
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
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

        anyHost() // ToDo: Restrict to specific hosts in production
        allowCredentials = true
        maxAgeDuration = 1.days
    }

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

    install(StatusPages) {

        // 400 Bad Request - Validation errors
        exception<IllegalArgumentException> { call, cause ->
            call.application.log.warn("Validation error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ResponseFactory.error(
                    error = "ValidationError",
                    message = cause.message ?: "Invalid request"
                )
            )
        }

        // 404 Not Found
        exception<NoSuchElementException> { call, cause ->
            call.application.log.warn("Resource not found: ${cause.message}")
            call.respond(
                HttpStatusCode.NotFound,
                ResponseFactory.error(
                    error = "NotFound",
                    message = cause.message ?: "Resource not found"
                )
            )
        }

        // 500 Internal Server Error
        exception<Exception> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ResponseFactory.error(
                    error = "InternalServerError",
                    message = "An unexpected error occurred"
                )
            )
        }

        // 404 for undefined routes
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ResponseFactory.error(
                    error = "NotFound",
                    message = "Endpoint not found: ${call.request.uri}"
                )
            )
        }
    }

    routing {

        // Health check
        get("/health") {
            call.respond(
                HealthResponse(
                    status = "healthy",
                    version = "1.0.0"
                )
            )
        }

        route("/api") {
            authRoutes()

            authenticate("auth-jwt") {
                get("/me") {
                    val userId = call.getUserId()
                    call.respond(
                        MeResponse(
                            userId = userId,
                            message = "You are authenticated"
                        )
                    )
                }

                expenseRoutes()
            }
        }
    }
}