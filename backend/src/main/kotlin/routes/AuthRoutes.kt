package com.japp.routes

import com.japp.ConflictException
import com.japp.models.LoginRequest
import com.japp.models.SignupRequest
import com.japp.services.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject


fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth"){
        post("/signup") {
            val request = call.receive<SignupRequest>()

            try {
                val response = authService.signup(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: IllegalStateException) {
                throw ConflictException(e.message ?: "Email already registered")
            }
        }

        post("/login"){
            val request = call.receive<LoginRequest>()

            try {
                val response = authService.login(request)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                throw ConflictException(e.message ?: "Invalid email or password")
            }
        }
    }
}