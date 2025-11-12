package com.japp.routes

import com.japp.models.dto.LoginRequest
import com.japp.models.dto.SignupRequest
import com.japp.services.AuthService
import com.japp.utils.respondResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Route.authRoutes() {
    val authService = get<AuthService>()

    route("/auth") {
        post("/signup") {
            val request = call.receive<SignupRequest>()
            val result = authService.signup(request)
            call.respondResult(result, HttpStatusCode.Created)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = authService.login(request)
            call.respondResult(result)
        }
    }
}