package com.example.japp.api.responses.group

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupService {
    companion object {
        private const val BASE_ROUTE = "groups"
    }

    @POST(BASE_ROUTE)
    fun create_group(@Body request: GroupCreateRequest): Call<GroupDto?>?

    @GET(BASE_ROUTE)
    fun get_my_groups(): Call<List<GroupDto>>

    @POST("$BASE_ROUTE/join")
    fun join_group(@Body request: JoinGroupRequest): Call<GroupDto?>?

    @GET("$BASE_ROUTE/{id}")
    fun get_group(@Path("id") id: Int): Call<GroupDto?>?

    @GET("$BASE_ROUTE/{id}/members")
    fun get_group_members(@Path("id") id: Int): Call<List<GroupMemberDto>?>?

    @DELETE("$BASE_ROUTE/{id}/leave")
    fun leave_group(@Path("id") id: Int): Call<Unit?>?

}