package com.japp.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.japp.utils.ResponseFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtSecret: String by inject(named("jwtSecret"))
    val jwtIssuer: String by inject(named("jwtIssuer"))
    val jwtAudience: String by inject(named("jwtAudience"))

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Japp API"

            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ResponseFactory.error(
                        error = "Unauthorized",
                        message = "Token is not valid or has expired"
                    )
                )
            }
        }
    }
}

/**
 * Extension function to extract userId from authenticated JWT token
 */
fun ApplicationCall.getUserId(): Int {
    val principal = principal<JWTPrincipal>()
        ?: throw IllegalStateException("No JWT principal found")
    return principal.payload.getClaim("userId").asInt()
        ?: throw IllegalStateException("No userId in JWT token")
}