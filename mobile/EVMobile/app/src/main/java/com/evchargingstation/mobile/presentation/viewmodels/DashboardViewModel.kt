package com.evchargingstation.mobile.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evchargingstation.mobile.data.repositories.BookingRepository
import com.evchargingstation.mobile.data.repositories.ChargingStationRepository
import com.evchargingstation.mobile.data.repositories.UserRepository
import com.evchargingstation.mobile.domain.models.Booking
import com.evchargingstation.mobile.domain.models.ChargingStation
import com.evchargingstation.mobile.domain.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val chargingStationRepository: ChargingStationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userBookings = MutableStateFlow<List<Booking>>(emptyList())
    val userBookings: StateFlow<List<Booking>> = _userBookings.asStateFlow()

    private val _nearbyStations = MutableStateFlow<List<ChargingStation>>(emptyList())
    val nearbyStations: StateFlow<List<ChargingStation>> = _nearbyStations.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load current user
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                
                if (user != null) {
                    // Load user's bookings
                    loadUserBookings(user.nic)
                    
                    // Load nearby stations
                    loadNearbyStations()
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    private suspend fun loadUserBookings(userNic: String) {
        try {
            val bookings = bookingRepository.getUserBookings(userNic)
            _userBookings.value = bookings
            
            // Update booking counts
            val pendingCount = bookings.count { it.status == "Pending" }
            val approvedCount = bookings.count { it.status == "Approved" }
            
            _uiState.value = _uiState.value.copy(
                pendingReservations = pendingCount,
                approvedReservations = approvedCount
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load bookings: ${e.message}"
            )
        }
    }

    private suspend fun loadNearbyStations() {
        try {
            // For now, get all active stations
            // In a real app, you would filter by user's location
            val stations = chargingStationRepository.getAllStations()
                .filter { it.isActive }
                .take(5) // Show only first 5 stations
            
            _nearbyStations.value = stations
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load stations: ${e.message}"
            )
        }
    }

    fun refreshDashboard() {
        loadDashboardData()
    }

    fun navigateToBookings() {
        // This will be handled by the fragment/activity
        _uiState.value = _uiState.value.copy(navigationEvent = NavigationEvent.BOOKINGS)
    }

    fun navigateToMap() {
        _uiState.value = _uiState.value.copy(navigationEvent = NavigationEvent.MAP)
    }

    fun navigateToQuickBooking() {
        _uiState.value = _uiState.value.copy(navigationEvent = NavigationEvent.QUICK_BOOKING)
    }

    fun navigateToQRScanner() {
        _uiState.value = _uiState.value.copy(navigationEvent = NavigationEvent.QR_SCANNER)
    }

    fun navigateToManageBookings() {
        _uiState.value = _uiState.value.copy(navigationEvent = NavigationEvent.MANAGE_BOOKINGS)
    }

    fun clearNavigationEvent() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun isOperator(): Boolean {
        return _currentUser.value?.userType == "Operator"
    }

    class Factory(
        private val userRepository: UserRepository,
        private val bookingRepository: BookingRepository,
        private val chargingStationRepository: ChargingStationRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(userRepository, bookingRepository, chargingStationRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingReservations: Int = 0,
    val approvedReservations: Int = 0,
    val navigationEvent: NavigationEvent? = null
)

enum class NavigationEvent {
    BOOKINGS,
    MAP,
    QUICK_BOOKING,
    QR_SCANNER,
    MANAGE_BOOKINGS
}