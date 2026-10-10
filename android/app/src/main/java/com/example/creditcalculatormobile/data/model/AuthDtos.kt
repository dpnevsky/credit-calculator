package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String,
    @SerialName("middleName")
    val middleName: String? = null,
    @SerialName("birthDate")
    val birthDate: String? = null,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refreshToken")
    val refreshToken: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refreshToken")
    val refreshToken: String,
)

@Serializable
data class AuthResponseDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
    @SerialName("tokenType")
    val tokenType: String = "Bearer",
)

data class StoredAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val tokenType: String,
) {
    fun isExpiringSoon(
        minValiditySeconds: Long = DEFAULT_MIN_VALIDITY_SECONDS,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        return expiresAtEpochMillis - nowMillis <= minValiditySeconds * 1_000L
    }

    companion object {
        const val DEFAULT_MIN_VALIDITY_SECONDS: Long = 30

        fun fromAuthResponse(
            response: AuthResponseDto,
            nowMillis: Long = System.currentTimeMillis(),
        ): StoredAuthTokens {
            return StoredAuthTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAtEpochMillis = nowMillis + response.expiresIn * 1_000L,
                tokenType = response.tokenType,
            )
        }
    }
}
