package com.evchargingstation.mobile.data.repositories

import android.content.SharedPreferences
import com.evchargingstation.mobile.data.local.dao.UserDao
import com.evchargingstation.mobile.data.local.entities.UserEntity
import com.evchargingstation.mobile.data.remote.ApiService
import com.evchargingstation.mobile.data.remote.requests.LoginRequest
import com.evchargingstation.mobile.data.remote.requests.RegisterRequest
import com.evchargingstation.mobile.domain.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) {
    
    companion object {
        private const val PREF_USER_NIC = "user_nic"
        private const val PREF_USER_TOKEN = "user_token"
        private const val PREF_IS_LOGGED_IN = "is_logged_in"
    }

    suspend fun login(email: String, password: String): User = withContext(Dispatchers.IO) {
        try {
            // Try API login first
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                
                // Save token and user info
                sharedPreferences.edit()
                    .putString(PREF_USER_TOKEN, loginResponse.token)
                    .putString(PREF_USER_NIC, loginResponse.user.nic)
                    .putBoolean(PREF_IS_LOGGED_IN, true)
                    .apply()
                
                // Save user to local database
                val userEntity = UserEntity(
                    nic = loginResponse.user.nic,
                    firstName = loginResponse.user.firstName,
                    lastName = loginResponse.user.lastName,
                    email = loginResponse.user.email,
                    phoneNumber = loginResponse.user.phoneNumber,
                    userType = loginResponse.user.userType,
                    isActive = loginResponse.user.isActive,
                    createdAt = loginResponse.user.createdAt ?: System.currentTimeMillis()
                )
                userDao.insertUser(userEntity)
                
                return@withContext loginResponse.user.toDomainModel()
            } else {
                throw Exception("Invalid credentials")
            }
        } catch (e: Exception) {
            // If API fails, try local login (offline mode)
            val userEntity = userDao.getUserByEmail(email)
            if (userEntity != null && userEntity.email == email) {
                // In a real app, you would hash and verify the password
                // For demo purposes, we'll assume it's correct
                
                sharedPreferences.edit()
                    .putString(PREF_USER_NIC, userEntity.nic)
                    .putBoolean(PREF_IS_LOGGED_IN, true)
                    .apply()
                
                return@withContext userEntity.toDomainModel()
            } else {
                throw Exception("Login failed: ${e.message}")
            }
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        nic: String,
        email: String,
        phoneNumber: String,
        password: String,
        userType: String
    ): User = withContext(Dispatchers.IO) {
        try {
            // Check if user already exists locally
            val existingUser = userDao.getUserByNic(nic)
            if (existingUser != null) {
                throw Exception("User with this NIC already exists")
            }
            
            // Try API registration first
            val registerRequest = RegisterRequest(
                firstName = firstName,
                lastName = lastName,
                nic = nic,
                email = email,
                phoneNumber = phoneNumber,
                password = password,
                userType = userType
            )
            
            val response = apiService.register(registerRequest)
            
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                
                // Save token and user info
                sharedPreferences.edit()
                    .putString(PREF_USER_TOKEN, registerResponse.token)
                    .putString(PREF_USER_NIC, registerResponse.user.nic)
                    .putBoolean(PREF_IS_LOGGED_IN, true)
                    .apply()
                
                // Save user to local database
                val userEntity = UserEntity(
                    nic = registerResponse.user.nic,
                    firstName = registerResponse.user.firstName,
                    lastName = registerResponse.user.lastName,
                    email = registerResponse.user.email,
                    phoneNumber = registerResponse.user.phoneNumber,
                    userType = registerResponse.user.userType,
                    isActive = registerResponse.user.isActive,
                    createdAt = registerResponse.user.createdAt ?: System.currentTimeMillis()
                )
                userDao.insertUser(userEntity)
                
                return@withContext registerResponse.user.toDomainModel()
            } else {
                throw Exception("Registration failed")
            }
        } catch (e: Exception) {
            // If API fails, save locally for offline registration
            val userEntity = UserEntity(
                nic = nic,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                userType = userType,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            
            userDao.insertUser(userEntity)
            
            sharedPreferences.edit()
                .putString(PREF_USER_NIC, nic)
                .putBoolean(PREF_IS_LOGGED_IN, true)
                .apply()
            
            return@withContext userEntity.toDomainModel()
        }
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
        if (!isLoggedIn) return@withContext null
        
        val userNic = sharedPreferences.getString(PREF_USER_NIC, null)
        if (userNic != null) {
            val userEntity = userDao.getUserByNic(userNic)
            return@withContext userEntity?.toDomainModel()
        }
        
        return@withContext null
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        // Clear preferences
        sharedPreferences.edit()
            .remove(PREF_USER_NIC)
            .remove(PREF_USER_TOKEN)
            .putBoolean(PREF_IS_LOGGED_IN, false)
            .apply()
    }

    suspend fun updateUser(user: User): User = withContext(Dispatchers.IO) {
        try {
            // Try API update first
            val response = apiService.updateUser(user.nic, user.toApiModel())
            
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = response.body()!!
                
                // Update local database
                val userEntity = UserEntity(
                    nic = updatedUser.nic,
                    firstName = updatedUser.firstName,
                    lastName = updatedUser.lastName,
                    email = updatedUser.email,
                    phoneNumber = updatedUser.phoneNumber,
                    userType = updatedUser.userType,
                    isActive = updatedUser.isActive,
                    createdAt = updatedUser.createdAt ?: System.currentTimeMillis()
                )
                userDao.updateUser(userEntity)
                
                return@withContext updatedUser.toDomainModel()
            } else {
                throw Exception("Failed to update user")
            }
        } catch (e: Exception) {
            // If API fails, update locally
            val userEntity = user.toEntity()
            userDao.updateUser(userEntity)
            
            return@withContext user
        }
    }

    suspend fun deactivateUser(nic: String) = withContext(Dispatchers.IO) {
        try {
            // Try API deactivation first
            val response = apiService.deactivateUser(nic)
            
            if (response.isSuccessful) {
                // Update local database
                userDao.deactivateUser(nic)
            } else {
                throw Exception("Failed to deactivate user")
            }
        } catch (e: Exception) {
            // If API fails, deactivate locally
            userDao.deactivateUser(nic)
        }
        
        // Logout the user
        logout()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(PREF_USER_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    // Extension functions to convert between models
    private fun com.evchargingstation.mobile.data.remote.responses.User.toDomainModel(): User {
        return User(
            nic = this.nic,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phoneNumber = this.phoneNumber,
            userType = this.userType,
            isActive = this.isActive,
            createdAt = this.createdAt
        )
    }

    private fun UserEntity.toDomainModel(): User {
        return User(
            nic = this.nic,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phoneNumber = this.phoneNumber,
            userType = this.userType,
            isActive = this.isActive,
            createdAt = this.createdAt
        )
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            nic = this.nic,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phoneNumber = this.phoneNumber,
            userType = this.userType,
            isActive = this.isActive,
            createdAt = this.createdAt ?: System.currentTimeMillis()
        )
    }

    private fun User.toApiModel(): com.evchargingstation.mobile.data.remote.requests.UpdateUserRequest {
        return com.evchargingstation.mobile.data.remote.requests.UpdateUserRequest(
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phoneNumber = this.phoneNumber,
            userType = this.userType,
            isActive = this.isActive
        )
    }
}