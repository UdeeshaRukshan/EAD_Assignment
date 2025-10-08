package com.example.evmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.*
import com.google.android.material.button.MaterialButton

class StationDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var station: ChargingStationModel

    companion object {
        const val EXTRA_STATION = "extra_station"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_detail)

        // Get station from intent
        station = intent.getParcelableExtra(EXTRA_STATION) ?: run {
            finish()
            return
        }

        setupUI()
        setupMap()
        setupClickListeners()
    }

    private fun setupUI() {
        // Station Info
        findViewById<TextView>(R.id.tvStationName).text = station.name
        findViewById<TextView>(R.id.tvStationAddress).text = station.address
        findViewById<TextView>(R.id.tvDistance).text = "1.2 km" // Placeholder for distance
        findViewById<TextView>(R.id.tvRating).text = "${String.format("%.1f", station.rating)} ★"

        // Status
        findViewById<TextView>(R.id.tvStatus).apply {
            text = station.getStatusText()
            val statusColor = station.getStatusColor()
            setTextColor(ContextCompat.getColor(this@StationDetailActivity, statusColor))
        }

        // Availability
        findViewById<TextView>(R.id.tvAvailableSlots).text = "${station.availableSlots}/${station.totalSlots} slots available"

        // Connector Types
        findViewById<TextView>(R.id.tvConnectorTypes).text = station.getConnectorTypesText()

        // Pricing
        findViewById<TextView>(R.id.tvPricing).text = "Rs. ${String.format("%.2f", station.pricePerKwh)}/kWh"

        // Operating Hours
        findViewById<TextView>(R.id.tvOperatingHours).text = if (station.isActive) "24/7" else "Closed"

        // Amenities (placeholder)
        val amenitiesLayout = findViewById<LinearLayout>(R.id.layoutAmenities)
        val amenitiesText = findViewById<TextView>(R.id.tvAmenities)
        
        val amenities = listOf("WiFi", "Parking", "Restaurant") // Placeholder
        if (amenities.isNotEmpty()) {
            amenitiesText.text = amenities.joinToString(" • ")
            amenitiesLayout.visibility = android.view.View.VISIBLE
        } else {
            amenitiesLayout.visibility = android.view.View.GONE
        }

        // Description (placeholder)
        val descriptionLayout = findViewById<LinearLayout>(R.id.layoutDescription)
        val descriptionText = findViewById<TextView>(R.id.tvDescription)
        
        val description = "Fast charging station with modern facilities and convenient location."
        if (description.isNotEmpty()) {
            descriptionText.text = description
            descriptionLayout.visibility = android.view.View.VISIBLE
        } else {
            descriptionLayout.visibility = android.view.View.GONE
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupClickListeners() {
        // Book Now Button
        findViewById<MaterialButton>(R.id.btnBookNow).setOnClickListener {
            if (station.availableSlots > 0) {
                showBookingDialog()
            } else {
                showNoSlotsDialog()
            }
        }

        // Get Directions Button
        findViewById<MaterialButton>(R.id.btnGetDirections).setOnClickListener {
            openDirections()
        }

        // Call Button
        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener {
            // Placeholder contact number
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+94771234567")
            }
            startActivity(intent)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val stationLocation = LatLng(station.latitude, station.longitude)
        
        // Add marker for the station
        googleMap.addMarker(
            MarkerOptions()
                .position(stationLocation)
                .title(station.name)
                .snippet(station.address)
        )

        // Move camera to station location
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(stationLocation, 15f)
        )

        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true
    }

    private fun showBookingDialog() {
        // For now, show a placeholder dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Book Charging Slot")
            .setMessage("Booking feature will be implemented soon. This will allow you to reserve a charging slot at ${station.name}.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showNoSlotsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No Available Slots")
            .setMessage("All charging slots at this station are currently occupied. Please try again later or choose a different station.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Find Nearby") { _, _ ->
                finish()
            }
            .show()
    }

    private fun openDirections() {
        val uri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${station.name})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to web browser
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}