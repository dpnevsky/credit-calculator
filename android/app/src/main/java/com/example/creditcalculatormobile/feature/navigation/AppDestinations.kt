package com.example.creditcalculatormobile.feature.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val title: String,
    val topLevel: Boolean = false,
    val shortLabel: String = title,
    val icon: ImageVector? = null,
) {
    data object Calculator : AppDestination(
        route = "calculator",
        title = "Кредитный калькулятор",
        topLevel = true,
        shortLabel = "Калькулятор",
        icon = Icons.Outlined.Calculate,
    )

    data object Login : AppDestination(
        route = "login",
        title = "Вход в систему",
        topLevel = true,
        shortLabel = "Вход",
        icon = Icons.Outlined.Login,
    )

    data object Register : AppDestination(
        route = "register",
        title = "Регистрация",
        topLevel = true,
        shortLabel = "Регистрация",
        icon = Icons.Outlined.PersonAddAlt1,
    )

    data object Profile : AppDestination(
        route = "profile",
        title = "Профиль",
        topLevel = true,
        shortLabel = "Профиль",
        icon = Icons.Outlined.Person,
    )

    data object ApplicationsList : AppDestination(
        route = "applications",
        title = "Мои заявки",
        topLevel = true,
        shortLabel = "Заявки",
        icon = Icons.Outlined.ListAlt,
    )

    data object CreateApplication : AppDestination(
        route = "applications/create",
        title = "Создание заявки",
        topLevel = true,
        shortLabel = "Новая",
        icon = Icons.Outlined.AddCircleOutline,
    )

    data object ApplicationDetails : AppDestination(
        route = "applications/{applicationId}",
        title = "Детали заявки",
        shortLabel = "Детали",
        icon = Icons.Outlined.ListAlt,
    ) {
        const val applicationIdArg = "applicationId"

        fun createRoute(applicationId: String): String =
            "applications/$applicationId"
    }

    data object ContractDocuments : AppDestination(
        route = "applications/{applicationId}/contract",
        title = "Договор и документы",
        shortLabel = "Документы",
        icon = Icons.Outlined.Description,
    ) {
        const val applicationIdArg = "applicationId"

        fun createRoute(applicationId: String): String =
            "applications/$applicationId/contract"
    }

    companion object {
        fun topLevelDestinations(isAuthenticated: Boolean): List<AppDestination> {
            return if (isAuthenticated) {
                listOf(
                    Calculator,
                    ApplicationsList,
                    CreateApplication,
                    Profile,
                )
            } else {
                listOf(
                    Calculator,
                    Login,
                    Register,
                )
            }
        }

        fun resolveTitle(route: String?): String = when (route) {
            Calculator.route -> Calculator.title
            Login.route -> Login.title
            Register.route -> Register.title
            Profile.route -> Profile.title
            ApplicationsList.route -> ApplicationsList.title
            CreateApplication.route -> CreateApplication.title
            ApplicationDetails.route -> ApplicationDetails.title
            ContractDocuments.route -> ContractDocuments.title
            else -> "Credit Calculator Mobile"
        }

        fun shouldShowBottomBar(route: String?): Boolean = when (route) {
            Login.route,
            Register.route,
            ApplicationDetails.route,
            ContractDocuments.route -> false
            else -> true
        }

        fun isTopLevelRoute(route: String?, authenticated: Boolean): Boolean {
            return topLevelDestinations(authenticated).any { it.route == route }
        }
    }
}
