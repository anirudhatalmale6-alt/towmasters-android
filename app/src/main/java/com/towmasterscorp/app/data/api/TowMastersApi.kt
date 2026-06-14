package com.towmasterscorp.app.data.api

import com.towmasterscorp.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface TowMastersApi {

    // Auth
    @POST("auth.php?action=login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<User>>

    @GET("auth.php?action=me")
    suspend fun getMe(): Response<ApiResponse<User>>

    // Calls
    @GET("calls.php?action=list")
    suspend fun getCalls(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<ListResponse<Call>>

    @GET("calls.php?action=active")
    suspend fun getActiveCalls(): Response<ListResponse<Call>>

    @GET("calls.php?action=get")
    suspend fun getCall(@Query("id") id: Int): Response<ApiResponse<Call>>

    @PUT("calls.php?action=update")
    suspend fun updateCall(
        @Query("id") id: Int,
        @Body request: UpdateCallRequest
    ): Response<ApiResponse<Call>>

    @POST("calls.php?action=create")
    suspend fun createCall(@Body request: CreateCallRequest): Response<ApiResponse<Call>>

    // Users / Drivers
    @POST("users.php?action=clock-in")
    suspend fun clockIn(): Response<ApiResponse<User>>

    @POST("users.php?action=clock-out")
    suspend fun clockOut(): Response<ApiResponse<User>>

    @POST("users.php?action=update-location")
    suspend fun updateLocation(@Body location: LocationUpdate): Response<ApiResponse<Unit>>

    @GET("users.php?action=list")
    suspend fun getUsers(): Response<ListResponse<User>>

    @GET("users.php?action=driver-locations")
    suspend fun getDriverLocations(): Response<ListResponse<User>>

    // Chat
    @GET("chat.php?action=conversations")
    suspend fun getConversations(): Response<ListResponse<ChatConversation>>

    @GET("chat.php?action=messages")
    suspend fun getMessages(@Query("with") userId: Int): Response<ListResponse<ChatMessage>>

    @POST("chat.php?action=send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<ApiResponse<ChatMessage>>

    // Dashboard
    @GET("reports.php?action=dashboard")
    suspend fun getDashboardStats(): Response<DashboardResponse>

    // Trucks
    @GET("trucks.php?action=list")
    suspend fun getTrucks(): Response<ListResponse<Truck>>

    // Customers
    @GET("customers.php?action=list")
    suspend fun getCustomers(
        @Query("search") search: String? = null
    ): Response<ListResponse<Customer>>

    // Invoices
    @GET("invoices.php?action=list")
    suspend fun getInvoices(): Response<ListResponse<Invoice>>

    // Impound
    @GET("impound.php?action=list")
    suspend fun getImpoundVehicles(): Response<ListResponse<ImpoundVehicle>>

    // Inspections
    @GET("inspections.php?action=list")
    suspend fun getInspections(): Response<ListResponse<Inspection>>

    // Fuel Receipts
    @GET("fuel.php?action=list")
    suspend fun getFuelReceipts(): Response<ListResponse<FuelReceipt>>

    // Device token for push notifications
    @POST("auth.php?action=update-device-token")
    suspend fun updateDeviceToken(@Body request: DeviceTokenRequest): Response<ApiResponse<Unit>>
}
