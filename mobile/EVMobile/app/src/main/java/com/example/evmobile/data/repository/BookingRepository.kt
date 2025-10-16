package com.example.evmobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.evmobile.data.api.BookingApiService
import com.example.evmobile.data.mappers.BookingMapper
import com.example.evmobile.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Repository class to handle booking data operations
 * Manages API calls and data mapping for booking functionality
 */
class BookingRepository(private val context: Context) {
    
    private val bookingApiService = BookingApiService()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("EVChargingApp", Context.MODE_PRIVATE)
    
    // Cache for stations to avoid frequent API calls
    private var cachedStations: List<StationForBooking>? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Get authentication token from shared preferences
     */
    private fun getAuthToken(): String? {
        val token = sharedPrefs.getString("auth_token", null)
        android.util.Log.d("BookingRepository", "Auth token: ${if (token.isNullOrBlank()) "NULL/EMPTY" else "EXISTS (${token.length} chars)"}")
        return token
    }
    
    /**
     * Get current user ID from shared preferences
     */
    private fun getCurrentUserId(): String? {
        val userId = sharedPrefs.getString("user_id", null)
        android.util.Log.d("BookingRepository", "User ID: ${userId ?: "NULL"}")
        return userId
    }
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        val authToken = getAuthToken()
        val userId = getCurrentUserId()
        val isAuth = !authToken.isNullOrBlank() && !userId.isNullOrBlank()
        android.util.Log.d("BookingRepository", "User authenticated: $isAuth")
        return isAuth
    }
    
    /**
     * Get user's bookings (both upcoming and past)
     */
    suspend fun getUserBookings(): Result<Pair<List<Booking>, List<ChargingHistory>>> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = getAuthToken()
                val userId = getCurrentUserId()
                
                android.util.Log.d("BookingRepository", "Attempting to get user bookings...")
                android.util.Log.d("BookingRepository", "Auth token exists: ${!authToken.isNullOrBlank()}")
                android.util.Log.d("BookingRepository", "User ID exists: ${!userId.isNullOrBlank()}")
                
                if (authToken.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No authentication token found. Please log in again."))
                }
                
                if (userId.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No user ID found. Please log in again."))
                }
                
                val result = bookingApiService.getUserBookings(userId, authToken)
                
                if (result.isSuccess) {
                    val apiBookings = result.getOrNull() ?: emptyList()
                    
                    // Get station details for better mapping
                    val stationsResult = getStationsForBooking()
                    val stations = stationsResult.getOrNull() ?: emptyList()
                    val stationsMap = stations.associateBy { it.id }
                    
                    val upcomingBookings = mutableListOf<Booking>()
                    val chargingHistory = mutableListOf<ChargingHistory>()
                    
                    apiBookings.forEach { apiBooking ->
                        val station = stationsMap[apiBooking.stationId]
                        val stationName = station?.name ?: "Unknown Station"
                        val stationLocation = station?.location ?: "Unknown Location"
                        
                        when (apiBooking.status) {
                            ApiBookingStatus.PENDING.value,
                            ApiBookingStatus.CONFIRMED.value,
                            ApiBookingStatus.IN_PROGRESS.value -> {
                                // Add to upcoming bookings (only active bookings)
                                val booking = BookingMapper.mapApiBookingToBooking(
                                    apiBooking, stationName, stationLocation
                                )
                                upcomingBookings.add(booking)
                            }
                            ApiBookingStatus.COMPLETED.value,
                            ApiBookingStatus.CANCELLED.value,
                            ApiBookingStatus.NO_SHOW.value -> {
                                // Add to charging history (finished bookings)
                                val history = BookingMapper.mapApiBookingToChargingHistory(
                                    apiBooking, stationName, stationLocation
                                )
                                history?.let { chargingHistory.add(it) }
                            }
                        }
                    }
                    
                    Result.success(Pair(upcomingBookings, chargingHistory))
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch bookings"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get available charging stations for booking
     */
    suspend fun getStationsForBooking(): Result<List<StationForBooking>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if cache is still valid
                val currentTime = System.currentTimeMillis()
                if (cachedStations != null && (currentTime - cacheTimestamp) < cacheValidityMs) {
                    return@withContext Result.success(cachedStations!!)
                }
                
                val result = bookingApiService.getChargingStations()
                
                if (result.isSuccess) {
                    val apiStations = result.getOrNull() ?: emptyList()
                    
                    // Filter only active stations with available connectors
                    val availableStations = apiStations
                        .filter { it.status == ApiStationStatus.ACTIVE.value }
                        .filter { it.connectors.any { connector -> 
                            connector.isAvailable && connector.status == ApiConnectorStatus.AVAILABLE.value 
                        }}
                        .mapIndexed { index, apiStation ->
                            // Calculate distance (mock calculation for now)
                            val distance = (index + 1) * 2.5 // Mock distance in km
                            BookingMapper.mapApiStationToStationForBooking(apiStation, distance)
                        }
                    
                    // Update cache
                    cachedStations = availableStations
                    cacheTimestamp = currentTime
                    
                    Result.success(availableStations)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch stations"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(
        stationId: String,
        connectorId: String,
        startTime: Date,
        endTime: Date,
        notes: String?
    ): Result<CreateBookingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = getAuthToken()
                val userId = getCurrentUserId()
                
                if (authToken.isNullOrBlank() || userId.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("User not authenticated"))
                }
                
                // Validate booking creation time (within 7 days)
                if (!BookingMapper.validateBookingCreation(startTime)) {
                    return@withContext Result.failure(Exception("Booking must be within 7 days from now"))
                }
                
                val apiRequest = BookingMapper.mapCreateBookingRequestToApi(
                    userId, stationId, connectorId, startTime, endTime, notes
                )
                
                val result = bookingApiService.createBooking(apiRequest, authToken)
                
                if (result.isSuccess) {
                    val apiResponse = result.getOrNull()!!
                    
                    // Map to UI response model
                    val response = CreateBookingResponse(
                        id = apiResponse.id,
                        userId = apiResponse.userId,
                        stationId = apiResponse.stationId,
                        connectorId = apiResponse.connectorId,
                        startTime = apiResponse.startTime,
                        endTime = apiResponse.endTime,
                        status = apiResponse.status,
                        qrCode = apiResponse.qrCode ?: "",
                        energyConsumed = apiResponse.energyConsumed,
                        totalCost = apiResponse.totalCost,
                        notes = apiResponse.notes,
                        createdAt = apiResponse.createdAt,
                        updatedAt = apiResponse.updatedAt
                    )
                    
                    Result.success(response)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to create booking"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update an existing booking
     */
    suspend fun updateBooking(
        bookingId: String,
        stationId: String,
        connectorId: String,
        startTime: Date,
        endTime: Date,
        notes: String?
    ): Result<Booking> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = getAuthToken()
                
                if (authToken.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("User not authenticated"))
                }
                
                // Validate booking update time (at least 12 hours before)
                val currentTime = System.currentTimeMillis()
                val twelveHoursInMs = 12 * 60 * 60 * 1000L
                if ((startTime.time - currentTime) < twelveHoursInMs) {
                    return@withContext Result.failure(Exception("Booking can only be modified at least 12 hours before start time"))
                }
                
                val apiRequest = BookingMapper.mapUpdateBookingRequestToApi(
                    stationId, connectorId, startTime, endTime, notes
                )
                
                val result = bookingApiService.updateBooking(bookingId, apiRequest, authToken)
                
                if (result.isSuccess) {
                    val apiBooking = result.getOrNull()!!
                    
                    // Get station details for mapping
                    val stationsResult = getStationsForBooking()
                    val stations = stationsResult.getOrNull() ?: emptyList()
                    val station = stations.find { it.id == apiBooking.stationId }
                    
                    val booking = BookingMapper.mapApiBookingToBooking(
                        apiBooking,
                        station?.name ?: "Unknown Station",
                        station?.location ?: "Unknown Location"
                    )
                    
                    Result.success(booking)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to update booking"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = getAuthToken()
                
                if (authToken.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("User not authenticated"))
                }
                
                val result = bookingApiService.cancelBooking(bookingId, authToken)
                
                if (result.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to cancel booking"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clear cached stations data
     */
    fun clearCache() {
        cachedStations = null
        cacheTimestamp = 0
    }
}