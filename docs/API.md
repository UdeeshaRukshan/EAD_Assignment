# API Documentation

## Base URL

```


https://localhost:7000/api
```
## TO run the API
```
dotnet run --launch-profile https

or 

dotnet run
```
## Authentication

All protected endpoints require a JWT Bearer token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Endpoints

### Authentication

#### POST /auth/register
Register a new user.

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "password": "SecurePassword123",
  "role": 1
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "EVOwner"
  }
}
```

#### POST /auth/login
Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "EVOwner"
  }
}
```

#### GET /auth/profile
Get current user profile. Requires authentication.

**Response:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "role": "EVOwner",
  "phoneNumber": "+1234567890",
  "isActive": true
}
```

### Charging Stations

#### GET /chargingstations
Get all charging stations. Public endpoint.

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439012",
    "name": "Downtown Charging Hub",
    "description": "Fast charging station in city center",
    "location": {
      "latitude": 40.7128,
      "longitude": -74.0060
    },
    "address": "123 Main St, New York, NY 10001",
    "operatorId": "507f1f77bcf86cd799439013",
    "connectors": [
      {
        "id": "507f1f77bcf86cd799439014",
        "type": 3,
        "power": 150.0,
        "isAvailable": true,
        "status": 0
      }
    ],
    "status": 0,
    "amenities": ["WiFi", "Parking", "Restroom"],
    "openingHours": "24/7",
    "pricePerKWh": 0.25,
    "imageUrls": []
  }
]
```

#### GET /chargingstations/{id}
Get specific charging station by ID. Public endpoint.

#### GET /chargingstations/nearby
Get charging stations near a location. Public endpoint.

**Query Parameters:**
- `latitude` (required): Latitude coordinate
- `longitude` (required): Longitude coordinate  
- `radius` (optional): Search radius in kilometers (default: 10)

**Example:**
```
GET /chargingstations/nearby?latitude=40.7128&longitude=-74.0060&radius=5
```

#### POST /chargingstations
Create a new charging station. Requires Admin or Operator role.

**Request Body:**
```json
{
  "name": "New Charging Station",
  "description": "Description of the station",
  "location": {
    "latitude": 40.7128,
    "longitude": -74.0060
  },
  "address": "456 Oak Ave, New York, NY 10002",
  "operatorId": "507f1f77bcf86cd799439013",
  "connectors": [
    {
      "type": 3,
      "power": 150.0
    }
  ],
  "amenities": ["WiFi", "Parking"],
  "openingHours": "6:00 AM - 10:00 PM",
  "pricePerKWh": 0.30
}
```

#### PUT /chargingstations/{id}
Update a charging station. Requires Admin role or station operator.

#### DELETE /chargingstations/{id}
Delete a charging station. Requires Admin role.

#### PATCH /chargingstations/{id}/status
Update station status. Requires Admin role or station operator.

**Request Body:**
```json
{
  "status": 2
}
```

### Bookings

#### GET /bookings
Get all bookings. Requires Admin or BackofficeUser role.

#### GET /bookings/user/{userId}
Get bookings for a specific user. Users can only see their own bookings.

#### GET /bookings/station/{stationId}
Get bookings for a specific station. Operators can only see bookings for their stations.

#### GET /bookings/pending
Get all pending bookings. Requires Admin, Operator, or BackofficeUser role.

#### GET /bookings/{id}
Get specific booking by ID. Access restricted based on user role and ownership.

#### POST /bookings
Create a new booking.

**Request Body:**
```json
{
  "userId": "507f1f77bcf86cd799439011",
  "stationId": "507f1f77bcf86cd799439012",
  "connectorId": "507f1f77bcf86cd799439014",
  "startTime": "2024-09-20T10:00:00Z",
  "endTime": "2024-09-20T12:00:00Z",
  "notes": "Fast charging needed"
}
```

**Response:**
```json
{
  "id": "507f1f77bcf86cd799439015",
  "userId": "507f1f77bcf86cd799439011",
  "stationId": "507f1f77bcf86cd799439012",
  "connectorId": "507f1f77bcf86cd799439014",
  "startTime": "2024-09-20T10:00:00Z",
  "endTime": "2024-09-20T12:00:00Z",
  "status": 0,
  "qrCode": "Qk9PS0lORzo1MDdmMWY3N2JjZjg2Y2Q3OTk0MzkwMTU=",
  "energyConsumed": 0,
  "totalCost": 0,
  "notes": "Fast charging needed",
  "createdAt": "2024-09-18T15:30:00Z",
  "updatedAt": "2024-09-18T15:30:00Z"
}
```

#### PATCH /bookings/{id}/confirm
Confirm a booking. Requires Admin or Operator role.

#### PATCH /bookings/{id}/cancel
Cancel a booking. Users can cancel their own bookings.

#### PATCH /bookings/{id}/complete
Complete a booking with energy consumption data. Requires Admin or Operator role.

**Request Body:**
```json
{
  "energyConsumed": 45.5
}
```

## Data Types

### User Roles
- `0`: Admin
- `1`: EVOwner  
- `2`: Operator
- `3`: BackofficeUser

### Station Status
- `0`: Active
- `1`: Inactive
- `2`: Maintenance
- `3`: OutOfOrder

### Booking Status
- `0`: Pending
- `1`: Confirmed
- `2`: InProgress
- `3`: Completed
- `4`: Cancelled
- `5`: NoShow

### Connector Types
- `0`: Type1
- `1`: Type2
- `2`: CHAdeMO
- `3`: CCS
- `4`: TeslaSuper

### Connector Status
- `0`: Available
- `1`: Occupied
- `2`: Reserved
- `3`: OutOfOrder
- `4`: Maintenance

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "message": "Validation error message"
}
```

### 401 Unauthorized
```json
{
  "message": "Invalid email or password"
}
```

### 403 Forbidden
```json
{
  "message": "Access denied"
}
```

### 404 Not Found
```json
{
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error"
}
```

## Rate Limiting

API endpoints are rate limited to prevent abuse:
- Public endpoints: 100 requests per minute
- Authenticated endpoints: 1000 requests per minute

## CORS

The API supports CORS for web applications. Configure allowed origins in production.

## Testing

Use the Swagger UI at `/swagger` for interactive API testing, or import the provided Postman collection for comprehensive endpoint testing.