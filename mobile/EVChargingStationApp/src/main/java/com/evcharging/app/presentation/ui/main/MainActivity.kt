package com.evcharging.app.presentation.ui.main

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.evcharging.app.R
import com.evcharging.app.databinding.ActivityMainBinding
import com.evcharging.app.presentation.ui.dashboard.DashboardFragment
import com.evcharging.app.presentation.ui.booking.BookingsFragment  
import com.evcharging.app.presentation.ui.map.MapFragment
import com.evcharging.app.presentation.ui.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity that hosts the bottom navigation and main fragments
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        setupFragments()
        observeViewModel()
        
        // Load initial data
        viewModel.loadUserData()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    switchFragment(DashboardFragment(), "dashboard")
                    true
                }
                R.id.nav_bookings -> {
                    switchFragment(BookingsFragment(), "bookings") 
                    true
                }
                R.id.nav_map -> {
                    switchFragment(MapFragment(), "map")
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(ProfileFragment(), "profile")
                    true
                }
                else -> false
            }
        }
        
        // Set default selection
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }
    
    private fun setupFragments() {
        // Load dashboard fragment by default
        switchFragment(DashboardFragment(), "dashboard")
    }
    
    private fun switchFragment(fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        
        supportFragmentManager.beginTransaction().apply {
            // Hide all existing fragments
            supportFragmentManager.fragments.forEach { hide(it) }
            
            if (existingFragment != null) {
                show(existingFragment)
            } else {
                add(R.id.fragment_container, fragment, tag)
            }
        }.commit()
    }
    
    private fun observeViewModel() {
        viewModel.user.observe(this) { user ->
            user?.let {
                // Update UI based on user data if needed
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator if needed
        }
        
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                // Show error message
            }
        }
    }
}