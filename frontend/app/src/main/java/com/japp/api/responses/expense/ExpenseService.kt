package com.japp.api.responses.expense

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExpenseService {
    companion object {
        private const val BASE_ROUTE = "expenses"
    }

    @POST(BASE_ROUTE)
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<ExpenseDto>

    @GET("$BASE_ROUTE/group/{groupId}")
    suspend fun getGroupExpenses(@Path("groupId") groupId: Int): Response<List<ExpenseDto>>

    @GET("$BASE_ROUTE/group/{groupId}/balances")
    suspend fun getGroupBalances(@Path("groupId") groupId: Int): Response<GroupBalanceSummaryDto>

    @DELETE("$BASE_ROUTE/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Response<Unit>
}