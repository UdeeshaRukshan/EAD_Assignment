package com.evcharging.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.evcharging.app.data.local.entity.*

/**
 * User Data Access Object
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE nic = :nic")
    suspend fun getUserByNic(nic: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?
    
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<UserEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET isActive = :isActive WHERE nic = :nic")
    suspend fun updateUserActiveStatus(nic: String, isActive: Boolean)
    
    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()
    
    @Query("UPDATE users SET isLoggedIn = 1 WHERE nic = :nic")
    suspend fun loginUser(nic: String)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

/**
 * Charging Station Data Access Object
 */
@Dao
interface ChargingStationDao {
    
    @Query("SELECT * FROM charging_stations")
    fun getAllStationsFlow(): Flow<List<ChargingStationEntity>>
    
    @Query("SELECT * FROM charging_stations")
    suspend fun getAllStations(): List<ChargingStationEntity>
    
    @Query("SELECT * FROM charging_stations WHERE id = :stationId")
    suspend fun getStationById(stationId: String): ChargingStationEntity?
    
    @Query("SELECT * FROM charging_stations WHERE id = :stationId")
    fun getStationByIdFlow(stationId: String): Flow<ChargingStationEntity?>
    
    @Query("""
        SELECT * FROM charging_stations 
        WHERE status = 0 
        ORDER BY distance ASC
        LIMIT :limit
    """)
    suspend fun getNearbyActiveStations(limit: Int = 20): List<ChargingStationEntity>
    
    @Query("SELECT * FROM charging_stations WHERE isFavorite = 1")
    suspend fun getFavoriteStations(): List<ChargingStationEntity>
    
    @Query("SELECT * FROM charging_stations WHERE isFavorite = 1")
    fun getFavoriteStationsFlow(): Flow<List<ChargingStationEntity>>
    
    @Query("UPDATE charging_stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun updateFavoriteStatus(stationId: String, isFavorite: Boolean)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: ChargingStationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<ChargingStationEntity>)
    
    @Update
    suspend fun updateStation(station: ChargingStationEntity)
    
    @Query("DELETE FROM charging_stations")
    suspend fun deleteAllStations()
    
    @Query("DELETE FROM charging_stations WHERE id = :stationId")
    suspend fun deleteStation(stationId: String)
}

/**
 * Connector Data Access Object
 */
@Dao
interface ConnectorDao {
    
    @Query("SELECT * FROM connectors WHERE stationId = :stationId")
    suspend fun getConnectorsByStationId(stationId: String): List<ConnectorEntity>
    
    @Query("SELECT * FROM connectors WHERE stationId = :stationId")
    fun getConnectorsByStationIdFlow(stationId: String): Flow<List<ConnectorEntity>>
    
    @Query("SELECT * FROM connectors WHERE id = :connectorId")
    suspend fun getConnectorById(connectorId: String): ConnectorEntity?
    
    @Query("SELECT * FROM connectors WHERE stationId = :stationId AND isAvailable = 1")
    suspend fun getAvailableConnectors(stationId: String): List<ConnectorEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnector(connector: ConnectorEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectors(connectors: List<ConnectorEntity>)
    
    @Update
    suspend fun updateConnector(connector: ConnectorEntity)
    
    @Query("UPDATE connectors SET isAvailable = :isAvailable WHERE id = :connectorId")
    suspend fun updateConnectorAvailability(connectorId: String, isAvailable: Boolean)
    
    @Query("DELETE FROM connectors WHERE stationId = :stationId")
    suspend fun deleteConnectorsByStationId(stationId: String)
}

/**
 * Booking Data Access Object
 */
@Dao
interface BookingDao {
    
    @Query("SELECT * FROM bookings WHERE userNic = :userNic ORDER BY createdAt DESC")
    suspend fun getBookingsByUser(userNic: String): List<BookingEntity>
    
    @Query("SELECT * FROM bookings WHERE userNic = :userNic ORDER BY createdAt DESC")
    fun getBookingsByUserFlow(userNic: String): Flow<List<BookingEntity>>
    
    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: String): BookingEntity?
    
    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    fun getBookingByIdFlow(bookingId: String): Flow<BookingEntity?>
    
    @Query("""
        SELECT * FROM bookings 
        WHERE userNic = :userNic AND status IN (0, 1) 
        ORDER BY startTime ASC
    """)
    suspend fun getPendingBookings(userNic: String): List<BookingEntity>
    
    @Query("""
        SELECT * FROM bookings 
        WHERE userNic = :userNic AND status = 1 AND startTime > datetime('now')
        ORDER BY startTime ASC
    """)
    suspend fun getUpcomingBookings(userNic: String): List<BookingEntity>
    
    @Query("""
        SELECT * FROM bookings 
        WHERE userNic = :userNic AND status IN (2, 3)
        ORDER BY startTime DESC
    """)
    suspend fun getCompletedBookings(userNic: String): List<BookingEntity>
    
    @Query("SELECT COUNT(*) FROM bookings WHERE userNic = :userNic AND status = 0")
    suspend fun getPendingBookingsCount(userNic: String): Int
    
    @Query("SELECT COUNT(*) FROM bookings WHERE userNic = :userNic AND status = 1 AND startTime > datetime('now')")
    suspend fun getApprovedBookingsCount(userNic: String): Int
    
    @Query("SELECT COUNT(*) FROM bookings WHERE userNic = :userNic AND status = 0")
    fun getPendingBookingsCountFlow(userNic: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM bookings WHERE userNic = :userNic AND status = 1 AND startTime > datetime('now')")
    fun getApprovedBookingsCountFlow(userNic: String): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)
    
    @Update
    suspend fun updateBooking(booking: BookingEntity)
    
    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: Int)
    
    @Query("UPDATE bookings SET syncStatus = :syncStatus WHERE id = :bookingId")
    suspend fun updateSyncStatus(bookingId: String, syncStatus: Int)
    
    @Query("SELECT * FROM bookings WHERE syncStatus = 0")
    suspend fun getUnsyncedBookings(): List<BookingEntity>
    
    @Delete
    suspend fun deleteBooking(booking: BookingEntity)
    
    @Query("DELETE FROM bookings WHERE userNic = :userNic")
    suspend fun deleteBookingsByUser(userNic: String)
}

/**
 * App Session Data Access Object
 */
@Dao
interface AppSessionDao {
    
    @Query("SELECT * FROM app_sessions WHERE id = 'current_session'")
    suspend fun getCurrentSession(): AppSessionEntity?
    
    @Query("SELECT * FROM app_sessions WHERE id = 'current_session'")
    fun getCurrentSessionFlow(): Flow<AppSessionEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AppSessionEntity)
    
    @Update
    suspend fun updateSession(session: AppSessionEntity)
    
    @Query("UPDATE app_sessions SET lastActiveAt = :timestamp WHERE id = 'current_session'")
    suspend fun updateLastActiveTime(timestamp: String)
    
    @Query("UPDATE app_sessions SET lastLocationLat = :lat, lastLocationLng = :lng WHERE id = 'current_session'")
    suspend fun updateLastLocation(lat: Double, lng: Double)
    
    @Query("DELETE FROM app_sessions")
    suspend fun clearSession()
}