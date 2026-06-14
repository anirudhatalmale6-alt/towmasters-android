package com.towmasterscorp.app.data.api

import com.towmasterscorp.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface TowMastersApi {

    // Auth
    @POST("auth.php?action=login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth.php?action=me")
    suspend fun getMe(): Response<MeResponse>

    @POST("auth.php?action=update-device-token")
    suspend fun updateDeviceToken(@Body request: DeviceTokenRequest): Response<GenericResponse>

    // Calls
    @GET("calls.php?action=list")
    suspend fun getCalls(
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<CallsResponse>

    @GET("calls.php?action=active")
    suspend fun getActiveCalls(): Response<CallsResponse>

    @GET("calls.php?action=get")
    suspend fun getCall(@Query("id") id: Int): Response<CallResponse>

    @PUT("calls.php?action=update")
    suspend fun updateCall(
        @Query("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<GenericResponse>

    @POST("calls.php?action=create")
    suspend fun createCall(@Body request: Map<String, @JvmSuppressWildcards Any?>): Response<GenericResponse>

    // Users / Drivers
    @POST("users.php?action=clock-in")
    suspend fun clockIn(): Response<GenericResponse>

    @POST("users.php?action=clock-out")
    suspend fun clockOut(): Response<GenericResponse>

    @POST("users.php?action=update-location")
    suspend fun updateLocation(@Body location: LocationUpdate): Response<GenericResponse>

    @GET("users.php?action=list")
    suspend fun getUsers(): Response<UsersResponse>

    @GET("users.php?action=driver-locations")
    suspend fun getDriverLocations(): Response<UsersResponse>

    // Chat
    @GET("chat.php?action=conversations")
    suspend fun getConversations(): Response<ConversationsResponse>

    @GET("chat.php?action=messages")
    suspend fun getMessages(@Query("with") userId: Int): Response<MessagesResponse>

    @POST("chat.php?action=send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<GenericResponse>

    // Dashboard
    @GET("reports.php?action=dashboard")
    suspend fun getDashboardStats(): Response<DashboardResponse>

    // Trucks
    @GET("trucks.php?action=list")
    suspend fun getTrucks(): Response<GenericResponse>

    // Customers
    @GET("customers.php?action=list")
    suspend fun getCustomers(@Query("search") search: String? = null): Response<GenericResponse>

    // Invoices
    @GET("invoices.php?action=list")
    suspend fun getInvoices(): Response<GenericResponse>

    // Impound
    @GET("impound.php?action=list")
    suspend fun getImpoundVehicles(): Response<GenericResponse>

    // Inspections
    @GET("inspections.php?action=list")
    suspend fun getInspections(): Response<GenericResponse>

    // Fuel Receipts
    @GET("fuel.php?action=list")
    suspend fun getFuelReceipts(): Response<GenericResponse>
}
