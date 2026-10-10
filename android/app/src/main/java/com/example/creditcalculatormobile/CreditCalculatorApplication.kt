package com.example.creditcalculatormobile

import android.app.Application

class CreditCalculatorApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
