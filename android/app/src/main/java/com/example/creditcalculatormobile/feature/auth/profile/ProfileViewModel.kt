package com.example.creditcalculatormobile.feature.auth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.session.SessionManager
import com.example.creditcalculatormobile.feature.auth.shared.AuthUiState
import com.example.creditcalculatormobile.feature.auth.shared.toUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<AuthUiState> = sessionManager.authState
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthUiState.Initial,
        )

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
        }
    }

    companion object {
        fun factory(sessionManager: SessionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
                    return ProfileViewModel(sessionManager) as T
                }
            }
    }
}
