package com.example.creditcalculatormobile.core.session

import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto
import com.example.creditcalculatormobile.data.model.StoredAuthTokens

enum class AuthOperation {
    INITIALIZING,
    LOGINING,
    REGISTERING,
    REFRESHING,
    LOGGING_OUT,
}

sealed interface AuthState {
    val user: CurrentUserProfileDto?
    val tokens: StoredAuthTokens?
    val isAuthenticated: Boolean
    val isLoading: Boolean
    val errorMessage: String?

    data object Uninitialized : AuthState {
        override val user: CurrentUserProfileDto? = null
        override val tokens: StoredAuthTokens? = null
        override val isAuthenticated: Boolean = false
        override val isLoading: Boolean = false
        override val errorMessage: String? = null
    }

    data class Loading(
        val operation: AuthOperation,
        override val user: CurrentUserProfileDto? = null,
        override val tokens: StoredAuthTokens? = null,
        override val errorMessage: String? = null,
    ) : AuthState {
        override val isAuthenticated: Boolean = user != null && tokens != null
        override val isLoading: Boolean = true
    }

    data class Authenticated(
        override val user: CurrentUserProfileDto,
        override val tokens: StoredAuthTokens,
    ) : AuthState {
        override val isAuthenticated: Boolean = true
        override val isLoading: Boolean = false
        override val errorMessage: String? = null
    }

    data class Unauthenticated(
        override val errorMessage: String? = null,
    ) : AuthState {
        override val user: CurrentUserProfileDto? = null
        override val tokens: StoredAuthTokens? = null
        override val isAuthenticated: Boolean = false
        override val isLoading: Boolean = false
    }
}

val AuthState.currentUserOrNull: CurrentUserProfileDto?
    get() = user
