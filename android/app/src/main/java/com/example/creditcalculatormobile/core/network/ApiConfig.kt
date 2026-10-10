package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.BuildConfig

data class ApiConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long,
    val readTimeoutSeconds: Long,
    val enableNetworkLogging: Boolean,
) {
    init {
        require(baseUrl.isNotBlank()) { "BASE_URL must not be blank" }
        require(baseUrl.endsWith("/")) { "BASE_URL must end with '/'" }
        require(connectTimeoutSeconds > 0) { "connectTimeoutSeconds must be > 0" }
        require(readTimeoutSeconds > 0) { "readTimeoutSeconds must be > 0" }
    }

    fun absoluteUrl(relativePath: String): String {
        val normalizedPath = relativePath.removePrefix("/")
        return "$baseUrl$normalizedPath"
    }

    companion object {
        fun fromBuildConfig(): ApiConfig = ApiConfig(
            baseUrl = BuildConfig.BASE_URL,
            connectTimeoutSeconds = BuildConfig.NETWORK_CONNECT_TIMEOUT_SECONDS.toLong(),
            readTimeoutSeconds = BuildConfig.NETWORK_READ_TIMEOUT_SECONDS.toLong(),
            enableNetworkLogging = BuildConfig.ENABLE_NETWORK_LOGGING,
        )
    }
}

object ApiPaths {
    const val AUTH_LOGIN = "api/auth/login"
    const val AUTH_REGISTER = "api/auth/register"
    const val AUTH_REFRESH = "api/auth/refresh"
    const val AUTH_ME = "api/auth/me"
    const val AUTH_LOGOUT = "api/auth/logout"

    const val APPLICATIONS = "api/applications"
    const val DOCUMENTS = "api/documents"

    fun applicationById(applicationId: String): String =
        "api/applications/$applicationId"

    fun applicationSubmit(applicationId: String): String =
        "api/applications/$applicationId/submit"

    fun applicationScoringResult(applicationId: String): String =
        "api/applications/$applicationId/scoring-result"

    fun applicationOffers(applicationId: String): String =
        "api/applications/$applicationId/offers"

    fun selectOffer(applicationId: String, offerId: String): String =
        "api/applications/$applicationId/offers/$offerId/select"

    fun applicationDocuments(applicationId: String): String =
        "api/applications/$applicationId/documents"

    fun requestDocuments(applicationId: String): String =
        "api/applications/$applicationId/request-documents"

    fun documentDownload(documentId: String): String =
        "api/documents/$documentId/download"
}
