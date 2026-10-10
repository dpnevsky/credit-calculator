package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.core.session.TokenStore
import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import kotlinx.coroutines.sync.Mutex

internal class InMemoryTokenStore(initial: StoredAuthTokens? = null) : TokenStore {
    override val refreshMutex = Mutex()
    private val version = java.util.concurrent.atomic.AtomicLong()
    override val sessionVersion get() = version.get()
    @Volatile private var tokens = initial
    override suspend fun readTokens() = tokens
    override suspend fun saveTokens(tokens: StoredAuthTokens) { this.tokens = tokens; version.incrementAndGet() }
    override suspend fun clear() { tokens = null; version.incrementAndGet() }
    override suspend fun replaceIfCurrent(expected: StoredAuthTokens, replacement: StoredAuthTokens): Boolean {
        if (tokens != expected) return false
        tokens = replacement
        return true
    }
    override suspend fun clearIfCurrent(expected: StoredAuthTokens): Boolean {
        if (tokens != expected) return false
        tokens = null
        version.incrementAndGet()
        return true
    }
}
