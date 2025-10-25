package com.example.evmobile.models

import java.util.Date

// Request model for creating a booking
data class CreateBookingRequest(
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String, // ISO 8601 format: "2024-09-20T10:00:00Z"
    val endTime: String,   // ISO 8601 format: "2024-09-20T12:00:00Z"
    val notes: String?
)

// Response model for created booking
data class CreateBookingResponse(
    val id: String,
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String,
    val endTime: String,
    val status: Int, // 0 = Pending, 1 = Confirmed, 2 = In Progress, 3 = Completed, 4 = Cancelled
    val qrCode: String,
    val energyConsumed: Double,
    val totalCost: Double,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

// Connector data class
data class Connector(
    val id: String,
    val type: String, // "AC", "DC Fast", "DC Ultra Fast"
    val power: String, // "7 kW", "22 kW", "50 kW", "150 kW"
    val isAvailable: Boolean,
    val pricePerKwh: Double
)

// Station data class for booking selection
data class StationForBooking(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val connectors: List<Connector>,
    val isOperational: Boolean,
    val distanceKm: Double? = null
)

// NOTE: Dummy data removed - now using real API integration
// Use BookingRepository for actual API calls to get stations and create bookings