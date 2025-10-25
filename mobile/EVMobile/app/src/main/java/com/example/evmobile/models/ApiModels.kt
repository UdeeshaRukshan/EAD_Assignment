package com.example.evmobile.models

/**
 * API Models for booking operations - These match the API response structure
 */

// API Request Models
data class CreateBookingApiRequest(
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String, // ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val endTime: String,   // ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val notes: String? = null
)

data class UpdateBookingApiRequest(
    val stationId: String,
    val connectorId: String,
    val startTime: String, // ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val endTime: String,   // ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val notes: String? = null
)

// API Response Models
data class CreateBookingApiResponse(
    val id: String,
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String,
    val endTime: String,
    val status: Int, // 0=Pending, 1=Confirmed, 2=InProgress, 3=Completed, 4=Cancelled, 5=NoShow
    val qrCode: String?,
    val energyConsumed: Double,
    val totalCost: Double,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ApiBooking(
    val id: String,
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String,
    val endTime: String,
    val status: Int, // 0=Pending, 1=Confirmed, 2=InProgress, 3=Completed, 4=Cancelled, 5=NoShow
    val qrCode: String?,
    val energyConsumed: Double,
    val totalCost: Double,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ApiChargingStation(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val operatorId: String,
    val status: Int, // 0=Active, 1=Inactive, 2=Maintenance, 3=OutOfOrder
    val connectors: List<ApiConnector>,
    val pricePerKWh: Double
)

data class ApiConnector(
    val id: String,
    val type: Int, // 0=Type1, 1=Type2, 2=CHAdeMO, 3=CCS, 4=TeslaSuper
    val power: Double, // kW
    val isAvailable: Boolean,
    val status: Int // 0=Available, 1=Occupied, 2=Reserved, 3=OutOfOrder, 4=Maintenance
)

// Enums to match API values
enum class ApiBookingStatus(val value: Int, val displayName: String) {
    PENDING(0, "Pending"),
    CONFIRMED(1, "Confirmed"),
    IN_PROGRESS(2, "In Progress"),
    COMPLETED(3, "Completed"),
    CANCELLED(4, "Cancelled"),
    NO_SHOW(5, "No Show");
    
    companion object {
        fun fromValue(value: Int): ApiBookingStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}

enum class ApiStationStatus(val value: Int, val displayName: String) {
    ACTIVE(0, "Active"),
    INACTIVE(1, "Inactive"),
    MAINTENANCE(2, "Maintenance"),
    OUT_OF_ORDER(3, "Out of Order");
    
    companion object {
        fun fromValue(value: Int): ApiStationStatus {
            return values().find { it.value == value } ?: ACTIVE
        }
    }
}

enum class ApiConnectorType(val value: Int, val displayName: String) {
    TYPE1(0, "Type 1"),
    TYPE2(1, "Type 2"),
    CHADEMO(2, "CHAdeMO"),
    CCS(3, "CCS"),
    TESLA_SUPER(4, "Tesla Supercharger");
    
    companion object {
        fun fromValue(value: Int): ApiConnectorType {
            return values().find { it.value == value } ?: TYPE2
        }
    }
}

enum class ApiConnectorStatus(val value: Int, val displayName: String) {
    AVAILABLE(0, "Available"),
    OCCUPIED(1, "Occupied"),
    RESERVED(2, "Reserved"),
    OUT_OF_ORDER(3, "Out of Order"),
    MAINTENANCE(4, "Maintenance");
    
    companion object {
        fun fromValue(value: Int): ApiConnectorStatus {
            return values().find { it.value == value } ?: AVAILABLE
        }
    }
}