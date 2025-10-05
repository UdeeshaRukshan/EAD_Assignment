package com.example.evmobile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChargingStationModel(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,
    val connectorTypes: List<String>,
    val pricePerKwh: Double,
    val operatingHours: String,
    val amenities: List<String>,
    val rating: Double,
    val totalSlots: Int,
    val availableSlots: Int
) : Parcelable {
    
    fun getStatusText(): String {
        return when {
            !isActive -> "Inactive"
            availableSlots > 0 -> "Available"
            else -> "Fully Occupied"
        }
    }
    
    fun getStatusColor(): Int {
        return when {
            !isActive -> R.color.status_error
            availableSlots > 0 -> R.color.status_success
            else -> R.color.status_warning
        }
    }
    
    fun getConnectorTypesText(): String {
        return connectorTypes.joinToString(", ")
    }
    
    fun getDistance(userLatitude: Double, userLongitude: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLatitude, userLongitude,
            latitude, longitude,
            results
        )
        return (results[0] / 1000.0) // Convert to kilometers
    }
    
    fun getDistanceText(userLatitude: Double?, userLongitude: Double?): String {
        return if (userLatitude != null && userLongitude != null) {
            val distance = getDistance(userLatitude, userLongitude)
            String.format("%.1f km away", distance)
        } else {
            ""
        }
    }
}

@Parcelize
data class ConnectorModel(
    val id: String,
    val type: String,
    val powerOutput: String,
    val isAvailable: Boolean,
    val pricePerKwh: Double
) : Parcelable