# Deployment Guide

This guide covers deploying the EV Charging Station Booking System to production environments.

## Prerequisites

### Infrastructure Requirements
- **Web Server**: IIS 10+ or Linux with nginx/Apache
- **Database**: MongoDB 4.4+ (local or cloud)
- **Runtime**: .NET 8.0 Runtime
- **SSL Certificate**: For HTTPS (required for production)

### Development Tools
- Visual Studio 2022 or VS Code
- .NET 8.0 SDK
- MongoDB Compass (optional)
- Android Studio (for mobile app)

## Environment Setup

### 1. Database Configuration

#### MongoDB Setup (Local)
```bash
# Install MongoDB
# Windows: Download from https://www.mongodb.com/try/download/community
# Ubuntu: 
sudo apt update
sudo apt install -y mongodb

# Start MongoDB service
sudo systemctl start mongod
sudo systemctl enable mongod

# Create database and user
mongo
> use EVChargingStationDB
> db.createUser({
    user: "evcharging_user",
    pwd: "secure_password_here",
    roles: [{ role: "readWrite", db: "EVChargingStationDB" }]
  })
```

#### MongoDB Atlas (Cloud)
1. Create account at https://www.mongodb.com/atlas
2. Create a new cluster
3. Configure network access (whitelist IPs)
4. Create database user
5. Get connection string

### 2. API Deployment (IIS)

#### Prepare for Deployment
```bash
# Build for production
cd src/EVChargingStation.API
dotnet publish -c Release -o ./publish

# Update appsettings.Production.json
```

#### Production Configuration
```json
{
  "DatabaseSettings": {
    "ConnectionString": "mongodb://username:password@your-mongo-host:27017/EVChargingStationDB",
    "DatabaseName": "EVChargingStationDB",
    "UsersCollectionName": "Users",
    "ChargingStationsCollectionName": "ChargingStations",
    "BookingsCollectionName": "Bookings"
  },
  "JwtSettings": {
    "Secret": "your-very-secure-secret-key-at-least-32-characters-long"
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "AllowedHosts": "yourdomain.com,*.yourdomain.com"
}
```

#### IIS Configuration
1. Install .NET 8.0 Hosting Bundle
2. Create new website in IIS Manager
3. Set application pool to "No Managed Code"
4. Copy published files to wwwroot
5. Set proper permissions (IIS_IUSRS)
6. Configure SSL certificate
7. Add URL rewrite rules (optional)

#### web.config Example
```xml
<?xml version="1.0" encoding="utf-8"?>
<configuration>
  <location path="." inheritInChildApplications="false">
    <system.webServer>
      <handlers>
        <add name="aspNetCore" path="*" verb="*" modules="AspNetCoreModuleV2" resourceType="Unspecified" />
      </handlers>
      <aspNetCore processPath="dotnet" 
                  arguments=".\EVChargingStation.API.dll" 
                  stdoutLogEnabled="false" 
                  stdoutLogFile=".\logs\stdout" 
                  hostingModel="inprocess" />
      <httpErrors errorMode="Detailed" />
    </system.webServer>
  </location>
</configuration>
```

### 3. Web Application Deployment

#### Build and Deploy
```bash
cd src/EVChargingStation.Web
dotnet publish -c Release -o ./publish
```

#### Update Configuration
```json
{
  "ApiSettings": {
    "BaseUrl": "https://your-api-domain.com/api"
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "AllowedHosts": "your-web-domain.com"
}
```

### 4. Mobile App Deployment

#### Android App Store Deployment

1. **Update Configuration**
```gradle
// In build.gradle
buildTypes {
    release {
        buildConfigField "String", "API_BASE_URL", "\"https://your-api-domain.com/api/\""
        buildConfigField "String", "MAPS_API_KEY", "\"your-production-maps-key\""
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

2. **Generate Signed APK**
```bash
# Create keystore
keytool -genkey -v -keystore release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

# Build signed APK
./gradlew assembleRelease
```

3. **Upload to Play Store**
   - Create Play Console account
   - Prepare store listing
   - Upload signed APK
   - Complete content rating
   - Submit for review

## Environment Variables

### Production Secrets
Store sensitive configuration in environment variables:

```bash
# API Environment Variables
export ConnectionStrings__MongoDB="mongodb://user:pass@host:port/db"
export JwtSettings__Secret="your-secret-key"
export ASPNETCORE_ENVIRONMENT="Production"

