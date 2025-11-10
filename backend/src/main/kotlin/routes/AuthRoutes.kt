package com.japp.routes

import com.japp.models.*
import com.japp.services.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Route.authRoutes() {
    val authService = get<AuthService>()

    route("/auth") {
        post("/signup") {
            val request = call.receive<SignupRequest>()

            when (val result = authService.signup(request)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.Created, result.value)
                }
                is Result.Failure -> {
                    val error = result.error
                    call.respond(
                        HttpStatusCode.fromValue(error.httpStatus),
                        mapOf(
                            "success" to false,
                            "error" to error.message,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            when (val result = authService.login(request)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, result.value)
                }
                is Result.Failure -> {
                    val error = result.error
                    call.respond(
                        HttpStatusCode.fromValue(error.httpStatus),
                        mapOf(
                            "success" to false,
                            "error" to error.message,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}