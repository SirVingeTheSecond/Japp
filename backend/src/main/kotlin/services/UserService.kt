package com.japp.services

import com.japp.models.Result
import com.japp.models.dto.UpdateUserRequest
import com.japp.models.dto.UserDto
import com.japp.models.error.AppError
import com.japp.services.interfaces.IUserRepository
import com.japp.utils.toDto
import com.japp.validation.UserValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.io.InputStream
import java.util.UUID

class UserService(
    private val userRepository: IUserRepository,
    private val profilePicturesBasePath: String = "/var/japp/profile-pictures"
) {
    companion object {
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png"
        )
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    }

    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: Int): Result<UserDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    val user = userRepository.findById(userId)
                        ?: return@transaction Result.Failure(AppError.NotFound("User", userId))

                    Result.Success(user.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve user profile")
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
    ): Result<UserDto, AppError> {
        return when (val validation = UserValidator.validateUpdateProfile(request)) {
            is Result.Failure -> validation
            is Result.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        transaction {
                            val existingUser = userRepository.findById(userId)
                                ?: return@transaction Result.Failure(AppError.NotFound("User", userId))

                            val updatedUser = existingUser.copy(
                                firstname = request.firstname ?: existingUser.firstname,
                                lastname = request.lastname ?: existingUser.lastname,
                                phone = request.phone ?: existingUser.phone,
                                profilePicture = request.profilePicture ?: existingUser.profilePicture
                            )

                            userRepository.update(userId, updatedUser)

                            val savedUser = userRepository.findById(userId)
                                ?: return@transaction Result.Failure(
                                    AppError.Internal("Failed to retrieve updated user")
                                )

                            Result.Success(savedUser.toDto())
                        }
                    } catch (e: Exception) {
                        Result.Failure(
                            AppError.Internal(e.message ?: "Failed to update profile")
                        )
                    }
                }
            }
        }
    }

    /**
     * Upload profile picture for a user
     */
    suspend fun uploadProfilePicture(
        userId: Int,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        inputStream: InputStream
    ): Result<UserDto, AppError> {
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            return Result.Failure(
                AppError.Validation("Invalid file type. Allowed: JPEG, PNG")
            )
        }

        if (fileSize > MAX_FILE_SIZE) {
            return Result.Failure(
                AppError.Validation("File too large. Maximum size: 5MB")
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val user = transaction {
                    userRepository.findById(userId)
                } ?: return@withContext Result.Failure(AppError.NotFound("User", userId))

                user.profilePicture?.let { oldPath ->
                    val oldFile = File(profilePicturesBasePath, oldPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                val extension = when (mimeType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    else -> File(fileName).extension.ifEmpty { "jpg" }
                }

                val uniqueFileName = "${userId}_${UUID.randomUUID()}.$extension"
                val fullPath = File(profilePicturesBasePath, uniqueFileName)

                fullPath.parentFile?.mkdirs()

                fullPath.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                transaction {
                    userRepository.updateProfilePicture(userId, uniqueFileName)

                    val updatedUser = userRepository.findById(userId)
                        ?: return@transaction Result.Failure(
                            AppError.Internal("Failed to retrieve updated user")
                        )

                    Result.Success(updatedUser.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to upload profile picture")
                )
            }
        }
    }

    /**
     * Get profile picture file for a user
     */
    suspend fun getProfilePicture(userId: Int): Result<File, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val user = transaction {
                    userRepository.findById(userId)
                } ?: return@withContext Result.Failure(AppError.NotFound("User", userId))

                val picturePath = user.profilePicture
                    ?: return@withContext Result.Failure(
                        AppError.NotFound("Profile picture", userId)
                    )

                val file = File(profilePicturesBasePath, picturePath)

                if (!file.exists()) {
                    return@withContext Result.Failure(
                        AppError.NotFound("Profile picture file", userId)
                    )
                }

                Result.Success(file)
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to retrieve profile picture")
                )
            }
        }
    }

    /**
     * Delete profile picture and remove from filesystem
     */
    suspend fun deleteProfilePicture(userId: Int): Result<UserDto, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                val user = transaction {
                    userRepository.findById(userId)
                } ?: return@withContext Result.Failure(AppError.NotFound("User", userId))

                user.profilePicture?.let { picturePath ->
                    val file = File(profilePicturesBasePath, picturePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                transaction {
                    userRepository.updateProfilePicture(userId, null)

                    val updatedUser = userRepository.findById(userId)
                        ?: return@transaction Result.Failure(
                            AppError.Internal("Failed to retrieve updated user")
                        )

                    Result.Success(updatedUser.toDto())
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to delete profile picture")
                )
            }
        }
    }

    /**
     * Register or update FCM token for push notifications
     */
    suspend fun updateFcmToken(userId: Int, token: String): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    userRepository.findById(userId)
                        ?: return@transaction Result.Failure(AppError.NotFound("User", userId))

                    userRepository.updateFcmToken(userId, token)
                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to update FCM token")
                )
            }
        }
    }

    /**
     * Clear FCM token on logout
     */
    suspend fun clearFcmToken(userId: Int): Result<Unit, AppError> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    userRepository.updateFcmToken(userId, null)
                    Result.Success(Unit)
                }
            } catch (e: Exception) {
                Result.Failure(
                    AppError.Internal(e.message ?: "Failed to clear FCM token")
                )
            }
        }
    }
}
