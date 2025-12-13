package com.japp.api.responses.user

import com.japp.api.responses.auth.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface UserService {
    companion object {
        private const val BASE_ROUTE = "user"
    }

    @GET("${BASE_ROUTE}/me")
    suspend fun getMyUser(): Response<UserDto>

    @PATCH("${BASE_ROUTE}/me")
    suspend fun updateMyUser(@Body request: UpdateUserRequest): Response<UserDto>

    @GET("${BASE_ROUTE}/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<UserDto>

    @Multipart
    @POST("${BASE_ROUTE}/me/pp")
    suspend fun uploadProfilePicture(
        @Part file: MultipartBody.Part
    ): Response<UserDto>

    @DELETE("${BASE_ROUTE}/me/pp")
    suspend fun deleteProfilePicture(): Response<UserDto>

    @Streaming
    @GET("${BASE_ROUTE}/{id}/pp")
    suspend fun getProfilePicture(@Path("id") id: Int): Response<ResponseBody>

    @POST("${BASE_ROUTE}/me/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    @DELETE("${BASE_ROUTE}/me/fcm-token")
    suspend fun clearFcmToken(): Response<Unit>
}
