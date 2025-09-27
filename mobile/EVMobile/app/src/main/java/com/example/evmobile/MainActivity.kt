package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupNavigationCards()
    }
    
    private fun setupNavigationCards() {
        findViewById<CardView>(R.id.cardStations).setOnClickListener {
            showToast("Charging Stations")
            // TODO: Navigate to stations list
        }
        
        findViewById<CardView>(R.id.cardBookings).setOnClickListener {
            showToast("My Bookings")
            // TODO: Navigate to bookings
        }
        
        findViewById<CardView>(R.id.cardMap).setOnClickListener {
            showToast("Station Map")
            // TODO: Navigate to map
        }
        
        findViewById<CardView>(R.id.cardProfile).setOnClickListener {
            showToast("Profile")
            // TODO: Navigate to profile
        }
        
        findViewById<CardView>(R.id.cardQRScanner).setOnClickListener {
            showToast("QR Scanner")
            // TODO: Navigate to QR scanner
        }
        
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            showToast("Settings")
            // TODO: Navigate to settings
        }
    }
    
    private fun showToast(feature: String) {
        Toast.makeText(this, "$feature feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        // Show exit confirmation or minimize app
        super.onBackPressed()
    }
}