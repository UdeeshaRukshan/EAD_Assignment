package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_TIME_OUT = 3000L // 3 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent
        setupFullScreen()
        
        setContentView(R.layout.activity_splash)
        
        // Navigate to LoginActivity after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLogin()
        }, SPLASH_TIME_OUT)
    }
    
    private fun setupFullScreen() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        
        // Add smooth transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // Disable back button on splash screen
        // Do nothing - intentionally not calling super.onBackPressed()
    }
}