package com.example.creditcalculatormobile.feature.applications.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.EmptyState
import com.example.creditcalculatormobile.core.ui.ErrorState
import com.example.creditcalculatormobile.core.ui.InfoCard
import com.example.creditcalculatormobile.core.ui.LoadingState
import com.example.creditcalculatormobile.core.ui.SectionTitle
import com.example.creditcalculatormobile.core.util.formatDateSafe
import com.example.creditcalculatormobile.core.util.formatMoneyRub
import com.example.creditcalculatormobile.data.model.GetApplicationResponseDto
import com.example.creditcalculatormobile.feature.applications.shared.formatApplicationNumber
import com.example.creditcalculatormobile.feature.applications.shared.getApplicationStatusColor
import com.example.creditcalculatormobile.feature.applications.shared.getApplicationStatusLabel

@Composable
fun ApplicationsListRoute(
    appContainer: AppContainer,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApplicationsListViewModel = viewModel(
        factory = ApplicationsListViewModel.factory(appContainer.applicationsRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ApplicationsListScreen(
        uiState = uiState,
        onRefresh = { viewModel.loadApplications(forceRefresh = true) },
        onApplicationClick = onNavigateToDetails,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsListScreen(
    uiState: ApplicationsListUiState,
    onRefresh: () -> Unit,
    onApplicationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionTitle(
                    title = "Ваши заявки",
                    subtitle = "Список всех созданных вами заявок на кредит.",
                )
            }

            when {
                uiState.loading -> {
                    item {
                        InfoCard {
                            LoadingState(message = "Загружаем список заявок…")
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    item {
                        InfoCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ErrorState(message = uiState.errorMessage ?: "Ошибка")
                                Text(
                                    text = "Попробовать снова",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .clickable { onRefresh() }
                                )
                            }
                        }
                    }
                }

                uiState.applications.isEmpty() -> {
                    item {
                        InfoCard {
                            EmptyState(
                                title = "Заявок пока нет",
                                description = "Создайте свою первую заявку в калькуляторе.",
                            )
                        }
                    }
                }

                else -> {
                    items(
                        items = uiState.applications,
                        key = { it.applicationId },
                    ) { application ->
                        ApplicationItem(
                            application = application,
                            onClick = { onApplicationClick(application.applicationId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationItem(
    application: GetApplicationResponseDto,
    onClick: () -> Unit,
) {
    InfoCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Заявка #${formatApplicationNumber(application.applicationId)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Badge(
                    containerColor = getApplicationStatusColor(application.status),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = getApplicationStatusLabel(application.status),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Сумма",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = formatMoneyRub(application.amount),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Дата создания",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = formatDateSafe(application.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
