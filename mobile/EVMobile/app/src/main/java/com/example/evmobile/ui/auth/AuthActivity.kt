package com.example.evmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.evmobile.R
import com.example.evmobile.databinding.ActivityAuthBinding
import com.example.evmobile.ui.main.MainActivity

/**
 * Authentication activity that handles login and registration
 */
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupFragments()
        observeViewModel()
        
        // Check if user is already logged in
        viewModel.checkLoginStatus()
    }
    
    private fun setupFragments() {
        // Start with login fragment
        switchToFragment(LoginFragment(), "login")
    }
    
    private fun switchToFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_fragment_container, fragment, tag)
            .commit()
    }
    
    private fun observeViewModel() {
        viewModel.authState.observe(this) { authState ->
            when (authState) {
                is AuthState.Authenticated -> {
                    navigateToMain()
                }
                is AuthState.Unauthenticated -> {
                    // Stay on auth screen
                }
                is AuthState.Loading -> {
                    // Show loading if needed
                }
                is AuthState.Error -> {
                    Toast.makeText(this, authState.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    fun switchToRegister() {
        switchToFragment(RegisterFragment(), "register")
    }
    
    fun switchToLogin() {
        switchToFragment(LoginFragment(), "login")
    }
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}