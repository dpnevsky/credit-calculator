package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.core.session.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStorage: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val encodedPath = request.url.encodedPath

        if (request.header(HEADER_AUTHORIZATION) != null || shouldSkipPath(encodedPath)) {
            return chain.proceed(request)
        }

        val version = tokenStorage.sessionVersion
        val accessToken = tokenStorage.readAccessTokenBlocking()
        if (accessToken.isNullOrBlank() || version != tokenStorage.sessionVersion) {
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .tag(SessionVersion::class.java, SessionVersion(version))
            .header(HEADER_AUTHORIZATION, bearerValue(accessToken))
            .build()

        return chain.proceed(authenticatedRequest)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "

        fun bearerValue(token: String): String = "$BEARER_PREFIX$token"

        fun extractBearerToken(headerValue: String?): String? {
            if (headerValue.isNullOrBlank()) return null
            if (!headerValue.startsWith(BEARER_PREFIX)) return null
            return headerValue.removePrefix(BEARER_PREFIX).trim().takeIf { it.isNotEmpty() }
        }

        private fun shouldSkipPath(path: String): Boolean {
            return path.endsWith("/api/auth/login") ||
                    path.endsWith("/api/auth/register") ||
                    path.endsWith("/api/auth/refresh") ||
                    path.endsWith("/api/auth/logout")
        }
    }
}
internal data class SessionVersion(val value: Long)
