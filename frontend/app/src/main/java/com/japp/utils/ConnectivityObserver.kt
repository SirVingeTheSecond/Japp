package com.japp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes network connectivity state.
 * Returns true when device has internet connectivity, false otherwise.
 */
@androidx.annotation.RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.observeConnectivityAsFlow(): Flow<Boolean> = callbackFlow @androidx.annotation.RequiresPermission(
    android.Manifest.permission.ACCESS_NETWORK_STATE
) {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        private val networks = mutableSetOf<Network>()

        override fun onAvailable(network: Network) {
            networks.add(network)
            trySend(networks.isNotEmpty())
        }

        override fun onLost(network: Network) {
            networks.remove(network)
            trySend(networks.isNotEmpty())
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .build()

    connectivityManager.registerNetworkCallback(request, callback)

    // This will emit the initial state
    val currentNetwork = connectivityManager.activeNetwork
    val hasConnection = currentNetwork != null &&
            connectivityManager.getNetworkCapabilities(currentNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    trySend(hasConnection)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}

/**
 * Composable that observes connectivity state.
 * Returns State<Boolean> where true = connected, false = disconnected.
 */
@Composable
fun rememberConnectivityState(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = false) {
        context.observeConnectivityAsFlow().collect { isConnected ->
            value = isConnected
        }
    }
}
