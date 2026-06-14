package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class Customer(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("zip") val zip: String? = null,
    @SerializedName("type") val type: String? = null, // individual, account, motor_club, insurance
    @SerializedName("account_number") val accountNumber: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("total_calls") val totalCalls: Int = 0,
    @SerializedName("total_revenue") val totalRevenue: Double = 0.0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class Truck(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("number") val number: String? = null,
    @SerializedName("type") val type: String? = null, // flatbed, wheel_lift, wrecker, heavy_duty
    @SerializedName("make") val make: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("vin") val vin: String? = null,
    @SerializedName("plate") val plate: String? = null,
    @SerializedName("status") val status: String? = null, // available, in_use, maintenance, out_of_service
    @SerializedName("assigned_driver_id") val assignedDriverId: Int? = null,
    @SerializedName("assigned_driver_name") val assignedDriverName: String? = null,
    @SerializedName("mileage") val mileage: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class Invoice(
    @SerializedName("id") val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("call_id") val callId: Int? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("tax") val tax: Double = 0.0,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("status") val status: String? = null, // draft, sent, paid, overdue, cancelled
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("paid_date") val paidDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ImpoundVehicle(
    @SerializedName("id") val id: Int,
    @SerializedName("call_id") val callId: Int? = null,
    @SerializedName("vehicle_year") val vehicleYear: String? = null,
    @SerializedName("vehicle_make") val vehicleMake: String? = null,
    @SerializedName("vehicle_model") val vehicleModel: String? = null,
    @SerializedName("vehicle_color") val vehicleColor: String? = null,
    @SerializedName("vehicle_vin") val vehicleVin: String? = null,
    @SerializedName("vehicle_plate") val vehiclePlate: String? = null,
    @SerializedName("status") val status: String? = null, // stored, released, auction, scrapped
    @SerializedName("lot_location") val lotLocation: String? = null,
    @SerializedName("intake_date") val intakeDate: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("daily_rate") val dailyRate: Double = 0.0,
    @SerializedName("total_charges") val totalCharges: Double = 0.0,
    @SerializedName("owner_name") val ownerName: String? = null,
    @SerializedName("owner_phone") val ownerPhone: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val vehicleDescription: String
        get() {
            val parts = listOfNotNull(vehicleYear, vehicleMake, vehicleModel)
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Unknown Vehicle"
        }
}

data class Inspection(
    @SerializedName("id") val id: Int,
    @SerializedName("truck_id") val truckId: Int? = null,
    @SerializedName("truck_name") val truckName: String? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("type") val type: String? = null, // pre_trip, post_trip, monthly
    @SerializedName("status") val status: String? = null, // pass, fail, needs_attention
    @SerializedName("mileage") val mileage: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<InspectionItem>? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class InspectionItem(
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String, // pass, fail, na
    @SerializedName("notes") val notes: String? = null
)

data class FuelReceipt(
    @SerializedName("id") val id: Int,
    @SerializedName("truck_id") val truckId: Int? = null,
    @SerializedName("truck_name") val truckName: String? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("gallons") val gallons: Double = 0.0,
    @SerializedName("price_per_gallon") val pricePerGallon: Double = 0.0,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("odometer") val odometer: Int? = null,
    @SerializedName("station_name") val stationName: String? = null,
    @SerializedName("station_address") val stationAddress: String? = null,
    @SerializedName("receipt_image_url") val receiptImageUrl: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
