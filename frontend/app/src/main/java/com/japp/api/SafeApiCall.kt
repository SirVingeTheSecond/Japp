package com.japp.api

import android.util.Log
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Safely execute a Retrofit suspend call, converting exceptions to NetworkResult.Error.
 *
 * @param tag Logging tag for debugging
 * @param call The suspend function returning Retrofit Response<T>
 * @return NetworkResult.Success with data, or NetworkResult.Error with message
 */
// Source: https://www.geeksforgeeks.org/android/how-to-handle-api-responses-success-error-in-android/
suspend fun <T> safeApiCall(
    tag: String = "SafeApiCall",
    call: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = call()
        handleResponse(response, tag)
    } catch (e: CancellationException) {
        // Rethrow to respect cancellation of coroutine
        throw e
    } catch (e: UnknownHostException) {
        Log.e(tag, "No internet connection", e)
        NetworkResult.Error(
            message = "No internet connection",
            isRetryable = true
        )
    } catch (e: SocketTimeoutException) {
        Log.e(tag, "Request timed out", e)
        NetworkResult.Error(
            message = "Request timed out",
            isRetryable = true
        )
    } catch (e: SSLException) {
        Log.e(tag, "Secure connection failed", e)
        NetworkResult.Error(
            message = "Secure connection failed",
            isRetryable = true
        )
    } catch (e: IOException) {
        Log.e(tag, "Network error", e)
        NetworkResult.Error(
            message = "Network error occurred",
            isRetryable = true
        )
    } catch (e: Exception) {
        Log.e(tag, "Unexpected error", e)
        NetworkResult.Error(
            message = "Something went wrong",
            isRetryable = false
        )
    }
}

/**
 * Handle Retrofit Response, parsing errors when unsuccessful.
 */
private fun <T> handleResponse(
    response: Response<T>,
    tag: String
): NetworkResult<T> {
    return if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            Log.w(tag, "Response body is null")
            NetworkResult.Error(
                message = "Empty response",
                isRetryable = false
            )
        }
    } else {
        val errorResponse = ErrorUtils.parseError(response)
        val message = errorResponse?.message ?: "Request failed (${response.code()})"
        Log.w(tag, "API error: ${response.code()} - $message")

        NetworkResult.Error(
            message = message,
            // 5xx errors are server-side and are worth retrying
            isRetryable = response.code() in 500..599
        )
    }
}
