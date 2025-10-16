package com.example.evmobile.utils

import com.example.evmobile.models.ApiBooking
import com.example.evmobile.models.ApiBookingStatus
import com.example.evmobile.models.Booking
import com.example.evmobile.models.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for booking validation and business rules
 */
object BookingValidationUtils {
    
    private const val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
    private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Validates if a new booking can be created
     * - Booking must be within 7 days from now
     * - Start time must be in the future
     * - End time must be after start time
     */
    fun validateBookingCreation(startTime: Date, endTime: Date): BookingValidationResult {
        val currentTime = System.currentTimeMillis()
        val startTimeMs = startTime.time
        val endTimeMs = endTime.time
        
        // Check if start time is in the future
        if (startTimeMs <= currentTime) {
            return BookingValidationResult(false, "Booking start time must be in the future")
        }
        
        // Check if booking is within 7 days
        if ((startTimeMs - currentTime) > SEVEN_DAYS_MS) {
            return BookingValidationResult(false, "Booking must be within 7 days from now")
        }
        
        // Check if end time is after start time
        if (endTimeMs <= startTimeMs) {
            return BookingValidationResult(false, "End time must be after start time")
        }
        
        // Check minimum booking duration (15 minutes)
        val minDurationMs = 15 * 60 * 1000L
        if ((endTimeMs - startTimeMs) < minDurationMs) {
            return BookingValidationResult(false, "Minimum booking duration is 15 minutes")
        }
        
        // Check maximum booking duration (8 hours)
        val maxDurationMs = 8 * 60 * 60 * 1000L
        if ((endTimeMs - startTimeMs) > maxDurationMs) {
            return BookingValidationResult(false, "Maximum booking duration is 8 hours")
        }
        
        return BookingValidationResult(true, "Booking creation is valid")
    }
    
    /**
     * Validates if a booking can be modified
     * - At least 12 hours before start time
     * - Booking must be in Pending or Confirmed status
     */
    fun validateBookingModification(booking: Booking): BookingValidationResult {
        return validateBookingModification(booking.bookingDateTime, booking.status)
    }
    
    /**
     * Validates if an API booking can be modified
     */
    fun validateBookingModification(apiBooking: ApiBooking): BookingValidationResult {
        val startTime = try {
            apiDateFormat.parse(apiBooking.startTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val status = when (ApiBookingStatus.fromValue(apiBooking.status)) {
            ApiBookingStatus.PENDING -> BookingStatus.PENDING
            ApiBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
            ApiBookingStatus.IN_PROGRESS -> BookingStatus.IN_PROGRESS
            ApiBookingStatus.COMPLETED -> BookingStatus.COMPLETED
            ApiBookingStatus.CANCELLED -> BookingStatus.CANCELLED
            ApiBookingStatus.NO_SHOW -> BookingStatus.CANCELLED
        }
        
        return validateBookingModification(startTime, status)
    }
    
    private fun validateBookingModification(startTime: Date, status: BookingStatus): BookingValidationResult {
        // Check if booking is in modifiable status
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            return BookingValidationResult(false, "Only pending or confirmed bookings can be modified")
        }
        
        // Check if modification is at least 12 hours before start time
        val currentTime = System.currentTimeMillis()
        val timeUntilStart = startTime.time - currentTime
        
        if (timeUntilStart < TWELVE_HOURS_MS) {
            return BookingValidationResult(false, "Bookings can only be modified at least 12 hours before start time")
        }
        
        return BookingValidationResult(true, "Booking can be modified")
    }
    
    /**
     * Validates if a booking can be cancelled
     * - At least 12 hours before start time
     * - Booking must not be completed or already cancelled
     */
    fun validateBookingCancellation(booking: Booking): BookingValidationResult {
        return validateBookingCancellation(booking.bookingDateTime, booking.status)
    }
    
    /**
     * Validates if an API booking can be cancelled
     */
    fun validateBookingCancellation(apiBooking: ApiBooking): BookingValidationResult {
        val startTime = try {
            apiDateFormat.parse(apiBooking.startTime) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val status = when (ApiBookingStatus.fromValue(apiBooking.status)) {
            ApiBookingStatus.PENDING -> BookingStatus.PENDING
            ApiBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
            ApiBookingStatus.IN_PROGRESS -> BookingStatus.IN_PROGRESS
            ApiBookingStatus.COMPLETED -> BookingStatus.COMPLETED
            ApiBookingStatus.CANCELLED -> BookingStatus.CANCELLED
            ApiBookingStatus.NO_SHOW -> BookingStatus.CANCELLED
        }
        
        return validateBookingCancellation(startTime, status)
    }
    
    private fun validateBookingCancellation(startTime: Date, status: BookingStatus): BookingValidationResult {
        // Check if booking is in cancellable status
        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            return BookingValidationResult(false, "This booking cannot be cancelled")
        }
        
        // Check if cancellation is at least 12 hours before start time
        val currentTime = System.currentTimeMillis()
        val timeUntilStart = startTime.time - currentTime
        
        if (timeUntilStart < TWELVE_HOURS_MS) {
            return BookingValidationResult(false, "Bookings can only be cancelled at least 12 hours before start time")
        }
        
        return BookingValidationResult(true, "Booking can be cancelled")
    }
    
    /**
     * Checks if a booking has QR code available (approved booking)
     */
    fun hasQRCodeAvailable(booking: Booking): Boolean {
        return booking.status == BookingStatus.APPROVED && !booking.qrCode.isNullOrBlank()
    }
    
    /**
     * Checks if an API booking has QR code available
     */
    fun hasQRCodeAvailable(apiBooking: ApiBooking): Boolean {
        return !apiBooking.qrCode.isNullOrBlank() && 
               (apiBooking.status == ApiBookingStatus.CONFIRMED.value || 
                apiBooking.status == ApiBookingStatus.IN_PROGRESS.value)
    }
    
    /**
     * Formats the time remaining until booking start
     */
    fun getTimeUntilBooking(startTime: Date): String {
        val currentTime = System.currentTimeMillis()
        val timeDiff = startTime.time - currentTime
        
        if (timeDiff <= 0) {
            return "Started"
        }
        
        val hours = timeDiff / (60 * 60 * 1000)
        val minutes = (timeDiff % (60 * 60 * 1000)) / (60 * 1000)
        
        return when {
            hours >= 24 -> {
                val days = hours / 24
                "${days}d ${hours % 24}h"
            }
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * Result class for booking validation
 */
data class BookingValidationResult(
    val isValid: Boolean,
    val message: String
)