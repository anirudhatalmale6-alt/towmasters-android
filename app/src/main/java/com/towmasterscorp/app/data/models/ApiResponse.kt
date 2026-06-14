package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: User? = null,
    @SerializedName("company") val company: Company? = null,
    @SerializedName("call") val call: T? = null
) {
    fun getItem(): T? = data ?: call
}

data class ListResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<T>? = null,
    @SerializedName("calls") val calls: List<T>? = null,
    @SerializedName("users") val users: List<T>? = null,
    @SerializedName("conversations") val conversations: List<T>? = null,
    @SerializedName("messages") val messages: List<T>? = null,
    @SerializedName("drivers") val drivers: List<T>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("total_pages") val totalPages: Int? = null
) {
    fun getItems(): List<T> {
        return data ?: calls ?: users ?: conversations ?: messages ?: drivers ?: emptyList()
    }
}
