/*
 * File: BookingsController.cs
 * Purpose: Manages booking endpoints for the EV Charging Station API
 * Author: EAD Assignment
 * Date: 2025
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using System.Security.Claims;
using Microsoft.Extensions.Logging;

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class BookingsController : ControllerBase
{
    private readonly IBookingService _bookingService;
    private readonly IChargingStationService _stationService;
    private readonly ILogger<BookingsController> _logger;

    public BookingsController(IBookingService bookingService, IChargingStationService stationService, ILogger<BookingsController> logger)
    {
        _bookingService = bookingService;
        _stationService = stationService;
        _logger = logger;
    }

    /*
     * GET: api/bookings
     * Retrieves all bookings
     */
    [HttpGet]
    [Authorize(Roles = "Admin,Operator,BackofficeUser")]
    public async Task<ActionResult<IEnumerable<Booking>>> GetAllBookings()
    {
        _logger.LogInformation("GetAllBookings request received");
        try
        {
            var bookings = await _bookingService.GetAllBookingsAsync();
            _logger.LogInformation("Returned {Count} bookings", bookings.Count());
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetAllBookings");
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * GET: api/bookings/user/{userId}
     * Retrieves bookings for a specific user
     */
    [HttpGet("user/{userId}")]
    public async Task<ActionResult<IEnumerable<Booking>>> GetUserBookings(string userId)
    {
        _logger.LogInformation("GetUserBookings request for userId: {UserId}", userId);
        try
        {
            var currentUserId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Users can only see their own bookings unless they're admin/backoffice
            if (userRole != "Admin" && userRole != "BackofficeUser" && currentUserId != userId)
            {
                _logger.LogWarning("GetUserBookings forbidden for userId: {UserId}", userId);
                return Forbid(); // 403 Forbidden for role/account-based forbidden
            }

            var bookings = await _bookingService.GetBookingsByUserAsync(userId);
            _logger.LogInformation("Returned {Count} bookings for userId: {UserId}", bookings.Count(), userId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetUserBookings for userId: {UserId}", userId);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * GET: api/bookings/station/{stationId}
     * Retrieves bookings for a specific station
     */
    [HttpGet("station/{stationId}")]
    [Authorize(Roles = "Admin,Operator,BackofficeUser")]
    public async Task<ActionResult<IEnumerable<Booking>>> GetStationBookings(string stationId)
    {
        _logger.LogInformation("GetStationBookings request for stationId: {StationId}", stationId);
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Operators can only see bookings for their stations
            if (userRole == "Operator")
            {
                var station = await _stationService.GetStationByIdAsync(stationId);
                if (station == null || station.OperatorId != userId)
                {
                    _logger.LogWarning("GetStationBookings forbidden for stationId: {StationId}", stationId);
                    return Forbid(); // 403 Forbidden for role/account-based forbidden
                }
            }

            var bookings = await _bookingService.GetBookingsByStationAsync(stationId);
            _logger.LogInformation("Returned {Count} bookings for stationId: {StationId}", bookings.Count(), stationId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetStationBookings for stationId: {StationId}", stationId);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * GET: api/bookings/pending
     * Retrieves all pending bookings
     */
    [HttpGet("pending")]
    [Authorize(Roles = "Admin,Operator,BackofficeUser")]
    public async Task<ActionResult<IEnumerable<Booking>>> GetPendingBookings()
    {
        _logger.LogInformation("GetPendingBookings request received");
        try
        {
            var bookings = await _bookingService.GetPendingBookingsAsync();
            _logger.LogInformation("Returned {Count} pending bookings", bookings.Count());
            return Ok(bookings);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetPendingBookings");
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * GET: api/bookings/{id}
     * Retrieves a booking by ID
     */
    [HttpGet("{id}")]
    public async Task<ActionResult<Booking>> GetBooking(string id)
    {
        _logger.LogInformation("GetBooking request for id: {Id}", id);
        try
        {
            var booking = await _bookingService.GetBookingByIdAsync(id);
            if (booking == null)
            {
                _logger.LogWarning("GetBooking not found for id: {Id}", id);
                return NotFound();
            }

            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Check authorization
            if (userRole != "Admin" && userRole != "BackofficeUser")
            {
                if (userRole == "EVOwner" && booking.UserId != userId)
                {
                    _logger.LogWarning("GetBooking forbidden for id: {Id}", id);
                    return Forbid(); // 403 Forbidden for role/account-based forbidden
                }
                // Allow all Operators to view any booking (station ownership not enforced)
                // else if (userRole == "Operator")
                // {
                //     var station = await _stationService.GetStationByIdAsync(booking.StationId);
                //     if (station == null || station.OperatorId != userId)
                //     {
                //         return Forbid();
                //     }
                // }
            }

            _logger.LogInformation("Returned booking for id: {Id}", id);
            return Ok(booking);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetBooking for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * POST: api/bookings
     * Creates a new booking
     */
    [HttpPost]
    public async Task<ActionResult<Booking>> CreateBooking([FromBody] CreateBookingRequest request)
    {
        _logger.LogInformation("CreateBooking request for userId: {UserId}, stationId: {StationId}", request.UserId, request.StationId);
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Only EV owners can create bookings for themselves
            if (userRole == "EVOwner" && request.UserId != userId)
            {
                _logger.LogWarning("CreateBooking forbidden for userId: {UserId}", request.UserId);
                return Forbid(); // 403 Forbidden for role/account-based forbidden
            }

            // Validate station and connector exist
            var station = await _stationService.GetStationByIdAsync(request.StationId);
            if (station == null)
            {
                return BadRequest(new { message = "Charging station not found" });
            }

            // Check if station is available for booking
            if (station.Status != StationStatus.Active)
            {
                string statusMessage = station.Status switch
                {
                    StationStatus.Maintenance => "Station is currently under maintenance and not accepting bookings",
                    StationStatus.OutOfOrder => "Station is out of order and not accepting bookings", 
                    StationStatus.Inactive => "Station is inactive and not accepting bookings",
                    _ => "Station is not available for booking"
                };
                return BadRequest(new { message = statusMessage });
            }

            var connector = station.Connectors.FirstOrDefault(c => c.Id == request.ConnectorId);
            if (connector == null)
            {
                return BadRequest(new { message = "Connector not found" });
            }

            if (!connector.IsAvailable || connector.Status != ConnectorStatus.Available)
            {
                return BadRequest(new { message = "Connector is not available" });
            }

            var booking = new Booking
            {
                UserId = request.UserId,
                StationId = request.StationId,
                ConnectorId = request.ConnectorId,
                StartTime = request.StartTime,
                EndTime = request.EndTime,
                Notes = request.Notes
            };

            var createdBooking = await _bookingService.CreateBookingAsync(booking);
            _logger.LogInformation("Booking created with id: {Id}", createdBooking.Id);
            return CreatedAtAction(nameof(GetBooking), new { id = createdBooking.Id }, createdBooking);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in CreateBooking for userId: {UserId}", request.UserId);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * PUT: api/bookings/{id}
     * Updates a booking
     */
    [HttpPut("{id}")]
    public async Task<ActionResult<Booking>> UpdateBooking(string id, [FromBody] UpdateBookingRequest request)
    {
        _logger.LogInformation("UpdateBooking request for id: {Id}", id);
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            var existingBooking = await _bookingService.GetBookingByIdAsync(id);
            if (existingBooking == null)
            {
                return NotFound();
            }

            // Only EV owners can update their own bookings
            if (userRole == "EVOwner" && existingBooking.UserId != userId)
            {
                return Forbid();
            }

            // Admins and Operators can update any booking
            if (userRole != "Admin" && userRole != "Operator" && userRole != "EVOwner")
            {
                return Forbid();
            }

            // Check if booking can be modified (at least 12 hours before start time)
            var startTime = existingBooking.StartTime;
            var now = DateTime.UtcNow;
            var hoursUntilStart = (startTime - now).TotalHours;

            if (userRole == "EVOwner" && hoursUntilStart < 12)
            {
                return BadRequest(new { message = "Bookings can only be modified at least 12 hours before the reservation time." });
            }

            // Only allow modifications for pending or confirmed bookings
            if (existingBooking.Status != BookingStatus.Pending && existingBooking.Status != BookingStatus.Confirmed)
            {
                return BadRequest(new { message = "Only pending or confirmed bookings can be modified." });
            }

            // Validate station and connector exist if they're being changed
            if (request.StationId != existingBooking.StationId || request.ConnectorId != existingBooking.ConnectorId)
            {
                var station = await _stationService.GetStationByIdAsync(request.StationId);
                if (station == null)
                {
                    return BadRequest(new { message = "Charging station not found" });
                }

                // Check if station is available for booking
                if (station.Status != StationStatus.Active)
                {
                    string statusMessage = station.Status switch
                    {
                        StationStatus.Maintenance => "Station is currently under maintenance and not accepting bookings",
                        StationStatus.OutOfOrder => "Station is out of order and not accepting bookings",
                        StationStatus.Inactive => "Station is inactive and not accepting bookings",
                        _ => "Station is not available for booking"
                    };
                    return BadRequest(new { message = statusMessage });
                }

                var connector = station.Connectors.FirstOrDefault(c => c.Id == request.ConnectorId);
                if (connector == null)
                {
                    return BadRequest(new { message = "Connector not found" });
                }

                if (!connector.IsAvailable || connector.Status != ConnectorStatus.Available)
                {
                    return BadRequest(new { message = "Connector is not available" });
                }
            }

            // Update the booking
            existingBooking.StationId = request.StationId;
            existingBooking.ConnectorId = request.ConnectorId;
            existingBooking.StartTime = request.StartTime;
            existingBooking.EndTime = request.EndTime;
            existingBooking.Notes = request.Notes ?? existingBooking.Notes;

            var updatedBooking = await _bookingService.UpdateBookingAsync(existingBooking);
            _logger.LogInformation("Booking updated for id: {Id}", id);
            return Ok(updatedBooking);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in UpdateBooking for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * PATCH: api/bookings/{id}/confirm
     * Confirms a booking
     */
    [HttpPatch("{id}/confirm")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult> ConfirmBooking(string id)
    {
        _logger.LogInformation("ConfirmBooking request for id: {Id}", id);
        try
        {
            var booking = await _bookingService.GetBookingByIdAsync(id);
            if (booking == null)
            {
                return NotFound();
            }

            var operatorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Allow Admins and Operators to confirm any booking
            // (Removed station ownership restriction for Operators)

            var result = await _bookingService.ConfirmBookingAsync(id, operatorId!);
            if (!result)
            {
                _logger.LogWarning("Failed to confirm booking for id: {Id}", id);
                return BadRequest(new { message = "Failed to confirm booking" });
            }
            _logger.LogInformation("Booking confirmed for id: {Id}", id);
            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in ConfirmBooking for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * PATCH: api/bookings/{id}/cancel
     * Cancels a booking
     */
    [HttpPatch("{id}/cancel")]
    public async Task<ActionResult> CancelBooking(string id)
    {
        _logger.LogInformation("CancelBooking request for id: {Id}", id);
        try
        {
            var booking = await _bookingService.GetBookingByIdAsync(id);
            if (booking == null)
            {
                return NotFound();
            }

            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Users can only cancel their own bookings unless they're admin/operator
            if (userRole == "EVOwner" && booking.UserId != userId)
            {
                return Forbid();
            }
            
            // Admins and Operators can cancel any booking
            if (userRole != "Admin" && userRole != "Operator" && userRole != "EVOwner")
            {
                return Forbid();
            }

            var result = await _bookingService.CancelBookingAsync(id);
            if (!result)
            {
                _logger.LogWarning("Failed to cancel booking for id: {Id}", id);
                return BadRequest(new { message = "Failed to cancel booking" });
            }
            _logger.LogInformation("Booking cancelled for id: {Id}", id);
            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in CancelBooking for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    /*
     * PATCH: api/bookings/{id}/complete
     * Completes a booking
     */
    [HttpPatch("{id}/complete")]
    [Authorize(Roles = "Admin,Operator")]
    public async Task<ActionResult> CompleteBooking(string id, [FromBody] CompleteBookingRequest request)
    {
        _logger.LogInformation("CompleteBooking request for id: {Id}", id);
        try
        {
            var booking = await _bookingService.GetBookingByIdAsync(id);
            if (booking == null)
            {
                return NotFound();
            }

            var operatorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Allow Admins and Operators to complete any booking
            // (Removed station ownership restriction for Operators)

            var result = await _bookingService.CompleteBookingAsync(id, request.EnergyConsumed);
            if (!result)
            {
                _logger.LogWarning("Failed to complete booking for id: {Id}", id);
                return BadRequest(new { message = "Failed to complete booking" });
            }
            _logger.LogInformation("Booking completed for id: {Id}", id);
            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in CompleteBooking for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }
}

// DTOs
public class CreateBookingRequest
{
    public string UserId { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string ConnectorId { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public string Notes { get; set; } = string.Empty;
}

public class UpdateBookingRequest
{
    public string StationId { get; set; } = string.Empty;
    public string ConnectorId { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public string? Notes { get; set; }
}

public class CompleteBookingRequest
{
    public decimal EnergyConsumed { get; set; }
}