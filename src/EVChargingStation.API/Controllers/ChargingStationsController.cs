using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using System.Security.Claims;

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

    [HttpPut("{id}")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult<ChargingStation>> UpdateStation(string id, [FromBody] UpdateStationRequest request)
    {
        try
        {
            var existingStation = await _stationService.GetStationByIdAsync(id);
            if (existingStation == null)
            {
                return NotFound();
            }

            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Only operators can update their own stations, admins can update any
            if (userRole != "Admin" && existingStation.OperatorId != userId)
            {
                return Forbid();
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

    [HttpDelete("{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult> DeleteStation(string id)
    {
        try
        {
            var result = await _stationService.DeleteStationAsync(id);
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