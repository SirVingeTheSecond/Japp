package com.japp.models
import kotlinx.serialization.Serializable

@Serializable
data class  User(val id: Int,
                      val name: String,
                      val email: String,
                      val passwordHash: String,
                      val createdAt: String,
                      val profilePicture: String? = null,
                      val phone: String? = null)
