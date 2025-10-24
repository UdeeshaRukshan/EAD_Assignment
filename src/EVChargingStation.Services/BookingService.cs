/*
 * File: BookingService.cs
 * Purpose: Implements booking-related business logic for the EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 */

using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using MongoDB.Driver;
using Microsoft.Extensions.Options;
using System.Text;

namespace EVChargingStation.Services.Implementations;

public class BookingService : IBookingService
{
    private readonly IMongoCollection<Booking> _bookings;
    private readonly IChargingStationService _stationService;

    public BookingService(IOptions<DatabaseSettings> databaseSettings, IChargingStationService stationService)
    {
        var mongoClient = new MongoClient(databaseSettings.Value.ConnectionString);
        var mongoDatabase = mongoClient.GetDatabase(databaseSettings.Value.DatabaseName);
        _bookings = mongoDatabase.GetCollection<Booking>(databaseSettings.Value.BookingsCollectionName);
        _stationService = stationService;
    }

    public async Task<IEnumerable<Booking>> GetAllBookingsAsync()
    {
        return await _bookings.Find(booking => !booking.IsDeleted).ToListAsync();
    }

    public async Task<Booking?> GetBookingByIdAsync(string id)
    {
        return await _bookings.Find(booking => booking.Id == id && !booking.IsDeleted).FirstOrDefaultAsync();
    }

    public async Task<IEnumerable<Booking>> GetBookingsByUserAsync(string userId)
    {
        return await _bookings.Find(booking => booking.UserId == userId && !booking.IsDeleted).ToListAsync();
    }

    public async Task<IEnumerable<Booking>> GetBookingsByStationAsync(string stationId)
    {
        return await _bookings.Find(booking => booking.StationId == stationId && !booking.IsDeleted).ToListAsync();
    }

    public async Task<IEnumerable<Booking>> GetPendingBookingsAsync()
    {
        return await _bookings.Find(booking => booking.Status == BookingStatus.Pending && !booking.IsDeleted).ToListAsync();
    }

    public async Task<Booking> CreateBookingAsync(Booking booking)
    {
        // Validate station exists and is active
        var station = await _stationService.GetStationByIdAsync(booking.StationId);
        if (station == null)
            throw new InvalidOperationException("Charging station not found");

        if (station.Status != StationStatus.Active)
            throw new InvalidOperationException("Station is not active and cannot accept bookings");

        // Validate connector exists and is available
        var connector = station.Connectors.FirstOrDefault(c => c.Id == booking.ConnectorId);
        if (connector == null)
            throw new InvalidOperationException("Connector not found");

        if (!connector.IsAvailable || connector.Status != ConnectorStatus.Available)
            throw new InvalidOperationException("Connector is not available for booking");

        // Check for overlapping bookings
        var overlappingBooking = await CheckForOverlappingBookingsAsync(
            booking.ConnectorId, 
            booking.StartTime, 
            booking.EndTime
        );
        
        if (overlappingBooking)
            throw new InvalidOperationException("This time slot is already booked. Please select a different time.");

        booking.QrCode = await GenerateQRCodeAsync(booking.Id);
        booking.CreatedAt = DateTime.UtcNow;
        booking.UpdatedAt = DateTime.UtcNow;
        await _bookings.InsertOneAsync(booking);
        return booking;
    }

    public async Task<Booking> UpdateBookingAsync(Booking booking)
    {
        var existingBooking = await GetBookingByIdAsync(booking.Id);
        if (existingBooking == null)
            throw new InvalidOperationException("Booking not found");

        if (existingBooking.Status != BookingStatus.Pending && existingBooking.Status != BookingStatus.Confirmed)
            throw new InvalidOperationException("Only pending or confirmed bookings can be updated");

        // If connector or time is being changed, validate
        if (booking.ConnectorId != existingBooking.ConnectorId ||
            booking.StartTime != existingBooking.StartTime ||
            booking.EndTime != existingBooking.EndTime)
        {
            var station = await _stationService.GetStationByIdAsync(booking.StationId);
            if (station == null)
                throw new InvalidOperationException("Charging station not found");

            if (station.Status != StationStatus.Active)
                throw new InvalidOperationException("Station is not active");

            var connector = station.Connectors.FirstOrDefault(c => c.Id == booking.ConnectorId);
            if (connector == null)
                throw new InvalidOperationException("Connector not found");

            // Check for overlapping bookings when updating
            var overlappingBooking = await CheckForOverlappingBookingsAsync(
                booking.ConnectorId,
                booking.StartTime,
                booking.EndTime,
                booking.Id // Exclude current booking from overlap check
            );

            if (overlappingBooking)
                throw new InvalidOperationException("This time slot is already booked. Please select a different time.");
        }

        booking.UpdatedAt = DateTime.UtcNow;
        await _bookings.ReplaceOneAsync(b => b.Id == booking.Id, booking);
        return booking;
    }

    public async Task<bool> ConfirmBookingAsync(string bookingId, string operatorId)
    {
        // Generate a new QR code with confirmation timestamp
        var qrCode = await GenerateConfirmedQRCodeAsync(bookingId);
        
        var update = Builders<Booking>.Update
            .Set(b => b.Status, BookingStatus.Confirmed)
            .Set(b => b.ConfirmedBy, operatorId)
            .Set(b => b.ConfirmedAt, DateTime.UtcNow)
            .Set(b => b.QrCode, qrCode)
            .Set(b => b.UpdatedAt, DateTime.UtcNow);
        
        var result = await _bookings.UpdateOneAsync(b => b.Id == bookingId, update);
        return result.ModifiedCount > 0;
    }

