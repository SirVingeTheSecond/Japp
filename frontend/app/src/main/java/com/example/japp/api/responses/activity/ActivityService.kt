package com.example.japp.api.responses.activity

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ActivityService {
    companion object {
        private const val BASE_ROUTE = "activities"
    }

    @POST("${BASE_ROUTE}/group/{groupId}")
    fun get_group_activities(
        @Path("groupId") groupId: Int,
        @Query("limit") limit: Int? = null
    ): Call<GroupActivitiesDto?>?

}