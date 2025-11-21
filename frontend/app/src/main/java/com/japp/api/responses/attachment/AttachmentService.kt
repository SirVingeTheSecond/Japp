package com.japp.api.responses.attachment

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AttachmentService {
    companion object {
        private const val BASE_ROUTE = "attachments"
    }

    @Multipart
    @POST(BASE_ROUTE)
    suspend fun uploadAttachment(
        @Part("expenseId") expenseId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<AttachmentDto>

    @GET("$BASE_ROUTE/expense/{expenseId}")
    suspend fun getExpenseAttachments(
        @Path("expenseId") expenseId: Int
    ): Response<AttachmentListDto>

    @Streaming
    @GET("$BASE_ROUTE/{id}/download")
    suspend fun downloadAttachment(
        @Path("id") attachmentId: Int
    ): Response<ResponseBody>

    @GET("$BASE_ROUTE/{id}/thumbnail")
    suspend fun getThumbnail(
        @Path("id") attachmentId: Int
    ): Response<ResponseBody>

    @DELETE("$BASE_ROUTE/{id}")
    suspend fun deleteAttachment(
        @Path("id") attachmentId: Int
    ): Response<Unit>
}