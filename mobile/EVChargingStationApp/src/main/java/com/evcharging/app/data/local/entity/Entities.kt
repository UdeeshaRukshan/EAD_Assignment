package com.evcharging.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Local user entity for offline storage
 * Uses NIC as primary key as per requirement
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val nic: String, // NIC as Primary Key
    val id: String = "",
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val role: String, // EVOwner, Operator, Admin
    val isActive: Boolean = true,
    val isLoggedIn: Boolean = false,
    val profileImageUrl: String? = null,
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val updatedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val lastSyncAt: String? = null
)

/**
 * Charging station entity for offline caching
 */
@Entity(tableName = "charging_stations")
data class ChargingStationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val operatorId: String,
    val status: Int, // 0=Active, 1=Inactive, 2=Maintenance
    val amenities: String, // JSON string of amenities array
    val openingHours: String,
    val pricePerKWh: Double,
    val imageUrls: String, // JSON string of image URLs array
    val totalConnectors: Int,
    val availableConnectors: Int,
    val distance: Double? = null, // Distance from user location
    val isFavorite: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

/**
 * Connector entity for charging station connectors
 */
@Entity(
    tableName = "connectors",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ChargingStationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class ConnectorEntity(
    @PrimaryKey
    val id: String,
    val stationId: String,
    val type: Int, // 0=Type1, 1=Type2, 2=CHAdeMO, 3=CCS, 4=TeslaSuper
    val power: Double,
    val isAvailable: Boolean,
    val status: Int, // 0=Available, 1=Occupied, 2=OutOfOrder, 3=Reserved
    val currentBookingId: String? = null
)

/**
 * Booking/Reservation entity for local storage
 */
@Entity(
    tableName = "bookings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["nic"],
            childColumns = ["userNic"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = ChargingStationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class BookingEntity(
    @PrimaryKey
    val id: String,
    val userNic: String,
    val stationId: String,
    val connectorId: String,
    val startTime: String, // ISO formatted date-time string
    val endTime: String, // ISO formatted date-time string
    val status: Int, // 0=Pending, 1=Approved, 2=Active, 3=Completed, 4=Cancelled
    val totalAmount: Double,
    val energyConsumed: Double = 0.0,
    val qrCodeData: String? = null,
    val notes: String? = null,
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val updatedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val syncStatus: Int = 0, // 0=NotSynced, 1=Synced, 2=SyncFailed
    val lastSyncAt: String? = null
)

/**
 * Session entity to track user sessions and offline data
 */
@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey
    val id: String = "current_session",
    val userNic: String?,
    val authToken: String?,
    val refreshToken: String?,
    val isOfflineMode: Boolean = false,
    val lastLocationLat: Double? = null,
    val lastLocationLng: Double? = null,
    val appVersion: String,
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val lastActiveAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)