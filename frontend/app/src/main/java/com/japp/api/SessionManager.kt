package com.japp.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    /**
     * Signal that the session has expired (401/403 received).
     * MainActivity observes this and performs cleanup.
     */
    fun invalidateSession() {
        _sessionExpired.value = true
    }

    /**
     * Reset after logout is handled to allow fresh login.
     */
    fun resetSessionState() {
        _sessionExpired.value = false
    }
}
