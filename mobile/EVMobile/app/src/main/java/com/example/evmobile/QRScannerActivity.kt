package com.example.evmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class QRScannerActivity : AppCompatActivity() {

    companion object {
        private val API_BASE_URL = if (Build.FINGERPRINT.contains("generic") ||
                   Build.FINGERPRINT.contains("emulator")) {
            "http://10.0.2.2:5105/api"        // Emulator
        } else {
            "http://172.20.10.3:5105/api"   // Physical Device
        }
        
        private const val CAMERA_PERMISSION_CODE = 1001
        private const val TAG = "QRScannerActivity"
    }

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var toolbar: MaterialToolbar
    private var isScanning = true
    private var authToken = ""
    private var userRole = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        initViews()
        setupToolbar()
        loadUserInfo()
        
        // Check if user is operator
        if (userRole != "Operator" && userRole != "Admin") {
            Toast.makeText(this, "Only Station Operators can scan QR codes", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        checkCameraPermission()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        barcodeView = findViewById(R.id.barcodeScanner)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Scan Booking QR Code"
        }
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadUserInfo() {
        val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
        authToken = prefs.getString("auth_token", "") ?: ""
        userRole = prefs.getString("user_role", "") ?: ""
        
        Log.d(TAG, "Loaded user info - Role: $userRole, Token: ${if (authToken.isEmpty()) "MISSING" else "Present"}")
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.CAMERA), 
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (isScanning && result != null) {
                    isScanning = false
                    barcodeView.pause()
                    handleQRCodeScanned(result.text)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // Optional: handle possible result points
            }
        })
        
        barcodeView.resume()
    }

    private fun handleQRCodeScanned(qrCodeData: String) {
        try {
            // Log the raw QR code data for debugging
            Log.d(TAG, "QR Code Scanned: $qrCodeData")
            
            // The QR code should contain booking information
            // Parse the QR code data to extract booking ID
            val bookingId = extractBookingId(qrCodeData)
            
            Log.d(TAG, "Extracted Booking ID: $bookingId")
            Log.d(TAG, "Booking ID length: ${bookingId.length} (should be 24 for MongoDB ObjectId)")
            
            if (bookingId.isEmpty()) {
                Toast.makeText(this, "Invalid QR Code - Could not extract Booking ID", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Failed to extract booking ID from QR code")
                resumeScanning()
            } else if (bookingId.length != 24) {
                // MongoDB ObjectId must be exactly 24 hex characters
                Toast.makeText(
                    this, 
                    "Invalid Booking ID format - Expected 24 characters, got ${bookingId.length}\nID: $bookingId", 
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Invalid booking ID length: ${bookingId.length} (expected 24)")
                resumeScanning()
            } else {
                // Fetch booking details from server
                fetchBookingDetails(bookingId, qrCodeData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing QR Code: ${e.message}", e)
            Toast.makeText(this, "Error processing QR Code: ${e.message}", Toast.LENGTH_LONG).show()
            resumeScanning()
        }
    }

    private fun extractBookingId(qrCodeData: String): String {
        // QR code format could be:
        // 1. Base64 encoded JSON with booking info
        // 2. Simple booking ID
        // 3. JSON string
        
        return try {
            // Try parsing as JSON directly
            Log.d(TAG, "Attempting to parse QR as direct JSON")
            val json = JSONObject(qrCodeData)
            // Try both "BookingId" (capital) and "bookingId" (lowercase)
            var bookingId = json.optString("BookingId", "")
            if (bookingId.isEmpty()) {
                bookingId = json.optString("bookingId", "")
            }
            Log.d(TAG, "Extracted booking ID from direct JSON: $bookingId")
            bookingId
        } catch (e: Exception) {
            // Try to decode from base64
            try {
                Log.d(TAG, "Attempting to decode QR from Base64")
                val decoded = android.util.Base64.decode(qrCodeData, android.util.Base64.DEFAULT)
                val decodedString = String(decoded)
                Log.d(TAG, "Decoded Base64 string: $decodedString")
                
                val json = JSONObject(decodedString)
                // Try both "BookingId" (capital) and "bookingId" (lowercase)
                var bookingId = json.optString("BookingId", "")
                if (bookingId.isEmpty()) {
                    bookingId = json.optString("bookingId", "")
                }
                Log.d(TAG, "Extracted booking ID from Base64 JSON: $bookingId")
                bookingId
            } catch (e2: Exception) {
                // Assume it's a plain booking ID
                Log.d(TAG, "Using QR data as plain booking ID: $qrCodeData")
                qrCodeData
            }
        }
    }

    private fun fetchBookingDetails(bookingId: String, originalQRCode: String) {
        // Check if we have auth token before making request
        if (authToken.isEmpty()) {
            Log.e(TAG, "No auth token found!")
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Authentication Required")
                    .setMessage("You are not logged in. Please login as an Operator first.\n\nGo back and login, then try again.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/bookings/$bookingId")
                Log.d(TAG, "Fetching booking from URL: $url")
                Log.d(TAG, "Auth token: ${if (authToken.isEmpty()) "EMPTY" else "Present (${authToken.length} chars)"}")
                Log.d(TAG, "User role: $userRole")
                
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    Log.d(TAG, "Response: ${response.take(200)}...") // Log first 200 chars
                    
                    // Check if response is an array (wrong endpoint) or object (correct)
                    val trimmedResponse = response.trim()
                    if (trimmedResponse.startsWith("[")) {
                        Log.e(TAG, "ERROR: Received array instead of single booking object!")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@QRScannerActivity, 
                                "API Error: Wrong endpoint - got list instead of single booking", 
                                Toast.LENGTH_LONG
                            ).show()
                            resumeScanning()
                        }
                        return@launch
                    }
                    
                    val bookingJson = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        showBookingDetailsDialog(bookingJson, bookingId)
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Failed to fetch booking details"
                    }
                    
                    Log.e(TAG, "API Error - Code: $responseCode, Response: $errorResponse")
                    
                    withContext(Dispatchers.Main) {
                        // Special handling for 403 Forbidden
                        if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                            AlertDialog.Builder(this@QRScannerActivity)
                                .setTitle("Access Denied (403)")
                                .setMessage(
                                    "You don't have permission to access this booking.\n\n" +
                                    "Possible reasons:\n" +
                                    "• Not logged in as Operator/Admin\n" +
                                    "• Auth token is missing or expired\n" +
                                    "• Wrong role (current: $userRole)\n\n" +
                                    "Auth Token: ${if (authToken.isEmpty()) "MISSING ❌" else "Present ✓"}\n\n" +
                                    "Solution: Logout and login again as Operator."
                                )
                                .setPositiveButton("Logout & Exit") { _, _ ->
                                    // Clear auth data
                                    getSharedPreferences("EVChargingApp", MODE_PRIVATE)
                                        .edit()
                                        .clear()
                                        .apply()
                                    finish()
                                }
                                .setNegativeButton("Cancel") { _, _ ->
                                    resumeScanning()
                                }
                                .setCancelable(false)
                                .show()
                        } else {
                            // Show detailed error in an AlertDialog for other errors
                            AlertDialog.Builder(this@QRScannerActivity)
                                .setTitle("Error Fetching Booking")
                                .setMessage("Response Code: $responseCode\n\nError: $errorResponse\n\nBooking ID: $bookingId\n\nURL: $url")
                                .setPositiveButton("Retry") { _, _ ->
                                    fetchBookingDetails(bookingId, originalQRCode)
                                }
                                .setNegativeButton("Cancel") { _, _ ->
                                    resumeScanning()
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Exception: ${e.message}", e)
                e.printStackTrace()
                
                withContext(Dispatchers.Main) {
                    // Show detailed error in an AlertDialog
                    AlertDialog.Builder(this@QRScannerActivity)
                        .setTitle("Network Error")
                        .setMessage("Error Type: ${e.javaClass.simpleName}\n\nMessage: ${e.message}\n\nBooking ID: $bookingId\n\nURL: $API_BASE_URL/bookings/$bookingId")
                        .setPositiveButton("Retry") { _, _ ->
                            fetchBookingDetails(bookingId, originalQRCode)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            resumeScanning()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun showBookingDetailsDialog(bookingJson: JSONObject, bookingId: String) {
        val status = bookingJson.optInt("status", -1)
        val stationId = bookingJson.optString("stationId", "N/A")
        val userId = bookingJson.optString("userId", "N/A")
        val startTime = bookingJson.optString("startTime", "N/A")
        val endTime = bookingJson.optString("endTime", "N/A")
        
        // Check if booking is in valid state for completion
        val statusNames = mapOf(
            0 to "Pending",
            1 to "Confirmed", 
            2 to "InProgress",
            3 to "Completed",
            4 to "Cancelled",
            5 to "NoShow"
        )
        val statusName = statusNames[status] ?: "Unknown"
        
        val message = """
            Booking ID: $bookingId
            Status: $statusName
            Station ID: $stationId
            User ID: $userId
            Start Time: $startTime
            End Time: $endTime
            
            Do you want to complete this booking?
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("Complete Booking") { _, _ ->
                if (status == 1 || status == 2) { // Confirmed or InProgress
                    showEnergyInputDialog(bookingId)
                } else {
                    Toast.makeText(this, "Booking cannot be completed. Current status: $statusName", Toast.LENGTH_LONG).show()
                    resumeScanning()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                resumeScanning()
            }
            .setCancelable(false)
            .show()
    }

    private fun showEnergyInputDialog(bookingId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_booking, null)
        val etEnergyConsumed = dialogView.findViewById<TextInputEditText>(R.id.etEnergyConsumed)
        
        AlertDialog.Builder(this)
            .setTitle("Complete Booking")
            .setMessage("Enter the energy consumed during charging")
            .setView(dialogView)
            .setPositiveButton("Complete") { _, _ ->
                val energyStr = etEnergyConsumed.text.toString().trim()
                if (energyStr.isEmpty()) {
                    Toast.makeText(this, "Please enter energy consumed", Toast.LENGTH_SHORT).show()
                    resumeScanning()
                    return@setPositiveButton
                }
                
                val energyConsumed = energyStr.toDoubleOrNull()
                if (energyConsumed == null || energyConsumed <= 0) {
                    Toast.makeText(this, "Please enter a valid energy value", Toast.LENGTH_SHORT).show()
                    resumeScanning()
                    return@setPositiveButton
                }
                
                completeBooking(bookingId, energyConsumed)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                resumeScanning()
            }
            .setCancelable(false)
            .show()
    }

    private fun completeBooking(bookingId: String, energyConsumed: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/bookings/$bookingId/complete")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                // Create JSON request body
                val jsonRequest = JSONObject().apply {
                    put("energyConsumed", energyConsumed)
                }
                
                // Write request body
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonRequest.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@QRScannerActivity, 
                            "Booking completed successfully! Energy consumed: $energyConsumed kWh", 
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Show success dialog with option to scan another or go back
                        showSuccessDialog()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Failed to complete booking"
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QRScannerActivity, "Error: $errorResponse", Toast.LENGTH_LONG).show()
                        resumeScanning()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QRScannerActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    resumeScanning()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success!")
            .setMessage("Booking has been completed successfully.\n\nWould you like to scan another QR code?")
            .setPositiveButton("Scan Another") { dialog, _ ->
                dialog.dismiss()
                resumeScanning()
            }
            .setNegativeButton("Go Back") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun resumeScanning() {
        isScanning = true
        barcodeView.resume()
    }

    override fun onResume() {
        super.onResume()
        if (isScanning) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
