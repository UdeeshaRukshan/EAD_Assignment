# EV Charging Station Mobile App (Android)

This Android application provides EV owners and operators with mobile access to the charging station booking system.

## Features

### For EV Owners:
- User account management
- Browse and search charging stations on map
- Make reservations with QR code generation
- View booking history
- Real-time station availability
- Dashboard with personal statistics

### For Operators:
- Confirm/reject bookings
- Update station status
- View station performance metrics
- Manage connector availability

## Architecture

The mobile app follows the MVVM (Model-View-ViewModel) pattern and communicates exclusively with the Web API.

### Key Components:

1. **Data Layer**
   - SQLite database for offline caching
   - Repository pattern for data access
   - API service for remote calls

2. **Business Layer**
   - ViewModels for UI logic
   - Use cases for business operations
   - Models for data representation

3. **Presentation Layer**
   - Activities and Fragments
   - Adapters for RecyclerViews
   - Custom Views

## Project Structure

```
app/
├── src/main/java/com/evcharging/
│   ├── data/
│   │   ├── local/
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── entities/
│   │   │   │   └── dao/
│   │   │   └── preferences/
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   ├── dto/
│   │   │   └── interceptors/
│   │   └── repository/
│   ├── domain/
│   │   ├── model/
│   │   ├── repository/
│   │   └── usecase/
│   ├── presentation/
│   │   ├── ui/
│   │   │   ├── auth/
│   │   │   ├── dashboard/
│   │   │   ├── stations/
│   │   │   ├── bookings/
│   │   │   └── profile/
│   │   ├── viewmodel/
│   │   └── adapter/
│   ├── utils/
│   └── di/
├── src/main/res/
│   ├── layout/
│   ├── values/
│   ├── drawable/
│   └── menu/
└── src/main/AndroidManifest.xml
```

## Key Dependencies

- **Retrofit**: HTTP client for API calls
- **Room**: SQLite ORM for local database
- **Dagger/Hilt**: Dependency injection
- **Navigation Component**: In-app navigation
- **LiveData & ViewModel**: MVVM architecture
- **Google Maps**: Location and mapping
- **ZXing**: QR code generation/scanning
- **Glide**: Image loading

## Database Schema (SQLite)

### Users Table
```sql
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    role TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### Charging Stations Table
```sql
CREATE TABLE charging_stations (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    address TEXT NOT NULL,
    operator_id TEXT NOT NULL,
    status INTEGER NOT NULL,
    amenities TEXT,
    opening_hours TEXT,
    price_per_kwh REAL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### Connectors Table
```sql
CREATE TABLE connectors (
    id TEXT PRIMARY KEY,
    station_id TEXT NOT NULL,
    type INTEGER NOT NULL,
    power REAL NOT NULL,
    is_available INTEGER DEFAULT 1,
    status INTEGER NOT NULL,
    FOREIGN KEY (station_id) REFERENCES charging_stations(id)
);
```

### Bookings Table
```sql
CREATE TABLE bookings (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    station_id TEXT NOT NULL,
    connector_id TEXT NOT NULL,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    status INTEGER NOT NULL,
    qr_code TEXT,
    energy_consumed REAL DEFAULT 0,
    total_cost REAL DEFAULT 0,
    notes TEXT,
    confirmed_by TEXT,
    confirmed_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (station_id) REFERENCES charging_stations(id)
);
```

## API Integration

The mobile app communicates with the Web API using RESTful endpoints:

- **Authentication**: POST /api/auth/login, /api/auth/register
- **Stations**: GET /api/chargingstations, GET /api/chargingstations/nearby
- **Bookings**: POST /api/bookings, GET /api/bookings/user/{userId}
- **Profile**: GET /api/auth/profile

## Security

- JWT token-based authentication
- Secure storage of tokens using Android Keystore
- HTTPS only communication
- Input validation and sanitization

## Offline Support

- Cache frequently accessed data in SQLite
- Sync changes when connection is restored
- Show cached stations when offline
- Queue booking requests for online submission

## Maps Integration

- Google Maps for station locations
- Real-time location tracking
- Route planning to selected station
- Filtering stations by distance and availability

## QR Code Functionality

- Generate QR codes for confirmed bookings
- Scan QR codes to validate reservations
- Operator QR scanning for booking confirmation

## Push Notifications

- Booking confirmations
- Charging session updates
- Station availability alerts
- Promotional offers

## Installation & Setup

1. Clone the repository
2. Open in Android Studio
3. Configure API base URL in `local.properties`
4. Add Google Maps API key
5. Build and run on device/emulator

## Build Configuration

```gradle
android {
    compileSdk 34
    defaultConfig {
        applicationId "com.evcharging.app"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Architecture Components
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Dependency Injection
    implementation 'com.google.dagger:hilt-android:2.48.1'
    kapt 'com.google.dagger:hilt-compiler:2.48.1'
    
    // Maps
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    
    // QR Code
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    implementation 'com.google.zxing:core:3.5.2'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

This mobile app provides a comprehensive solution for EV owners and operators to manage charging station bookings, with full offline support and modern Android development practices.