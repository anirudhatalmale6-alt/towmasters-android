package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("email") val email: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String = "driver",
    @SerializedName("company_id") val companyId: Int? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_clocked_in") val isClockedIn: Int = 0,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("last_clock_in") val lastClockIn: String? = null,
    @SerializedName("last_clock_out") val lastClockOut: String? = null,
    @SerializedName("last_lat") val lastLat: Double? = null,
    @SerializedName("last_lng") val lastLng: Double? = null,
    @SerializedName("last_location_at") val lastLocationAt: String? = null,
    @SerializedName("location_id") val locationId: Int? = null,
    @SerializedName("location_name") val locationName: String? = null,
    @SerializedName("commission_rate") val commissionRate: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
    val isAdmin: Boolean get() = role == "admin"
    val isDispatcher: Boolean get() = role == "dispatcher" || role == "admin"
    val isDriver: Boolean get() = role == "driver"
    val clockedIn: Boolean get() = isClockedIn != 0
}

data class Company(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("subscription_plan") val subscriptionPlan: String? = null,
    @SerializedName("subscription_status") val subscriptionStatus: String? = null,
    @SerializedName("trial_expired") val trialExpired: Boolean? = null,
    @SerializedName("trial_days_remaining") val trialDaysRemaining: Int? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LocationUpdate(
    val lat: Double,
    val lng: Double
)
