package com.example.evmobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.evmobile.models.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CreateBookingActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var actvStation: MaterialAutoCompleteTextView
    private lateinit var actvConnector: MaterialAutoCompleteTextView
    private lateinit var tvStationDetails: TextView
    private lateinit var cardConnector: CardView
    private lateinit var cardCostEstimation: CardView
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var tvEstimatedCost: TextView
    private lateinit var tvEstimatedDuration: TextView
    private lateinit var btnCreateBooking: Button
    private lateinit var loadingOverlay: FrameLayout
    
    // Data
    private var availableStations = mutableListOf<StationForBooking>()
    private var selectedStation: StationForBooking? = null
    private var selectedConnector: Connector? = null
    private var startDateTime: Calendar? = null
    private var endDateTime: Calendar? = null
    
    // Date/Time formatters
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_booking)
        
        initViews()
        setupToolbar()
        loadStations()
        setupClickListeners()
        setupFormValidation()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        actvStation = findViewById(R.id.actvStation)
        actvConnector = findViewById(R.id.actvConnector)
        tvStationDetails = findViewById(R.id.tvStationDetails)
        cardConnector = findViewById(R.id.cardConnector)
        cardCostEstimation = findViewById(R.id.cardCostEstimation)
        etStartDate = findViewById(R.id.etStartDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndDate = findViewById(R.id.etEndDate)
        etEndTime = findViewById(R.id.etEndTime)
        etNotes = findViewById(R.id.etNotes)
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost)
        tvEstimatedDuration = findViewById(R.id.tvEstimatedDuration)
        btnCreateBooking = findViewById(R.id.btnCreateBooking)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Create Booking"
        }
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun loadStations() {
        availableStations.clear()
        availableStations.addAll(CreateBookingDummyData.generateStationsForBooking())
        
        val stationNames = availableStations.map { "${it.name} (${it.distanceKm} km)" }
        val stationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stationNames)
        actvStation.setAdapter(stationAdapter)
    }
    
    private fun setupClickListeners() {
        // Station selection
        actvStation.setOnItemClickListener { _, _, position, _ ->
            selectedStation = availableStations[position]
            updateStationDetails()
            loadConnectors()
        }
        
        // Connector selection
        actvConnector.setOnItemClickListener { _, _, position, _ ->
            selectedStation?.let { station ->
                selectedConnector = station.connectors[position]
                updateCostEstimation()
            }
        }
        
        // Date and time pickers
        etStartDate.setOnClickListener { showDatePicker(true) }
        etStartTime.setOnClickListener { showTimePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }
        etEndTime.setOnClickListener { showTimePicker(false) }
        
        // Create booking button
        btnCreateBooking.setOnClickListener { createBooking() }
    }
    
    private fun updateStationDetails() {
        selectedStation?.let { station ->
            tvStationDetails.text = "${station.location}\n${station.connectors.size} connectors available"
            tvStationDetails.visibility = View.VISIBLE
        }
    }
    
    private fun loadConnectors() {
        selectedStation?.let { station ->
            val availableConnectors = station.connectors.filter { it.isAvailable }
            
            if (availableConnectors.isNotEmpty()) {
                val connectorNames = availableConnectors.map { "${it.type} - ${it.power} (₹${it.pricePerKwh}/kWh)" }
                val connectorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, connectorNames)
                actvConnector.setAdapter(connectorAdapter)
                
                cardConnector.visibility = View.VISIBLE
                actvConnector.text.clear()
                selectedConnector = null
            } else {
                showToast("No available connectors at this station")
                cardConnector.visibility = View.GONE
            }
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        // Set minimum date to today
        val minDate = Calendar.getInstance()
        
        // Set default date based on selection
        if (isStartDate && startDateTime != null) {
            calendar.time = startDateTime!!.time
        } else if (!isStartDate && endDateTime != null) {
            calendar.time = endDateTime!!.time
        } else if (!isStartDate && startDateTime != null) {
            // Default end date to same as start date
            calendar.time = startDateTime!!.time
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                
                if (isStartDate) {
                    if (startDateTime == null) startDateTime = Calendar.getInstance()
                    startDateTime!!.set(Calendar.YEAR, year)
                    startDateTime!!.set(Calendar.MONTH, month)
                    startDateTime!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    etStartDate.setText(dateFormat.format(startDateTime!!.time))
                } else {
                    if (endDateTime == null) endDateTime = Calendar.getInstance()
                    endDateTime!!.set(Calendar.YEAR, year)
                    endDateTime!!.set(Calendar.MONTH, month)
                    endDateTime!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    etEndDate.setText(dateFormat.format(endDateTime!!.time))
                }
                
                updateCostEstimation()
                validateForm()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.show()
    }
    
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        
        // Set default time based on selection
        if (isStartTime && startDateTime != null) {
            calendar.time = startDateTime!!.time
        } else if (!isStartTime && endDateTime != null) {
            calendar.time = endDateTime!!.time
        } else if (!isStartTime && startDateTime != null) {
            // Default end time to 2 hours after start time
            calendar.time = startDateTime!!.time
            calendar.add(Calendar.HOUR_OF_DAY, 2)
        }
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                if (isStartTime) {
                    if (startDateTime == null) startDateTime = Calendar.getInstance()
                    startDateTime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    startDateTime!!.set(Calendar.MINUTE, minute)
                    etStartTime.setText(timeFormat.format(startDateTime!!.time))
                } else {
                    if (endDateTime == null) endDateTime = Calendar.getInstance()
                    endDateTime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    endDateTime!!.set(Calendar.MINUTE, minute)
                    etEndTime.setText(timeFormat.format(endDateTime!!.time))
                }
                
                updateCostEstimation()
                validateForm()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24 hour format
        )
        
        timePickerDialog.show()
    }
    
    private fun updateCostEstimation() {
        if (selectedConnector != null && startDateTime != null && endDateTime != null) {
            val durationHours = (endDateTime!!.timeInMillis - startDateTime!!.timeInMillis) / (1000 * 60 * 60)
            
            if (durationHours > 0) {
                // Estimate energy consumption based on connector power and duration
                val connectorPowerKw = when {
                    selectedConnector!!.power.contains("7") -> 7.0
                    selectedConnector!!.power.contains("22") -> 22.0
                    selectedConnector!!.power.contains("50") -> 50.0
                    selectedConnector!!.power.contains("150") -> 150.0
                    else -> 7.0
                }
                
                val estimatedEnergyKwh = minOf(connectorPowerKw * durationHours, 100.0) // Cap at 100 kWh
                val estimatedCost = estimatedEnergyKwh * selectedConnector!!.pricePerKwh
                
                tvEstimatedCost.text = "₹${String.format("%.2f", estimatedCost)}"
                tvEstimatedDuration.text = "${durationHours}h ${String.format("%.1f", estimatedEnergyKwh)} kWh"
                
                cardCostEstimation.visibility = View.VISIBLE
            } else {
                cardCostEstimation.visibility = View.GONE
            }
        } else {
            cardCostEstimation.visibility = View.GONE
        }
    }
    
    private fun setupFormValidation() {
        // Add text change listeners for validation
        validateForm()
    }
    
    private fun validateForm(): Boolean {
        val isValid = selectedStation != null &&
                     selectedConnector != null &&
                     startDateTime != null &&
                     endDateTime != null &&
                     etStartDate.text?.isNotEmpty() == true &&
                     etStartTime.text?.isNotEmpty() == true &&
                     etEndDate.text?.isNotEmpty() == true &&
                     etEndTime.text?.isNotEmpty() == true &&
                     (endDateTime?.timeInMillis ?: 0) > (startDateTime?.timeInMillis ?: 0)
        
        btnCreateBooking.isEnabled = isValid
        return isValid
    }
    
    private fun createBooking() {
        if (!validateForm()) {
            showToast("Please fill all required fields")
            return
        }
        
        showLoading(true)
        
        // Simulate API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get user session
                val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
                val userId = prefs.getString("user_id", "dummy_user_123") ?: "dummy_user_123"
                
                // Prepare request
                val request = CreateBookingRequest(
                    userId = userId,
                    stationId = selectedStation!!.id,
                    connectorId = selectedConnector!!.id,
                    startTime = isoFormat.format(startDateTime!!.time),
                    endTime = isoFormat.format(endDateTime!!.time),
                    notes = etNotes.text?.toString()?.takeIf { it.isNotBlank() }
                )
                
                // Simulate network delay
                delay(2000)
                
                // Simulate API call
                val response = CreateBookingDummyData.simulateCreateBooking(request)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showBookingSuccess(response)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showToast("Failed to create booking. Please try again.")
                }
            }
        }
    }
    
    private fun showBookingSuccess(response: CreateBookingResponse) {
        val message = """
            Booking created successfully!
            
            Booking ID: ${response.id}
            Station: ${selectedStation!!.name}
            Date: ${dateFormat.format(startDateTime!!.time)}
            Time: ${timeFormat.format(startDateTime!!.time)} - ${timeFormat.format(endDateTime!!.time)}
            Status: Pending Confirmation
            
            You will receive a QR code once confirmed.
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Booking Confirmed")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}