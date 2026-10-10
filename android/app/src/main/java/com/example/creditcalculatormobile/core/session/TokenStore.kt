package com.example.creditcalculatormobile.core.session

import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

interface TokenStore {
    val refreshMutex: Mutex
    val sessionVersion: Long
    suspend fun readTokens(): StoredAuthTokens?
    suspend fun saveTokens(tokens: StoredAuthTokens)
    suspend fun clear()
    suspend fun replaceIfCurrent(expected: StoredAuthTokens, replacement: StoredAuthTokens): Boolean
    suspend fun clearIfCurrent(expected: StoredAuthTokens): Boolean

    suspend fun readAccessToken(): String? = readTokens()?.accessToken
    suspend fun readRefreshToken(): String? = readTokens()?.refreshToken
    fun readAccessTokenBlocking(): String? = runBlocking { readAccessToken() }
}
