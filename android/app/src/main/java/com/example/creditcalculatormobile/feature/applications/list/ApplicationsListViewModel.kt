package com.example.creditcalculatormobile.feature.applications.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.data.model.GetApplicationResponseDto
import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationsListUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val applications: List<GetApplicationResponseDto> = emptyList(),
)

class ApplicationsListViewModel(
    private val applicationsRepository: ApplicationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationsListUiState())
    val uiState: StateFlow<ApplicationsListUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                loading = current.applications.isEmpty(),
                refreshing = forceRefresh && current.applications.isNotEmpty(),
                errorMessage = null,
            )

            when (val result = applicationsRepository.getApplications()) {
                is com.example.creditcalculatormobile.core.network.ApiResult.Success -> {
                    _uiState.value = ApplicationsListUiState(
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                        applications = result.value.sortedByDescending { it.createdAt },
                    )
                }

                is com.example.creditcalculatormobile.core.network.ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = result.error.message,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            applicationsRepository: ApplicationsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ApplicationsListViewModel::class.java))
                return ApplicationsListViewModel(applicationsRepository) as T
            }
        }
    }
}
