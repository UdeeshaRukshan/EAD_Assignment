package com.example.evmobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evmobile.R
import com.example.evmobile.models.ChargingHistory
import com.example.evmobile.models.ChargingStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChargingHistoryAdapter(
    private var historyList: List<ChargingHistory>,
    private val onViewDetailsClick: (ChargingHistory) -> Unit
) : RecyclerView.Adapter<ChargingHistoryAdapter.HistoryViewHolder>() {
    
    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        val tvChargingDate: TextView = itemView.findViewById(R.id.tvChargingDate)
        val tvSessionStatus: TextView = itemView.findViewById(R.id.tvSessionStatus)
        val tvEnergyCharged: TextView = itemView.findViewById(R.id.tvEnergyCharged)
        val tvChargingDuration: TextView = itemView.findViewById(R.id.tvChargingDuration)
        val tvChargingCost: TextView = itemView.findViewById(R.id.tvChargingCost)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_history, parent, false)
        return HistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        
        holder.tvStationName.text = history.stationName
        
        // Format date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
        holder.tvChargingDate.text = dateFormat.format(history.chargingDate)
        
        holder.tvSessionStatus.text = history.status.displayName
        holder.tvEnergyCharged.text = "${String.format("%.1f", history.energyCharged)} kWh"
        holder.tvChargingCost.text = "$${String.format("%.2f", history.totalCost)}"
        
        // Calculate duration
        val durationMillis = history.endTime.time - history.startTime.time
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        
        holder.tvChargingDuration.text = when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
        
        // Set status background based on charging status
        val statusBackgroundRes = when (history.status) {
            ChargingStatus.COMPLETED -> R.drawable.status_completed_bg
            ChargingStatus.INTERRUPTED -> R.drawable.status_interrupted_bg
            ChargingStatus.FAILED -> R.drawable.status_failed_bg
        }
        holder.tvSessionStatus.setBackgroundResource(statusBackgroundRes)
        
        // Set click listener
        holder.btnViewDetails.setOnClickListener { onViewDetailsClick(history) }
    }
    
    override fun getItemCount(): Int = historyList.size
    
    fun updateHistory(newHistory: List<ChargingHistory>) {
        historyList = newHistory
        notifyDataSetChanged()
    }
}