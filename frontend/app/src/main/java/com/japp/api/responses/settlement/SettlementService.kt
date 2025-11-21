package com.japp.api.responses.settlement

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SettlementService {
    companion object {
        private const val BASE_ROUTE = "settlements"
    }

    @POST(BASE_ROUTE)
    suspend fun createSettlement(
        @Body request: CreateSettlementRequest,
        @Query("pending") pendingOnly: Boolean? = null
    ): Response<SettlementDto>

    @GET("${BASE_ROUTE}/group/{groupId}")
    suspend fun getGroupSettlements(
        @Path("groupId") groupId: Int,
        @Query("pending") pendingOnly: Boolean? = null
    ): Response<List<SettlementDto>>

    @GET("${BASE_ROUTE}/group/{groupId}/suggestions")
    suspend fun getGroupSettlementSuggestions(
        @Path("groupId") groupId: Int
    ): Response<GroupSettlementSuggestionsDto>

    @PATCH("${BASE_ROUTE}/{id}/complete")
    suspend fun completeSettlement(
        @Path("id") id: Int
    ): Response<SettlementDto>
}