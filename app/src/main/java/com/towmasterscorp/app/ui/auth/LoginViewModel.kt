package com.towmasterscorp.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.LoginRequest
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            try {
                val token = authPreferences.tokenFlow.first()
                if (!token.isNullOrEmpty()) {
                    ApiClient.token = token
                    val user = authPreferences.userFlow.first()
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            user = user
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginVM", "Session check failed", e)
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
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().login(
                        LoginRequest(email = state.email.trim(), password = state.password)
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.token != null && body.user != null) {
                        ApiClient.token = body.token
                        authPreferences.saveToken(body.token)
                        authPreferences.saveUser(body.user)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = body.user
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = body?.error ?: body?.message ?: "Login failed"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        val gson = com.google.gson.Gson()
                        val err = gson.fromJson(errorBody, Map::class.java)
                        err?.get("error")?.toString() ?: "Login failed (${response.code()})"
                    } catch (e: Exception) {
                        "Login failed (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                Log.e("LoginVM", "Login error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connection error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
}
