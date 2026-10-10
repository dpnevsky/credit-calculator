package com.example.creditcalculatormobile.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.AppTextField
import com.example.creditcalculatormobile.core.ui.InfoCard
import com.example.creditcalculatormobile.core.ui.PrimaryButton

@Composable
fun LoginRoute(
    appContainer: AppContainer,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(appContainer.sessionManager),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.auth.authenticated) {
        if (uiState.auth.authenticated) {
            onLoginSuccess()
        }
    }

    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onLoginClick = viewModel::login,
        onNavigateToRegister = onNavigateToRegister,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginScreenState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Убран SectionTitle, так как заголовок есть в TopBar

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppTextField(
                    value = uiState.form.email,
                    onValueChange = onEmailChange,
                    label = "Логин (Email)",
                    placeholder = "example@mail.com",
                    modifier = Modifier.fillMaxWidth(),
                )

                AppTextField(
                    value = uiState.form.password,
                    onValueChange = onPasswordChange,
                    label = "Пароль",
                    placeholder = "••••••••",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                val error = uiState.visibleError
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                PrimaryButton(
                    text = "Войти",
                    onClick = onLoginClick,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Ещё нет аккаунта?",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onNavigateToRegister) {
                Text(text = "Регистрация")
            }
        }
    }
}
