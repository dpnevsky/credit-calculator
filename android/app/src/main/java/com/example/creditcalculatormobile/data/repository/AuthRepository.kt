package com.example.creditcalculatormobile.data.repository

import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.core.network.AuthInterceptor
import com.example.creditcalculatormobile.core.network.safeApiCall
import com.example.creditcalculatormobile.core.session.TokenStore
import com.example.creditcalculatormobile.data.model.AuthResponseDto
import com.example.creditcalculatormobile.data.model.CurrentSession
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto
import com.example.creditcalculatormobile.data.model.LoginRequestDto
import com.example.creditcalculatormobile.data.model.LogoutRequestDto
import com.example.creditcalculatormobile.data.model.RefreshRequestDto
import com.example.creditcalculatormobile.data.model.RegisterRequestDto
import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import com.example.creditcalculatormobile.data.remote.AuthApi
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.withLock
import com.example.creditcalculatormobile.core.network.ApiFailure

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStore,
    private val json: Json,
) {

    suspend fun login(
        username: String,
        password: String,
    ): ApiResult<CurrentSession> {
        val tokenResult = safeApiCall(json) {
            authApi.login(
                LoginRequestDto(
                    username = username,
                    password = password,
                ),
            )
        }

        return completeSessionAfterAuth(tokenResult)
    }

    suspend fun register(
        request: RegisterRequestDto,
    ): ApiResult<CurrentSession> {
        val tokenResult = safeApiCall(json) {
            authApi.register(request)
        }

        return completeSessionAfterAuth(tokenResult)
    }

    suspend fun refreshSession(
        refreshToken: String,
    ): ApiResult<CurrentSession> {
        return tokenStorage.refreshMutex.withLock {
            val current = tokenStorage.readTokens()
                ?: return@withLock ApiResult.Failure(ApiFailure.Http(401, "Сессия завершена"))
            if (current.refreshToken != refreshToken) {
                return@withLock loadCurrentUser(current.accessToken).let { result ->
                    when (result) {
                        is ApiResult.Success -> ApiResult.Success(CurrentSession(result.value, current), result.code)
                        is ApiResult.Failure -> result
                    }
                }
            }
            val refreshResult = safeApiCall(json) { authApi.refresh(RefreshRequestDto(refreshToken)) }
            when (refreshResult) {
                is ApiResult.Failure -> {
                    if ((refreshResult.error as? ApiFailure.Http)?.code in setOf(400, 401, 403)) {
                        tokenStorage.clearIfCurrent(current)
                    }
                    refreshResult
                }
                is ApiResult.Success -> {
                    val replacement = StoredAuthTokens.fromAuthResponse(refreshResult.value)
                    if (!tokenStorage.replaceIfCurrent(current, replacement)) {
                        return@withLock ApiResult.Failure(ApiFailure.Http(401, "Сессия завершена"))
                    }
                    when (val profile = loadCurrentUser(replacement.accessToken)) {
                        is ApiResult.Success -> ApiResult.Success(CurrentSession(profile.value, replacement), profile.code)
                        is ApiResult.Failure -> profile
                    }
                }
            }
        }
    }

    suspend fun loadCurrentUser(
        accessToken: String,
    ): ApiResult<CurrentUserProfileDto> {
        return safeApiCall(json) {
            authApi.getCurrentUser(
                bearerToken = AuthInterceptor.bearerValue(accessToken),
            )
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        val refreshToken = tokenStorage.readRefreshToken()
        // Clear before the network call so an in-flight refresh cannot restore the session.
        clearLocalSession()
        if (!refreshToken.isNullOrBlank()) {
            safeApiCall(json) {
                authApi.logout(
                    LogoutRequestDto(refreshToken = refreshToken),
                )
            }
        }

        clearLocalSession()
        return ApiResult.Success(Unit, code = 200)
    }

    suspend fun clearLocalSession() {
        tokenStorage.clear()
    }

    private suspend fun completeSessionAfterAuth(
        tokenResult: ApiResult<AuthResponseDto>,
    ): ApiResult<CurrentSession> {
        return when (tokenResult) {
            is ApiResult.Failure -> {
                clearLocalSession()
                tokenResult
            }

            is ApiResult.Success -> {
                val tokens = StoredAuthTokens.fromAuthResponse(tokenResult.value)
                tokenStorage.saveTokens(tokens)

                val profileResult = loadCurrentUser(tokens.accessToken)
                when (profileResult) {
                    is ApiResult.Success -> {
                        ApiResult.Success(
                            value = CurrentSession(
                                user = profileResult.value,
                                tokens = tokens,
                            ),
                            code = tokenResult.code,
                        )
                    }

                    is ApiResult.Failure -> {
                        clearLocalSession()
                        profileResult
                    }
                }
            }
        }
    }
}
