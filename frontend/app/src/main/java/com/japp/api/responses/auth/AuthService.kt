package com.japp.api.responses.auth

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    companion object {
        private const val BASE_ROUTE = "auth"
    }

    @POST("${BASE_ROUTE}/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("${BASE_ROUTE}/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}