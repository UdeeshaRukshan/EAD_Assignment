package com.evchargingstation.mobile.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evchargingstation.mobile.data.local.entities.UserEntity
import com.evchargingstation.mobile.data.repositories.UserRepository
import com.evchargingstation.mobile.domain.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkLoggedInUser()
    }

    private fun checkLoggedInUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
            } catch (e: Exception) {
                // No logged in user or error occurred
                _currentUser.value = null
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateLoginForm(email, password)) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val user = userRepository.login(email, password)
                _currentUser.value = user
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        nic: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String,
        userType: String
    ) {
        if (!validateRegisterForm(firstName, lastName, nic, email, phoneNumber, password, confirmPassword)) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val user = userRepository.register(
                    firstName = firstName,
                    lastName = lastName,
                    nic = nic,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password,
                    userType = userType
                )
                _currentUser.value = user
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _currentUser.value = null
                _uiState.value = AuthUiState() // Reset state
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    emailError = "Email is required"
                )
                false
            }
            !isValidEmail(email) -> {
                _uiState.value = _uiState.value.copy(
                    emailError = "Please enter a valid email address"
                )
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    passwordError = "Password is required"
                )
                false
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    emailError = null,
                    passwordError = null
                )
                true
            }
        }
    }

    private fun validateRegisterForm(
        firstName: String,
        lastName: String,
        nic: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        val errors = mutableMapOf<String, String>()

        if (firstName.isBlank()) {
            errors["firstName"] = "First name is required"
        }

        if (lastName.isBlank()) {
            errors["lastName"] = "Last name is required"
        }

        if (nic.isBlank()) {
            errors["nic"] = "NIC is required"
        } else if (!isValidNIC(nic)) {
            errors["nic"] = "Please enter a valid NIC number"
        }

        if (email.isBlank()) {
            errors["email"] = "Email is required"
        } else if (!isValidEmail(email)) {
            errors["email"] = "Please enter a valid email address"
        }

        if (phoneNumber.isBlank()) {
            errors["phoneNumber"] = "Phone number is required"
        } else if (!isValidPhoneNumber(phoneNumber)) {
            errors["phoneNumber"] = "Please enter a valid phone number"
        }

        if (password.isBlank()) {
            errors["password"] = "Password is required"
        } else if (password.length < 8) {
            errors["password"] = "Password must be at least 8 characters"
        }

        if (confirmPassword.isBlank()) {
            errors["confirmPassword"] = "Please confirm your password"
        } else if (password != confirmPassword) {
            errors["confirmPassword"] = "Passwords do not match"
        }

        if (errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                firstNameError = errors["firstName"],
                lastNameError = errors["lastName"],
                nicError = errors["nic"],
                emailError = errors["email"],
                phoneNumberError = errors["phoneNumber"],
                passwordError = errors["password"],
                confirmPasswordError = errors["confirmPassword"]
            )
            return false
        }

        // Clear all errors
        _uiState.value = _uiState.value.copy(
            firstNameError = null,
            lastNameError = null,
            nicError = null,
            emailError = null,
            phoneNumberError = null,
            passwordError = null,
            confirmPasswordError = null
        )
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidNIC(nic: String): Boolean {
        // Basic NIC validation for Sri Lankan NIC
        return when {
            nic.length == 10 && nic.substring(0, 9).all { it.isDigit() } && 
            (nic.last() == 'V' || nic.last() == 'X') -> true
            nic.length == 12 && nic.all { it.isDigit() } -> true
            else -> false
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic phone number validation
        return phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val nicError: String? = null,
    val phoneNumberError: String? = null,
    val confirmPasswordError: String? = null
)