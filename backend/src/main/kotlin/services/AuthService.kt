package com.japp.services

import com.japp.models.*
import com.japp.repositories.UserRepository
import com.japp.security.PasswordHasher
import com.japp.validation.AuthValidator
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {

    private val jwtExpirationMs = 7 * 24 * 60 * 60 * 1000L // 7 days

    /**
     * Register a new user
     */
    suspend fun signup(request: SignupRequest): Result<AuthResponse, AuthError> {
        val validatedRequest = when (val validation = AuthValidator.validateSignup(request)) {
            is Result.Failure -> return Result.Failure(validation.error)
            is Result.Success -> validation.value
        }

        return try {
            if (userRepository.emailExists(validatedRequest.email)) {
                return Result.Failure(AuthError.EmailAlreadyExists(validatedRequest.email))
            }

            val user = User(
                id = 0,
                name = validatedRequest.name,
                email = validatedRequest.email,
                passwordHash = passwordHasher.hash(validatedRequest.password),
                phone = validatedRequest.phone,
                profilePicture = null,
                createdAt = System.currentTimeMillis().toString()
            )

            val userId = userRepository.create(user)
            val savedUser = userRepository.findById(userId)
                ?: return Result.Failure(AuthError.InternalError("Failed to retrieve created user"))

            val token = generateToken(savedUser.id, savedUser.email)

            Result.Success(
                AuthResponse(
                    token = token,
                    user = savedUser.toDto()
                )
            )
        } catch (e: Exception) {
            Result.Failure(AuthError.InternalError(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Authenticate existing user
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse, AuthError> {
        val validatedRequest = when (val validation = AuthValidator.validateLogin(request)) {
            is Result.Failure -> return Result.Failure(validation.error)
            is Result.Success -> validation.value
        }

        return try {
            val user = userRepository.findByEmail(validatedRequest.email)
                ?: return Result.Failure(AuthError.InvalidCredentials())

            if (!passwordHasher.verify(validatedRequest.password, user.passwordHash)) {
                return Result.Failure(AuthError.InvalidCredentials())
            }

            val token = generateToken(user.id, user.email)

            Result.Success(
                AuthResponse(
                    token = token,
                    user = user.toDto()
                )
            )
        } catch (e: Exception) {
            Result.Failure(AuthError.InternalError(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Generate JWT token for authenticated user
     */
    private fun generateToken(userId: Int, email: String): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtExpirationMs))
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    // To be used...
    /**
     * Verify JWT token to determine user ID
     */
    fun verifyToken(token: String): Int? {
        return try {
            val verifier = JWT.require(Algorithm.HMAC256(jwtSecret))
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .build()

            val decodedJWT = verifier.verify(token)
            decodedJWT.getClaim("userId").asInt()
        } catch (e: Exception) {
            // Well, since the exception is never used, we could just omit it using _
            null
        }
    }
}

// This might not be the cleanest approach...
/**
 * Extension function to convert User to DTO
 */
private fun User.toDto() = UserDto(
    id = id,
    name = name,
    email = email,
    phone = phone,
    profilePicture = profilePicture
)