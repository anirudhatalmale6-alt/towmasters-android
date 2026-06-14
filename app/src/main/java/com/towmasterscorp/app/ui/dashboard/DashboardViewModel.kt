package com.towmasterscorp.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.DashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.getApi().getDashboardStats()
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val stats = DashboardStats(
                        todayCalls = body.today?.getTotalCalls() ?: 0,
                        activeCalls = body.today?.getActive() ?: 0,
                        completedToday = body.today?.getCompleted() ?: 0,
                        todayRevenue = body.today?.getTotalRevenue() ?: 0.0,
                        driversOnline = body.driversActive
                    )
                    _uiState.value = _uiState.value.copy(
                        stats = stats,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.error ?: "Failed to load stats"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Network error"
                )
            }
        }
    }
}
