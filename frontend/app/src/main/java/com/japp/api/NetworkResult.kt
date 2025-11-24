package com.japp.api

/**
 * Wrapper for network operation results.
 * Provides handling of success/error states without exceptions.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val isRetryable: Boolean = false
    ) : NetworkResult<Nothing>()

    /**
     * Execute a given action only on success
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }
}