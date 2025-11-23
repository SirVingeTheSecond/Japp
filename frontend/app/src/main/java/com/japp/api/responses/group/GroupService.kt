package com.japp.api.responses.group

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupService {
    companion object {
        private const val BASE_ROUTE = "groups"
    }

    // Public methods
    /**
     * Get a group from an invite code.
     *
     * JWT token is not required here.
     *
     * @param inviteCode String from the GroupDto
     */
    @GET("$BASE_ROUTE/preview/{inviteCode}")
    suspend fun getGroup(@Path("inviteCode") inviteCode: String): Response<GroupPreviewDto>

    // Private methods (logged in)
    @POST(BASE_ROUTE)
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GroupDto>

    @GET(BASE_ROUTE)
    suspend fun getMyGroups(): Response<List<GroupDto>>

    @POST("$BASE_ROUTE/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): Response<GroupDto>

    @GET("$BASE_ROUTE/{id}")
    suspend fun getGroup(@Path("id") id: Int): Response<GroupDto>

    @GET("$BASE_ROUTE/{id}/members")
    suspend fun getGroupMembers(@Path("id") id: Int): Response<List<GroupMemberDto>>

    @POST("$BASE_ROUTE/{id}/members")
    suspend fun addGroupMember(
        @Path("id") id: Int,
        @Body request: AddMemberRequest
    ): Response<List<GroupMemberDto>>

    @DELETE("$BASE_ROUTE/{id}/leave")
    suspend fun leaveGroup(@Path("id") id: Int): Response<Unit>

    @DELETE("$BASE_ROUTE/{id}")
    suspend fun deleteGroup(@Path("id") id: Int): Response<Unit>

    @GET("$BASE_ROUTE/{id}/invite")
    suspend fun getGroupInvite(@Path("id") id: Int): Response<GroupInviteDetailsDto>

    @GET("$BASE_ROUTE/{id}/debt-history")
    suspend fun getGroupDebtHistory(@Path("id") id: Int): Response<List<DebtHistoryDto>>

    @DELETE("$BASE_ROUTE/{id}/members/{memberId}")
    suspend fun kickGroupMember(@Path("id") groupId: Int, @Path("memberId") memberId: Int): Response<Unit>
}