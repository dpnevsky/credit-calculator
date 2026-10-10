package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.core.session.TokenStorage
import com.example.creditcalculatormobile.data.remote.ApplicationsApi
import com.example.creditcalculatormobile.data.remote.AuthApi
import com.example.creditcalculatormobile.data.remote.DocumentsApi
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class NetworkModule(
    private val apiConfig: ApiConfig,
    private val tokenStorage: TokenStorage,
    private val onAuthenticationFailure: (String) -> Unit = {},
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val contentType = "application/json".toMediaType()

    private val publicHttpClient: OkHttpClient by lazy {
        baseClientBuilder()
            .build()
    }

    private val authenticatedHttpClient: OkHttpClient by lazy {
        baseClientBuilder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .authenticator(
                TokenAuthenticator(
                    apiConfig = apiConfig,
                    tokenStorage = tokenStorage,
                    json = json,
                    onAuthenticationFailure = onAuthenticationFailure,
                ),
            )
            .build()
    }

    private val publicRetrofit: Retrofit by lazy {
        retrofit(publicHttpClient)
    }

    private val authenticatedRetrofit: Retrofit by lazy {
        retrofit(authenticatedHttpClient)
    }

    val authApi: AuthApi by lazy {
        publicRetrofit.create(AuthApi::class.java)
    }

    val applicationsApi: ApplicationsApi by lazy {
        authenticatedRetrofit.create(ApplicationsApi::class.java)
    }

    val documentsApi: DocumentsApi by lazy {
        authenticatedRetrofit.create(DocumentsApi::class.java)
    }

    private fun retrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private fun baseClientBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(apiConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(apiConfig.readTimeoutSeconds, TimeUnit.SECONDS)

        if (apiConfig.enableNetworkLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                },
            )
        }

        return builder
    }
}
