/*
 * File: ChargingStationService.cs
 * Purpose: Implements charging station-related business logic for the EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 */

using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using MongoDB.Driver;
using Microsoft.Extensions.Options;

namespace EVChargingStation.Services.Implementations;

public class ChargingStationService : IChargingStationService
{
    private readonly IMongoCollection<ChargingStation> _stations;

    public ChargingStationService(IOptions<DatabaseSettings> databaseSettings)
    {
        var mongoClient = new MongoClient(databaseSettings.Value.ConnectionString);
        var mongoDatabase = mongoClient.GetDatabase(databaseSettings.Value.DatabaseName);
        _stations = mongoDatabase.GetCollection<ChargingStation>(databaseSettings.Value.ChargingStationsCollectionName);
    }

    public async Task<IEnumerable<ChargingStation>> GetAllStationsAsync()
    {
        return await _stations.Find(station => !station.IsDeleted).ToListAsync();
    }

    public async Task<ChargingStation?> GetStationByIdAsync(string id)
    {
        return await _stations.Find(station => station.Id == id && !station.IsDeleted).FirstOrDefaultAsync();
    }

    public async Task<IEnumerable<ChargingStation>> GetStationsByOperatorAsync(string operatorId)
    {
        return await _stations.Find(station => station.OperatorId == operatorId && !station.IsDeleted).ToListAsync();
    }

    public async Task<IEnumerable<ChargingStation>> GetStationsNearLocationAsync(double latitude, double longitude, double radiusKm)
    {
        // For simplicity, using basic distance calculation
        // In production, you would use MongoDB's geospatial queries
        var allStations = await GetAllStationsAsync();
        return allStations.Where(station => 
            CalculateDistance(latitude, longitude, station.Location.Latitude, station.Location.Longitude) <= radiusKm);
    }

    public async Task<ChargingStation> CreateStationAsync(ChargingStation station)
    {
        await _stations.InsertOneAsync(station);
        return station;
    }

    public async Task<ChargingStation> UpdateStationAsync(ChargingStation station)
    {
        station.UpdatedAt = DateTime.UtcNow;
        await _stations.ReplaceOneAsync(s => s.Id == station.Id, station);
        return station;
    }

    public async Task<bool> DeleteStationAsync(string id)
    {
        var update = Builders<ChargingStation>.Update
            .Set(s => s.IsDeleted, true)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);
        var result = await _stations.UpdateOneAsync(s => s.Id == id, update);
        return result.ModifiedCount > 0;
    }

    public async Task<bool> UpdateStationStatusAsync(string id, StationStatus status)
    {
        var update = Builders<ChargingStation>.Update
            .Set(s => s.Status, status)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);
        var result = await _stations.UpdateOneAsync(s => s.Id == id, update);
        return result.ModifiedCount > 0;
    }

    private static double CalculateDistance(double lat1, double lon1, double lat2, double lon2)
    {
        var R = 6371; // Earth's radius in kilometers
        var dLat = DegreesToRadians(lat2 - lat1);
        var dLon = DegreesToRadians(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(DegreesToRadians(lat1)) * Math.Cos(DegreesToRadians(lat2)) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        return R * c;
    }

    public async Task<bool> HasActiveBookingsAsync(string stationId)
    {
        var database = _stations.Database;
        var bookingsCollection = database.GetCollection<Booking>("Bookings");
        
        var activeStatuses = new[] { BookingStatus.Confirmed, BookingStatus.InProgress };
        var activeBookings = await bookingsCollection
            .Find(b => b.StationId == stationId && activeStatuses.Contains(b.Status))
            .CountDocumentsAsync();
            
        return activeBookings > 0;
    }

    private static double DegreesToRadians(double degrees)
    {
        return degrees * (Math.PI / 180);
    }
}