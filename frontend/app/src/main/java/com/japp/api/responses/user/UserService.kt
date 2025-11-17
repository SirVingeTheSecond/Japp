package com.japp.api.responses.user

import com.japp.api.responses.auth.UserDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserService {
    companion object {
        private const val BASE_ROUTE = "user"
    }

    @GET("${BASE_ROUTE}/me")
    suspend fun get_my_user(): UserDto

    @PATCH("${BASE_ROUTE}/me")
    fun update_my_user(@Body request: UpdateUserRequest): Call<UserDto?>?

    @GET("${BASE_ROUTE}/{id}")
    fun get_user(@Path("id") id: Int): Call<UserDto?>?
}