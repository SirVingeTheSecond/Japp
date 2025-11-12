package com.example.japp.api

import com.example.japp.api.responses.AuthResponses
import com.example.japp.api.responses.HealthResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthService {
    companion object {
        private const val BASE_ROUTE = "auth"
    }

    @POST("${BASE_ROUTE}/signup")
    fun signup(@Body request: AuthResponses.SignupRequest): Call<AuthResponses.AuthResponse?>?

    @POST("${BASE_ROUTE}/login")
    fun login(@Body request: AuthResponses.LoginRequest): Call<AuthResponses.AuthResponse?>?
}

interface JappService {
    @GET("health")
    suspend fun getHealth(): HealthResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://japp-app-api.itnerd.net/api/"

    val intercepter = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }
    val client = OkHttpClient.Builder().apply {
        this.addInterceptor(intercepter)
    }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthService = retrofit.create(AuthService::class.java)
    val jappService: JappService = retrofit.create(JappService::class.java)
}
