# EV Charging Station Booking System

A comprehensive EV charging station booking system with 4 main components: Web Service API, Web Application (Backoffice), Mobile Application, and NoSQL Database.

## Architecture Overview

The system follows a service-oriented architecture where the Web Service API serves as the central business logic layer, with web and mobile applications acting as UI-only clients.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web App       │    │   Mobile App    │    │   External      │
│  (Backoffice)   │    │   (Android)     │    │   Integrations  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Web Service   │
                    │     (API)       │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │   MongoDB       │
                    │  (NoSQL DB)     │
                    └─────────────────┘
```

## Components

### 1. Web Service API (C# ASP.NET Core)
- **Location**: `src/EVChargingStation.API/`
- **Technology**: ASP.NET Core 8.0 Web API
- **Database**: MongoDB
- **Authentication**: JWT Bearer Token
- **Documentation**: Swagger/OpenAPI

**Key Features:**
- User authentication and authorization
- Charging station management
- Booking system with real-time availability
- Role-based access control (Admin, Operator, EVOwner, BackofficeUser)
- RESTful API design
- CORS enabled for cross-origin requests

**API Endpoints:**
- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - User registration
- `GET /api/auth/profile` - User profile
- `GET /api/chargingstations` - List all stations
- `GET /api/chargingstations/nearby?lat={lat}&lon={lon}&radius={km}` - Find nearby stations
- `POST /api/chargingstations` - Create new station (Admin/Operator)
- `GET /api/bookings` - List all bookings (Admin/Backoffice)
- `POST /api/bookings` - Create new booking
- `PATCH /api/bookings/{id}/confirm` - Confirm booking (Operator)

### 2. Web Application (ASP.NET Core MVC)
- **Location**: `src/EVChargingStation.Web/`
- **Technology**: ASP.NET Core 8.0 MVC
- **Purpose**: Backoffice management and operator interface

**Features:**
- Dashboard with system statistics
- User management
- Charging station management
- Booking management and monitoring
- Operator tools for booking confirmation
- Responsive web design

**User Roles:**
- **Admin**: Full system access, user management, station management
- **BackofficeUser**: Booking management, reporting, customer support
- **Operator**: Station management, booking confirmation for their stations

### 3. Mobile Application (Android)
- **Location**: `mobile/EVChargingStationApp/`
- **Technology**: Android (Kotlin), SQLite
- **Architecture**: MVVM with Repository pattern

**Features for EV Owners:**
- User account management
- Interactive map with charging stations
- Station search and filtering
- Reservation system with QR code generation
- Booking history and status tracking
- Real-time notifications
- Offline data caching

**Features for Operators:**
- Booking confirmation via QR scanning
- Station status updates
- Connector availability management
- Performance metrics

**Key Technologies:**
- **Room Database**: Local SQLite caching
- **Retrofit**: HTTP API client
- **Google Maps**: Location services and mapping
- **ZXing**: QR code generation/scanning
- **Hilt**: Dependency injection
- **Navigation Component**: In-app navigation

### 4. Data Models

**Core Entities:**
- **User**: Authentication and profile information
- **ChargingStation**: Station details, location, amenities
- **Connector**: Charging connectors with types and power ratings
- **Booking**: Reservation details with status tracking

**User Roles:**
- `Admin`: System administration
- `EVOwner`: End users who book charging stations
- `Operator`: Charging station operators
- `BackofficeUser`: Customer service and support

**Booking Status Flow:**
```
Pending → Confirmed → InProgress → Completed
   ↓         ↓           ↓
