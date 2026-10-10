package com.example.creditcalculatormobile.feature.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.AppTopBar
import com.example.creditcalculatormobile.feature.contract.ContractDocumentsRoute
import com.example.creditcalculatormobile.feature.applications.create.CreateApplicationRoute
import com.example.creditcalculatormobile.feature.applications.details.ApplicationDetailsRoute
import com.example.creditcalculatormobile.feature.applications.list.ApplicationsListRoute
import com.example.creditcalculatormobile.feature.auth.login.LoginRoute
import com.example.creditcalculatormobile.feature.auth.profile.ProfileRoute
import com.example.creditcalculatormobile.feature.auth.register.RegisterRoute
import com.example.creditcalculatormobile.feature.calculator.CalculatorRoute

@Composable
fun AppNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val authState by appContainer.sessionManager.authState.collectAsStateWithLifecycle()
    val isAuthenticated = authState.isAuthenticated
    val topLevelDestinations = AppDestination.topLevelDestinations(isAuthenticated)
    val canNavigateBack = !AppDestination.isTopLevelRoute(currentRoute, isAuthenticated)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = AppDestination.resolveTitle(currentRoute),
                canNavigateBack = canNavigateBack,
                onNavigateBack = { navController.popBackStack() },
            )
        },
        bottomBar = {
            if (AppDestination.shouldShowBottomBar(currentRoute)) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    topLevelDestinations.forEach { destination ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.Calculator.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = requireNotNull(destination.icon),
                                    contentDescription = destination.title,
                                )
                            },
                            label = { Text(destination.shortLabel) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Calculator.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(AppDestination.Calculator.route) {
                CalculatorRoute()
            }

            composable(AppDestination.Login.route) {
                LoginRoute(
                    appContainer = appContainer,
                    onLoginSuccess = {
                        navController.navigate(AppDestination.ApplicationsList.route) {
                            popUpTo(AppDestination.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(AppDestination.Register.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(AppDestination.Register.route) {
                RegisterRoute(
                    appContainer = appContainer,
                    onRegisterSuccess = {
                        navController.navigate(AppDestination.ApplicationsList.route) {
                            popUpTo(AppDestination.Register.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(AppDestination.Login.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(AppDestination.Profile.route) {
                if (isAuthenticated) {
                    ProfileRoute(
                        appContainer = appContainer,
                        onNavigateToLogin = {
                            navController.navigate(AppDestination.Login.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                } else {
                    LoginRoute(
                        appContainer = appContainer,
                        onLoginSuccess = {
                            navController.navigate(AppDestination.Profile.route) {
                                popUpTo(AppDestination.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(AppDestination.Register.route)
                        },
                    )
                }
            }

            composable(AppDestination.ApplicationsList.route) {
                if (isAuthenticated) {
                    ApplicationsListRoute(
                        appContainer = appContainer,
                        onNavigateToDetails = { applicationId ->
                            navController.navigate(
                                AppDestination.ApplicationDetails.createRoute(applicationId),
                            )
                        },
                    )
                } else {
                    LoginRoute(
                        appContainer = appContainer,
                        onLoginSuccess = {
                            navController.navigate(AppDestination.ApplicationsList.route) {
                                popUpTo(AppDestination.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(AppDestination.Register.route)
                        },
                    )
                }
            }

            composable(AppDestination.CreateApplication.route) {
                if (isAuthenticated) {
                    CreateApplicationRoute(
                        appContainer = appContainer,
                        onCreated = { applicationId ->
                            navController.navigate(
                                AppDestination.ApplicationDetails.createRoute(applicationId),
                            ) {
                                popUpTo(AppDestination.CreateApplication.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                } else {
                    LoginRoute(
                        appContainer = appContainer,
                        onLoginSuccess = {
                            navController.navigate(AppDestination.CreateApplication.route) {
                                popUpTo(AppDestination.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(AppDestination.Register.route)
                        },
                    )
                }
            }

            composable(
                route = AppDestination.ApplicationDetails.route,
                arguments = listOf(
                    navArgument(AppDestination.ApplicationDetails.applicationIdArg) {
                        type = NavType.StringType
                    },
                ),
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString(
                    AppDestination.ApplicationDetails.applicationIdArg,
                ) ?: return@composable

                if (isAuthenticated) {
                    ApplicationDetailsRoute(
                        appContainer = appContainer,
                        applicationId = applicationId,
                        onOpenContract = { appId ->
                            navController.navigate(
                                AppDestination.ContractDocuments.createRoute(appId),
                            )
                        },
                    )
                } else {
                    LoginRoute(
                        appContainer = appContainer,
                        onLoginSuccess = {
                            navController.navigate(
                                AppDestination.ApplicationDetails.createRoute(applicationId),
                            ) {
                                popUpTo(AppDestination.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(AppDestination.Register.route)
                        },
                    )
                }
            }

            composable(
                route = AppDestination.ContractDocuments.route,
                arguments = listOf(
                    navArgument(AppDestination.ContractDocuments.applicationIdArg) {
                        type = NavType.StringType
                    },
                ),
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString(
                    AppDestination.ContractDocuments.applicationIdArg,
                ) ?: return@composable

                if (isAuthenticated) {
                    ContractDocumentsRoute(
                        appContainer = appContainer,
                        applicationId = applicationId,
                    )
                } else {
                    LoginRoute(
                        appContainer = appContainer,
                        onLoginSuccess = {
                            navController.navigate(
                                AppDestination.ContractDocuments.createRoute(applicationId),
                            ) {
                                popUpTo(AppDestination.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(AppDestination.Register.route)
                        },
                    )
                }
            }
        }
    }
}
