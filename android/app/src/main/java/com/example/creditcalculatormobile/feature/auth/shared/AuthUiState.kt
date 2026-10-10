package com.example.creditcalculatormobile.feature.auth.shared

import com.example.creditcalculatormobile.core.session.AuthOperation
import com.example.creditcalculatormobile.core.session.AuthState
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto

data class AuthUiState(
    val initialized: Boolean,
    val authenticated: Boolean,
    val loading: Boolean,
    val operation: AuthOperation? = null,
    val user: CurrentUserProfileDto? = null,
    val errorMessage: String? = null,
) {
    companion object {
        val Initial = AuthUiState(
            initialized = false,
            authenticated = false,
            loading = false,
            operation = null,
            user = null,
            errorMessage = null,
        )
    }
}

fun AuthState.toUiState(): AuthUiState = when (this) {
    is AuthState.Uninitialized -> AuthUiState(
        initialized = false,
        authenticated = false,
        loading = false,
        operation = null,
        user = null,
        errorMessage = null,
    )

    is AuthState.Loading -> AuthUiState(
        initialized = true,
        authenticated = isAuthenticated,
        loading = true,
        operation = operation,
        user = user,
        errorMessage = errorMessage,
    )

    is AuthState.Authenticated -> AuthUiState(
        initialized = true,
        authenticated = true,
        loading = false,
        operation = null,
        user = user,
        errorMessage = null,
    )

    is AuthState.Unauthenticated -> AuthUiState(
        initialized = true,
        authenticated = false,
        loading = false,
        operation = null,
        user = null,
        errorMessage = errorMessage,
    )
}
