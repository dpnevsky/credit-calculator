package com.example.creditcalculatormobile.core.session

import com.example.creditcalculatormobile.data.model.CurrentSession
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto
import com.example.creditcalculatormobile.data.model.RegisterRequestDto
import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import com.example.creditcalculatormobile.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStore,
    private val applicationScope: CoroutineScope,
) {
    private val authMutex = Mutex()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun start() {
        applicationScope.launch {
            initialize()
        }
    }

    suspend fun initialize() {
        authMutex.withLock {
            val currentState = _authState.value
            if (currentState !is AuthState.Uninitialized) {
                return
            }

            _authState.value = AuthState.Loading(operation = AuthOperation.INITIALIZING)

            val storedTokens = tokenStorage.readTokens()
            if (storedTokens == null) {
                _authState.value = AuthState.Unauthenticated()
                return
            }

            if (storedTokens.isExpiringSoon()) {
                when (val refreshResult = authRepository.refreshSession(storedTokens.refreshToken)) {
                    is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                        _authState.value = AuthState.Authenticated(
                            user = refreshResult.value.user,
                            tokens = refreshResult.value.tokens,
                        )
                    }
                    is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                        clearRejectedSession(refreshResult.error)
                        _authState.value = AuthState.Unauthenticated(
                            errorMessage = refreshResult.error.message,
                        )
                    }
                }
                return
            }

            when (val userResult = authRepository.loadCurrentUser(storedTokens.accessToken)) {
                is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                    _authState.value = AuthState.Authenticated(
                        user = userResult.value,
                        tokens = storedTokens,
                    )
                }
                is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                    when (val refreshResult = authRepository.refreshSession(storedTokens.refreshToken)) {
                        is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                            _authState.value = AuthState.Authenticated(
                                user = refreshResult.value.user,
                                tokens = refreshResult.value.tokens,
                            )
                        }
                        is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                            clearRejectedSession(refreshResult.error)
                            _authState.value = AuthState.Unauthenticated(
                                errorMessage = refreshResult.error.message,
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun login(
        username: String,
        password: String,
    ): com.example.creditcalculatormobile.core.network.ApiResult<CurrentSession> {
        return authMutex.withLock {
            _authState.value = AuthState.Loading(
                operation = AuthOperation.LOGINING,
                user = _authState.value.user,
                tokens = _authState.value.tokens,
            )

            when (val result = authRepository.login(username = username, password = password)) {
                is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                    _authState.value = AuthState.Authenticated(
                        user = result.value.user,
                        tokens = result.value.tokens,
                    )
                    result
                }
                is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                    _authState.value = AuthState.Unauthenticated(
                        errorMessage = result.error.message,
                    )
                    result
                }
            }
        }
    }

    suspend fun register(
        request: RegisterRequestDto,
    ): com.example.creditcalculatormobile.core.network.ApiResult<CurrentSession> {
        return authMutex.withLock {
            _authState.value = AuthState.Loading(
                operation = AuthOperation.REGISTERING,
                user = _authState.value.user,
                tokens = _authState.value.tokens,
            )

            when (val result = authRepository.register(request)) {
                is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                    _authState.value = AuthState.Authenticated(
                        user = result.value.user,
                        tokens = result.value.tokens,
                    )
                    result
                }
                is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                    _authState.value = AuthState.Unauthenticated(
                        errorMessage = result.error.message,
                    )
                    result
                }
            }
        }
    }

    suspend fun refreshSessionIfNeeded(
        minValiditySeconds: Long = StoredAuthTokens.DEFAULT_MIN_VALIDITY_SECONDS,
    ): Boolean {
        return authMutex.withLock {
            val tokens = tokenStorage.readTokens() ?: run {
                _authState.value = AuthState.Unauthenticated()
                return false
            }

            if (!tokens.isExpiringSoon(minValiditySeconds)) {
                if (_authState.value is AuthState.Authenticated) {
                    return true
                }

                when (val profileResult = authRepository.loadCurrentUser(tokens.accessToken)) {
                    is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                        _authState.value = AuthState.Authenticated(
                            user = profileResult.value,
                            tokens = tokens,
                        )
                        return true
                    }
                    is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                        // continue to refresh below
                    }
                }
            }

            _authState.value = AuthState.Loading(
                operation = AuthOperation.REFRESHING,
                user = _authState.value.user,
                tokens = _authState.value.tokens,
            )

            when (val refreshResult = authRepository.refreshSession(tokens.refreshToken)) {
                is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                    _authState.value = AuthState.Authenticated(
                        user = refreshResult.value.user,
                        tokens = refreshResult.value.tokens,
                    )
                    true
                }
                is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                    clearRejectedSession(refreshResult.error)
                    _authState.value = AuthState.Unauthenticated(
                        errorMessage = refreshResult.error.message,
                    )
                    false
                }
            }
        }
    }

    suspend fun logout() {
        authMutex.withLock {
            _authState.value = AuthState.Loading(
                operation = AuthOperation.LOGGING_OUT,
                user = _authState.value.user,
                tokens = _authState.value.tokens,
            )

            authRepository.logout()
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun forceLogout(message: String? = null, expectedRefreshToken: String? = null) {
        applicationScope.launch {
            authMutex.withLock {
                val current = tokenStorage.readTokens()
                if (expectedRefreshToken != null && current != null && current.refreshToken != expectedRefreshToken) return@withLock
                authRepository.clearLocalSession()
                _authState.value = AuthState.Unauthenticated(errorMessage = message)
            }
        }
    }

    private suspend fun clearRejectedSession(error: com.example.creditcalculatormobile.core.network.ApiFailure) {
        if ((error as? com.example.creditcalculatormobile.core.network.ApiFailure.Http)?.code in setOf(400, 401, 403)) {
            authRepository.clearLocalSession()
        }
    }

    fun currentUserOrNull(): CurrentUserProfileDto? = _authState.value.user

    fun currentTokensOrNull(): StoredAuthTokens? = _authState.value.tokens
}
