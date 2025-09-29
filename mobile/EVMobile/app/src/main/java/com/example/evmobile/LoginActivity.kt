package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button
    private lateinit var pbLoading: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initViews()
        setupListeners()
        setupValidation()
    }
    
    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignup = findViewById(R.id.btnSignup)
        pbLoading = findViewById(R.id.pbLoading)
    }
    
    private fun setupListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }
        
        btnSignup.setOnClickListener {
            navigateToSignup()
        }
    }
    
    private fun setupValidation() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tilEmail.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tilPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    
    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        // Reset errors
        tilEmail.error = null
        tilPassword.error = null
        
        // Validate fields
        if (!validateInput(email, password)) {
            return
        }
        
        // Show loading
        showLoading(true)
        
        // Simulate login process
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showLoading(false)
            
            // For demo purposes, accept any valid email/password
            if (email.isNotEmpty() && password.length >= 6) {
                navigateToMain()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            isValid = false
        }
        
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        pbLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnLogin.isEnabled = !show
        btnSignup.isEnabled = !show
    }
    
    private fun navigateToSignup() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}