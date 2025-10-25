package com.example.evmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
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

class ChargingStationsActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var mapView: MapView
    private lateinit var toggleViewButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    
    private lateinit var stationsAdapter: ChargingStationsAdapter
    
    private var isMapView = false
    private var stationsList = mutableListOf<ChargingStationModel>()
    private var currentLocation: Location? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val API_BASE_URL = "http://10.0.2.2:5105/api" // Use 10.0.2.2 for emulator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        setContentView(R.layout.activity_charging_stations)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        initViews()
        setupRecyclerView()
        setupMapView()
        setupListeners()
        checkLocationPermission()
        
        loadChargingStations()
    }
    
    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        mapView = findViewById(R.id.mapView)
        toggleViewButton = findViewById(R.id.fabToggleView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
    }
    
    private fun setupRecyclerView() {
        stationsAdapter = ChargingStationsAdapter(stationsList) { station ->
            openStationDetails(station)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = stationsAdapter
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
    }
    
    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadChargingStations()
        }
        
        toggleViewButton.setOnClickListener {
            toggleView()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.charging_stations_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_refresh -> {
                loadChargingStations()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleView() {
        isMapView = !isMapView
        
        if (isMapView) {
            recyclerView.visibility = View.GONE
            mapView.visibility = View.VISIBLE
            toggleViewButton.setImageResource(R.drawable.ic_list_view)
            supportActionBar?.title = "Stations Map"
        } else {
            mapView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            toggleViewButton.setImageResource(R.drawable.ic_map_view)
            supportActionBar?.title = "Charging Stations"
        }
    }
    
    private fun loadChargingStations() {
        showLoading(true)
        
        // Use coroutines for API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = fetchStationsFromAPI()
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    updateStationsList(stations)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Failed to load stations: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun fetchStationsFromAPI(): List<ChargingStationModel> {
        return withContext(Dispatchers.IO) {
            val fullUrl = "$API_BASE_URL/chargingstations"
            println("Making API request to: $fullUrl")
            
            try {
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000  // Increased to 30 seconds
                connection.readTimeout = 30000     // Increased to 30 seconds
                connection.doInput = true
                
                println("Connecting to API...")
                connection.connect()
                
                val responseCode = connection.responseCode
                println("API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }
                    
                    println("API Response received, length: ${response.length}")
                    println("Response preview: ${response.take(200)}")
                    
                    if (response.isBlank()) {
                        throw Exception("Empty response from server")
                    }
                    
                    parseStationsJSON(response)
                } else {
                    // Read error response
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "No error details available"
                    }
                    
                    println("API Error Response: $errorResponse")
                    throw Exception("Server error (Code: $responseCode): $errorResponse")
                }
            } catch (e: java.net.ConnectException) {
                println("Connection failed: ${e.message}")
                throw Exception("Cannot connect to server. Make sure the API is running on port 5105.")
            } catch (e: java.net.UnknownHostException) {
                println("Unknown host: ${e.message}")
                throw Exception("Cannot resolve server address. Check your network connection.")
            } catch (e: java.net.SocketTimeoutException) {
                println("Request timeout: ${e.message}")
                throw Exception("Request timed out. Server might be slow or unavailable.")
            } catch (e: Exception) {
                println("Network error: ${e.message}")
                e.printStackTrace()
                throw Exception("Network error: ${e.message}")
            }
        }
    }
    
    private fun parseStationsJSON(jsonString: String): List<ChargingStationModel> {
        val stations = mutableListOf<ChargingStationModel>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val stationJson = jsonArray.getJSONObject(i)
                
                // Parse location object
                val locationJson = stationJson.getJSONObject("location")
                val latitude = locationJson.getDouble("latitude")
                val longitude = locationJson.getDouble("longitude")
                
                // Parse connectors array
                val connectorsArray = stationJson.optJSONArray("connectors")
                val connectorTypes = parseConnectorTypes(connectorsArray)
                val (totalSlots, availableSlots) = calculateSlots(connectorsArray)
                
                // Determine if station is active based on status
                val status = stationJson.optInt("status", 0)
                val isActive = status != 3 && !stationJson.optBoolean("isDeleted", false)
                
                val station = ChargingStationModel(
                    id = stationJson.getString("id"),
                    name = stationJson.getString("name"),
                    address = stationJson.getString("address"),
                    latitude = latitude,
                    longitude = longitude,
                    isActive = isActive,
                    connectorTypes = connectorTypes,
                    pricePerKwh = stationJson.optDouble("pricePerKWh", 0.0), // Note: API uses capital W
                    operatingHours = stationJson.optString("openingHours", "24/7"),
                    amenities = parseAmenities(stationJson.optJSONArray("amenities")),
                    rating = calculateRating(), // Generate random rating for now
                    totalSlots = totalSlots,
                    availableSlots = availableSlots
                )
                
                stations.add(station)
            }
        } catch (e: Exception) {
            println("Error parsing stations: ${e.message}")
            e.printStackTrace()
            throw Exception("Failed to parse stations data: ${e.message}")
        }
        
        return stations
    }
    
    private fun parseConnectorTypes(connectorsArray: JSONArray?): List<String> {
        val connectorTypes = mutableListOf<String>()
        
        connectorsArray?.let { array ->
            for (i in 0 until array.length()) {
                val connector = array.getJSONObject(i)
                val typeNumber = connector.optInt("type", 1)
                val typeName = when (typeNumber) {
                    1 -> "AC Type 1"
                    2 -> "AC Type 2" 
                    3 -> "DC Fast"
                    4 -> "CHAdeMO"
                    5 -> "CCS"
                    else -> "AC"
                }
                if (!connectorTypes.contains(typeName)) {
                    connectorTypes.add(typeName)
                }
            }
        }
        
        return if (connectorTypes.isEmpty()) listOf("AC") else connectorTypes
    }
    
    private fun parseAmenities(amenitiesArray: JSONArray?): List<String> {
        val amenities = mutableListOf<String>()
        
        amenitiesArray?.let { array ->
            for (i in 0 until array.length()) {
                amenities.add(array.getString(i))
            }
        }
        
        return amenities
    }
    
    private fun calculateSlots(connectorsArray: JSONArray?): Pair<Int, Int> {
        var totalSlots = 0
        var availableSlots = 0
        
        connectorsArray?.let { array ->
            totalSlots = array.length()
            for (i in 0 until array.length()) {
                val connector = array.getJSONObject(i)
                val isAvailable = connector.optBoolean("isAvailable", false)
                if (isAvailable) {
                    availableSlots++
                }
            }
        }
        
        return Pair(maxOf(totalSlots, 1), availableSlots)
    }
    
    private fun calculateRating(): Double {
        // Generate a realistic rating between 3.0 and 5.0
        return 3.0 + (kotlin.random.Random.nextDouble() * 2.0)
    }
    
    private fun updateStationsList(stations: List<ChargingStationModel>) {
        stationsList.clear()
        stationsList.addAll(stations)
        stationsAdapter.notifyDataSetChanged()
        
        // Update map markers
        updateMapMarkers()
        
        // Show/hide empty view
        if (stations.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = if (isMapView) View.GONE else View.VISIBLE
        }
    }
    
    private fun updateMapMarkers() {
        // Clear existing markers (keep location overlay)
        val overlaysToKeep = mapView.overlays.filter { it is MyLocationNewOverlay }
        mapView.overlays.clear()
        mapView.overlays.addAll(overlaysToKeep)
        
        if (stationsList.isEmpty()) {
            mapView.invalidate()
            return
        }
        
        // Add markers for each station
        stationsList.forEach { station ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(station.latitude, station.longitude)
            marker.title = station.name
            marker.snippet = "${station.availableSlots}/${station.totalSlots} available"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set marker click listener
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                openStationDetails(station)
                true
            }
            
            mapView.overlays.add(marker)
        }
        
        // Center map to show all stations
        if (stationsList.size == 1) {
            val station = stationsList[0]
            mapView.controller.animateTo(GeoPoint(station.latitude, station.longitude))
        } else if (stationsList.size > 1) {
            // Calculate bounding box
            var minLat = stationsList[0].latitude
            var maxLat = stationsList[0].latitude
            var minLon = stationsList[0].longitude
            var maxLon = stationsList[0].longitude
            
            stationsList.forEach { station ->
                minLat = minOf(minLat, station.latitude)
                maxLat = maxOf(maxLat, station.latitude)
                minLon = minOf(minLon, station.longitude)
                maxLon = maxOf(maxLon, station.longitude)
            }
            
            val centerLat = (minLat + maxLat) / 2
            val centerLon = (minLon + maxLon) / 2
            mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
        }
        
        mapView.invalidate()
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
            // Enable location overlay
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
                    
                    // Center on user location if no stations loaded yet
                    if (stationsList.isEmpty()) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            location?.let {
                                currentLocation = it
                                val userLocation = GeoPoint(it.latitude, it.longitude)
                                mapView.controller.animateTo(userLocation)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun openStationDetails(station: ChargingStationModel) {
        val intent = Intent(this, StationDetailActivity::class.java)
        intent.putExtra(StationDetailActivity.EXTRA_STATION, station)
        startActivity(intent)
    }
    
    private fun showFilterDialog() {
        // TODO: Implement filter dialog
        Toast.makeText(this, "Filter feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showLoading(show: Boolean) {
        swipeRefresh.isRefreshing = show
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}