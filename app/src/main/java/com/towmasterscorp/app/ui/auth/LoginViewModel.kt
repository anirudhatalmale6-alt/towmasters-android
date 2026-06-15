package com.towmasterscorp.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.LoginRequest
import com.towmasterscorp.app.data.models.LoginResponse
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
        if (state.isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val jsonBody = """{"email":"${state.email.trim()}","password":"${state.password}"}"""
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url("https://app.towmasterscorp.com/api/auth.php?action=login")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                Log.d("LoginVM", "Response: ${response.code} - $responseBody")

                val gson = com.google.gson.Gson()
                val result = gson.fromJson(responseBody, LoginResponse::class.java)

                withContext(Dispatchers.Main) {
                    if (result?.success == true && result.token != null && result.user != null) {
                        ApiClient.token = result.token
                        authPreferences.saveToken(result.token)
                        authPreferences.saveUser(result.user)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.user
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result?.error ?: result?.message ?: "Login failed"
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e("LoginVM", "Login error", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error: ${e.javaClass.simpleName}: ${e.message ?: "Connection failed"}"
                    )
                }
            }
        }
    }
}
