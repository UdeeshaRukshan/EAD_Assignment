/*
 * File: Interfaces.cs
 * Purpose: Declares service interfaces for the EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 */

using EVChargingStation.Models;

namespace EVChargingStation.Services.Interfaces;

public interface IUserService
{
    Task<IEnumerable<User>> GetAllUsersAsync();
    Task<User?> GetUserByIdAsync(string id);
    Task<User?> GetUserByEmailAsync(string email);
    Task<User> CreateUserAsync(User user);
    Task<User> UpdateUserAsync(User user);
    Task<bool> DeleteUserAsync(string id);
    Task<bool> AuthenticateUserAsync(string email, string password);
    Task<string> GenerateJwtTokenAsync(User user);
    Task<User?> GetUserByNICAsync(string nic);

}

public interface IChargingStationService
{
    Task<IEnumerable<ChargingStation>> GetAllStationsAsync();
    Task<ChargingStation?> GetStationByIdAsync(string id);
    Task<IEnumerable<ChargingStation>> GetStationsByOperatorAsync(string operatorId);
    Task<IEnumerable<ChargingStation>> GetStationsNearLocationAsync(double latitude, double longitude, double radiusKm);
    Task<ChargingStation> CreateStationAsync(ChargingStation station);
    Task<ChargingStation> UpdateStationAsync(ChargingStation station);
    Task<bool> DeleteStationAsync(string id);
    Task<bool> UpdateStationStatusAsync(string id, StationStatus status);
    Task<bool> HasActiveBookingsAsync(string stationId);
}

public interface IBookingService
{
    Task<IEnumerable<Booking>> GetAllBookingsAsync();
    Task<Booking?> GetBookingByIdAsync(string id);
    Task<IEnumerable<Booking>> GetBookingsByUserAsync(string userId);
    Task<IEnumerable<Booking>> GetBookingsByStationAsync(string stationId);
    Task<IEnumerable<Booking>> GetPendingBookingsAsync();
    Task<Booking> CreateBookingAsync(Booking booking);
    Task<Booking> UpdateBookingAsync(Booking booking);
    Task<bool> ConfirmBookingAsync(string bookingId, string operatorId);
    Task<bool> CancelBookingAsync(string bookingId);
    Task<bool> CompleteBookingAsync(string bookingId, decimal energyConsumed);
    Task<string> GenerateQRCodeAsync(string bookingId);
    Task<string> GenerateConfirmedQRCodeAsync(string bookingId);
}

public interface INotificationService
{
    Task SendBookingConfirmationAsync(string userId, Booking booking);
    Task SendBookingReminderAsync(string userId, Booking booking);
    Task SendBookingCancellationAsync(string userId, Booking booking);
}
