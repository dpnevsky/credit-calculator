package com.example.creditcalculatormobile.feature.auth.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.EmptyState
import com.example.creditcalculatormobile.core.ui.InfoCard
import com.example.creditcalculatormobile.core.ui.LoadingState
import com.example.creditcalculatormobile.core.ui.PrimaryButton
import com.example.creditcalculatormobile.core.util.formatBirthDateSafe
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto

@Composable
fun ProfileRoute(
    appContainer: AppContainer,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(appContainer.sessionManager),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        onLogoutClick = viewModel::logout,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    uiState: com.example.creditcalculatormobile.feature.auth.shared.AuthUiState,
    onLogoutClick: () -> Unit,
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
        // Убрали SectionTitle(title = "Профиль"), так как заголовок уже есть в TopBar

        when {
            !uiState.initialized || uiState.loading -> {
                item {
                    InfoCard {
                        LoadingState(message = "Загружаем профиль пользователя…")
                    }
                }
            }

            !uiState.authenticated || uiState.user == null -> {
                item {
                    InfoCard {
                        EmptyState(
                            title = "Пользователь не авторизован",
                            description = "Чтобы открыть профиль, сначала выполните вход.",
                        )
                        PrimaryButton(
                            text = "Перейти ко входу",
                            onClick = onNavigateToLogin,
                        )
                    }
                }
            }

            else -> {
                item {
                    ProfileContentCard(
                        user = uiState.user,
                        onLogoutClick = onLogoutClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContentCard(
    user: CurrentUserProfileDto,
    onLogoutClick: () -> Unit,
) {
    InfoCard(title = "Информация о пользователе") {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileField(
                label = "ФИО",
                value = user.fullName,
            )
            ProfileField(
                label = "Email",
                value = user.email,
            )
            ProfileField(
                label = "Дата рождения",
                value = formatBirthDateSafe(user.birthDate),
            )

            PrimaryButton(
                text = "Выйти из аккаунта",
                onClick = onLogoutClick,
            )
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
