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
        setupDashboardButton()
    }
    
    private fun setupNavigationCards() {
        findViewById<CardView>(R.id.cardStations).setOnClickListener {
            showToast("Charging Stations")
            // TODO: Navigate to stations list
        }
        
        findViewById<CardView>(R.id.cardBookings).setOnClickListener {
            navigateToBookings()
        }
        
        findViewById<CardView>(R.id.cardMap).setOnClickListener {
            showToast("Station Map")
            // TODO: Navigate to map
        }
        
        findViewById<CardView>(R.id.cardProfile).setOnClickListener {
            showToast("Profile")
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<CardView>(R.id.cardQRScanner).setOnClickListener {
            navigateToQRScanner()
        }
        
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            showToast("Settings")
            // TODO: Navigate to settings
        }
        
        // Add Dashboard navigation for testing
        findViewById<CardView>(R.id.cardStations).setOnLongClickListener {
            navigateToDashboard()
            true
        }
    }
    
    private fun showToast(feature: String) {
        Toast.makeText(this, "$feature feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupDashboardButton() {
        findViewById<android.widget.Button>(R.id.btnViewDashboard).setOnClickListener {
            navigateToDashboard()
        }
    }
    
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun navigateToBookings() {
        val intent = Intent(this, BookingActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun navigateToQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

}