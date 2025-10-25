package com.example.evmobile

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {
    
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    // UI Components
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var cardPending: CardView
    private lateinit var cardApproved: CardView
    private lateinit var cardProfile: CardView
    private lateinit var cardSettings: CardView
    
    // Dashboard data - fetched from API
    private var pendingReservations = 0
    private var approvedReservations = 0
    private var userFirstName = ""
    private var userId = ""
    private var authToken = ""
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val API_BASE_URL = if (Build.FINGERPRINT.contains("generic") ||
            Build.FINGERPRINT.contains("emulator")) {
            "http://10.0.2.2:5105/api"        // Emulator
        } else {
            "http://172.20.10.3:5105/api"   // Physical Device
        } // Use 10.0.2.2 for emulator
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        setContentView(R.layout.activity_dashboard)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        initViews()
        loadUserSession()
        setupUI()
        setupMapView()
        setupClickListeners()
        checkLocationPermission()
        loadDashboardData()
    }
    
    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        cardPending = findViewById(R.id.cardPending)
        cardApproved = findViewById(R.id.cardApproved)
        cardProfile = findViewById(R.id.cardProfile)
        cardSettings = findViewById(R.id.cardSettings)
    }
    
    private fun setupUI() {
        // Set welcome message
        tvWelcomeMessage.text = "Welcome back, $userFirstName!"
        
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())
        
        // Set reservation counts
        tvPendingCount.text = pendingReservations.toString()
        tvApprovedCount.text = approvedReservations.toString()
    }
    
    private fun setupMapView() {
        // Configure map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)
        
        // Set default location (Colombo, Sri Lanka)
        val startPoint = GeoPoint(6.9271, 79.8612)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(startPoint)
        
        // Add my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        myLocationOverlay.enableMyLocation()
        mapView.overlays.add(myLocationOverlay)
        
        // Add sample charging station markers
        addSampleChargingStations()
    }
    
    private fun setupClickListeners() {
        cardPending.setOnClickListener {
            showToast("Pending Reservations: $pendingReservations")
            // TODO: Navigate to pending reservations list
        }
        
        cardApproved.setOnClickListener {
            showToast("Approved Reservations: $approvedReservations")
            // TODO: Navigate to approved reservations list
        }
        
        cardProfile.setOnClickListener {
            showToast("Profile")
            // TODO: Navigate to profile
        }
        
        cardSettings.setOnClickListener {
            showToast("Settings")
            // TODO: Navigate to settings
        }
    }
    
    private fun loadUserSession() {
        val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
        authToken = prefs.getString("auth_token", "") ?: ""
        userId = prefs.getString("user_id", "") ?: ""
        val fullName = prefs.getString("user_name", "") ?: ""
        
        println("DEBUG: Loading user session...")
        println("DEBUG: UserId: $userId")
        println("DEBUG: AuthToken present: ${authToken.isNotEmpty()}")
        println("DEBUG: Full name: $fullName")
        
        // Extract first name from full name
        userFirstName = if (fullName.isNotEmpty()) {
            fullName.split(" ").firstOrNull() ?: "User"
        } else {
            "User"
        }
        
        println("DEBUG: First name extracted: $userFirstName")
    }
    
    private fun loadDashboardData() {
        println("DEBUG: loadDashboardData called")
        println("DEBUG: UserId: $userId, AuthToken present: ${authToken.isNotEmpty()}")
        
        if (userId.isNotEmpty() && authToken.isNotEmpty()) {
            println("DEBUG: User is logged in, fetching bookings...")
            fetchUserBookings()
        } else {
            // User not logged in, show default values
            println("DEBUG: User not logged in - using default values")
            updateUI()
        }
    }
    
    private fun fetchUserBookings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("DEBUG: Fetching bookings for userId: $userId")
                println("DEBUG: Auth token: ${authToken.take(20)}...")
                
                val url = URL("$API_BASE_URL/bookings/user/$userId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000  // Increased to 30 seconds
                connection.readTimeout = 30000     // Increased to 30 seconds
                connection.doInput = true
                
                println("DEBUG: Connecting to: $url")
                connection.connect()
                
                val responseCode = connection.responseCode
                println("DEBUG: Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    
                    println("DEBUG: Response received: ${response.take(200)}")
                    parseBookingsData(response)
                } else {
                    // Handle error - read error response
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "No error details"
                    }
                    
                    println("DEBUG: Error response ($responseCode): $errorResponse")
                    
                    withContext(Dispatchers.Main) {
                        showToast("Failed to load bookings: $responseCode")
                        updateUI()
                    }
                }
                
            } catch (e: Exception) {
                // Network error - use default values
                println("DEBUG: Exception fetching bookings: ${e.message}")
                e.printStackTrace()
                
                withContext(Dispatchers.Main) {
                    showToast("Error loading dashboard data: ${e.message}")
                    updateUI()
                }
            }
        }
    }
    
    private suspend fun parseBookingsData(jsonString: String) {
        try {
            println("DEBUG: Parsing bookings data...")
            val bookingsArray = JSONArray(jsonString)
            println("DEBUG: Found ${bookingsArray.length()} bookings")
            
            var pendingCount = 0
            var approvedCount = 0
            
            for (i in 0 until bookingsArray.length()) {
                val booking = bookingsArray.getJSONObject(i)
                
                // Handle both numeric and string status values
                // BookingStatus enum: 0=Pending, 1=Confirmed, 2=InProgress, 3=Completed, 4=Cancelled, 5=NoShow
                val statusValue = if (booking.has("status")) {
                    when (val statusObj = booking.get("status")) {
                        is Int -> statusObj
                        is String -> {
                            // Try to parse as int first, then as string
                            statusObj.toIntOrNull() ?: when (statusObj.lowercase()) {
                                "pending" -> 0
                                "confirmed" -> 1
                                "inprogress" -> 2
                                "completed" -> 3
                                "cancelled" -> 4
                                "noshow" -> 5
                                else -> -1
                            }
                        }
                        else -> -1
                    }
                } else {
                    -1
                }
                
                println("DEBUG: Booking $i - Status value: $statusValue")
                
                // Count pending (0) and confirmed (1) bookings
                when (statusValue) {
                    0 -> {  // Pending
                        pendingCount++
                        println("DEBUG: Incrementing pending count to $pendingCount")
                    }
                    1 -> {  // Confirmed
                        approvedCount++
                        println("DEBUG: Incrementing approved count to $approvedCount")
                    }
                }
            }
            
            pendingReservations = pendingCount
            approvedReservations = approvedCount
            
            println("DEBUG: Final counts - Pending: $pendingReservations, Approved: $approvedReservations")
            
            withContext(Dispatchers.Main) {
                updateUI()
            }
            
        } catch (e: Exception) {
            println("DEBUG: Error parsing bookings: ${e.message}")
            e.printStackTrace()
            
            withContext(Dispatchers.Main) {
                showToast("Error parsing booking data")
                updateUI()
            }
        }
    }
    
    private fun updateUI() {
        println("DEBUG: updateUI called")
        println("DEBUG: Pending: $pendingReservations, Approved: $approvedReservations")
        println("DEBUG: User first name: $userFirstName")
        
        // Set welcome message
        tvWelcomeMessage.text = "Welcome back, $userFirstName!"
        
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())
        
        // Set reservation counts
        tvPendingCount.text = pendingReservations.toString()
        tvApprovedCount.text = approvedReservations.toString()
        
        println("DEBUG: UI updated - Pending TextView: ${tvPendingCount.text}, Approved TextView: ${tvApprovedCount.text}")
    }
    
    private fun addSampleChargingStations() {
        // Sample charging station locations in Colombo, Sri Lanka
        val stations = listOf(
            ChargingStation("EV Station - Colombo Fort", 6.9319, 79.8478, "Available", "AC/DC"),
            ChargingStation("Green Charge - Mount Lavinia", 6.8381, 79.8631, "Available", "DC Fast")
        )
        
        stations.forEach { station ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(station.latitude, station.longitude)
            marker.title = station.name
            marker.snippet = "${station.type} - ${station.status}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set marker icon based on status
            when (station.status) {
                "Available" -> marker.icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
                "Occupied" -> marker.icon = resources.getDrawable(android.R.drawable.ic_menu_compass, null)
                else -> marker.icon = resources.getDrawable(android.R.drawable.ic_dialog_info, null)
            }
            
            mapView.overlays.add(marker)
        }
        
        mapView.invalidate()
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }

    
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Enable location overlay if permission granted
            if (::myLocationOverlay.isInitialized) {
                myLocationOverlay.enableMyLocation()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::myLocationOverlay.isInitialized) {
                    myLocationOverlay.enableMyLocation()
                    
                    // Center map on user location
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLocation = GeoPoint(it.latitude, it.longitude)
                            mapView.controller.animateTo(userLocation)
                        }
                    }
                }
            } else {
                showToast("Location permission is required to show your location")
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Data class for charging stations
data class ChargingStation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val type: String
)