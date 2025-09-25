package com.evcharging.app.data.remote.api

import com.evcharging.app.domain.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for communicating with the backend
 * Maps to the existing C# Web API endpoints
 */
interface EVChargingApiService {
    
    // Authentication endpoints
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/register") 
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body refreshToken: String): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
    
    // User management endpoints
    @GET("users/profile")
    suspend fun getUserProfile(): Response<User>
    
    @PUT("users/profile")
    suspend fun updateUserProfile(@Body user: User): Response<User>
    
    @PUT("users/{nic}/deactivate")
    suspend fun deactivateUser(@Path("nic") nic: String): Response<Unit>
    
    @PUT("users/{nic}/activate") 
    suspend fun activateUser(@Path("nic") nic: String): Response<Unit>
    
    // Charging station endpoints
    @GET("chargingstations")
    suspend fun getChargingStations(): Response<List<ChargingStation>>
    
    @GET("chargingstations/{id}")
    suspend fun getChargingStationById(@Path("id") id: String): Response<ChargingStation>
    
    @GET("chargingstations/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0
    ): Response<List<ChargingStation>>
    
    // Booking/Reservation endpoints
    @GET("bookings")
    suspend fun getUserBookings(): Response<List<Booking>>
    
    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): Response<Booking>
    
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<Booking>
    
    @PUT("bookings/{id}")
    suspend fun updateBooking(
        @Path("id") id: String,
        @Body request: UpdateBookingRequest
    ): Response<Booking>
    
    @DELETE("bookings/{id}")
    suspend fun cancelBooking(@Path("id") id: String): Response<Unit>
    
    @POST("bookings/{id}/approve")
    suspend fun approveBooking(@Path("id") id: String): Response<Booking>
    
    @POST("bookings/{id}/start")
    suspend fun startCharging(@Path("id") id: String): Response<Booking>
    
    @POST("bookings/{id}/complete")
    suspend fun completeCharging(
        @Path("id") id: String,
        @Body energyConsumed: Double
    ): Response<Booking>
    
    // QR Code endpoints
    @GET("bookings/{id}/qrcode")
    suspend fun getBookingQRCode(@Path("id") id: String): Response<QRCodeData>
    
    @POST("qr/verify")
    suspend fun verifyQRCode(@Body qrData: QRCodeData): Response<Booking>
    
    // Dashboard endpoints
    @GET("dashboard")
    suspend fun getDashboardData(): Response<DashboardData>
    
    // Operator specific endpoints
    @GET("operator/bookings/active")
    suspend fun getActiveBookingsForOperator(): Response<List<Booking>>
    
    @POST("operator/bookings/{id}/confirm")
    suspend fun confirmBookingByOperator(@Path("id") id: String): Response<Booking>
}