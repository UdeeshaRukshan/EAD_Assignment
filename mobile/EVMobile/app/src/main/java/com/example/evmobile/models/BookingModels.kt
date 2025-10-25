package com.example.evmobile.models

import java.util.Date

// Data class for upcoming bookings
data class Booking(
    val id: String,
    val stationId: String,
    val stationName: String,
    val stationLocation: String,
    val bookingDateTime: Date,
    val duration: Int, // in minutes
    val status: BookingStatus,
    val chargingType: String, // AC/DC
    val estimatedCost: Double,
    val qrCode: String? = null // QR code for approved bookings
)

enum class BookingStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    APPROVED("Approved"), // New status for bookings with QR codes
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

// Data class for charging history
data class ChargingHistory(
    val id: String,
    val stationId: String,
    val stationName: String,
    val stationLocation: String,
    val chargingDate: Date,
    val startTime: Date,
    val endTime: Date,
    val energyCharged: Double, // in kWh
    val totalCost: Double,
    val chargingType: String, // AC/DC
    val status: ChargingStatus
)

enum class ChargingStatus(val displayName: String) {
    COMPLETED("Completed"),
    INTERRUPTED("Interrupted"),
    FAILED("Failed")
}

// NOTE: Dummy data removed - now using real API integration
// Use BookingRepository to get actual booking and history data from API