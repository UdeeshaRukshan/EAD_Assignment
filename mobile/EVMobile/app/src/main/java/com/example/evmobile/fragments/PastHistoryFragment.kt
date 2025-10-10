package com.example.evmobile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evmobile.R
import com.example.evmobile.adapters.ChargingHistoryAdapter
import com.example.evmobile.models.ChargingHistory
import com.example.evmobile.models.ChargingStatus
import com.example.evmobile.models.DummyDataGenerator

class PastHistoryFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var cardStatistics: CardView
    private lateinit var tvTotalSessions: TextView
    private lateinit var tvTotalEnergy: TextView
    private lateinit var tvTotalCost: TextView
    private lateinit var adapter: ChargingHistoryAdapter
    
    private var historyList = mutableListOf<ChargingHistory>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_past_history, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        loadChargingHistory()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        cardStatistics = view.findViewById(R.id.cardStatistics)
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions)
        tvTotalEnergy = view.findViewById(R.id.tvTotalEnergy)
        tvTotalCost = view.findViewById(R.id.tvTotalCost)
    }
    
    private fun setupRecyclerView() {
        adapter = ChargingHistoryAdapter(
            historyList = historyList,
            onViewDetailsClick = { history ->
                showChargingDetails(history)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PastHistoryFragment.adapter
        }
    }
    
    private fun loadChargingHistory() {
        // Load dummy data
        historyList.clear()
        historyList.addAll(DummyDataGenerator.generateChargingHistory())
        
        updateUI()
        updateStatistics()
    }
    
    private fun updateUI() {
        if (historyList.isEmpty()) {
            recyclerView.visibility = View.GONE
            cardStatistics.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            cardStatistics.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.updateHistory(historyList)
        }
    }
    
    private fun updateStatistics() {
        if (historyList.isNotEmpty()) {
            // Calculate statistics
            val completedSessions = historyList.filter { it.status == ChargingStatus.COMPLETED }
            val totalSessions = historyList.size
            val totalEnergy = historyList.sumOf { it.energyCharged }
            val totalCost = historyList.sumOf { it.totalCost }
            
            // Update UI
            tvTotalSessions.text = totalSessions.toString()
            tvTotalEnergy.text = String.format("%.1f", totalEnergy)
            tvTotalCost.text = "$${String.format("%.2f", totalCost)}"
        } else {
            // Default values for empty state
            tvTotalSessions.text = "0"
            tvTotalEnergy.text = "0.0"
            tvTotalCost.text = "$0.00"
        }
    }
    
    private fun showChargingDetails(history: ChargingHistory) {
        // Create a detailed view of the charging session
        val message = buildString {
            appendLine("Station: ${history.stationName}")
            appendLine("Location: ${history.stationLocation}")
            appendLine("Charging Type: ${history.chargingType}")
            appendLine("Energy Charged: ${String.format("%.1f", history.energyCharged)} kWh")
            appendLine("Total Cost: $${String.format("%.2f", history.totalCost)}")
            appendLine("Status: ${history.status.displayName}")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Charging Session Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}