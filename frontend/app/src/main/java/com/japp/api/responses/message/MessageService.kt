package com.japp.api.responses.message

import retrofit2.Response
import retrofit2.http.*

interface MessageService {
    @POST("messages")
    suspend fun createMessage(
        @Body request: CreateMessageRequest
    ): Response<MessageDto>

    @GET("messages/group/{groupId}")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: Int,
        @Query("limit") limit: Int? = 50,
        @Query("before") before: String? = null
    ): Response<MessagePageDto>

    @POST("messages/read")
    suspend fun markMessagesAsRead(
        @Body request: MarkMessageReadRequest,
        @Query("groupId") groupId: Int
    ): Response<Unit>
}