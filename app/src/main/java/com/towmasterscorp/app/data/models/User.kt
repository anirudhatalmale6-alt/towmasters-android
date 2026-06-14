package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String, // admin, dispatcher, driver, mechanic
    @SerializedName("company_id") val companyId: Int? = null,
    @SerializedName("status") val status: String? = null, // active, inactive
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_clocked_in") val isClockedIn: Boolean = false,
    @SerializedName("last_clock_in") val lastClockIn: String? = null,
    @SerializedName("last_clock_out") val lastClockOut: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("location_updated_at") val locationUpdatedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
    val isAdmin: Boolean get() = role == "admin"
    val isDispatcher: Boolean get() = role == "dispatcher" || role == "admin"
    val isDriver: Boolean get() = role == "driver"
}

data class Company(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("zip") val zip: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LocationUpdate(
    val latitude: Double,
    val longitude: Double
)