# Web App Environment Variables  
export ApiSettings__BaseUrl="https://api.yourdomain.com/api"
```

### Docker Deployment (Alternative)

#### API Dockerfile
```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY ["src/EVChargingStation.API/EVChargingStation.API.csproj", "src/EVChargingStation.API/"]
COPY ["src/EVChargingStation.Services/EVChargingStation.Services.csproj", "src/EVChargingStation.Services/"]
COPY ["src/EVChargingStation.Models/EVChargingStation.Models.csproj", "src/EVChargingStation.Models/"]
RUN dotnet restore "src/EVChargingStation.API/EVChargingStation.API.csproj"
COPY . .
WORKDIR "/src/src/EVChargingStation.API"
RUN dotnet build "EVChargingStation.API.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "EVChargingStation.API.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "EVChargingStation.API.dll"]
```

#### Docker Compose
```yaml
version: '3.8'
services:
  mongodb:
    image: mongo:7.0
    container_name: evcharging_mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
      MONGO_INITDB_DATABASE: EVChargingStationDB
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

  api:
    build:
      context: .
      dockerfile: src/EVChargingStation.API/Dockerfile
    container_name: evcharging_api
    restart: unless-stopped
    environment:
      - ASPNETCORE_ENVIRONMENT=Production
      - ConnectionStrings__MongoDB=mongodb://admin:password@mongodb:27017/EVChargingStationDB?authSource=admin
    ports:
      - "7000:80"
    depends_on:
      - mongodb

  web:
    build:
      context: .
      dockerfile: src/EVChargingStation.Web/Dockerfile
    container_name: evcharging_web
    restart: unless-stopped
    environment:
      - ASPNETCORE_ENVIRONMENT=Production
      - ApiSettings__BaseUrl=http://api:80/api
    ports:
      - "5000:80"
    depends_on:
      - api

volumes:
  mongodb_data:
```

## Load Balancing & Scaling

### nginx Configuration
```nginx
upstream api_backend {
    server 10.0.0.10:7000;
    server 10.0.0.11:7000;
    server 10.0.0.12:7000;
}

upstream web_backend {
    server 10.0.0.20:5000;
    server 10.0.0.21:5000;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl http2;
    server_name web.yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://web_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring & Logging

### Application Insights (Azure)
```json
{
  "ApplicationInsights": {
    "InstrumentationKey": "your-app-insights-key"
  }
}
```

### Health Checks
Add to API Program.cs:
```csharp
builder.Services.AddHealthChecks()
    .AddMongoDb(builder.Configuration.GetConnectionString("MongoDB"));

app.MapHealthChecks("/health");
```

### Logging Configuration
```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning",
      "Microsoft.Hosting.Lifetime": "Information"
    },
    "Console": {
      "LogLevel": {
        "Default": "Information"
      }
    },
    "EventSource": {
      "LogLevel": {
        "Default": "Warning"
      }
    }
  }
}
```

## Security Checklist

### Pre-Deployment Security
- [ ] Update all secrets and keys for production
- [ ] Enable HTTPS/SSL certificates
- [ ] Configure CORS for specific domains only
- [ ] Set secure JWT expiration times
- [ ] Enable rate limiting
- [ ] Implement input validation
- [ ] Configure firewall rules
- [ ] Set up intrusion detection
- [ ] Enable database authentication
- [ ] Implement proper backup strategy

### Runtime Security
- [ ] Monitor for suspicious activity
- [ ] Regular security updates
- [ ] Log analysis and alerting
- [ ] Regular penetration testing
- [ ] Certificate renewal automation

## Backup Strategy

### Database Backup
```bash
# MongoDB backup
mongodump --host localhost:27017 --db EVChargingStationDB --out /backup/$(date +%Y%m%d)

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d_%H%M%S)
mongodump --host localhost:27017 --db EVChargingStationDB --out $BACKUP_DIR/$DATE
tar -czf $BACKUP_DIR/mongodb_backup_$DATE.tar.gz -C $BACKUP_DIR $DATE
rm -rf $BACKUP_DIR/$DATE
find $BACKUP_DIR -name "mongodb_backup_*.tar.gz" -mtime +7 -delete
```

### Application Backup
- Source code in Git repository
- Configuration files
- SSL certificates
- Application logs

## Performance Optimization

### Database Optimization
```javascript
// MongoDB indexes
db.users.createIndex({ "email": 1 }, { unique: true })
db.chargingstations.createIndex({ "location": "2dsphere" })
db.chargingstations.createIndex({ "operatorId": 1 })
db.bookings.createIndex({ "userId": 1 })
db.bookings.createIndex({ "stationId": 1 })
db.bookings.createIndex({ "status": 1 })
db.bookings.createIndex({ "startTime": 1 })
```

### API Optimization
- Enable response caching
- Implement compression
- Use CDN for static assets
- Optimize database queries
- Implement pagination

### Mobile App Optimization
- Enable ProGuard/R8 for APK size reduction
- Implement image compression
- Use efficient caching strategies
- Optimize network requests
- Implement offline functionality

## Troubleshooting

### Common Issues

#### API Not Starting
1. Check .NET runtime installation
2. Verify MongoDB connection
3. Check log files
4. Validate configuration files
5. Ensure proper permissions

#### Mobile App Connection Issues
1. Verify API URL configuration
2. Check network connectivity
3. Validate SSL certificates
4. Test API endpoints manually
5. Check device permissions

#### Database Connection Problems
1. Verify MongoDB service status
2. Check connection string
3. Validate credentials
4. Test network connectivity
5. Review firewall settings

### Log Locations
- **API Logs**: `/logs/` or Event Viewer (Windows)
- **Web Logs**: IIS logs or application logs
- **Mobile Logs**: Android Logcat or crash reports

## Support

For deployment issues:
1. Check documentation
2. Review log files
3. Test individual components
4. Contact system administrator
5. Create support ticket with detailed error information