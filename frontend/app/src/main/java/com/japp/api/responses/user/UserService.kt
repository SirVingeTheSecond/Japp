package com.japp.api.responses.user

import com.japp.api.responses.auth.UserDto
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

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
}