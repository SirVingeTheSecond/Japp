package com.japp.services

import com.japp.models.*
import com.japp.models.domain.User
import com.japp.models.dto.SignupRequest
import com.japp.models.dto.LoginRequest
import com.japp.models.dto.AuthResponse
import com.japp.services.interfaces.IUserRepository
import com.japp.security.PasswordHasher
import com.japp.validation.AuthValidator
import com.japp.utils.toDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.japp.models.error.AppError
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
    suspend fun signup(request: SignupRequest): Result<AuthResponse, AppError> {
        return when (val validation = AuthValidator.validateSignup(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            when {
                                userRepository.emailExists(request.email) -> {
                                    Result.Failure(AppError.EmailAlreadyExists(request.email))
                                }
                                userRepository.usernameExists(request.username) -> {
                                    Result.Failure(AppError.Validation("Username already taken"))
                                }
                                else -> {
                                    val user = User(
                                        id = 0,
                                        email = request.email,
                                        username = request.username,
                                        firstname = request.firstname,
                                        lastname = request.lastname,
                                        passwordHash = passwordHasher.hash(request.password),
                                        phone = request.phone,
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
                                        Result.Failure(AppError.Internal("Failed to retrieve created user"))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Result.Failure(AppError.Internal(e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    /**
     * Authenticate existing user
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse, AppError> {
        return when (val validation = AuthValidator.validateLogin(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val user = userRepository.findByEmailOrUsername(request.emailOrUsername)

                            when {
                                user == null -> {
                                    Result.Failure(AppError.InvalidCredentials())
                                }
                                !passwordHasher.verify(request.password, user.passwordHash) -> {
                                    Result.Failure(AppError.InvalidCredentials())
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
                        Result.Failure(AppError.Internal(e.message ?: "Unknown error"))
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
            // Well, since the exception is never used, we could just omit it using _
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