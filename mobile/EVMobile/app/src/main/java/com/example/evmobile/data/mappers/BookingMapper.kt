package com.example.evmobile.data.mappers

import com.example.evmobile.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mappers to convert between API models and UI models
 */
object BookingMapper {
    
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
    
    /**
     * Convert API booking to UI booking model
     */
    fun mapApiBookingToBooking(apiBooking: ApiBooking, stationName: String = "Unknown Station", stationLocation: String = "Unknown Location"): Booking {
        // Parse dates
        val startDate = try {
            apiDateFormat.parse(apiBooking.startTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val endDate = try {
            apiDateFormat.parse(apiBooking.endTime) ?: Date()
        } catch (e: Exception) {
            Date(startDate.time + (2 * 60 * 60 * 1000)) // Default 2 hours
        }
        
        // Calculate duration in minutes
        val duration = ((endDate.time - startDate.time) / (1000 * 60)).toInt()
        
        // Map status
        val bookingStatus = when (ApiBookingStatus.fromValue(apiBooking.status)) {
            ApiBookingStatus.PENDING -> BookingStatus.PENDING
            ApiBookingStatus.CONFIRMED -> {
                // If booking has QR code, consider it approved
                if (!apiBooking.qrCode.isNullOrBlank()) BookingStatus.APPROVED else BookingStatus.CONFIRMED
            }
            ApiBookingStatus.IN_PROGRESS -> BookingStatus.IN_PROGRESS
            ApiBookingStatus.COMPLETED -> BookingStatus.COMPLETED
            ApiBookingStatus.CANCELLED -> BookingStatus.CANCELLED
            ApiBookingStatus.NO_SHOW -> BookingStatus.CANCELLED
        }
        
        // Determine charging type based on connector (this would need to be enhanced with actual connector data)
        val chargingType = "DC Fast" // Default, should be determined from connector type
        
        return Booking(
            id = apiBooking.id,
            stationId = apiBooking.stationId,
            stationName = stationName,
            stationLocation = stationLocation,
            bookingDateTime = startDate,
            duration = duration,
            status = bookingStatus,
            chargingType = chargingType,
            estimatedCost = apiBooking.totalCost,
            qrCode = apiBooking.qrCode
        )
    }
    
    /**
     * Convert API charging station to UI station model for booking
     */
    fun mapApiStationToStationForBooking(apiStation: ApiChargingStation, distanceKm: Double? = null): StationForBooking {
        // Convert API connectors to UI connectors
        val connectors = apiStation.connectors.map { apiConnector ->
            Connector(
                id = apiConnector.id,
                type = getConnectorTypeDisplayName(apiConnector.type),
                power = "${apiConnector.power.toInt()} kW",
                isAvailable = apiConnector.isAvailable && apiConnector.status == 0, // Available status
                pricePerKwh = apiStation.pricePerKWh
            )
        }
        
        return StationForBooking(
            id = apiStation.id,
            name = apiStation.name,
            location = apiStation.address,
            latitude = apiStation.latitude,
            longitude = apiStation.longitude,
            connectors = connectors,
            isOperational = apiStation.status == 0, // Active status
            distanceKm = distanceKm
        )
    }
    
    /**
     * Convert CreateBookingRequest to API request
     */
    fun mapCreateBookingRequestToApi(
        userId: String,
        stationId: String,
        connectorId: String,
        startTime: Date,
        endTime: Date,
        notes: String?
    ): CreateBookingApiRequest {
        return CreateBookingApiRequest(
            userId = userId,
            stationId = stationId,
            connectorId = connectorId,
            startTime = apiDateFormat.format(startTime),
            endTime = apiDateFormat.format(endTime),
            notes = notes
        )
    }
    
    /**
     * Convert UpdateBookingRequest to API request
     */
    fun mapUpdateBookingRequestToApi(
        stationId: String,
        connectorId: String,
        startTime: Date,
        endTime: Date,
        notes: String?
    ): UpdateBookingApiRequest {
        return UpdateBookingApiRequest(
            stationId = stationId,
            connectorId = connectorId,
            startTime = apiDateFormat.format(startTime),
            endTime = apiDateFormat.format(endTime),
            notes = notes
        )
    }
    
    /**
     * Convert API booking to charging history
     */
    fun mapApiBookingToChargingHistory(apiBooking: ApiBooking, stationName: String, stationLocation: String): ChargingHistory? {
        // Convert completed, cancelled, and no-show bookings to charging history
        if (apiBooking.status != ApiBookingStatus.COMPLETED.value && 
            apiBooking.status != ApiBookingStatus.CANCELLED.value && 
            apiBooking.status != ApiBookingStatus.NO_SHOW.value) {
            return null
        }
        
        val startDate = try {
            apiDateFormat.parse(apiBooking.startTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val endDate = try {
            apiDateFormat.parse(apiBooking.endTime) ?: Date()
        } catch (e: Exception) {
            Date(startDate.time + (2 * 60 * 60 * 1000))
        }
        
        // Determine charging status based on booking status
        val chargingStatus = when (apiBooking.status) {
            ApiBookingStatus.COMPLETED.value -> {
                if (apiBooking.energyConsumed > 0) {
                    ChargingStatus.COMPLETED
                } else {
                    ChargingStatus.INTERRUPTED
                }
            }
            ApiBookingStatus.CANCELLED.value -> ChargingStatus.INTERRUPTED
            ApiBookingStatus.NO_SHOW.value -> ChargingStatus.FAILED
            else -> ChargingStatus.FAILED
        }
        
        return ChargingHistory(
            id = apiBooking.id,
            stationId = apiBooking.stationId,
            stationName = stationName,
            stationLocation = stationLocation,
            chargingDate = startDate,
            startTime = startDate,
            endTime = endDate,
            energyCharged = apiBooking.energyConsumed,
            totalCost = apiBooking.totalCost,
            chargingType = "DC Fast", // Would need connector info to determine this properly
            status = chargingStatus
        )
    }
    
    // Helper functions
    private fun getConnectorTypeDisplayName(type: Int): String {
        return when (ApiConnectorType.fromValue(type)) {
            ApiConnectorType.TYPE1 -> "AC"
            ApiConnectorType.TYPE2 -> "AC"
            ApiConnectorType.CHADEMO -> "DC Fast"
            ApiConnectorType.CCS -> "DC Fast"
            ApiConnectorType.TESLA_SUPER -> "DC Ultra Fast"
        }
    }
    
    /**
     * Check if booking can be modified (at least 12 hours before start time)
     */
    fun canModifyBooking(apiBooking: ApiBooking): Boolean {
        val startTime = try {
            apiDateFormat.parse(apiBooking.startTime)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
        
        val currentTime = System.currentTimeMillis()
        val twelveHoursInMs = 12 * 60 * 60 * 1000L
        
        return (startTime - currentTime) >= twelveHoursInMs && 
               (apiBooking.status == ApiBookingStatus.PENDING.value || 
                apiBooking.status == ApiBookingStatus.CONFIRMED.value)
    }
    
    /**
     * Check if booking can be cancelled (at least 12 hours before start time)
     */
    fun canCancelBooking(apiBooking: ApiBooking): Boolean {
        return canModifyBooking(apiBooking)
    }
    
    /**
     * Validate booking creation (within 7 days from now)
     */
    fun validateBookingCreation(startTime: Date): Boolean {
        val currentTime = System.currentTimeMillis()
        val sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L
        val timeDiff = startTime.time - currentTime
        
        return timeDiff > 0 && timeDiff <= sevenDaysInMs
    }
}