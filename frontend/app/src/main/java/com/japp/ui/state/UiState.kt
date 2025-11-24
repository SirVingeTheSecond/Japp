package com.japp.ui.state

/**
 * Represents the state of data loading in UI components.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()

    /**
     * Returns data if Success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Loading, is Error -> null
    }
}