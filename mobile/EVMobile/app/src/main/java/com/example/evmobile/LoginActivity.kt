package com.example.evmobile

import android.content.Intent
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

class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val API_BASE_URL = "http://10.0.2.2:5105/api" // Use 10.0.2.2 for emulator
    }
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button
    private lateinit var btnSkipLogin: Button
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
        btnSkipLogin = findViewById(R.id.btnSkipLogin)
        pbLoading = findViewById(R.id.pbLoading)
    }
    
    private fun setupListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }
        
        btnSignup.setOnClickListener {
            navigateToSignup()
        }
        
        btnSkipLogin.setOnClickListener {
            navigateToMain()
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
        
        // Login via API
        loginUserViaAPI(email, password)
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
        btnSkipLogin.isEnabled = !show
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
    
    private fun loginUserViaAPI(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/auth/login")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                // Create JSON request body
                val jsonRequest = JSONObject().apply {
                    put("email", email)
                    put("password", password)
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
                            // Store token and user info in SharedPreferences
                            val token = jsonResponse.optString("token", "")
                            val userObj = jsonResponse.optJSONObject("user")
                            
                            // Store authentication data for future API calls
                            val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("auth_token", token)
                                if (userObj != null) {
                                    putString("user_id", userObj.optString("id", ""))
                                    putString("user_name", "${userObj.optString("firstName", "")} ${userObj.optString("lastName", "")}")
                                    putString("user_email", userObj.optString("email", ""))
                                    putString("user_role", userObj.optString("role", ""))
                                }
                                apply()
                            }
                            
                            Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed. Please check your credentials.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Handle error response
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Login failed with code: $responseCode"
                    }
                    
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        try {
                            val errorJson = JSONObject(errorResponse)
                            val errorMessage = errorJson.optString("message", "Invalid email or password.")
                            Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Invalid email or password.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
            } catch (e: java.net.ConnectException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@LoginActivity, "Cannot connect to server. Please check your internet connection.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}