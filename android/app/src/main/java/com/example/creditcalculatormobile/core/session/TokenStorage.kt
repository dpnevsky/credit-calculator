package com.example.creditcalculatormobile.core.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TOKEN_DATASTORE_NAME = "credit_calculator_mobile_auth"

private val Context.tokenDataStore by preferencesDataStore(
    name = TOKEN_DATASTORE_NAME,
)

class TokenStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TokenStore {
    override val refreshMutex = kotlinx.coroutines.sync.Mutex()
    private val version = java.util.concurrent.atomic.AtomicLong()
    override val sessionVersion: Long get() = version.get()
    private val applicationContext = context.applicationContext

    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val expiresAtEpochMillis = longPreferencesKey("expires_at_epoch_millis")
        val tokenType = stringPreferencesKey("token_type")
    }

    val tokensFlow: Flow<StoredAuthTokens?> = applicationContext.tokenDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map(::mapPreferencesToTokens)

    override suspend fun saveTokens(tokens: StoredAuthTokens) {
        withContext(ioDispatcher) {
            applicationContext.tokenDataStore.edit { preferences ->
                preferences[Keys.accessToken] = tokens.accessToken
                preferences[Keys.refreshToken] = tokens.refreshToken
                preferences[Keys.expiresAtEpochMillis] = tokens.expiresAtEpochMillis
                preferences[Keys.tokenType] = tokens.tokenType
            }
            version.incrementAndGet()
        }
    }

    override suspend fun readTokens(): StoredAuthTokens? {
        return tokensFlow.first()
    }

    override suspend fun readAccessToken(): String? = readTokens()?.accessToken

    override suspend fun readRefreshToken(): String? = readTokens()?.refreshToken

    override suspend fun clear() {
        withContext(ioDispatcher) {
            applicationContext.tokenDataStore.edit { preferences ->
                preferences.remove(Keys.accessToken)
                preferences.remove(Keys.refreshToken)
                preferences.remove(Keys.expiresAtEpochMillis)
                preferences.remove(Keys.tokenType)
            }
            version.incrementAndGet()
        }
    }

    fun readTokensBlocking(): StoredAuthTokens? = runBlocking(ioDispatcher) {
        readTokens()
    }

    override fun readAccessTokenBlocking(): String? = runBlocking(ioDispatcher) {
        readAccessToken()
    }

    fun readRefreshTokenBlocking(): String? = runBlocking(ioDispatcher) {
        readRefreshToken()
    }

    fun saveTokensBlocking(tokens: StoredAuthTokens) {
        runBlocking(ioDispatcher) {
            saveTokens(tokens)
        }
    }

    fun clearBlocking() {
        runBlocking(ioDispatcher) {
            clear()
        }
    }

    override suspend fun replaceIfCurrent(expected: StoredAuthTokens, replacement: StoredAuthTokens): Boolean {
        var replaced = false
        applicationContext.tokenDataStore.edit { preferences ->
            if (mapPreferencesToTokens(preferences) == expected) {
                preferences[Keys.accessToken] = replacement.accessToken
                preferences[Keys.refreshToken] = replacement.refreshToken
                preferences[Keys.expiresAtEpochMillis] = replacement.expiresAtEpochMillis
                preferences[Keys.tokenType] = replacement.tokenType
                replaced = true
            }
        }
        return replaced
    }

    override suspend fun clearIfCurrent(expected: StoredAuthTokens): Boolean {
        var cleared = false
        applicationContext.tokenDataStore.edit { preferences ->
            if (mapPreferencesToTokens(preferences) == expected) {
                preferences.clear()
                version.incrementAndGet()
                cleared = true
            }
        }
        return cleared
    }

    private fun mapPreferencesToTokens(preferences: Preferences): StoredAuthTokens? {
        val accessToken = preferences[Keys.accessToken]
        val refreshToken = preferences[Keys.refreshToken]
        val expiresAt = preferences[Keys.expiresAtEpochMillis]
        val tokenType = preferences[Keys.tokenType]

        if (accessToken.isNullOrBlank() ||
            refreshToken.isNullOrBlank() ||
            expiresAt == null ||
            tokenType.isNullOrBlank()
        ) {
            return null
        }

        return StoredAuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMillis = expiresAt,
            tokenType = tokenType,
        )
    }
}