Cancelled  Cancelled  NoShow
```

## Technology Stack

### Backend (.NET)
- **Framework**: ASP.NET Core 8.0
- **Database**: MongoDB (NoSQL)
- **Authentication**: JWT Bearer Tokens
- **API Documentation**: Swagger/OpenAPI
- **Logging**: Built-in ASP.NET Core logging
- **Hosting**: IIS Compatible

### Frontend (Web)
- **Framework**: ASP.NET Core MVC
- **UI**: Bootstrap 5, jQuery
- **Session Management**: ASP.NET Core Sessions
- **HTTP Client**: HttpClient with custom API service

### Mobile (Android)
- **Language**: Kotlin
- **Database**: SQLite with Room ORM
- **Architecture**: MVVM + Repository
- **DI**: Dagger/Hilt
- **HTTP**: Retrofit + OkHttp
- **Maps**: Google Maps SDK
- **QR Codes**: ZXing library

## Configuration

### API Configuration (`appsettings.json`)
```json
{
  "DatabaseSettings": {
    "ConnectionString": "mongodb://localhost:27017",
    "DatabaseName": "EVChargingStationDB"
  },
  "JwtSettings": {
    "Secret": "your-secret-key-here"
  }
}
```

### Web App Configuration
```json
{
  "ApiSettings": {
    "BaseUrl": "https://localhost:7000/api"
  }
}
```

### Mobile App Configuration
- API Base URL: Configured in `build.gradle`
- Google Maps API Key: Set in manifest
- Local database: Automatic SQLite setup

## Installation & Setup

### Prerequisites
- .NET 8.0 SDK
- MongoDB (local or cloud)
- Android Studio (for mobile development)
- Google Maps API Key

### Running the API
```bash
cd src/EVChargingStation.API
dotnet run
```
API will be available at `https://localhost:7000`

### Running the Web App
```bash
cd src/EVChargingStation.Web
dotnet run
```
Web app will be available at `https://localhost:5000`

### Building the Mobile App
1. Open Android Studio
2. Import the project from `mobile/EVChargingStationApp`
3. Add Google Maps API key to `local.properties`
4. Build and run on device/emulator

## Database Schema

### Users Collection
```json
{
  "_id": "ObjectId",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "phoneNumber": "string",
  "passwordHash": "string",
  "role": "enum",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### ChargingStations Collection
```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "location": {
    "latitude": "double",
    "longitude": "double"
  },
  "address": "string",
  "operatorId": "string",
  "connectors": [
    {
      "id": "string",
      "type": "enum",
      "power": "decimal",
      "isAvailable": "boolean",
      "status": "enum"
    }
  ],
  "status": "enum",
  "amenities": ["string"],
  "openingHours": "string",
  "pricePerKWh": "decimal",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### Bookings Collection
```json
{
  "_id": "ObjectId",
  "userId": "string",
  "stationId": "string",
  "connectorId": "string",
  "startTime": "datetime",
  "endTime": "datetime",
  "status": "enum",
  "qrCode": "string",
  "energyConsumed": "decimal",
  "totalCost": "decimal",
  "notes": "string",
  "confirmedBy": "string",
  "confirmedAt": "datetime",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

## Security

- **Authentication**: JWT tokens with role-based claims
- **Authorization**: Role-based access control throughout the system
- **API Security**: HTTPS only, CORS configuration
- **Mobile Security**: Android Keystore for token storage
- **Database**: MongoDB with authentication enabled
- **Input Validation**: Server-side validation for all inputs

## Testing

### API Testing
- Use Swagger UI at `/swagger` endpoint
- Postman collection available for comprehensive testing
- Unit tests for services and controllers

### Web App Testing
- Integration tests for MVC controllers
- UI tests for critical user journeys

### Mobile App Testing
- Unit tests for ViewModels and repositories
- UI tests with Espresso
- Integration tests for API communication

## Deployment

### Production Considerations
- **API**: Deploy to IIS or Azure App Service
- **Database**: MongoDB Atlas or self-hosted MongoDB cluster
- **Web App**: Deploy to IIS or Azure App Service
- **Mobile**: Publish to Google Play Store

### Environment Configuration
- Use environment-specific configuration files
- Secure secret management (Azure Key Vault, etc.)
- Load balancing for high availability
- CDN for static assets

## Future Enhancements

- Real-time notifications via SignalR
- Payment integration (Stripe, PayPal)
- Advanced analytics and reporting
- Multi-language support
- iOS mobile application
- Third-party station integrations
- Smart charging features
- Load balancing and auto-scaling

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit a pull request
5. Ensure all CI/CD checks pass

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.