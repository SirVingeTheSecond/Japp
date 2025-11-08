package com.japp.models

import kotlinx.serialization.Serializable

@Serializable
data class Group(val id: Int,
                 val name: String,
                 val invite_Code: String,
                 val total_expenses: Int,
                 val owner: User,
                 val members: List<User>)