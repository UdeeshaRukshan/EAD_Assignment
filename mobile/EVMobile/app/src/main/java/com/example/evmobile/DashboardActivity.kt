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
import java.text.SimpleDateFormat
import java.util.*

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
    
    // Sample data - in real app, this would come from API/database
    private var pendingReservations = 3
    private var approvedReservations = 7
    private var userFirstName = "John" // This would be fetched from user session
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
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
        setupUI()
        setupMapView(mapViewBundle)
        setupClickListeners()
        checkLocationPermission()
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