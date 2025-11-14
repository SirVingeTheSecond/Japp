package com.japp.services

import com.japp.models.*
import com.japp.models.domain.User
import com.japp.models.dto.SignupRequest
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.AuthResponse
import com.japp.repositories.interfaces.IUserRepository
import com.japp.security.PasswordHasher
import com.japp.validation.AuthValidator
import com.japp.utils.toDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.japp.models.error.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Date

class AuthService(
    private val userRepository: IUserRepository,
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
        return when (val validation = AuthValidator.validateSignup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                val validatedRequest = validation.value

                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            when {
                                userRepository.emailExists(validatedRequest.email) -> {
                                    Result.Failure(AuthError.EmailAlreadyExists(validatedRequest.email))
                                }
                                userRepository.usernameExists(validatedRequest.username) -> {
                                    Result.Failure(AuthError.ValidationError("Username already taken"))
                                }
                                else -> {
                                    val user = User(
                                        id = 0,
                                        email = validatedRequest.email,
                                        username = validatedRequest.username,
                                        firstname = validatedRequest.firstname,
                                        lastname = validatedRequest.lastname,
                                        passwordHash = passwordHasher.hash(validatedRequest.password),
                                        phone = validatedRequest.phone,
                                        profilePicture = null,
                                        createdAt = System.currentTimeMillis().toString()
                                    )

                                    val userId = userRepository.create(user)
                                    val savedUser = userRepository.findById(userId)

                                    if (savedUser != null) {
                                        val token = generateToken(savedUser.id, savedUser.email)
                                        Result.Success(
                                            AuthResponse(
                                                token = token,
                                                user = savedUser.toDto()
                                            )
                                        )
                                    } else {
                                        Result.Failure(AuthError.InternalError("Failed to retrieve created user"))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Result.Failure(AuthError.InternalError(e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    /**
     * Authenticate existing user
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse, AuthError> {
        return when (val validation = AuthValidator.validateLogin(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                val validatedRequest = validation.value

                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val user = userRepository.findByEmailOrUsername(validatedRequest.emailOrUsername)

                            when {
                                user == null -> {
                                    Result.Failure(AuthError.InvalidCredentials())
                                }
                                !passwordHasher.verify(validatedRequest.password, user.passwordHash) -> {
                                    Result.Failure(AuthError.InvalidCredentials())
                                }
                                else -> {
                                    val token = generateToken(user.id, user.email)
                                    Result.Success(
                                        AuthResponse(
                                            token = token,
                                            user = user.toDto()
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Result.Failure(AuthError.InternalError(e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

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
        } catch (_: Exception) {
            null
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
}