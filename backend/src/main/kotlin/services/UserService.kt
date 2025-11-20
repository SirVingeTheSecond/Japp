package com.japp.services

import com.japp.models.Result
import com.japp.models.error.UserError
import com.japp.models.dto.UpdateUserRequest
import com.japp.models.dto.UserDto
import com.japp.repositories.interfaces.IUserRepository
import com.japp.validation.UserValidator
import com.japp.utils.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class UserService(
    private val userRepository: IUserRepository
) {

    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: Int): Result<UserDto, UserError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val user = userRepository.findById(userId)
                        ?: return@transaction Result.Failure(UserError.NotFound(userId))

                    Result.Success(user.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    UserError.InternalError(e.message ?: "Failed to retrieve user profile")
                )
            }
        }
    }

    /**
     * Update user profile (excludes email, username, password)
     */
    suspend fun updateProfile(
        userId: Int,
        request: UpdateUserRequest
    ): Result<UserDto, UserError> {
        return when (val validation = UserValidator.validateUpdateProfile(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val existingUser = userRepository.findById(userId)
                                ?: return@transaction Result.Failure(UserError.NotFound(userId))

                            // Create updated user with only changed fields
                            val updatedUser = existingUser.copy(
                                firstname = request.firstname ?: existingUser.firstname,
                                lastname = request.lastname ?: existingUser.lastname,
                                phone = request.phone ?: existingUser.phone,
                                profilePicture = request.profilePicture ?: existingUser.profilePicture
                            )

                            userRepository.update(userId, updatedUser)

                            val savedUser = userRepository.findById(userId)
                                ?: return@transaction Result.Failure(
                                    UserError.InternalError("Failed to retrieve updated user")
                                )

                            Result.Success(savedUser.toDto())
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            UserError.InternalError(e.message ?: "Failed to update profile")
                        )
                    }
                }
            }
        }
    }
}