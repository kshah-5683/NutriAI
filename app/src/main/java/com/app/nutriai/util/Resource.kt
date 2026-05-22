package com.app.nutriai.util

/**
 * A generic wrapper class for representing the state of async operations.
 * Used across ViewModels and UseCases to propagate loading/success/error states.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
