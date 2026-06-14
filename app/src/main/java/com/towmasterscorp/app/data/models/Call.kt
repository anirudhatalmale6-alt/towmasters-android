package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class Call(
    @SerializedName("id") val id: Int,
    @SerializedName("call_number") val callNumber: String? = null,
    @SerializedName("status") val status: String, // new, dispatched, en_route, on_scene, hooked, in_transit, delivered, completed, cancelled
    @SerializedName("priority") val priority: String? = null, // low, normal, high, emergency
    @SerializedName("type") val type: String? = null, // tow, jumpstart, lockout, tire_change, fuel_delivery, winch, accident
    @SerializedName("company_id") val companyId: Int? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("truck_id") val truckId: Int? = null,
    @SerializedName("truck_name") val truckName: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("caller_name") val callerName: String? = null,
    @SerializedName("caller_phone") val callerPhone: String? = null,
    @SerializedName("vehicle_year") val vehicleYear: String? = null,
    @SerializedName("vehicle_make") val vehicleMake: String? = null,
    @SerializedName("vehicle_model") val vehicleModel: String? = null,
    @SerializedName("vehicle_color") val vehicleColor: String? = null,
    @SerializedName("vehicle_vin") val vehicleVin: String? = null,
    @SerializedName("vehicle_plate") val vehiclePlate: String? = null,
    @SerializedName("vehicle_state") val vehicleState: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_city") val pickupCity: String? = null,
    @SerializedName("pickup_state") val pickupState: String? = null,
    @SerializedName("pickup_zip") val pickupZip: String? = null,
    @SerializedName("pickup_latitude") val pickupLatitude: Double? = null,
    @SerializedName("pickup_longitude") val pickupLongitude: Double? = null,
    @SerializedName("dropoff_address") val dropoffAddress: String? = null,
    @SerializedName("dropoff_city") val dropoffCity: String? = null,
    @SerializedName("dropoff_state") val dropoffState: String? = null,
    @SerializedName("dropoff_zip") val dropoffZip: String? = null,
    @SerializedName("dropoff_latitude") val dropoffLatitude: Double? = null,
    @SerializedName("dropoff_longitude") val dropoffLongitude: Double? = null,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("eta_minutes") val etaMinutes: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("internal_notes") val internalNotes: String? = null,
    @SerializedName("po_number") val poNumber: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("account_name") val accountName: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("dispatched_at") val dispatchedAt: String? = null,
    @SerializedName("en_route_at") val enRouteAt: String? = null,
    @SerializedName("on_scene_at") val onSceneAt: String? = null,
    @SerializedName("hooked_at") val hookedAt: String? = null,
    @SerializedName("dropoff_at") val dropoffAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("cancelled_at") val cancelledAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val vehicleDescription: String
        get() {
            val parts = listOfNotNull(vehicleYear, vehicleMake, vehicleModel)
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Unknown Vehicle"
        }

    val fullPickupAddress: String
        get() {
            val parts = listOfNotNull(pickupAddress, pickupCity, pickupState, pickupZip)
            return if (parts.isNotEmpty()) parts.joinToString(", ") else "No address"
        }

    val fullDropoffAddress: String
        get() {
            val parts = listOfNotNull(dropoffAddress, dropoffCity, dropoffState, dropoffZip)
            return if (parts.isNotEmpty()) parts.joinToString(", ") else "No address"
        }

    val statusDisplayName: String
        get() = when (status) {
            "new" -> "New"
            "dispatched" -> "Dispatched"
            "en_route" -> "En Route"
            "on_scene" -> "On Scene"
            "hooked" -> "Hooked"
            "in_transit" -> "In Transit"
            "delivered" -> "Delivered"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> status.replaceFirstChar { it.uppercase() }
        }

    val nextDriverStatus: String?
        get() = when (status) {
            "dispatched" -> "en_route"
            "en_route" -> "on_scene"
            "on_scene" -> "hooked"
            "hooked" -> "in_transit"
            "in_transit" -> "delivered"
            "delivered" -> "completed"
            else -> null
        }

    val nextStatusDisplayName: String?
        get() = when (nextDriverStatus) {
            "en_route" -> "En Route"
            "on_scene" -> "On Scene"
            "hooked" -> "Hooked"
            "in_transit" -> "In Transit"
            "delivered" -> "Delivered"
            "completed" -> "Complete"
            else -> null
        }

    val isActive: Boolean
        get() = status !in listOf("completed", "cancelled")
}

data class CreateCallRequest(
    @SerializedName("type") val type: String,
    @SerializedName("priority") val priority: String = "normal",
    @SerializedName("caller_name") val callerName: String? = null,
    @SerializedName("caller_phone") val callerPhone: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("vehicle_year") val vehicleYear: String? = null,
    @SerializedName("vehicle_make") val vehicleMake: String? = null,
    @SerializedName("vehicle_model") val vehicleModel: String? = null,
    @SerializedName("vehicle_color") val vehicleColor: String? = null,
    @SerializedName("vehicle_plate") val vehiclePlate: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_city") val pickupCity: String? = null,
    @SerializedName("pickup_state") val pickupState: String? = null,
    @SerializedName("pickup_zip") val pickupZip: String? = null,
    @SerializedName("dropoff_address") val dropoffAddress: String? = null,
    @SerializedName("dropoff_city") val dropoffCity: String? = null,
    @SerializedName("dropoff_state") val dropoffState: String? = null,
    @SerializedName("dropoff_zip") val dropoffZip: String? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("truck_id") val truckId: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("account_name") val accountName: String? = null,
    @SerializedName("po_number") val poNumber: String? = null
)

data class UpdateCallRequest(
    @SerializedName("status") val status: String? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("truck_id") val truckId: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("internal_notes") val internalNotes: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null
)
