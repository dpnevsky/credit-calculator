package com.example.creditcalculatormobile.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorPaymentType
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(
            amount = sanitizeNumberInput(value, allowDecimal = true),
            errorMessage = null,
        )
    }

    fun updateMonths(value: String) {
        _uiState.value = _uiState.value.copy(
            months = sanitizeNumberInput(value, allowDecimal = false),
            errorMessage = null,
        )
    }

    fun updateRate(value: String) {
        _uiState.value = _uiState.value.copy(
            rate = sanitizeNumberInput(value, allowDecimal = true),
            errorMessage = null,
        )
    }

    fun updatePaymentType(paymentType: CalculatorPaymentType) {
        _uiState.value = _uiState.value.copy(
            paymentType = paymentType,
            errorMessage = null,
        )
    }

    fun clearInputs() {
        _uiState.value = _uiState.value.copy(
            amount = "",
            months = "",
            rate = "",
            result = null,
            errorMessage = null,
        )
    }

    fun calculate() {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                loading = true,
                errorMessage = null,
            )

            val parseResult = parseCalculatorInput(
                amountText = current.amount,
                monthsText = current.months,
                rateText = current.rate,
                paymentType = current.paymentType,
            )

            parseResult
                .onSuccess { input ->
                    val calculation = calculateLoan(input)
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        result = calculation,
                        errorMessage = null,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        result = null,
                        errorMessage = error.message ?: "Не удалось выполнить расчёт",
                    )
                }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CalculatorViewModel::class.java))
                return CalculatorViewModel() as T
            }
        }

        private fun sanitizeNumberInput(value: String, allowDecimal: Boolean): String {
            val filtered = buildString {
                value.forEach { char ->
                    when {
                        char.isDigit() -> append(char)
                        allowDecimal && (char == '.' || char == ',') -> append(char)
                    }
                }
            }

            if (!allowDecimal) return filtered

            var seenSeparator = false
            return buildString {
                filtered.forEach { char ->
                    if (char == '.' || char == ',') {
                        if (!seenSeparator) {
                            append('.')
                            seenSeparator = true
                        }
                    } else {
                        append(char)
                    }
                }
            }
        }
    }
}
