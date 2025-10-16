package com.example.evmobile.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Utility class for managing authentication state and debugging
 */
object AuthDebugUtils {
    
    private const val TAG = "AuthDebugUtils"
    
    /**
     * Check and log current authentication state
     */
    fun debugAuthState(context: Context): AuthState {
        val prefs = context.getSharedPreferences("EVChargingApp", Context.MODE_PRIVATE)
        
        val authToken = prefs.getString("auth_token", null)
        val userId = prefs.getString("user_id", null)
        val userName = prefs.getString("user_name", null)
        val userEmail = prefs.getString("user_email", null)
        val userRole = prefs.getString("user_role", null)
        
        val authState = AuthState(
            isAuthenticated = !authToken.isNullOrBlank() && !userId.isNullOrBlank(),
            hasToken = !authToken.isNullOrBlank(),
            hasUserId = !userId.isNullOrBlank(),
            tokenLength = authToken?.length ?: 0,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userRole = userRole
        )
        
        Log.d(TAG, "=== Authentication Debug ===")
        Log.d(TAG, "Is Authenticated: ${authState.isAuthenticated}")
        Log.d(TAG, "Has Token: ${authState.hasToken} (${authState.tokenLength} chars)")
        Log.d(TAG, "Has User ID: ${authState.hasUserId}")
        Log.d(TAG, "User ID: ${authState.userId ?: "NULL"}")
        Log.d(TAG, "User Name: ${authState.userName ?: "NULL"}")
        Log.d(TAG, "User Email: ${authState.userEmail ?: "NULL"}")
        Log.d(TAG, "User Role: ${authState.userRole ?: "NULL"}")
        Log.d(TAG, "Token Preview: ${authToken?.take(20) ?: "NULL"}...")
        Log.d(TAG, "========================")
        
        return authState
    }
    
    /**
     * Clear all authentication data
     */
    fun clearAuth(context: Context) {
        val prefs = context.getSharedPreferences("EVChargingApp", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "Authentication data cleared")
    }
    
    /**
     * Get all stored preferences for debugging
     */
    fun getAllPreferences(context: Context): Map<String, Any?> {
        val prefs = context.getSharedPreferences("EVChargingApp", Context.MODE_PRIVATE)
        return prefs.all
    }
}

/**
 * Data class representing authentication state
 */
data class AuthState(
    val isAuthenticated: Boolean,
    val hasToken: Boolean,
    val hasUserId: Boolean,
    val tokenLength: Int,
    val userId: String?,
    val userName: String?,
    val userEmail: String?,
    val userRole: String?
)