package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignupActivity : AppCompatActivity() {
    
    private lateinit var tilFirstName: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var tilLastName: TextInputLayout
    private lateinit var etLastName: TextInputEditText
    private lateinit var tilNic: TextInputLayout
    private lateinit var etNic: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var spinnerUserType: Spinner
    private lateinit var btnSignup: Button
    private lateinit var btnLogin: Button
    private lateinit var pbLoading: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        
        initViews()
        setupListeners()
        setupValidation()
        setupSpinner()
    }
    
    private fun initViews() {
        tilFirstName = findViewById(R.id.tilFirstName)
        etFirstName = findViewById(R.id.etFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        etLastName = findViewById(R.id.etLastName)
        tilNic = findViewById(R.id.tilNic)
        etNic = findViewById(R.id.etNic)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spinnerUserType = findViewById(R.id.spinnerUserType)
        btnSignup = findViewById(R.id.btnSignup)
        btnLogin = findViewById(R.id.btnLogin)
        pbLoading = findViewById(R.id.pbLoading)
    }
    
    private fun setupListeners() {
        btnSignup.setOnClickListener {
            attemptSignup()
        }
        
        btnLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    
    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                clearErrors()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        
        etFirstName.addTextChangedListener(textWatcher)
        etLastName.addTextChangedListener(textWatcher)
        etNic.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)
    }
    
    private fun setupSpinner() {
        val userTypes = arrayOf("EV Owner", "Station Operator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserType.adapter = adapter
    }
    
    private fun clearErrors() {
        tilFirstName.error = null
        tilLastName.error = null
        tilNic.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }
    
    private fun attemptSignup() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val nic = etNic.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val userType = spinnerUserType.selectedItem.toString()
        
        clearErrors()
        
        if (!validateInput(firstName, lastName, nic, email, phone, password, confirmPassword)) {
            return
        }
        
        showLoading(true)
        
        // Simulate signup process
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showLoading(false)
            
            // For demo purposes, always succeed
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }, 2000)
    }
    
    private fun validateInput(
        firstName: String, lastName: String, nic: String,
        email: String, phone: String, password: String, confirmPassword: String
    ): Boolean {
        var isValid = true
        
        if (firstName.isEmpty()) {
            tilFirstName.error = "First name is required"
            isValid = false
        }
        
        if (lastName.isEmpty()) {
            tilLastName.error = "Last name is required"
            isValid = false
        }
        
        if (nic.isEmpty()) {
            tilNic.error = "NIC is required"
            isValid = false
        } else if (!isValidNIC(nic)) {
            tilNic.error = "Please enter a valid NIC"
            isValid = false
        }
        
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            isValid = false
        }
        
        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            tilPhone.error = "Please enter a valid phone number"
            isValid = false
        }
        
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }
        
        return isValid
    }
    
    private fun isValidNIC(nic: String): Boolean {
        // Simple NIC validation - you can enhance this based on your requirements
        return nic.length >= 9 && (nic.matches("\\d{9}[vVxX]".toRegex()) || nic.matches("\\d{12}".toRegex()))
    }
    
    private fun showLoading(show: Boolean) {
        pbLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSignup.isEnabled = !show
        btnLogin.isEnabled = !show
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}