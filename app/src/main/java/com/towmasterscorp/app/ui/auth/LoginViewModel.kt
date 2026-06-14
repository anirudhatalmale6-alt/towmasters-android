package com.towmasterscorp.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.LoginRequest
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val user: User? = null
)

class LoginViewModel(
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val token = authPreferences.tokenFlow.first()
            if (!token.isNullOrEmpty()) {
                try {
                    val response = ApiClient.getApi().getMe()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val user = response.body()?.user
                        if (user != null) {
                            authPreferences.saveUser(user)
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                user = user
                            )
                        }
                    } else {
                        authPreferences.clearAll()
                    }
                } catch (e: Exception) {
                    // Token might be expired, stay on login
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = ApiClient.getApi().login(
                    LoginRequest(email = state.email.trim(), password = state.password)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val token = body.token
                        val user = body.user
                        if (token != null && user != null) {
                            authPreferences.saveToken(token)
                            authPreferences.saveUser(user)
                            ApiClient.resetClient()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = user
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Invalid server response"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = body?.error ?: body?.message ?: "Login failed"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Server error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.localizedMessage}"
                )
            }
        }
    }
}
