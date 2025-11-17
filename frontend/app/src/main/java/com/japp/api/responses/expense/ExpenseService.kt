package com.japp.api.responses.expense

import retrofit2.Call
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
    fun create_expense(@Body request: CreateExpenseRequest): Call<ExpenseDto?>?

    @GET("$BASE_ROUTE/group/{groupId}")
    fun get_group_expenses(@Path("groupId") groupId: Int): Call<List<ExpenseDto>?>?

    @GET("$BASE_ROUTE/group/{groupId}/balances")
    fun get_group_balances(@Path("groupId") groupId: Int): Call<GroupBalanceSummaryDto?>?

    @DELETE("$BASE_ROUTE/{id}")
    fun delete_expense(@Path("id") id: Int): Call<Unit?>?
}