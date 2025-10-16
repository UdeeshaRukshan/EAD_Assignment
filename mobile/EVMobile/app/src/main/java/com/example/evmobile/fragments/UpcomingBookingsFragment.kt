package com.example.evmobile.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evmobile.CreateBookingActivity
import com.example.evmobile.QRCodeActivity
import com.example.evmobile.R
import com.example.evmobile.adapters.UpcomingBookingsAdapter
import com.example.evmobile.models.Booking
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpcomingBookingsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabNewBooking: FloatingActionButton
    private lateinit var adapter: UpcomingBookingsAdapter
    
    private var bookingsList = mutableListOf<Booking>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upcoming_bookings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadBookings()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewBookings)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        fabNewBooking = view.findViewById(R.id.fabNewBooking)
    }
    
    private fun setupRecyclerView() {
        adapter = UpcomingBookingsAdapter(
            bookings = bookingsList,
            onModifyClick = { booking ->
                val validationResult = com.example.evmobile.utils.BookingValidationUtils.validateBookingModification(booking)
                if (validationResult.isValid) {
                    showToast("Modify booking for ${booking.stationName}")
                    // TODO: Navigate to modify booking screen
                } else {
                    showToast(validationResult.message)
                }
            },
            onCancelClick = { booking ->
                val validationResult = com.example.evmobile.utils.BookingValidationUtils.validateBookingCancellation(booking)
                if (validationResult.isValid) {
                    showCancelConfirmation(booking)
                } else {
                    showToast(validationResult.message)
                }
            },
            onQRCodeClick = { booking ->
                openQRCodeActivity(booking)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UpcomingBookingsFragment.adapter
        }
    }
    
    private fun setupClickListeners() {
        fabNewBooking.setOnClickListener {
            val intent = Intent(requireContext(), CreateBookingActivity::class.java)
            startActivityForResult(intent, CREATE_BOOKING_REQUEST_CODE)
            // Add slide animation
            requireActivity().overridePendingTransition(
                com.example.evmobile.R.anim.slide_in_right,
                com.example.evmobile.R.anim.slide_out_left
            )
        }
    }
    
    companion object {
        private const val CREATE_BOOKING_REQUEST_CODE = 1001
    }
    
    private fun loadBookings() {
        // Show loading state
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        
        // Debug authentication state
        val authState = com.example.evmobile.utils.AuthDebugUtils.debugAuthState(requireContext())
        
        val repository = com.example.evmobile.data.repository.BookingRepository(requireContext())
        
        // Check if user is authenticated first
        if (!repository.isUserAuthenticated()) {
            android.util.Log.w("UpcomingBookingsFragment", "User not authenticated!")
            showAuthenticationError()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = repository.getUserBookings()
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val (upcomingBookings, _) = result.getOrNull() ?: Pair(emptyList(), emptyList())
                        bookingsList.clear()
                        bookingsList.addAll(upcomingBookings)
                        
                        updateUI()
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        showToast("Failed to load bookings: $errorMessage")
                        
                        // If authentication error, show login prompt
                        if (errorMessage.contains("authentication", true) || errorMessage.contains("token", true)) {
                            showAuthenticationError()
                        } else {
                            updateUI()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error loading bookings: ${e.message}")
                    updateUI()
                }
            }
        }
    }
    
    private fun updateUI() {
        if (bookingsList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.updateBookings(bookingsList)
        }
    }
    
    private fun showCancelConfirmation(booking: Booking) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel your booking at ${booking.stationName}?")
            .setPositiveButton("Cancel Booking") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("Keep Booking", null)
            .show()
    }
    
    private fun cancelBooking(booking: Booking) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.example.evmobile.data.repository.BookingRepository(requireContext())
                val result = repository.cancelBooking(booking.id)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        // Remove booking from local list
                        bookingsList.remove(booking)
                        updateUI()
                        showToast("Booking cancelled successfully")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        showToast("Failed to cancel booking: $error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error cancelling booking: ${e.message}")
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showAuthenticationError() {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        
        // Show authentication error dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Authentication Required")
            .setMessage("Please log in to view your bookings.")
            .setPositiveButton("Login") { _, _ ->
                // Navigate to login screen
                val intent = Intent(requireContext(), com.example.evmobile.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun openQRCodeActivity(booking: Booking) {
        // Only show QR code for approved bookings
        if (booking.qrCode.isNullOrBlank()) {
            showToast("QR code is not available. Booking must be approved first.")
            return
        }
        
        val intent = Intent(requireContext(), QRCodeActivity::class.java).apply {
            putExtra(QRCodeActivity.EXTRA_BOOKING_ID, booking.id)
            putExtra(QRCodeActivity.EXTRA_STATION_NAME, booking.stationName)
            putExtra(QRCodeActivity.EXTRA_BOOKING_DATE_TIME, booking.bookingDateTime.time)
            putExtra(QRCodeActivity.EXTRA_QR_CODE, booking.qrCode)
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_BOOKING_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            // Refresh the bookings list
            loadBookings()
            showToast("Booking created successfully!")
        }
    }
}