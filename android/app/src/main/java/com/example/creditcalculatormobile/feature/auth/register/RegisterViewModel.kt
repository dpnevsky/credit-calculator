package com.example.creditcalculatormobile.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.session.AuthOperation
import com.example.creditcalculatormobile.core.session.SessionManager
import com.example.creditcalculatormobile.data.model.RegisterRequestDto
import com.example.creditcalculatormobile.feature.auth.shared.AuthUiState
import com.example.creditcalculatormobile.feature.auth.shared.toUiState
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RegisterFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val birthDate: String = "",
    val localErrorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                birthDate.isNotBlank()
}

data class RegisterScreenState(
    val form: RegisterFormState = RegisterFormState(),
    val auth: AuthUiState = AuthUiState.Initial,
) {
    val isSubmitting: Boolean
        get() = auth.loading && auth.operation == AuthOperation.REGISTERING

    val visibleError: String?
        get() = form.localErrorMessage ?: auth.errorMessage
}

class RegisterViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val formState = MutableStateFlow(RegisterFormState())

    val uiState: StateFlow<RegisterScreenState> = combine(
        formState,
        sessionManager.authState,
    ) { form, authState ->
        RegisterScreenState(
            form = form,
            auth = authState.toUiState(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RegisterScreenState(),
    )

    fun updateEmail(value: String) {
        formState.value = formState.value.copy(email = value.trim(), localErrorMessage = null)
    }

    fun updatePassword(value: String) {
        formState.value = formState.value.copy(password = value, localErrorMessage = null)
    }

    fun updateConfirmPassword(value: String) {
        formState.value = formState.value.copy(confirmPassword = value, localErrorMessage = null)
    }

    fun updateFirstName(value: String) {
        formState.value = formState.value.copy(firstName = value, localErrorMessage = null)
    }

    fun updateLastName(value: String) {
        formState.value = formState.value.copy(lastName = value, localErrorMessage = null)
    }

    fun updateMiddleName(value: String) {
        formState.value = formState.value.copy(middleName = value, localErrorMessage = null)
    }

    fun updateBirthDate(value: String) {
        formState.value = formState.value.copy(birthDate = value.trim(), localErrorMessage = null)
    }

    fun register() {
        val form = formState.value

        val validationError = validate(form)
        if (validationError != null) {
            formState.value = form.copy(localErrorMessage = validationError)
            return
        }

        viewModelScope.launch {
            sessionManager.register(
                RegisterRequestDto(
                    email = form.email,
                    password = form.password,
                    firstName = form.firstName.trim(),
                    lastName = form.lastName.trim(),
                    middleName = form.middleName.trim().ifBlank { null },
                    birthDate = form.birthDate.trim(),
                ),
            )
        }
    }

    private fun validate(form: RegisterFormState): String? {
        if (form.email.isBlank()) return "Введите email"
        if (form.password.isBlank()) return "Введите пароль"
        if (form.confirmPassword.isBlank()) return "Подтвердите пароль"
        if (form.firstName.isBlank()) return "Введите имя"
        if (form.lastName.isBlank()) return "Введите фамилию"
        if (form.birthDate.isBlank()) return "Введите дату рождения в формате YYYY-MM-DD"
        if (form.password != form.confirmPassword) return "Пароль и подтверждение не совпадают"

        val parsedDate = try {
            LocalDate.parse(form.birthDate.trim())
        } catch (_: DateTimeParseException) {
            return "Дата рождения должна быть в формате YYYY-MM-DD"
        }

        if (parsedDate.isAfter(LocalDate.now())) {
            return "Дата рождения не может быть в будущем"
        }

        return null
    }

    companion object {
        fun factory(sessionManager: SessionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(RegisterViewModel::class.java))
                    return RegisterViewModel(sessionManager) as T
                }
            }
    }
}
