package com.evcharging.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain models for the application
 * These represent the business entities used throughout the app
 */

@Parcelize
data class User(
    val nic: String, // Primary key
    val id: String = "",
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val profileImageUrl: String? = null
) : Parcelable

@Parcelize
data class ChargingStation(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val location: Location,
    val operatorId: String,
    val status: StationStatus,
    val amenities: List<String>,
    val openingHours: String,
    val pricePerKWh: Double,
    val imageUrls: List<String>,
    val connectors: List<Connector>,
    val distance: Double? = null,
    val isFavorite: Boolean = false
) : Parcelable

@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double
) : Parcelable

@Parcelize
data class Connector(
    val id: String,
    val type: ConnectorType,
    val power: Double,
    val isAvailable: Boolean,
    val status: ConnectorStatus
) : Parcelable

@Parcelize
data class Booking(
    val id: String,
    val userNic: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String,
    val endTime: String,
    val status: BookingStatus,
    val totalAmount: Double,
    val energyConsumed: Double = 0.0,
    val qrCodeData: String? = null,
    val notes: String? = null,
    val station: ChargingStation? = null,
    val connector: Connector? = null
) : Parcelable

@Parcelize
data class CreateBookingRequest(
    val stationId: String,
    val connectorId: String,
    val startTime: String,
    val endTime: String,
    val notes: String? = null
) : Parcelable

@Parcelize
data class UpdateBookingRequest(
    val startTime: String? = null,
    val endTime: String? = null,
    val notes: String? = null
) : Parcelable

@Parcelize
data class LoginRequest(
    val email: String,
    val password: String
) : Parcelable

@Parcelize
data class RegisterRequest(
    val nic: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val role: UserRole = UserRole.EVOwner
) : Parcelable

@Parcelize
data class AuthResponse(
    val success: Boolean,
    val token: String,
    val user: User,
    val refreshToken: String? = null
) : Parcelable

@Parcelize
data class QRCodeData(
    val bookingId: String,
    val userId: String,
    val stationId: String,
    val connectorId: String,
    val timestamp: String,
    val verificationCode: String
) : Parcelable

@Parcelize
data class DashboardData(
    val pendingReservations: Int,
    val approvedReservations: Int,
    val nearbyStations: List<ChargingStation>,
    val recentBookings: List<Booking>,
    val user: User
) : Parcelable

// Enums
enum class UserRole(val value: Int) {
    Admin(0),
    EVOwner(1),
    Operator(2),
    BackofficeUser(3)
}

enum class ConnectorType(val value: Int) {
    Type1(0),
    Type2(1),
    CHAdeMO(2),
    CCS(3),
    TeslaSuper(4);
    
    fun getDisplayName(): String {
        return when (this) {
            Type1 -> "Type 1"
            Type2 -> "Type 2" 
            CHAdeMO -> "CHAdeMO"
            CCS -> "CCS"
            TeslaSuper -> "Tesla Supercharger"
        }
    }
}

enum class ConnectorStatus(val value: Int) {
    Available(0),
    Occupied(1),
    OutOfOrder(2),
    Reserved(3);
    
    fun getDisplayName(): String {
        return when (this) {
            Available -> "Available"
            Occupied -> "Occupied"
            OutOfOrder -> "Out of Order"
            Reserved -> "Reserved"
        }
    }
}

enum class StationStatus(val value: Int) {
    Active(0),
    Inactive(1),
    Maintenance(2);
    
    fun getDisplayName(): String {
        return when (this) {
            Active -> "Active"
            Inactive -> "Inactive"
            Maintenance -> "Under Maintenance"
        }
    }
}

enum class BookingStatus(val value: Int) {
    Pending(0),
    Approved(1),
    Active(2),
    Completed(3),
    Cancelled(4);
    
    fun getDisplayName(): String {
        return when (this) {
            Pending -> "Pending Approval"
            Approved -> "Approved"
            Active -> "Active"
            Completed -> "Completed"
            Cancelled -> "Cancelled"
        }
    }
}

// Result wrapper for handling API responses
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Throwable, val message: String? = null) : Result<T>()
    class Loading<T> : Result<T>()
}

// Network state for UI
data class NetworkState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)