package com.example.evmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

    private var userId = ""
    private var authToken = ""

    companion object {
        private const val API_BASE_URL = "http://10.0.2.2:5105/api"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

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
        btnBack.setOnClickListener {
            finish()
        }

        btnEdit.setOnClickListener {
            showToast("Edit Profile - Coming Soon")
            // TODO: Navigate to edit profile
        }

        cardEditProfile.setOnClickListener {
            showToast("Edit Profile - Coming Soon")
            // TODO: Navigate to edit profile
        }

        cardChangePassword.setOnClickListener {
            showToast("Change Password - Coming Soon")
            // TODO: Navigate to change password
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadProfileData() {
        if (userId.isNotEmpty() && authToken.isNotEmpty()) {
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
                val url = URL("$API_BASE_URL/users/$userId")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $authToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }

                    parseProfileData(response)
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to load profile data")
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
            val jsonObject = JSONObject(jsonString)

            val firstName = jsonObject.optString("firstName", "User")
            val lastName = jsonObject.optString("lastName", "")
            val email = jsonObject.optString("email", "N/A")
            val phone = jsonObject.optString("phone", "N/A")
            val address = jsonObject.optString("address", "N/A")
            val joinDate = jsonObject.optString("createdAt", "N/A")

            withContext(Dispatchers.Main) {
                tvFirstName.text = firstName
                tvLastName.text = lastName
                tvEmail.text = email
                tvPhone.text = phone
                tvAddress.text = address
                tvJoinDate.text = formatDate(joinDate)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Error parsing profile: ${e.message}")
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
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }

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
            val bookingsArray = org.json.JSONArray(jsonString)
            var totalReservations = 0
            var completedCharges = 0

            for (i in 0 until bookingsArray.length()) {
                val booking = bookingsArray.getJSONObject(i)
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

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            dateString
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}