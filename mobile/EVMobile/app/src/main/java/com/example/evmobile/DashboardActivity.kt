package com.example.evmobile

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
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
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val API_BASE_URL = "http://10.0.2.2:5105/api" // Use 10.0.2.2 for emulator
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize MapView
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        
        initViews()
        loadUserSession()
        setupUI()
        setupMapView(mapViewBundle)
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
    
    private fun setupMapView(mapViewBundle: Bundle?) {
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
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
        
        // Extract first name from full name
        userFirstName = if (fullName.isNotEmpty()) {
            fullName.split(" ").firstOrNull() ?: "User"
        } else {
            "User"
        }
    }
    
    private fun loadDashboardData() {
        if (userId.isNotEmpty() && authToken.isNotEmpty()) {
            fetchUserBookings()
        } else {
            // User not logged in, show default values
            updateUI()
        }
    }
    
    private fun fetchUserBookings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/bookings/user/$userId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    
                    parseBookingsData(response)
                } else {
                    // Handle error - use default values
                    withContext(Dispatchers.Main) {
                        updateUI()
                    }
                }
                
            } catch (e: Exception) {
                // Network error - use default values
                withContext(Dispatchers.Main) {
                    updateUI()
                }
            }
        }
    }
    
    private suspend fun parseBookingsData(jsonString: String) {
        try {
            val bookingsArray = JSONArray(jsonString)
            var pendingCount = 0
            var approvedCount = 0
            
            for (i in 0 until bookingsArray.length()) {
                val booking = bookingsArray.getJSONObject(i)
                val status = booking.optString("status", "")
                
                when (status.lowercase()) {
                    "pending" -> pendingCount++
                    "confirmed", "approved" -> approvedCount++
                }
            }
            
            pendingReservations = pendingCount
            approvedReservations = approvedCount
            
            withContext(Dispatchers.Main) {
                updateUI()
            }
            
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
    }
    
    private fun updateUI() {
        // Set welcome message
        tvWelcomeMessage.text = "Welcome back, $userFirstName!"
        
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())
        
        // Set reservation counts
        tvPendingCount.text = pendingReservations.toString()
        tvApprovedCount.text = approvedReservations.toString()
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings
        with(googleMap) {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
        }
        
        // Add sample charging stations
        addSampleChargingStations()
        
        // Get user location and center map
        getCurrentLocationAndCenterMap()
    }
    
    private fun addSampleChargingStations() {
        // Sample charging station locations (you would get these from your API)
        val stations = listOf(
            ChargingStation("Station A", 6.9271, 79.8612, "Available", "AC/DC"),
            ChargingStation("Station B", 6.9319, 79.8478, "Occupied", "AC"),
            ChargingStation("Station C", 6.9223, 79.8563, "Available", "DC"),
            ChargingStation("Station D", 6.9344, 79.8547, "Maintenance", "AC/DC"),
            ChargingStation("Station E", 6.9289, 79.8634, "Available", "AC")
        )
        
        stations.forEach { station ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .title(station.name)
                    .snippet("${station.type} - ${station.status}")
                    .icon(getMarkerIcon(station.status))
            )
            marker?.tag = station
        }
        
        // Set camera to show all markers
        val builder = LatLngBounds.Builder()
        stations.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
        val bounds = builder.build()
        val padding = 100
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
    
    private fun getMarkerIcon(status: String): BitmapDescriptor {
        return when (status) {
            "Available" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            "Occupied" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            "Maintenance" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }
    
    private fun getCurrentLocationAndCenterMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            googleMap.isMyLocationEnabled = true
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                }
            }
        }
    }
    
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
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
                getCurrentLocationAndCenterMap()
            } else {
                showToast("Location permission is required to show nearby stations")
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }
    
    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }
    
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        
        mapView.onSaveInstanceState(mapViewBundle)
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