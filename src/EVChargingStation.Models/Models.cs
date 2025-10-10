/*
 * File: Models.cs
 * Purpose: Defines data models for EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 * Description: Contains entity models for Users, Charging Stations, Bookings,
 *              and supporting classes with MongoDB attributes.
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using System.ComponentModel.DataAnnotations;

namespace EVChargingStation.Models;

// Base entity class
public abstract class BaseEntity
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = ObjectId.GenerateNewId().ToString();

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("isDeleted")]
    public bool IsDeleted { get; set; } = false;
}

// User model
public class User:BaseEntity
{
    [BsonElement("nic")]
    public string NIC { get; set; } = string.Empty;
    
    [BsonElement("firstName")]
    [Required]
    public string FirstName { get; set; } = string.Empty;

    [BsonElement("lastName")]
    [Required]
    public string LastName { get; set; } = string.Empty;

    [BsonElement("email")]
    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [BsonElement("phoneNumber")]
    public string PhoneNumber { get; set; } = string.Empty;

    [BsonElement("passwordHash")]
    [Required]
    public string PasswordHash { get; set; } = string.Empty;

    [BsonElement("role")]
    [Required]
    public UserRole Role { get; set; }

    [BsonElement("isActive")]
    public bool IsActive { get; set; } = true;

    [BsonElement("profileImageUrl")]
    public string? ProfileImageUrl { get; set; }
}

// Charging Station model
public class ChargingStation : BaseEntity
{
    [BsonElement("name")]
    [Required]
    public string Name { get; set; } = string.Empty;

    [BsonElement("description")]
    public string Description { get; set; } = string.Empty;

    [BsonElement("location")]
    [Required]
    public Location Location { get; set; } = new();

    [BsonElement("address")]
    [Required]
    public string Address { get; set; } = string.Empty;

    [BsonElement("operatorId")]
    [Required]
    public string OperatorId { get; set; } = string.Empty;

    [BsonElement("connectors")]
    public List<Connector> Connectors { get; set; } = new();

    [BsonElement("status")]
    public StationStatus Status { get; set; } = StationStatus.Active;

    [BsonElement("amenities")]
    public List<string> Amenities { get; set; } = new();

    [BsonElement("openingHours")]
    public string OpeningHours { get; set; } = "24/7";

    [BsonElement("pricePerKWh")]
    public decimal PricePerKWh { get; set; }

    [BsonElement("imageUrls")]
    public List<string> ImageUrls { get; set; } = new();
}

// Booking model
public class Booking : BaseEntity
{
    [BsonElement("userId")]
    [Required]
    public string UserId { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [Required]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("connectorId")]
    [Required]
    public string ConnectorId { get; set; } = string.Empty;

    [BsonElement("startTime")]
    [Required]
    public DateTime StartTime { get; set; }

    [BsonElement("endTime")]
    [Required]
    public DateTime EndTime { get; set; }

    [BsonElement("status")]
    public BookingStatus Status { get; set; } = BookingStatus.Pending;

    [BsonElement("qrCode")]
    public string QrCode { get; set; } = string.Empty;

    [BsonElement("energyConsumed")]
    public decimal EnergyConsumed { get; set; }

    [BsonElement("totalCost")]
    public decimal TotalCost { get; set; }

    [BsonElement("notes")]
    public string Notes { get; set; } = string.Empty;

    [BsonElement("confirmedBy")]
    public string? ConfirmedBy { get; set; }

    [BsonElement("confirmedAt")]
    public DateTime? ConfirmedAt { get; set; }
}

// Supporting models
public class Location
{
    [BsonElement("latitude")]
    public double Latitude { get; set; }

    [BsonElement("longitude")]
    public double Longitude { get; set; }
}

public class Connector
{
    [BsonElement("id")]
    public string Id { get; set; } = ObjectId.GenerateNewId().ToString();

    [BsonElement("type")]
    public ConnectorType Type { get; set; }

    [BsonElement("power")]
    public decimal Power { get; set; } // in kW

    [BsonElement("isAvailable")]
    public bool IsAvailable { get; set; } = true;

    [BsonElement("status")]
    public ConnectorStatus Status { get; set; } = ConnectorStatus.Available;
}

// Enums
public enum UserRole
{
    Admin = 0,
    EVOwner = 1,
    Operator = 2,
    BackofficeUser = 3
}

public enum StationStatus
{
    Active = 0,
    Inactive = 1,
    Maintenance = 2,
    OutOfOrder = 3
}

public enum BookingStatus
{
    Pending = 0,
    Confirmed = 1,
    InProgress = 2,
    Completed = 3,
    Cancelled = 4,
    NoShow = 5
}

public enum ConnectorType
{
    Type1 = 0,
    Type2 = 1,
    CHAdeMO = 2,
    CCS = 3,
    TeslaSuper = 4
}

public enum ConnectorStatus
{
    Available = 0,
    Occupied = 1,
    Reserved = 2,
    OutOfOrder = 3,
    Maintenance = 4
}
