package com.japp.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Safely execute a network call with automatic retry for transient failures.
 *
 * @param tag Logging tag for debugging
 * @param maxRetries Maximum number of retry attempts (0 = no retries, just single attempt)
 * @param initialDelayMs Initial delay before first retry in milliseconds
 * @param maxDelayMs Maximum delay between retries in milliseconds
 * @param factor Multiplier for exponential backoff
 * @param call The suspend function that makes the network request
 * @return NetworkResult.Success with data or NetworkResult.Error with message
 */
// Source: https://www.geeksforgeeks.org/android/how-to-handle-api-responses-success-error-in-android/
suspend fun <T> safeApiCall(
    tag: String,
    maxRetries: Int = 0,
    initialDelayMs: Long = 1000L,
    maxDelayMs: Long = 10000L,
    factor: Double = 2.0,
    call: suspend () -> Response<T>
): NetworkResult<T> {
    var currentDelay = initialDelayMs
    var lastResult: NetworkResult<T>? = null

    repeat(maxRetries + 1) { attempt ->
        val result = executeCall(tag, call)

        if (result is NetworkResult.Success) {
            return result
        }

        if (result is NetworkResult.Error && !result.isRetryable) {
            return result
        }

        lastResult = result

        // Obviously do not delay after the last attempt
        if (attempt < maxRetries) {
            Log.d(tag, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }

    return lastResult ?: NetworkResult.Error("Unknown error", isRetryable = false)
}

/**
 * Execute a GET/read operation with automatic retry on failure.
 *
 * Queries are idempotent and safe to retry automatically.
 * Default: 2 retries with exponential backoff (1s -> 2s -> 4s).
 *
 * @param tag Logging tag for debugging
 * @param maxRetries Maximum retry attempts (default: 2)
 * @param call The suspend function that makes the network request
 */
suspend fun <T> safeApiQuery(
    tag: String,
    maxRetries: Int = 2,
    call: suspend () -> Response<T>
): NetworkResult<T> = safeApiCall(
    tag = tag,
    maxRetries = maxRetries,
    initialDelayMs = 1000L,
    maxDelayMs = 8000L,
    factor = 2.0,
    call = call
)

/**
 * Execute a POST/PUT/DELETE/write operation.
 *
 * Mutations are NOT automatically retried to prevent duplicate side effects.
 * Callers should handle retry logic explicitly.
 *
 * @param tag Logging tag for debugging
 * @param call The suspend function that makes the network request
 */
suspend fun <T> safeApiMutation(
    tag: String,
    call: suspend () -> Response<T>
): NetworkResult<T> = safeApiCall(
    tag = tag,
    maxRetries = 0,
    call = call
)

/**
 * Execute a single network call attempt.
 */
private suspend fun <T> executeCall(
    tag: String,
    call: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = call()
        handleResponse(tag, response)
    } catch (e: CancellationException) {
        throw e // WE SHOULD NOT catch coroutine cancellation
    } catch (e: UnknownHostException) {
        Log.e(tag, "No internet connection", e)
        NetworkResult.Error("No internet connection", isRetryable = true)
    } catch (e: SocketTimeoutException) {
        Log.e(tag, "Request timed out", e)
        NetworkResult.Error("Request timed out", isRetryable = true)
    } catch (e: SSLException) {
        Log.e(tag, "Secure connection failed", e)
        NetworkResult.Error("Secure connection failed", isRetryable = true)
    } catch (e: IOException) {
        Log.e(tag, "Network error", e)
        NetworkResult.Error("Network error occurred", isRetryable = true)
    } catch (e: Exception) {
        Log.e(tag, "Unexpected error", e)
        NetworkResult.Error("Something went wrong", isRetryable = false)
    }
}

/**
 * Process the HTTP response into a NetworkResult.
 */
private fun <T> handleResponse(tag: String, response: Response<T>): NetworkResult<T> {
    return if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            Log.w(tag, "Empty response body")
            NetworkResult.Error("Empty response", isRetryable = false)
        }
    } else {
        val errorMessage = ErrorUtils.parseError(response)
        Log.w(tag, "HTTP ${response.code()}: $errorMessage")
        // 5xx errors are server-side and are worth retrying
        val isRetryable = response.code() in 500..599
        NetworkResult.Error(errorMessage?.message ?: "Unknown error", isRetryable = isRetryable)
    }
}
