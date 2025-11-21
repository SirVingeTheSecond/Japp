package com.japp.api.responses.message

import retrofit2.Call
import retrofit2.Response
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
    suspend fun createMessage(
        @Body request: CreateMessageRequest
    ): Response<MessageDto>


    //TODO: ISSUE: Backend has no info on what the before should be formatted as, other than string. Therefore not implemented.
    @GET("${BASE_ROUTE}/group/{groupId}")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: Int,
        @Query("limit") limit: Int? = null
    ): Response<MessagePageDto>

    @POST("${BASE_ROUTE}/read")
    suspend fun readMessage(
        @Body request: MarkMessageReadRequest,
        @Query("groupId") groupId: Int
    ): Response<Unit>

    @DELETE("${BASE_ROUTE}/{id}")
    suspend fun deleteMessage(@Path("id") id: Int): Response<Unit>
}