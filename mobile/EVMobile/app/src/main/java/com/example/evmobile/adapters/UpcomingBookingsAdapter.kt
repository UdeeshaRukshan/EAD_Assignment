package com.example.evmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evmobile.R
import com.example.evmobile.models.Booking
import com.example.evmobile.models.BookingStatus
import java.text.SimpleDateFormat
import java.util.Locale

class UpcomingBookingsAdapter(
    private var bookings: List<Booking>,
    private val onModifyClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit,
    private val onQRCodeClick: (Booking) -> Unit
) : RecyclerView.Adapter<UpcomingBookingsAdapter.BookingViewHolder>() {
    
    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        val tvStationLocation: TextView = itemView.findViewById(R.id.tvStationLocation)
        val tvBookingStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
        val tvBookingDateTime: TextView = itemView.findViewById(R.id.tvBookingDateTime)
        val tvBookingDuration: TextView = itemView.findViewById(R.id.tvBookingDuration)
        val btnQRCode: Button = itemView.findViewById(R.id.btnQRCode)
        val btnModifyBooking: Button = itemView.findViewById(R.id.btnModifyBooking)
        val btnCancelBooking: Button = itemView.findViewById(R.id.btnCancelBooking)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_booking, parent, false)
        return BookingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        
        holder.tvStationName.text = booking.stationName
        holder.tvStationLocation.text = booking.stationLocation
        holder.tvBookingStatus.text = booking.status.displayName
        
        // Format date and time
        val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        holder.tvBookingDateTime.text = dateFormat.format(booking.bookingDateTime)
        
        // Format duration
        val hours = booking.duration / 60
        val minutes = booking.duration % 60
        holder.tvBookingDuration.text = when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours} hour${if (hours > 1) "s" else ""}"
            else -> "${minutes} minutes"
        }
        
        // Set status background based on booking status
        val statusBackgroundRes = when (booking.status) {
            BookingStatus.CONFIRMED -> R.drawable.status_confirmed_bg
            BookingStatus.PENDING -> R.drawable.status_pending_bg
            BookingStatus.IN_PROGRESS -> R.drawable.status_in_progress_bg
            BookingStatus.APPROVED -> R.drawable.status_approved_bg
            else -> R.drawable.status_confirmed_bg
        }
        holder.tvBookingStatus.setBackgroundResource(statusBackgroundRes)
        
        // Show QR code button only for approved bookings
        if (booking.status == BookingStatus.APPROVED) {
            holder.btnQRCode.visibility = View.VISIBLE
            holder.btnQRCode.setOnClickListener { onQRCodeClick(booking) }
        } else {
            holder.btnQRCode.visibility = View.GONE
        }
        
        // Set click listeners
        holder.btnModifyBooking.setOnClickListener { onModifyClick(booking) }
        holder.btnCancelBooking.setOnClickListener { onCancelClick(booking) }
    }
    
    override fun getItemCount(): Int = bookings.size
    
    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}