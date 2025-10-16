package com.example.evmobile.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.example.evmobile.models.*

/**
 * API Service for handling booking-related HTTP requests
 */
class BookingApiService {
    
    companion object {
        private const val API_BASE_URL = "http://10.0.2.2:5105/api"
        private const val TAG = "BookingApiService"
        
        // Date formatters for API communication
        private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    /**
     * Get all bookings for the current user
     */
    suspend fun getUserBookings(userId: String, authToken: String): Result<List<ApiBooking>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE_URL/bookings/user/$userId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "getUserBookings response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "getUserBookings response: $response")
                    
                    val bookings = parseBookingsFromJson(response)
                    Result.success(bookings)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "getUserBookings failed: $responseCode - $errorResponse")
                    Result.failure(Exception("Failed to fetch bookings: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserBookings exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(request: CreateBookingApiRequest, authToken: String): Result<CreateBookingApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE_URL/bookings")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.doOutput = true
                
                // Prepare request body
                val requestBody = JSONObject().apply {
                    put("userId", request.userId)
                    put("stationId", request.stationId)
                    put("connectorId", request.connectorId)
                    put("startTime", request.startTime)
                    put("endTime", request.endTime)
                    request.notes?.let { put("notes", it) }
                }
                
                Log.d(TAG, "createBooking request: $requestBody")
                
                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "createBooking response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "createBooking response: $response")
                    
                    val bookingResponse = parseCreateBookingResponse(response)
                    Result.success(bookingResponse)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "createBooking failed: $responseCode - $errorResponse")
                    Result.failure(Exception("Failed to create booking: $responseCode - $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "createBooking exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update an existing booking
     */
    suspend fun updateBooking(bookingId: String, request: UpdateBookingApiRequest, authToken: String): Result<ApiBooking> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE_URL/bookings/$bookingId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.doOutput = true
                
                // Prepare request body
                val requestBody = JSONObject().apply {
                    put("stationId", request.stationId)
                    put("connectorId", request.connectorId)
                    put("startTime", request.startTime)
                    put("endTime", request.endTime)
                    request.notes?.let { put("notes", it) }
                }
                
                Log.d(TAG, "updateBooking request: $requestBody")
                
                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "updateBooking response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "updateBooking response: $response")
                    
                    val booking = parseBookingFromJson(JSONObject(response))
                    Result.success(booking)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "updateBooking failed: $responseCode - $errorResponse")
                    Result.failure(Exception("Failed to update booking: $responseCode - $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateBooking exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: String, authToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE_URL/bookings/$bookingId/cancel")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "cancelBooking response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    Result.success(Unit)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "cancelBooking failed: $responseCode - $errorResponse")
                    Result.failure(Exception("Failed to cancel booking: $responseCode - $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "cancelBooking exception", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get charging stations for booking
     */
    suspend fun getChargingStations(): Result<List<ApiChargingStation>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE_URL/chargingstations")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "getChargingStations response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "getChargingStations response: ${response.take(200)}")
                    
                    val stations = parseStationsFromJson(response)
                    Result.success(stations)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "getChargingStations failed: $responseCode - $errorResponse")
                    Result.failure(Exception("Failed to fetch stations: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getChargingStations exception", e)
                Result.failure(e)
            }
        }
    }
    
    // JSON parsing methods
    private fun parseBookingsFromJson(json: String): List<ApiBooking> {
        val bookings = mutableListOf<ApiBooking>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val bookingJson = jsonArray.getJSONObject(i)
                val booking = parseBookingFromJson(bookingJson)
                bookings.add(booking)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bookings JSON", e)
        }
        return bookings
    }
    
    private fun parseBookingFromJson(json: JSONObject): ApiBooking {
        return ApiBooking(
            id = json.getString("id"),
            userId = json.getString("userId"),
            stationId = json.getString("stationId"),
            connectorId = json.getString("connectorId"),
            startTime = json.getString("startTime"),
            endTime = json.getString("endTime"),
            status = json.getInt("status"),
            qrCode = json.optString("qrCode"),
            energyConsumed = json.optDouble("energyConsumed", 0.0),
            totalCost = json.optDouble("totalCost", 0.0),
            notes = json.optString("notes"),
            createdAt = json.getString("createdAt"),
            updatedAt = json.getString("updatedAt")
        )
    }
    
    private fun parseCreateBookingResponse(json: String): CreateBookingApiResponse {
        val jsonObj = JSONObject(json)
        return CreateBookingApiResponse(
            id = jsonObj.getString("id"),
            userId = jsonObj.getString("userId"),
            stationId = jsonObj.getString("stationId"),
            connectorId = jsonObj.getString("connectorId"),
            startTime = jsonObj.getString("startTime"),
            endTime = jsonObj.getString("endTime"),
            status = jsonObj.getInt("status"),
            qrCode = jsonObj.optString("qrCode"),
            energyConsumed = jsonObj.optDouble("energyConsumed", 0.0),
            totalCost = jsonObj.optDouble("totalCost", 0.0),
            notes = jsonObj.optString("notes"),
            createdAt = jsonObj.getString("createdAt"),
            updatedAt = jsonObj.getString("updatedAt")
        )
    }
    
    private fun parseStationsFromJson(json: String): List<ApiChargingStation> {
        val stations = mutableListOf<ApiChargingStation>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val stationJson = jsonArray.getJSONObject(i)
                
                // Parse connectors
                val connectorsJson = stationJson.optJSONArray("connectors") ?: JSONArray()
                val connectors = mutableListOf<ApiConnector>()
                for (j in 0 until connectorsJson.length()) {
                    val connectorJson = connectorsJson.getJSONObject(j)
                    connectors.add(ApiConnector(
                        id = connectorJson.getString("id"),
                        type = connectorJson.getInt("type"),
                        power = connectorJson.optDouble("power", 0.0),
                        isAvailable = connectorJson.optBoolean("isAvailable", true),
                        status = connectorJson.optInt("status", 0)
                    ))
                }
                
                // Parse location
                val locationJson = stationJson.optJSONObject("location")
                
                stations.add(ApiChargingStation(
                    id = stationJson.getString("id"),
                    name = stationJson.getString("name"),
                    description = stationJson.optString("description", ""),
                    address = stationJson.optString("address", ""),
                    latitude = locationJson?.optDouble("latitude", 0.0) ?: 0.0,
                    longitude = locationJson?.optDouble("longitude", 0.0) ?: 0.0,
                    operatorId = stationJson.optString("operatorId", ""),
                    status = stationJson.optInt("status", 0),
                    connectors = connectors,
                    pricePerKWh = stationJson.optDouble("pricePerKWh", 0.0)
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stations JSON", e)
        }
        return stations
    }
}