package com.tuckercr.catsdogs.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface LoadingState<out T> {
    data object Idle : LoadingState<Nothing>

    data object Loading : LoadingState<Nothing>

    data class Success<T>(
        val data: T,
    ) : LoadingState<T>

    data class Error(
        val errorKey: String,
        val canRetry: Boolean = true,
    ) : LoadingState<Nothing>
}
