package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class Call(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("call_number") val callNumber: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("priority") val priority: String? = "normal",
    @SerializedName("call_type") val callType: String? = "tow",
    @SerializedName("company_id") val companyId: Int? = null,
    @SerializedName("assigned_driver_id") val assignedDriverId: Int? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("assigned_truck_id") val assignedTruckId: Int? = null,
    @SerializedName("truck_number") val truckNumber: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("caller_name") val callerName: String? = null,
    @SerializedName("caller_phone") val callerPhone: String? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("vehicle_year") val vehicleYear: Any? = null,
    @SerializedName("vehicle_make") val vehicleMake: String? = null,
    @SerializedName("vehicle_model") val vehicleModel: String? = null,
    @SerializedName("vehicle_color") val vehicleColor: String? = null,
    @SerializedName("vehicle_vin") val vehicleVin: String? = null,
    @SerializedName("vehicle_license") val vehicleLicense: String? = null,
    @SerializedName("plate_state") val plateState: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_city") val pickupCity: String? = null,
    @SerializedName("pickup_state") val pickupState: String? = null,
    @SerializedName("pickup_zip") val pickupZip: String? = null,
    @SerializedName("pickup_lat") val pickupLat: Any? = null,
    @SerializedName("pickup_lng") val pickupLng: Any? = null,
    @SerializedName("dropoff_address") val dropoffAddress: String? = null,
    @SerializedName("dropoff_city") val dropoffCity: String? = null,
    @SerializedName("dropoff_state") val dropoffState: String? = null,
    @SerializedName("dropoff_zip") val dropoffZip: String? = null,
    @SerializedName("dropoff_lat") val dropoffLat: Any? = null,
    @SerializedName("dropoff_lng") val dropoffLng: Any? = null,
    @SerializedName("base_rate") val baseRate: Any? = null,
    @SerializedName("mileage_rate") val mileageRate: Any? = null,
    @SerializedName("mileage") val mileage: Any? = null,
    @SerializedName("additional_charges") val additionalCharges: Any? = null,
    @SerializedName("tax_rate") val taxRate: Any? = null,
    @SerializedName("tax_amount") val taxAmount: Any? = null,
    @SerializedName("total_amount") val totalAmount: Any? = null,
    @SerializedName("amount_paid") val amountPaid: Any? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("dispatch_notes") val dispatchNotes: String? = null,
    @SerializedName("driver_notes") val driverNotes: String? = null,
    @SerializedName("reason_for_tow") val reasonForTow: String? = null,
    @SerializedName("po_number") val poNumber: String? = null,
    @SerializedName("dispatcher_name") val dispatcherName: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("location_id") val locationId: Int? = null,
    @SerializedName("dispatched_at") val dispatchedAt: String? = null,
    @SerializedName("en_route_at") val enRouteAt: String? = null,
    @SerializedName("on_scene_at") val onSceneAt: String? = null,
    @SerializedName("hooked_at") val hookedAt: String? = null,
    @SerializedName("delivered_at") val deliveredAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val vehicleDescription: String
        get() {
            val year = when (vehicleYear) {
                is Number -> if (vehicleYear.toInt() > 0) vehicleYear.toString() else null
                is String -> if (vehicleYear.isNotEmpty() && vehicleYear != "0") vehicleYear else null
                else -> null
            }
            val parts = listOfNotNull(year, vehicleMake?.ifEmpty { null }, vehicleModel?.ifEmpty { null })
            return if (parts.isNotEmpty()) parts.joinToString(" ") else ""
        }

    val vehiclePlate: String? get() = vehicleLicense?.ifEmpty { null }

    val fullPickupAddress: String
        get() {
            val parts = listOfNotNull(
                pickupAddress?.ifEmpty { null },
                pickupCity?.ifEmpty { null },
                pickupState?.ifEmpty { null },
                pickupZip?.ifEmpty { null }
            )
            return if (parts.isNotEmpty()) parts.joinToString(", ") else "No address"
        }

    val fullDropoffAddress: String
        get() {
            val parts = listOfNotNull(
                dropoffAddress?.ifEmpty { null },
                dropoffCity?.ifEmpty { null },
                dropoffState?.ifEmpty { null },
                dropoffZip?.ifEmpty { null }
            )
            return if (parts.isNotEmpty()) parts.joinToString(", ") else ""
        }

    val statusDisplayName: String
        get() = when (status) {
            "pending" -> "Pending"
            "scheduled" -> "Scheduled"
            "dispatched" -> "Dispatched"
            "en_route" -> "En Route"
            "on_scene" -> "On Scene"
            "hooked" -> "Hooked"
            "in_transit" -> "In Transit"
            "delivered" -> "Delivered"
            "destination_arrival" -> "Destination Arrival"
            "completed" -> "Completed"
            "canceled" -> "Canceled"
            else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
        }

    val callTypeDisplay: String
        get() = (callType ?: "tow").replace("_", " ").replaceFirstChar { it.uppercase() }

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
        get() = status !in listOf("completed", "canceled")
}

