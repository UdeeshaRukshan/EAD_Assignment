package com.example.evmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChargingStationsAdapter(
    private val stations: List<ChargingStationModel>,
    private val onStationClick: (ChargingStationModel) -> Unit
) : RecyclerView.Adapter<ChargingStationsAdapter.StationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position])
    }

    override fun getItemCount(): Int = stations.size

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val ivStationIcon: ImageView = itemView.findViewById(R.id.ivStationIcon)
        private val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvConnectorTypes: TextView = itemView.findViewById(R.id.tvConnectorTypes)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tvAvailability)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)

        fun bind(station: ChargingStationModel) {
            tvStationName.text = station.name
            tvAddress.text = station.address
            tvStatus.text = station.getStatusText()
            tvConnectorTypes.text = station.getConnectorTypesText()
            tvAvailability.text = "${station.availableSlots}/${station.totalSlots} slots"
            tvPrice.text = "Rs. ${String.format("%.2f", station.pricePerKwh)}/kWh"
            tvRating.text = String.format("%.1f ★", station.rating)

            // Set status color
            val statusColor = ContextCompat.getColor(itemView.context, station.getStatusColor())
            tvStatus.setTextColor(statusColor)

            // Set station icon based on status
            val iconRes = when {
                !station.isActive -> R.drawable.ic_station_inactive
                station.availableSlots > 0 -> R.drawable.ic_station_available
                else -> R.drawable.ic_station_occupied
            }
            ivStationIcon.setImageResource(iconRes)

            // Set distance (this would be calculated based on user location)
            tvDistance.text = "1.2 km" // Placeholder

            cardView.setOnClickListener {
                onStationClick(station)
            }
        }
    }
}