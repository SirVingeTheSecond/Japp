package com.japp.api.responses.activity

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ActivityService {
    companion object {
        private const val BASE_ROUTE = "activities"
    }

    @GET(BASE_ROUTE)
    suspend fun getUserActivities(
        @Query("limit") limit: Int? = null
    ): Response<List<ActivityDto>>

    @GET("${BASE_ROUTE}/group/{groupId}")
    suspend fun getGroupActivities(
        @Path("groupId") groupId: Int,
        @Query("limit") limit: Int? = null
    ): Response<GroupActivitiesDto>

}