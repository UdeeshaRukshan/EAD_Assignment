package com.example.evmobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SignupActivity : AppCompatActivity() {

    companion object {
        private val API_BASE_URL = if (Build.FINGERPRINT.contains("generic") ||
            Build.FINGERPRINT.contains("emulator")) {
            "http://10.0.2.2:5105/api"        // Emulator
        } else {
            "http://172.20.10.3:5105/api"   // Physical Device
        }
    }
    
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
        
        // Register user via API
        registerUserViaAPI(firstName, lastName, nic, email, phone, password, userType)
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
    
    private fun registerUserViaAPI(
        firstName: String, lastName: String, nic: String, email: String, 
        phone: String, password: String, userType: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/auth/register")
                println("DEBUG: Connecting to: $url")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000  // Increased to 30 seconds
                connection.readTimeout = 30000     // Increased to 30 seconds
                
                // Map user type to role number
                val roleNumber = when (userType) {
                    "EV Owner" -> 0
                    "Admin" -> 1
                    "Operator" -> 2
                    "Backoffice User" -> 3
                    else -> 0 // Default to EV Owner
                }
                
                // Create JSON request body
                val jsonRequest = JSONObject().apply {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("email", email)
                    put("phoneNumber", phone)
                    put("password", password)
                    put("role", roleNumber)
                }
                
                // Write request body
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonRequest.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success", false)
                    
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        if (success) {
                            // Store token and user info if needed
                            val token = jsonResponse.optString("token", "")
                            val userObj = jsonResponse.optJSONObject("user")
                            
                            // You can store these in SharedPreferences for later use
                            // For now, just show success message
                            Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        } else {
                            Toast.makeText(this@SignupActivity, "Registration failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Handle error response
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Registration failed with code: $responseCode"
                    }
                    
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        try {
                            val errorJson = JSONObject(errorResponse)
                            val errorMessage = errorJson.optString("message", "Registration failed. Please try again.")
                            Toast.makeText(this@SignupActivity, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@SignupActivity, "Registration failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
            } catch (e: java.net.ConnectException) {
                println("DEBUG: Connection error - ${e.message}")
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignupActivity, "Cannot connect to server. Check API is running on port 5105.", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.net.SocketTimeoutException) {
                println("DEBUG: Timeout error - ${e.message}")
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignupActivity, "Connection timeout. Please check your network connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("DEBUG: General error - ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@SignupActivity, "Registration failed: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}