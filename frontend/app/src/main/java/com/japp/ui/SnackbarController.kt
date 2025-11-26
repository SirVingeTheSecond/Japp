package com.japp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * For accessing SnackbarHostState in the app.
 * Must be provided at the app root (MainActivity in our case).
 */
val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

data class JappSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val isError: Boolean = false
) : SnackbarVisuals

@Composable
fun JappSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val visuals = snackbarData.visuals
    val isError = (visuals as? JappSnackbarVisuals)?.isError ?: false

    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Snackbar(
        modifier = modifier.padding(12.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        actionContentColor = contentColor,
        action = visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    ) {
        Text(
            text = visuals.message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Remember a [SnackbarController] scoped to the current composition.
 */
@Composable
fun rememberSnackbar(): SnackbarController {
    val hostState = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        SnackbarController(scope, hostState)
    }
}

/**
 * Scoped helper for showing snackbars.
 * Get via [rememberSnackbar].
 */
class SnackbarController internal constructor(
    private val scope: CoroutineScope,
    private val hostState: SnackbarHostState
) {
    fun showSuccess(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()

            val result = hostState.showSnackbar(
                JappSnackbarVisuals(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short,
                    isError = false
                )
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun showError(
        message: String,
        actionLabel: String? = "Retry",
        onRetry: (() -> Unit)? = null
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()

            val result = hostState.showSnackbar(
                JappSnackbarVisuals(
                    message = message,
                    actionLabel = if (onRetry != null) actionLabel else null,
                    duration = SnackbarDuration.Long,
                    isError = true
                )
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRetry?.invoke()
            }
        }
    }

    fun dismiss() {
        hostState.currentSnackbarData?.dismiss()
    }
}
