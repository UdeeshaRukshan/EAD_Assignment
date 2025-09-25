package com.evcharging.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.threeten.bp.zone.ZoneRulesProvider
import com.jakewharton.threetenabp.AndroidThreeTen

/**
 * Application class for EV Charging Station App
 * Initializes Hilt dependency injection and other global configurations
 */
@HiltAndroidApp
class EVChargingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ThreeTenABP for date/time handling
        AndroidThreeTen.init(this)
        
        // Initialize any other global configurations here
        initializeAppConfigurations()
    }

    private fun initializeAppConfigurations() {
        // Add any global app initialization here
        // e.g., Crash reporting, Analytics, etc.
    }
}