package com.example.creditcalculatormobile

import android.content.Context
import com.example.creditcalculatormobile.core.network.ApiConfig
import com.example.creditcalculatormobile.core.network.NetworkModule
import com.example.creditcalculatormobile.core.session.SessionManager
import com.example.creditcalculatormobile.core.session.TokenStorage
import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import com.example.creditcalculatormobile.data.repository.AuthRepository
import com.example.creditcalculatormobile.data.repository.DocumentsRepository
import com.example.creditcalculatormobile.feature.contract.DocumentDownloadManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class AndroidAppConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Int,
    val readTimeoutSeconds: Int,
    val enableNetworkLogging: Boolean,
)

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val appConfig: AndroidAppConfig = AndroidAppConfig(
        baseUrl = BuildConfig.BASE_URL,
        connectTimeoutSeconds = BuildConfig.NETWORK_CONNECT_TIMEOUT_SECONDS,
        readTimeoutSeconds = BuildConfig.NETWORK_READ_TIMEOUT_SECONDS,
        enableNetworkLogging = BuildConfig.ENABLE_NETWORK_LOGGING,
    )

    val apiConfig: ApiConfig = ApiConfig.fromBuildConfig()

    val tokenStorage: TokenStorage by lazy {
        TokenStorage(applicationContext, ioDispatcher)
    }

    val documentDownloadManager: DocumentDownloadManager by lazy {
        DocumentDownloadManager(applicationContext, ioDispatcher)
    }

    private lateinit var internalSessionManager: SessionManager

    val networkModule: NetworkModule by lazy {
        NetworkModule(
            apiConfig = apiConfig,
            tokenStorage = tokenStorage,
            onAuthenticationFailure = { expiredRefreshToken ->
                if (::internalSessionManager.isInitialized) {
                    internalSessionManager.forceLogout("Сессия истекла. Выполните вход повторно.", expiredRefreshToken)
                }
            },
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authApi = networkModule.authApi,
            tokenStorage = tokenStorage,
            json = networkModule.json,
        )
    }

    val applicationsRepository: ApplicationsRepository by lazy {
        ApplicationsRepository(
            applicationsApi = networkModule.applicationsApi,
            json = networkModule.json,
        )
    }

    val documentsRepository: DocumentsRepository by lazy {
        DocumentsRepository(
            documentsApi = networkModule.documentsApi,
        )
    }

    val sessionManager: SessionManager
        get() = internalSessionManager

    init {
        internalSessionManager = SessionManager(
            authRepository = authRepository,
            tokenStorage = tokenStorage,
            applicationScope = applicationScope,
        )
        internalSessionManager.start()
    }
}
