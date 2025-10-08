package com.example.evmobile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evmobile.R
import com.example.evmobile.adapters.UpcomingBookingsAdapter
import com.example.evmobile.models.Booking
import com.example.evmobile.models.DummyDataGenerator
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
                showToast("Modify booking for ${booking.stationName}")
                // TODO: Navigate to modify booking screen
            },
            onCancelClick = { booking ->
                showCancelConfirmation(booking)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UpcomingBookingsFragment.adapter
        }
    }
    
    private fun setupClickListeners() {
        fabNewBooking.setOnClickListener {
            showToast("Navigate to charging stations to book")
            // TODO: Navigate to charging stations list or map
        }
    }
    
    private fun loadBookings() {
        // Load dummy data
        bookingsList.clear()
        bookingsList.addAll(DummyDataGenerator.generateUpcomingBookings())
        
        updateUI()
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
        // Remove booking from list
        bookingsList.remove(booking)
        updateUI()
        showToast("Booking cancelled successfully")
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}