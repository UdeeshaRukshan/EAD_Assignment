package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvJoinDate: TextView
    private lateinit var tvTotalReservations: TextView
    private lateinit var tvCompletedCharges: TextView
    private lateinit var cardEditProfile: CardView
    private lateinit var cardChangePassword: CardView
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageView
    private lateinit var btnEdit: ImageView

    private var authToken = ""
    private var userId = ""

    companion object {
        private const val API_BASE_URL = "http://10.0.2.2:5105/api"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        initViews()
        loadUserSession()
        setupClickListeners()
        loadProfileData()
    }

    private fun initViews() {
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAddress = findViewById(R.id.tvAddress)
        tvJoinDate = findViewById(R.id.tvJoinDate)
        tvTotalReservations = findViewById(R.id.tvTotalReservations)
        tvCompletedCharges = findViewById(R.id.tvCompletedCharges)
        cardEditProfile = findViewById(R.id.cardEditProfile)
        cardChangePassword = findViewById(R.id.cardChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)
    }

    private fun loadUserSession() {
        val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
        authToken = prefs.getString("auth_token", "") ?: ""
        userId = prefs.getString("user_id", "") ?: ""
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            showToast("Edit Profile - Coming Soon")
        }

        cardEditProfile.setOnClickListener {
            showToast("Edit Profile - Coming Soon")
        }

        cardChangePassword.setOnClickListener {
            showToast("Change Password - Coming Soon")
        }

        btnLogout.setOnClickListener { performLogout() }
    }

    private fun loadProfileData() {
        if (authToken.isNotEmpty()) {
            fetchUserProfile()
            fetchUserStatistics()
        } else {
            showToast("User not logged in")
            finish()
        }
    }

    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/auth/profile")   // ✅ correct endpoint
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseProfileData(response)
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to load profile: ${connection.responseCode}")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private suspend fun parseProfileData(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val firstName = json.optString("firstName", "User")
            val lastName = json.optString("lastName", "")
            val email = json.optString("email", "N/A")
            val phone = json.optString("phoneNumber", "N/A") // ✅ matches backend UserResponse
            val role = json.optString("role", "")
            val isActive = json.optBoolean("isActive", false)

            withContext(Dispatchers.Main) {
                tvFirstName.text = firstName
                tvLastName.text = lastName
                tvEmail.text = email
                tvPhone.text = phone
                tvAddress.text = role // you don’t return address, so show role instead
                tvJoinDate.text = if (isActive) "Active ✅" else "Inactive ❌"
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Error parsing profile")
            }
        }
    }

    private fun fetchUserStatistics() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/bookings/user/$userId")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseStatisticsData(response)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error loading statistics")
                }
            }
        }
    }

    private suspend fun parseStatisticsData(jsonString: String) {
        try {
            val bookings = org.json.JSONArray(jsonString)
            var totalReservations = 0
            var completedCharges = 0

            for (i in 0 until bookings.length()) {
                val booking = bookings.getJSONObject(i)
                val status = booking.optString("status", "").lowercase()
                totalReservations++
                if (status == "completed" || status == "finished") {
                    completedCharges++
                }
            }

            withContext(Dispatchers.Main) {
                tvTotalReservations.text = totalReservations.toString()
                tvCompletedCharges.text = completedCharges.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                tvTotalReservations.text = "0"
                tvCompletedCharges.text = "0"
            }
        }
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("EVChargingApp", MODE_PRIVATE)
        prefs.edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
