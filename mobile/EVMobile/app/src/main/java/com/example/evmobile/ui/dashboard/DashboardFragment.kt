package com.example.evmobile.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.evmobile.R
import com.example.evmobile.databinding.FragmentDashboardBinding
import com.example.evmobile.domain.model.ChargingStation
import com.example.evmobile.ui.booking.BookingActivity
import com.example.evmobile.ui.station.StationDetailActivity

/**
 * Dashboard fragment - Home screen with:
 * - Reservation counts
 * - Nearby charging stations map
 * - Quick actions
 */
class DashboardFragment : Fragment(), OnMapReadyCallback {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    
    private lateinit var nearbyStationsAdapter: NearbyStationsAdapter
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupLocationClient()
        setupUI()
        setupMap()
        setupRecyclerView()
        setupObservers()
        
        // Load dashboard data
        viewModel.loadDashboardData()
        checkLocationPermissionAndLoadNearbyStations()
    }
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    
    private fun setupUI() {
        binding.apply {
            // Refresh functionality
            swipeRefresh.setOnRefreshListener {
                viewModel.refreshDashboardData()
                loadNearbyStations()
            }
            
            // Quick action buttons
            btnQuickBooking.setOnClickListener {
                // Navigate to booking screen
                startActivity(Intent(requireContext(), BookingActivity::class.java))
            }
            
            btnViewAllStations.setOnClickListener {
                // Switch to map tab
                (requireActivity() as? MainActivity)?.let { activity ->
                    // Switch to map fragment
                }
            }
            
            btnMyBookings.setOnClickListener {
                // Switch to bookings tab
                (requireActivity() as? MainActivity)?.let { activity ->
                    // Switch to bookings fragment
                }
            }
        }
    }
    
    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }
    
    private fun setupRecyclerView() {
        nearbyStationsAdapter = NearbyStationsAdapter { station ->
            navigateToStationDetail(station)
        }
        
        binding.rvNearbyStations.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = nearbyStationsAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.dashboardData.observe(viewLifecycleOwner) { dashboardData ->
            dashboardData?.let { data ->
                updateDashboardUI(data)
            }
        }
        
        viewModel.nearbyStations.observe(viewLifecycleOwner) { stations ->
            updateNearbyStations(stations)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            
            // Show/hide loading
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentLayout.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Show error message
                binding.tvErrorMessage.text = it
                binding.tvErrorMessage.visibility = View.VISIBLE
            }
        }
        
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvWelcomeMessage.text = "Welcome back, ${user.firstName}!"
                
                // Show operator features if user is an operator
                if (user.role == com.example.evmobile.domain.model.UserRole.Operator) {
                    binding.operatorSection.visibility = View.VISIBLE
                    setupOperatorFeatures()
                } else {
                    binding.operatorSection.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updateDashboardUI(data: com.example.evmobile.domain.model.DashboardData) {
        binding.apply {
            // Update reservation counts
            tvPendingCount.text = data.pendingReservations.toString()
            tvApprovedCount.text = data.approvedReservations.toString()
            
            // Update recent bookings if any
            if (data.recentBookings.isNotEmpty()) {
                tvRecentBookingsTitle.visibility = View.VISIBLE
                // Setup recent bookings recycler view if needed
            }
        }
    }
    
    private fun updateNearbyStations(stations: List<ChargingStation>) {
        nearbyStationsAdapter.submitList(stations)
        
        // Update map markers
        googleMap?.let { map ->
            map.clear()
            stations.forEach { station ->
                val position = LatLng(station.location.latitude, station.location.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(station.name)
                        .snippet("${station.connectors.count { it.isAvailable }}/${station.connectors.size} available")
                )
            }
            
            // Move camera to show all stations
            if (stations.isNotEmpty()) {
                val firstStation = stations.first()
                val position = LatLng(firstStation.location.latitude, firstStation.location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
            }
        }
    }
    
    private fun setupOperatorFeatures() {
        binding.apply {
            btnScanQR.setOnClickListener {
                // Navigate to QR scanner
                startActivity(Intent(requireContext(), com.example.evmobile.ui.qr.QRScannerActivity::class.java))
            }
            
            btnManageBookings.setOnClickListener {
                // Navigate to operator bookings management
            }
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Enable location if permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        }
        
        // Set map click listener
        map.setOnMarkerClickListener { marker ->
            // Handle marker click - show station details
            true
        }
    }
    
    private fun checkLocationPermissionAndLoadNearbyStations() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    loadNearbyStations()
                    
                    // Update map camera
                    googleMap?.let { map ->
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                }
            }
        }
    }
    
    private fun loadNearbyStations() {
        currentLocation?.let { location ->
            viewModel.loadNearbyStations(location.latitude, location.longitude)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    // Permission denied - load default data
                    viewModel.loadNearbyStations(6.9271, 79.8612) // Default to Colombo
                }
            }
        }
    }
    
    private fun navigateToStationDetail(station: ChargingStation) {
        val intent = Intent(requireContext(), StationDetailActivity::class.java)
        intent.putExtra("station", station)
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}