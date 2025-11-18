package com.japp.api.responses.settlement

import retrofit2.Call
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
    fun create_settlement(
        @Body request: CreateSettlementRequest,
        @Query("pending") pendingOnly: Boolean? = null
    ): Call<SettlementDto?>?

    @GET("${BASE_ROUTE}/group/{groupId}")
    fun get_group_settlements(@Path("groupId") groupId: Int, @Query("pending") pendingOnly: Boolean? = null): Call<List<SettlementDto>?>?

    @GET("${BASE_ROUTE}/group/{groupId}/suggestions")
    fun get_group_settlement_suggestions(@Path("groupId") groupId: Int): Call<GroupSettlementSuggestionsDto?>?

    @PATCH("${BASE_ROUTE}/{id}/complete")
    fun complete_settlement(@Path("id") id: Int): Call<SettlementDto?>?
}