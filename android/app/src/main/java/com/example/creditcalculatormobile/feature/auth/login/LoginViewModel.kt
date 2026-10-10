package com.example.creditcalculatormobile.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.session.AuthOperation
import com.example.creditcalculatormobile.core.session.SessionManager
import com.example.creditcalculatormobile.feature.auth.shared.AuthUiState
import com.example.creditcalculatormobile.feature.auth.shared.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val localErrorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank()
}

data class LoginScreenState(
    val form: LoginFormState = LoginFormState(),
    val auth: AuthUiState = AuthUiState.Initial,
) {
    val isSubmitting: Boolean
        get() = auth.loading && auth.operation == AuthOperation.LOGINING

    val visibleError: String?
        get() = form.localErrorMessage ?: auth.errorMessage
}

class LoginViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val formState = MutableStateFlow(LoginFormState())

    val uiState: StateFlow<LoginScreenState> = combine(
        formState,
        sessionManager.authState,
    ) { form, authState ->
        LoginScreenState(
            form = form,
            auth = authState.toUiState(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoginScreenState(),
    )

    fun updateEmail(value: String) {
        formState.value = formState.value.copy(
            email = value.trim(),
            localErrorMessage = null,
        )
    }

    fun updatePassword(value: String) {
        formState.value = formState.value.copy(
            password = value,
            localErrorMessage = null,
        )
    }

    fun login() {
        val form = formState.value

        if (form.email.isBlank()) {
            formState.value = form.copy(localErrorMessage = "Введите email")
            return
        }
        if (form.password.isBlank()) {
            formState.value = form.copy(localErrorMessage = "Введите пароль")
            return
        }

        viewModelScope.launch {
            sessionManager.login(
                username = form.email,
                password = form.password,
            )
        }
    }

    companion object {
        fun factory(sessionManager: SessionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LoginViewModel::class.java))
                    return LoginViewModel(sessionManager) as T
                }
            }
    }
}