    public async Task<bool> CancelBookingAsync(string bookingId)
    {
        var update = Builders<Booking>.Update
            .Set(b => b.Status, BookingStatus.Cancelled)
            .Set(b => b.UpdatedAt, DateTime.UtcNow);
        
        var result = await _bookings.UpdateOneAsync(b => b.Id == bookingId, update);
        return result.ModifiedCount > 0;
    }

    public async Task<bool> CompleteBookingAsync(string bookingId, decimal energyConsumed)
    {
        var booking = await GetBookingByIdAsync(bookingId);
        if (booking == null) return false;

        // Calculate total cost based on energy consumed (simple calculation)
        var totalCost = energyConsumed * 0.25m; // $0.25 per kWh

        var update = Builders<Booking>.Update
            .Set(b => b.Status, BookingStatus.Completed)
            .Set(b => b.EnergyConsumed, energyConsumed)
            .Set(b => b.TotalCost, totalCost)
            .Set(b => b.UpdatedAt, DateTime.UtcNow);
        
        var result = await _bookings.UpdateOneAsync(b => b.Id == bookingId, update);
        return result.ModifiedCount > 0;
    }

    public Task<string> GenerateQRCodeAsync(string bookingId)
    {
        // Generate a comprehensive QR code data structure
        var qrData = new
        {
            Type = "EV_CHARGING_BOOKING",
            BookingId = bookingId,
            Timestamp = DateTime.UtcNow.ToString("O"), // ISO 8601 format
            Version = "1.0",
            Status = "PENDING"
        };
        
        var jsonData = System.Text.Json.JsonSerializer.Serialize(qrData);
        return Task.FromResult(Convert.ToBase64String(Encoding.UTF8.GetBytes(jsonData)));
    }

    public Task<string> GenerateConfirmedQRCodeAsync(string bookingId)
    {
        // Generate a confirmed QR code with additional security information
        var qrData = new
        {
            Type = "EV_CHARGING_BOOKING",
            BookingId = bookingId,
            Timestamp = DateTime.UtcNow.ToString("O"), // ISO 8601 format
            Version = "1.0",
            Status = "CONFIRMED",
            ConfirmedAt = DateTime.UtcNow.ToString("O"),
            // Add a simple hash for verification (in production, use proper cryptographic methods)
            Hash = Convert.ToBase64String(Encoding.UTF8.GetBytes($"{bookingId}:{DateTime.UtcNow.Ticks}")).Substring(0, 8)
        };
        
        var jsonData = System.Text.Json.JsonSerializer.Serialize(qrData);
        return Task.FromResult(Convert.ToBase64String(Encoding.UTF8.GetBytes(jsonData)));
    }

    /// <summary>
    /// Checks if there are any overlapping bookings for a specific connector within the given time range
    /// </summary>
    /// <param name="connectorId">The connector ID to check</param>
    /// <param name="startTime">The start time of the booking</param>
    /// <param name="endTime">The end time of the booking</param>
    /// <param name="excludeBookingId">Optional booking ID to exclude from the check (used when updating)</param>
    /// <returns>True if there is an overlap, false otherwise</returns>
    private async Task<bool> CheckForOverlappingBookingsAsync(string connectorId, DateTime startTime, DateTime endTime, string? excludeBookingId = null)
    {
        var filter = Builders<Booking>.Filter.And(
            Builders<Booking>.Filter.Eq(b => b.ConnectorId, connectorId),
            Builders<Booking>.Filter.In(b => b.Status, new[] { 
                BookingStatus.Pending, 
                BookingStatus.Confirmed, 
                BookingStatus.InProgress 
            }),
            Builders<Booking>.Filter.Eq(b => b.IsDeleted, false),
            Builders<Booking>.Filter.Or(
                // New booking starts during existing booking
                Builders<Booking>.Filter.And(
                    Builders<Booking>.Filter.Lte(b => b.StartTime, startTime),
                    Builders<Booking>.Filter.Gt(b => b.EndTime, startTime)
                ),
                // New booking ends during existing booking
                Builders<Booking>.Filter.And(
                    Builders<Booking>.Filter.Lt(b => b.StartTime, endTime),
                    Builders<Booking>.Filter.Gte(b => b.EndTime, endTime)
                ),
                // New booking completely contains existing booking
                Builders<Booking>.Filter.And(
                    Builders<Booking>.Filter.Gte(b => b.StartTime, startTime),
                    Builders<Booking>.Filter.Lte(b => b.EndTime, endTime)
                )
            )
        );

        // Exclude current booking when updating
        if (!string.IsNullOrEmpty(excludeBookingId))
        {
            filter = Builders<Booking>.Filter.And(
                filter,
                Builders<Booking>.Filter.Ne(b => b.Id, excludeBookingId)
            );
        }

        var overlappingBooking = await _bookings.Find(filter).FirstOrDefaultAsync();
        return overlappingBooking != null;
    }
}