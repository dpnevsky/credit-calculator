package com.example.creditcalculatormobile.feature.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.AppTextField
import com.example.creditcalculatormobile.core.ui.ErrorState
import com.example.creditcalculatormobile.core.ui.InfoCard
import com.example.creditcalculatormobile.core.ui.LoadingState
import com.example.creditcalculatormobile.core.ui.PrimaryButton
import com.example.creditcalculatormobile.core.ui.SecondaryButton

@Composable
fun RegisterRoute(
    appContainer: AppContainer,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.factory(appContainer.sessionManager),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.auth.authenticated) {
        if (uiState.auth.authenticated) {
            onRegisterSuccess()
        }
    }

    RegisterScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        onFirstNameChange = viewModel::updateFirstName,
        onLastNameChange = viewModel::updateLastName,
        onMiddleNameChange = viewModel::updateMiddleName,
        onBirthDateChange = viewModel::updateBirthDate,
        onRegisterClick = viewModel::register,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterScreenState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Убран SectionTitle, так как заголовок есть в TopBar

        item {
            InfoCard { // Убрали заголовок "Данные пользователя"
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppTextField(
                        value = uiState.form.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        placeholder = "name@example.com",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    AppTextField(
                        value = uiState.form.password,
                        onValueChange = onPasswordChange,
                        label = "Пароль",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    AppTextField(
                        value = uiState.form.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Подтверждение пароля",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    AppTextField(
                        value = uiState.form.firstName,
                        onValueChange = onFirstNameChange,
                        label = "Имя",
                    )

                    AppTextField(
                        value = uiState.form.lastName,
                        onValueChange = onLastNameChange,
                        label = "Фамилия",
                    )

                    AppTextField(
                        value = uiState.form.middleName,
                        onValueChange = onMiddleNameChange,
                        label = "Отчество",
                        supportingText = "Поле можно оставить пустым",
                    )

                    AppTextField(
                        value = uiState.form.birthDate,
                        onValueChange = onBirthDateChange,
                        label = "Дата рождения",
                        placeholder = "ГГГГ-ММ-ДД",
                        supportingText = "Дата не может быть в будущем",
                    )

                    val error = uiState.visibleError
                    if (error != null) {
                        ErrorState(message = error)
                    }

                    if (uiState.isSubmitting) {
                        LoadingState(message = "Создаём аккаунт...")
                    }

                    PrimaryButton(
                        text = "Зарегистрироваться",
                        onClick = onRegisterClick,
                        enabled = uiState.form.canSubmit,
                        loading = uiState.isSubmitting,
                    )

                    PrimaryButton( // Изменили SecondaryButton на PrimaryButton для синего цвета
                        text = "У меня уже есть аккаунт",
                        onClick = onNavigateToLogin,
                        enabled = !uiState.isSubmitting,
                    )
                }
            }
        }
    }
}
