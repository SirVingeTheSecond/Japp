package com.japp.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.japp.models.AuthResponse
import com.japp.models.LoginRequest
import com.japp.models.SignupRequest
import com.japp.models.User
import com.japp.models.UserDto
import com.japp.repositories.UserRepository
import com.japp.security.PasswordHasher
import java.util.Date

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {

    private val jwtExpirationMs = 7 * 24 * 60 * 60 * 1000L

    suspend fun signup(request: SignupRequest) : AuthResponse {
        // Validation
        require(request.email.contains("@")) { "Email is not valid" }
        require(request.password.length >= 8) { "Password is not valid" }
        require(request.name.isNotBlank()) { "Name is not valid" }

        if (userRepository.emailExists(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        val user = User(
            id = 0,
            name = request.name,
            email = request.email,
            passwordHash = passwordHasher.hash(request.password),
            phone = request.phone,
            profilePicture = null,
            createdAt = System.currentTimeMillis().toString()
        )

        val userId = userRepository.create(user)
        val savedUser = userRepository.findById(userId)
            ?: throw IllegalStateException("Failed to retrieve created user")

        val token = generateToken(user.id, user.email)

        return AuthResponse(
            token = token,
            user = savedUser.toDto()
        )
    }

    suspend fun login(request: LoginRequest) : AuthResponse {
        require(request.email.isNotBlank()) { "Email is required " }
        require(request.password.isNotBlank()) { "Password is required " }

        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalStateException("Invalid email or password")

        if (!passwordHasher.verify(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val token = generateToken(user.id, user.email)

        return AuthResponse(
            token = token,
            user = user.toDto()
        )
    }

    private fun generateToken(userId: Int, email: String): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtExpirationMs))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private fun verifyToken(token: String): Int? {
        return try {
            val verifier =  JWT.require(Algorithm.HMAC256(jwtSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .build()

            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asInt()
        } catch (e: Exception) {
            null
        }
    }
}

private fun User.toDto() = UserDto(
    id = id,
    name = name,
    email = email,
    phone = phone,
    profilePicture = profilePicture
)