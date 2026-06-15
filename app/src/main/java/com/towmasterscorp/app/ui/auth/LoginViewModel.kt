package com.towmasterscorp.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towmasterscorp.app.data.api.ApiClient
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
        _uiState.value = state.copy(isLoading = true, error = null)

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        Thread {
            try {
                val url = java.net.URL("https://app.towmasterscorp.com/api/auth.php?action=login")
                val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val body = org.json.JSONObject()
                body.put("email", state.email.trim())
                body.put("password", state.password)

                conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

                val code = conn.responseCode
                val responseText = try {
                    if (code in 200..299) conn.inputStream.bufferedReader().readText()
                    else conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (e: Exception) { "" }
                conn.disconnect()

                Log.d("LoginVM", "HTTP $code: $responseText")

                val json = org.json.JSONObject(responseText)
                val success = json.optBoolean("success", false)
                val token = json.optString("token", "")
                val error = json.optString("error", "")
                val message = json.optString("message", "")

                if (success && token.isNotEmpty() && json.has("user")) {
                    val userJson = json.getJSONObject("user")
                    val user = User(
                        id = userJson.optInt("id", 0),
                        email = userJson.optString("email", ""),
                        firstName = userJson.optString("first_name", ""),
                        lastName = userJson.optString("last_name", ""),
                        role = userJson.optString("role", "driver"),
                        phone = userJson.optString("phone", null)
                    )

                    ApiClient.token = token

                    handler.post {
                        viewModelScope.launch {
                            authPreferences.saveToken(token)
                            authPreferences.saveUser(user)
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = user
                        )
                    }
                } else {
                    val errMsg = if (error.isNotEmpty()) error else if (message.isNotEmpty()) message else "Login failed"
                    handler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = errMsg)
                    }
                }
            } catch (e: Throwable) {
                Log.e("LoginVM", "Login thread error", e)
                handler.post {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }
        }.start()
    }
}
