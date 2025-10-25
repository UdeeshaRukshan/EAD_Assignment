/*
 * File: ChargingStationsController.cs
 * Purpose: Manages charging station endpoints for the EV Charging Station API
 * Author: EAD Assignment
 * Date: 2025
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using System.Security.Claims;
using System.Text.Json;

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ChargingStationsController : ControllerBase
{
    private readonly IChargingStationService _stationService;

    public ChargingStationsController(IChargingStationService stationService)
    {
        _stationService = stationService;
    }

    // GET: api/chargingstations
    // Retrieves all charging stations
    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetStations()
    {
        try
        {
            var stations = await _stationService.GetAllStationsAsync();
            return Ok(stations);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/{id}
    // Retrieves a charging station by ID
    [HttpGet("{id}")]
    [AllowAnonymous]
    public async Task<ActionResult<ChargingStation>> GetStation(string id)
    {
        try
        {
            var station = await _stationService.GetStationByIdAsync(id);
            if (station == null)
            {
                return NotFound();
            }
            return Ok(station);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/operator/{operatorId}
    // Retrieves charging stations by operator ID
    [HttpGet("operator/{operatorId}")]
    [Authorize(Roles = "Operator,Admin")]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetStationsByOperator(string operatorId)
    {
        try
        {
            var stations = await _stationService.GetStationsByOperatorAsync(operatorId);
            return Ok(stations);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/nearby
    // Retrieves nearby charging stations based on location and radius
    [HttpGet("nearby")]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetNearbyStations([FromQuery] double latitude, [FromQuery] double longitude, [FromQuery] double radius = 10)
    {
        try
        {
            var stations = await _stationService.GetStationsNearLocationAsync(latitude, longitude, radius);
            return Ok(stations);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // POST: api/chargingstations
    // Creates a new charging station
    [HttpPost]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult<ChargingStation>> CreateStation([FromBody] CreateStationRequest request)
    {
        try
        {
            var operatorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            var station = new ChargingStation
            {
                Name = request.Name,
                Description = request.Description,
                Location = request.Location,
                Address = request.Address,
                OperatorId = userRole == "Admin" ? request.OperatorId : operatorId!,
                Connectors = request.Connectors,
                Amenities = request.Amenities,
                OpeningHours = request.OpeningHours,
                PricePerKWh = request.PricePerKWh,
                ImageUrls = request.ImageUrls
            };

            var createdStation = await _stationService.CreateStationAsync(station);
            return CreatedAtAction(nameof(GetStation), new { id = createdStation.Id }, createdStation);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

// PATCH: api/chargingstations/{id}
[HttpPatch("{id}")]
[Authorize(Roles = "Admin,Operator")]
public async Task<ActionResult<ChargingStation>> PatchStation(string id, [FromBody] JsonElement updates)
{
    try
    {
        var existingStation = await _stationService.GetStationByIdAsync(id);
        if (existingStation == null)
            return NotFound();

        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

        if (userRole != "Admin" && existingStation.OperatorId != userId)
            return Forbid();

        // Deserialize dynamic partial update
        var updateDict = System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, object>>(updates.GetRawText());

        if (updateDict == null)
            return BadRequest("Invalid JSON payload.");

        foreach (var kvp in updateDict)
        {
            switch (kvp.Key.ToLower())
            {
                case "name":
                    existingStation.Name = kvp.Value?.ToString();
                    break;
                case "description":
                    existingStation.Description = kvp.Value?.ToString();
                    break;
                case "address":
                    existingStation.Address = kvp.Value?.ToString();
                    break;
                case "priceperkwh":
                    existingStation.PricePerKWh = Convert.ToDecimal(kvp.Value);
                    break;
                case "openinghours":
                    existingStation.OpeningHours = kvp.Value?.ToString();
                    break;
                case "status":
                    if (Enum.TryParse(typeof(StationStatus), kvp.Value?.ToString(), out var status))
                        existingStation.Status = (StationStatus)status;
                    break;
                case "amenities":
                    existingStation.Amenities = System.Text.Json.JsonSerializer.Deserialize<List<string>>(kvp.Value.ToString());
                    break;
                case "imageurls":
                    existingStation.ImageUrls = System.Text.Json.JsonSerializer.Deserialize<List<string>>(kvp.Value.ToString());
                    break;
                case "location":
                    existingStation.Location = System.Text.Json.JsonSerializer.Deserialize<Location>(kvp.Value.ToString());
                    break;
                case "connectors":
                    existingStation.Connectors = System.Text.Json.JsonSerializer.Deserialize<List<Connector>>(kvp.Value.ToString());
                    break;
            }
        }

        var updatedStation = await _stationService.UpdateStationAsync(existingStation);
        return Ok(updatedStation);
    }
    catch (Exception ex)
    {
        return BadRequest(new { message = ex.Message });
    }
}

    // PATCH: api/chargingstations/{id}/status
    // Updates the status of a charging station
    [HttpPatch("{id}/status")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult> UpdateStationStatus(string id, [FromBody] UpdateStationStatusRequest request)
    {
        try
        {
            var station = await _stationService.GetStationByIdAsync(id);
            if (station == null)
            {
                return NotFound();
            }

            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            if (userRole != "Admin" && station.OperatorId != userId)
            {
                return Forbid();
            }

            // Prevent deactivating stations with active bookings
            if (request.Status == StationStatus.Inactive || request.Status == StationStatus.OutOfOrder)
            {
                var hasActiveBookings = await _stationService.HasActiveBookingsAsync(id);
                if (hasActiveBookings)
                {
                    return BadRequest(new { 
                        message = "Cannot deactivate station with active bookings. Please wait for current bookings to complete or cancel them first." 
                    });
                }
            }

            var result = await _stationService.UpdateStationStatusAsync(id, request.Status);
            if (!result)
            {
                return NotFound();
            }
            return NoContent();
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}

// DTOs
public class CreateStationRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public Location Location { get; set; } = new();
    public string Address { get; set; } = string.Empty;
    public string OperatorId { get; set; } = string.Empty;
    public List<Connector> Connectors { get; set; } = new();
    public List<string> Amenities { get; set; } = new();
    public string OpeningHours { get; set; } = "24/7";
    public decimal PricePerKWh { get; set; }
    public List<string> ImageUrls { get; set; } = new();
}

public class UpdateStationRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public Location Location { get; set; } = new();
    public string Address { get; set; } = string.Empty;
    public List<Connector> Connectors { get; set; } = new();
    public StationStatus Status { get; set; }
    public List<string> Amenities { get; set; } = new();
    public string OpeningHours { get; set; } = "24/7";
    public decimal PricePerKWh { get; set; }
    public List<string> ImageUrls { get; set; } = new();
}

public class UpdateStationStatusRequest
{
    public StationStatus Status { get; set; }
}