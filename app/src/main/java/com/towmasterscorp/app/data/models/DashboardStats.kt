package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("today") val today: TodayStats? = null,
    @SerializedName("drivers_active") val driversActive: Int = 0,
    @SerializedName("vehicles_stored") val vehiclesStored: Int = 0,
    @SerializedName("unpaid_invoices") val unpaidInvoices: UnpaidInvoices? = null
)

data class TodayStats(
    @SerializedName("total_calls") val totalCalls: Any? = null,
    @SerializedName("completed") val completed: Any? = null,
    @SerializedName("active") val active: Any? = null,
    @SerializedName("pending") val pending: Any? = null,
    @SerializedName("total_revenue") val totalRevenue: Any? = null
) {
    fun getTotalCalls(): Int = parseIntValue(totalCalls)
    fun getCompleted(): Int = parseIntValue(completed)
    fun getActive(): Int = parseIntValue(active)
    fun getPending(): Int = parseIntValue(pending)
    fun getTotalRevenue(): Double = parseDoubleValue(totalRevenue)

    private fun parseIntValue(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun parseDoubleValue(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}

data class UnpaidInvoices(
    @SerializedName("count") val count: Any? = null,
    @SerializedName("amount") val amount: Any? = null
) {
    fun getCount(): Int = when (count) {
        is Number -> count.toInt()
        is String -> count.toIntOrNull() ?: 0
        else -> 0
    }
    fun getAmount(): Double = when (amount) {
        is Number -> amount.toDouble()
        is String -> amount.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

data class DashboardStats(
    val todayCalls: Int = 0,
    val activeCalls: Int = 0,
    val completedToday: Int = 0,
    val todayRevenue: Double = 0.0,
    val driversOnline: Int = 0
)

data class DeviceTokenRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("device_platform") val devicePlatform: String = "android"
)
