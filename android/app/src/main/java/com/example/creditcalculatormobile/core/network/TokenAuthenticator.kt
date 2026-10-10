package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.core.session.TokenStore
import com.example.creditcalculatormobile.data.model.AuthResponseDto
import com.example.creditcalculatormobile.data.model.RefreshRequestDto
import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val apiConfig: ApiConfig,
    private val tokenStorage: TokenStore,
    private val json: Json,
    private val onAuthenticationFailure: (String) -> Unit = {},
) : Authenticator {
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(apiConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(apiConfig.readTimeoutSeconds, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        if (response.request.url.encodedPath in PUBLIC_PATHS) return@runBlocking null
        tokenStorage.refreshMutex.withLock {
            val current = tokenStorage.readTokens() ?: return@withLock null
            val version = response.request.tag(SessionVersion::class.java)
            if (version != null && version.value != tokenStorage.sessionVersion) return@withLock null
            val requestToken = AuthInterceptor.extractBearerToken(response.request.header("Authorization"))
            // Anonymous requests must not acquire a session through the authenticator.
            if (requestToken == null) return@withLock null
            if (response.priorResponse != null) {
                if (current.accessToken == requestToken) invalidate(current)
                return@withLock null
            }
            if (current.accessToken != requestToken) return@withLock retry(response, current)
            when (val result = refreshTokens(current.refreshToken)) {
                is RefreshResult.Success -> {
                    if (tokenStorage.replaceIfCurrent(current, result.tokens)) {
                        retry(response, result.tokens)
                    } else null
                }
                RefreshResult.Rejected -> { invalidate(current); null }
                RefreshResult.Unavailable -> null
            }
        }
    }

    private fun retry(response: Response, tokens: StoredAuthTokens): Request = response.request.newBuilder()
        .header("Authorization", AuthInterceptor.bearerValue(tokens.accessToken)).build()

    private suspend fun invalidate(tokens: StoredAuthTokens) {
        if (tokenStorage.clearIfCurrent(tokens)) onAuthenticationFailure(tokens.refreshToken)
    }

    private fun refreshTokens(refreshToken: String): RefreshResult {
        val request = Request.Builder()
            .url(apiConfig.absoluteUrl(ApiPaths.AUTH_REFRESH))
            .post(json.encodeToString(RefreshRequestDto.serializer(), RefreshRequestDto(refreshToken))
                .toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()
        return try {
            refreshClient.newCall(request).execute().use { response ->
                when {
                    response.code in setOf(400, 401, 403) -> RefreshResult.Rejected
                    !response.isSuccessful -> RefreshResult.Unavailable
                    else -> {
                        val body = response.body?.string() ?: return RefreshResult.Unavailable
                        RefreshResult.Success(StoredAuthTokens.fromAuthResponse(
                            json.decodeFromString(AuthResponseDto.serializer(), body)))
                    }
                }
            }
        } catch (_: IOException) {
            RefreshResult.Unavailable
        } catch (_: SerializationException) {
            RefreshResult.Unavailable
        }
    }

    private sealed interface RefreshResult {
        data class Success(val tokens: StoredAuthTokens) : RefreshResult
        data object Rejected : RefreshResult
        data object Unavailable : RefreshResult
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val PUBLIC_PATHS = setOf("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout")
    }
}
