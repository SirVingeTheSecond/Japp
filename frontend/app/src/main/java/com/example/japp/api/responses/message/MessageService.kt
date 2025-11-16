package com.example.japp.api.responses.message

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageService {
    companion object {
        private const val BASE_ROUTE = "messages"
    }

    @POST(BASE_ROUTE)
    fun create_message(@Body request: CreateMessageRequest): Call<MessageDto?>?


    //TODO: ISSUE: Backend has no info on what the before should be formatted as, other than string. Therefore not implemented.
    @GET("${BASE_ROUTE}/group/{groupId}")
    fun get_group_messages(@Path("groupId") groupId: Int, @Query("limit") limit: Int? = null): Call<MessagePageDto?>?

    @POST("${BASE_ROUTE}/read")
    fun read_message(@Body request: MarkMessageReadRequest, @Query("groupId") groupId: Int): Call<Unit?>?

    @DELETE("${BASE_ROUTE}/{id}")
    fun delete_message(@Path("id") id: Int): Call<Unit?>?
}