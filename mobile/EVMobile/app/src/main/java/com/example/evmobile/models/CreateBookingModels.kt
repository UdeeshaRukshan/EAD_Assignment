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

// Dummy data generator for create booking functionality
object CreateBookingDummyData {
    
    fun generateStationsForBooking(): List<StationForBooking> {
        return listOf(
            StationForBooking(
                id = "S001",
                name = "Colombo City Center Station",
                location = "123 Main Street, Colombo",
                latitude = 6.9271,
                longitude = 79.8612,
                isOperational = true,
                distanceKm = 2.5,
                connectors = listOf(
                    Connector("C001", "AC", "7 kW", true, 25.50),
                    Connector("C002", "DC Fast", "50 kW", true, 45.00),
                    Connector("C003", "AC", "22 kW", false, 35.00)
                )
            ),
            StationForBooking(
                id = "S002",
                name = "Kandy Shopping Mall",
                location = "456 Kandy Road, Kandy",
                latitude = 7.2906,
                longitude = 80.6337,
                isOperational = true,
                distanceKm = 5.8,
                connectors = listOf(
                    Connector("C004", "AC", "7 kW", true, 22.00),
                    Connector("C005", "AC", "22 kW", true, 32.00)
                )
            ),
            StationForBooking(
                id = "S003",
                name = "Galle Face Green Station",
                location = "Galle Face Green, Colombo",
                latitude = 6.9319,
                longitude = 79.8478,
                isOperational = true,
                distanceKm = 1.2,
                connectors = listOf(
                    Connector("C006", "DC Fast", "50 kW", true, 48.00),
                    Connector("C007", "DC Ultra Fast", "150 kW", true, 65.00),
                    Connector("C008", "AC", "7 kW", true, 24.00)
                )
            ),
            StationForBooking(
                id = "S004",
                name = "Negombo Beach Resort",
                location = "Beach Road, Negombo",
                latitude = 7.2084,
                longitude = 79.8380,
                isOperational = true,
                distanceKm = 12.4,
                connectors = listOf(
                    Connector("C009", "AC", "7 kW", true, 20.00),
                    Connector("C010", "DC Fast", "50 kW", false, 42.00)
                )
            ),
            StationForBooking(
                id = "S005",
                name = "Airport Express Station",
                location = "Bandaranaike International Airport",
                latitude = 7.1808,
                longitude = 79.8841,
                isOperational = true,
                distanceKm = 35.6,
                connectors = listOf(
                    Connector("C011", "DC Fast", "50 kW", true, 50.00),
                    Connector("C012", "DC Ultra Fast", "150 kW", true, 70.00),
                    Connector("C013", "AC", "22 kW", true, 38.00)
                )
            )
        )
    }
    
    fun simulateCreateBooking(request: CreateBookingRequest): CreateBookingResponse {
        // Simulate API response with dummy data
        val bookingId = "B${System.currentTimeMillis()}"
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())
        
        return CreateBookingResponse(
            id = bookingId,
            userId = request.userId,
            stationId = request.stationId,
            connectorId = request.connectorId,
            startTime = request.startTime,
            endTime = request.endTime,
            status = 0, // Pending
            qrCode = "Qk9PS0lORzo${bookingId}=", // Base64 encoded booking reference
            energyConsumed = 0.0,
            totalCost = 0.0,
            notes = request.notes,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
}