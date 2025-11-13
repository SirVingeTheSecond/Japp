package com.example.japp.api.responses.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    companion object {
        private const val BASE_ROUTE = "auth"
    }

    @POST("${BASE_ROUTE}/signup")
    fun signup(@Body request: SignupRequest): Call<AuthResponse?>?

    @POST("${BASE_ROUTE}/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse?>?
}