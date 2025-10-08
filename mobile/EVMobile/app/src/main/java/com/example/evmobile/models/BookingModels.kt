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
    val estimatedCost: Double
)

enum class BookingStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
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

// Dummy data generators
object DummyDataGenerator {
    
    fun generateUpcomingBookings(): List<Booking> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            Booking(
                id = "B001",
                stationId = "S001",
                stationName = "Colombo City Center Station",
                stationLocation = "123 Main Street, Colombo",
                bookingDateTime = Date(currentTime + 5 * 24 * 60 * 60 * 1000), // 5 days from now
                duration = 120, // 2 hours
                status = BookingStatus.CONFIRMED,
                chargingType = "DC Fast",
                estimatedCost = 25.50
            ),
            Booking(
                id = "B002",
                stationId = "S002",
                stationName = "Kandy Shopping Mall",
                stationLocation = "456 Kandy Road, Kandy",
                bookingDateTime = Date(currentTime + 7 * 24 * 60 * 60 * 1000), // 7 days from now
                duration = 90, // 1.5 hours
                status = BookingStatus.PENDING,
                chargingType = "AC",
                estimatedCost = 18.75
            ),
            Booking(
                id = "B003",
                stationId = "S003",
                stationName = "Galle Face Green Station",
                stationLocation = "Galle Face Green, Colombo",
                bookingDateTime = Date(currentTime + 10 * 24 * 60 * 60 * 1000), // 10 days from now
                duration = 180, // 3 hours
                status = BookingStatus.CONFIRMED,
                chargingType = "DC Fast",
                estimatedCost = 42.00
            ),
            Booking(
                id = "B004",
                stationId = "S004",
                stationName = "Negombo Beach Resort",
                stationLocation = "Beach Road, Negombo",
                bookingDateTime = Date(currentTime + 2 * 24 * 60 * 60 * 1000), // 2 days from now
                duration = 60, // 1 hour
                status = BookingStatus.PENDING,
                chargingType = "AC",
                estimatedCost = 12.25
            )
        )
    }
    
    fun generateChargingHistory(): List<ChargingHistory> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            ChargingHistory(
                id = "H001",
                stationId = "S001",
                stationName = "Downtown EV Hub",
                stationLocation = "789 Central Avenue, Colombo",
                chargingDate = Date(currentTime - 2 * 24 * 60 * 60 * 1000), // 2 days ago
                startTime = Date(currentTime - 2 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 2 * 24 * 60 * 60 * 1000 + 2 * 60 * 60 * 1000), // 2 hours later
                energyCharged = 45.2,
                totalCost = 12.50,
                chargingType = "DC Fast",
                status = ChargingStatus.COMPLETED
            ),
            ChargingHistory(
                id = "H002",
                stationId = "S002",
                stationName = "University Campus Station",
                stationLocation = "University of Colombo",
                chargingDate = Date(currentTime - 5 * 24 * 60 * 60 * 1000), // 5 days ago
                startTime = Date(currentTime - 5 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 5 * 24 * 60 * 60 * 1000 + 90 * 60 * 1000), // 1.5 hours later
                energyCharged = 28.8,
                totalCost = 8.75,
                chargingType = "AC",
                status = ChargingStatus.COMPLETED
            ),
            ChargingHistory(
                id = "H003",
                stationId = "S003",
                stationName = "Airport Express Station",
                stationLocation = "Bandaranaike International Airport",
                chargingDate = Date(currentTime - 10 * 24 * 60 * 60 * 1000), // 10 days ago
                startTime = Date(currentTime - 10 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 10 * 24 * 60 * 60 * 1000 + 3 * 60 * 60 * 1000), // 3 hours later
                energyCharged = 65.5,
                totalCost = 18.25,
                chargingType = "DC Fast",
                status = ChargingStatus.COMPLETED
            ),
            ChargingHistory(
                id = "H004",
                stationId = "S004",
                stationName = "Coastal Highway Station",
                stationLocation = "Galle Road, Mount Lavinia",
                chargingDate = Date(currentTime - 15 * 24 * 60 * 60 * 1000), // 15 days ago
                startTime = Date(currentTime - 15 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 15 * 24 * 60 * 60 * 1000 + 45 * 60 * 1000), // 45 minutes later
                energyCharged = 15.3,
                totalCost = 4.50,
                chargingType = "AC",
                status = ChargingStatus.INTERRUPTED
            ),
            ChargingHistory(
                id = "H005",
                stationId = "S005",
                stationName = "Tech Park Station",
                stationLocation = "Colombo Technology Park",
                chargingDate = Date(currentTime - 20 * 24 * 60 * 60 * 1000), // 20 days ago
                startTime = Date(currentTime - 20 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 20 * 24 * 60 * 60 * 1000 + (2.5 * 60 * 60 * 1000).toLong()), // 2.5 hours later
                energyCharged = 52.1,
                totalCost = 15.75,
                chargingType = "DC Fast",
                status = ChargingStatus.COMPLETED
            ),
            ChargingHistory(
                id = "H006",
                stationId = "S006",
                stationName = "Hotel Blue Waters",
                stationLocation = "Wadduwa Beach",
                chargingDate = Date(currentTime - 25 * 24 * 60 * 60 * 1000), // 25 days ago
                startTime = Date(currentTime - 25 * 24 * 60 * 60 * 1000),
                endTime = Date(currentTime - 25 * 24 * 60 * 60 * 1000 + 4 * 60 * 60 * 1000), // 4 hours later
                energyCharged = 78.9,
                totalCost = 22.50,
                chargingType = "AC",
                status = ChargingStatus.COMPLETED
            )
        )
    }
}