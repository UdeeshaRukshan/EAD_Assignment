package com.example.evmobile

import android.app.Application

/**
 * Application class for EV Charging Station App
 * Initializes global configurations
 */
class EVChargingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
        initializeAppConfigurations()
    }

    private fun initializeAppConfigurations() {
        // Add any global app initialization here
        // e.g., Crash reporting, Analytics, etc.
    }
}