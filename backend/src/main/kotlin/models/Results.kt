package com.japp.models

// sealed class = compiler knows all possible subclasses so no need for an else-branch :D
// Makes error handling cleaner since we can return typed failures instead of throwing exceptions
/**
 * Represents the result of an operation that can succeed or fail
 */
sealed class Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
    data class Failure<out E>(val error: E) : Result<Nothing, E>()
    
    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }

    // Mappers
    // Functional mapping utils where we can chain operations

    inline fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
        // transform only applies to the success value (if any),
        // keeping the error type E untouched
    }
}