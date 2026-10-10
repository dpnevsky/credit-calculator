package com.example.creditcalculatormobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.creditcalculatormobile.feature.navigation.AppNavGraph
import com.example.creditcalculatormobile.ui.theme.CreditCalculatorMobileTheme

@Composable
fun CreditCalculatorApp(appContainer: AppContainer) {
    CreditCalculatorMobileTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            AppNavGraph(appContainer = appContainer)
        }
    }
}
