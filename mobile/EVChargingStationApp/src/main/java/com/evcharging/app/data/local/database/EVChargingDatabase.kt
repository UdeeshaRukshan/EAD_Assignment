package com.evcharging.app.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.evcharging.app.data.local.dao.*
import com.evcharging.app.data.local.entity.*

/**
 * Type converters for Room database
 */
class Converters {
    // Add any type converters if needed for complex data types
}

/**
 * Main Room database for EV Charging Station app
 */
@Database(
    entities = [
        UserEntity::class,
        ChargingStationEntity::class,
        ConnectorEntity::class,
        BookingEntity::class,
        AppSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EVChargingDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun chargingStationDao(): ChargingStationDao
    abstract fun connectorDao(): ConnectorDao
    abstract fun bookingDao(): BookingDao
    abstract fun appSessionDao(): AppSessionDao
    
    companion object {
        private const val DATABASE_NAME = "ev_charging_database"
        
        @Volatile
        private var INSTANCE: EVChargingDatabase? = null
        
        fun getInstance(context: Context): EVChargingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EVChargingDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}