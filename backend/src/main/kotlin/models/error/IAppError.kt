package com.japp.models.error

sealed interface IAppError {
    val message: String
    val httpStatus: Int
}
