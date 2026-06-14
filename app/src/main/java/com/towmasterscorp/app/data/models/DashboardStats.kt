package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class DashboardStats(
    @SerializedName("today_calls") val todayCalls: Int = 0,
    @SerializedName("active_calls") val activeCalls: Int = 0,
    @SerializedName("completed_today") val completedToday: Int = 0,
    @SerializedName("cancelled_today") val cancelledToday: Int = 0,
    @SerializedName("today_revenue") val todayRevenue: Double = 0.0,
    @SerializedName("week_revenue") val weekRevenue: Double = 0.0,
    @SerializedName("month_revenue") val monthRevenue: Double = 0.0,
    @SerializedName("drivers_online") val driversOnline: Int = 0,
    @SerializedName("drivers_total") val driversTotal: Int = 0,
    @SerializedName("avg_response_time") val avgResponseTime: Int = 0, // minutes
    @SerializedName("pending_invoices") val pendingInvoices: Int = 0,
    @SerializedName("calls_by_type") val callsByType: Map<String, Int>? = null,
    @SerializedName("calls_by_status") val callsByStatus: Map<String, Int>? = null
)
