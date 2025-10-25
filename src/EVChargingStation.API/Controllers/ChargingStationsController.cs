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
using Microsoft.Extensions.Logging;

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ChargingStationsController : ControllerBase
{
    private readonly IChargingStationService _stationService;
    private readonly ILogger<ChargingStationsController> _logger;

    public ChargingStationsController(IChargingStationService stationService, ILogger<ChargingStationsController> logger)
    {
        _stationService = stationService;
        _logger = logger;
    }

    // GET: api/chargingstations
    // Retrieves all charging stations
    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetStations()
    {
        _logger.LogInformation("GetStations request received");
        try
        {
            var stations = await _stationService.GetAllStationsAsync();
            _logger.LogInformation("Returned {Count} stations", stations.Count());
            return Ok(stations);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetStations");
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/{id}
    // Retrieves a charging station by ID
    [HttpGet("{id}")]
    [AllowAnonymous]
    public async Task<ActionResult<ChargingStation>> GetStation(string id)
    {
        _logger.LogInformation("GetStation request for id: {Id}", id);
        try
        {
            var station = await _stationService.GetStationByIdAsync(id);
            if (station == null)
            {
                _logger.LogWarning("GetStation not found for id: {Id}", id);
                return NotFound();
            }
            _logger.LogInformation("Returned station for id: {Id}", id);
            return Ok(station);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetStation for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/operator/{operatorId}
    // Retrieves charging stations by operator ID
    [HttpGet("operator/{operatorId}")]
    [Authorize(Roles = "Operator,Admin")]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetStationsByOperator(string operatorId)
    {
        _logger.LogInformation("GetStationsByOperator request for operatorId: {OperatorId}", operatorId);
        try
        {
            var stations = await _stationService.GetStationsByOperatorAsync(operatorId);
            _logger.LogInformation("Returned {Count} stations for operatorId: {OperatorId}", stations.Count(), operatorId);
            return Ok(stations);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetStationsByOperator for operatorId: {OperatorId}", operatorId);
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/chargingstations/nearby
    // Retrieves nearby charging stations based on location and radius
    [HttpGet("nearby")]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<ChargingStation>>> GetNearbyStations([FromQuery] double latitude, [FromQuery] double longitude, [FromQuery] double radius = 10)
    {
        _logger.LogInformation("GetNearbyStations request for lat: {Latitude}, long: {Longitude}, radius: {Radius}", latitude, longitude, radius);
        try
        {
            var stations = await _stationService.GetStationsNearLocationAsync(latitude, longitude, radius);
            _logger.LogInformation("Returned {Count} nearby stations", stations.Count());
            return Ok(stations);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetNearbyStations");
            return BadRequest(new { message = ex.Message });
        }
    }

    // POST: api/chargingstations
    // Creates a new charging station
    [HttpPost]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult<ChargingStation>> CreateStation([FromBody] CreateStationRequest request)
    {
        _logger.LogInformation("CreateStation request for name: {Name}", request.Name);
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
            _logger.LogInformation("Station created with id: {Id}", createdStation.Id);
            return CreatedAtAction(nameof(GetStation), new { id = createdStation.Id }, createdStation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in CreateStation for name: {Name}", request.Name);
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
    // PUT: api/chargingstations/{id}
    // Updates a charging station
    [HttpPut("{id}")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult<ChargingStation>> UpdateStation(string id, [FromBody] UpdateStationRequest request)
    {
        _logger.LogInformation("UpdateStation request for id: {Id}", id);
        try
        {
            var existingStation = await _stationService.GetStationByIdAsync(id);
            if (existingStation == null)
            {
                return NotFound();
            }

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
            // Only operators can update their own stations, admins can update any
            if (userRole != "Admin" && existingStation.OperatorId != userId)
            {
                _logger.LogWarning("UpdateStation forbidden for id: {Id}", id);
                return Forbid(); // 403 Forbidden for role/account-based forbidden
            }

            existingStation.Name = request.Name;
            existingStation.Description = request.Description;
            existingStation.Location = request.Location;
            existingStation.Address = request.Address;
            existingStation.Connectors = request.Connectors;
            existingStation.Status = request.Status;
            existingStation.Amenities = request.Amenities;
            existingStation.OpeningHours = request.OpeningHours;
            existingStation.PricePerKWh = request.PricePerKWh;
            existingStation.ImageUrls = request.ImageUrls;

        var updatedStation = await _stationService.UpdateStationAsync(existingStation);
        return Ok(updatedStation);
    }
    catch (Exception ex)
    {
        return BadRequest(new { message = ex.Message });
    }
}
            var updatedStation = await _stationService.UpdateStationAsync(existingStation);
            _logger.LogInformation("Station updated for id: {Id}", id);
            return Ok(updatedStation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in UpdateStation for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // DELETE: api/chargingstations/{id}
    // Deletes a charging station
    [HttpDelete("{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult> DeleteStation(string id)
    {
        _logger.LogInformation("DeleteStation request for id: {Id}", id);
        try
        {
            var result = await _stationService.DeleteStationAsync(id);
            if (!result)
            {
                _logger.LogWarning("DeleteStation not found for id: {Id}", id);
                return NotFound();
            }
            _logger.LogInformation("Station deleted for id: {Id}", id);
            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in DeleteStation for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // PATCH: api/chargingstations/{id}/status
    // Updates the status of a charging station
    [HttpPatch("{id}/status")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult> UpdateStationStatus(string id, [FromBody] UpdateStationStatusRequest request)
    {
        _logger.LogInformation("UpdateStationStatus request for id: {Id}, status: {Status}", id, request.Status);
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
                _logger.LogWarning("Failed to update station status for id: {Id}", id);
                return NotFound();
            }
            _logger.LogInformation("Station status updated for id: {Id}", id);
            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in UpdateStationStatus for id: {Id}", id);
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