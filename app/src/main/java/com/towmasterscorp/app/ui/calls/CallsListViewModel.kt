package com.towmasterscorp.app.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallsListUiState(
    val calls: List<Call> = emptyList(),
    val filteredCalls: List<Call> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CallsListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CallsListUiState())
    val uiState: StateFlow<CallsListUiState> = _uiState.asStateFlow()

    fun loadCalls() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.getApi().getCalls(perPage = 100)
                if (response.isSuccessful && response.body()?.success == true) {
                    val calls = response.body()?.data ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        calls = calls,
                        isLoading = false
                    )
                    applyFilters()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.error ?: "Failed to load calls"
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

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun updateStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.calls

        // Apply status filter
        when (state.statusFilter) {
            "active" -> filtered = filtered.filter { it.isActive }
            "completed" -> filtered = filtered.filter { it.status == "completed" }
            "cancelled" -> filtered = filtered.filter { it.status == "cancelled" }
        }

        // Apply search
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { call ->
                call.callNumber?.lowercase()?.contains(query) == true ||
                        call.vehicleDescription.lowercase().contains(query) ||
                        call.pickupAddress?.lowercase()?.contains(query) == true ||
                        call.dropoffAddress?.lowercase()?.contains(query) == true ||
                        call.customerName?.lowercase()?.contains(query) == true ||
                        call.driverName?.lowercase()?.contains(query) == true ||
                        call.callerName?.lowercase()?.contains(query) == true ||
                        call.vehiclePlate?.lowercase()?.contains(query) == true
            }
        }

        _uiState.value = state.copy(filteredCalls = filtered)
    }
}
