package com.japp.api

import android.content.Context
import android.content.Intent
import com.japp.StartupActivity
import com.japp.messaging.JappMessagingService
import com.japp.websocket.ChatWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Session management for authentication state.
 */
object SessionManager {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun invalidateSession() {
        val ctx = applicationContext ?: return

        // Clear FCM token first (requires valid credentials for API call)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.userService.clearFcmToken()
            } catch (_: Exception) {
                // Simply ignore, token is invalid anyway
            }
        }

        ChatWebSocketClient.disconnect()

        CredentialsStorage.clear(ctx)

        _sessionExpired.value = true
    }

    fun resetSessionState() {
        _sessionExpired.value = false
    }

    fun logout(context: Context) {
        JappMessagingService.clearToken(context)

        ChatWebSocketClient.disconnect()

        CredentialsStorage.clear(context)

        navigateToStartup(context)
    }

    fun navigateToStartup(context: Context) {
        val intent = Intent(context, StartupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
