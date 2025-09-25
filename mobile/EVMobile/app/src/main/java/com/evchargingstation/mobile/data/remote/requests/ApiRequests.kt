package com.evchargingstation.mobile.data.remote.requests

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val nic: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val userType: String
)

data class UpdateUserRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val userType: String,
    val isActive: Boolean
)

data class CreateBookingRequest(
    val userNic: String,
    val stationId: String,
    val connectorId: String,
    val startTime: Long,
    val endTime: Long,
    val notes: String? = null
)

data class UpdateBookingRequest(
    val startTime: Long,
    val endTime: Long,
    val notes: String? = null,
    val status: String? = null
)