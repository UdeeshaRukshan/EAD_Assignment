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

    public BookingService(IOptions<DatabaseSettings> databaseSettings)
    {
        var mongoClient = new MongoClient(databaseSettings.Value.ConnectionString);
        var mongoDatabase = mongoClient.GetDatabase(databaseSettings.Value.DatabaseName);
        _bookings = mongoDatabase.GetCollection<Booking>(databaseSettings.Value.BookingsCollectionName);
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
        booking.QrCode = await GenerateQRCodeAsync(booking.Id);
        await _bookings.InsertOneAsync(booking);
        return booking;
    }

    public async Task<Booking> UpdateBookingAsync(Booking booking)
    {
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
}