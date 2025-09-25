package com.evchargingstation.mobile.data.remote.responses

data class LoginResponse(
    val token: String,
    val user: User
)

data class RegisterResponse(
    val token: String,
    val user: User
)

data class User(
    val nic: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val userType: String,
    val isActive: Boolean,
    val createdAt: Long? = null
)

data class ChargingStation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val openingHours: String,
    val pricePerKWh: Double,
    val isActive: Boolean,
    val connectors: List<Connector>
)

data class Connector(
    val id: String,
    val stationId: String,
    val type: String, // AC or DC
    val power: Double,
    val isAvailable: Boolean,
    val status: String
)

data class Booking(
    val id: String,
    val userNic: String,
    val stationId: String,
    val stationName: String,
    val connectorId: String,
    val connectorType: String,
    val startTime: Long,
    val endTime: Long,
    val totalAmount: Double,
    val status: String, // Pending, Approved, Completed, Cancelled
    val notes: String? = null,
    val qrCode: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class ErrorResponse(
    val success: Boolean,
    val message: String,
    val errors: Map<String, String>? = null
)