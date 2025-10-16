/*
 * File: DatabaseSettings.cs
 * Purpose: Defines database configuration settings for the EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 */

namespace EVChargingStation.Services;

public class DatabaseSettings
{
    public string ConnectionString { get; set; } = string.Empty;
    public string DatabaseName { get; set; } = string.Empty;
    public string UsersCollectionName { get; set; } = "Users";
    public string ChargingStationsCollectionName { get; set; } = "ChargingStations";
    public string BookingsCollectionName { get; set; } = "Bookings";
}