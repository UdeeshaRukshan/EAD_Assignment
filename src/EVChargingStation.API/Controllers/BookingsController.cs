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

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class BookingsController : ControllerBase
{
    private readonly IBookingService _bookingService;
    private readonly IChargingStationService _stationService;

    public BookingsController(IBookingService bookingService, IChargingStationService stationService)
    {
        _bookingService = bookingService;
        _stationService = stationService;
    }

    /*
     * GET: api/bookings
     * Retrieves all bookings
     */
    [HttpGet]
    [Authorize(Roles = "Admin,Operator,BackofficeUser")]
    public async Task<ActionResult<IEnumerable<Booking>>> GetAllBookings()
    {
        try
        {
            var bookings = await _bookingService.GetAllBookingsAsync();
            return Ok(bookings);
        }
        catch (Exception ex)
        {
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
        try
        {
            var currentUserId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Users can only see their own bookings unless they're admin/backoffice
            if (userRole != "Admin" && userRole != "BackofficeUser" && currentUserId != userId)
            {
                return Forbid();
            }

            var bookings = await _bookingService.GetBookingsByUserAsync(userId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
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
                    return Forbid();
                }
            }

            var bookings = await _bookingService.GetBookingsByStationAsync(stationId);
            return Ok(bookings);
        }
        catch (Exception ex)
        {
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
        try
        {
            var bookings = await _bookingService.GetPendingBookingsAsync();
            return Ok(bookings);
        }
        catch (Exception ex)
        {
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
        try
        {
            var booking = await _bookingService.GetBookingByIdAsync(id);
            if (booking == null)
            {
                return NotFound();
            }

            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Check authorization
            if (userRole != "Admin" && userRole != "BackofficeUser")
            {
                if (userRole == "EVOwner" && booking.UserId != userId)
                {
                    return Forbid();
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

            return Ok(booking);
        }
        catch (Exception ex)
        {
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
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var userRole = User.FindFirst(ClaimTypes.Role)?.Value;

            // Only EV owners can create bookings for themselves
            if (userRole == "EVOwner" && request.UserId != userId)
            {
                return Forbid();
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
            return CreatedAtAction(nameof(GetBooking), new { id = createdBooking.Id }, createdBooking);
        }
        catch (Exception ex)
        {
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
            return Ok(updatedBooking);
        }
        catch (Exception ex)
        {
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
                return BadRequest(new { message = "Failed to confirm booking" });
            }

            return NoContent();
        }
        catch (Exception ex)
        {
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
                return BadRequest(new { message = "Failed to cancel booking" });
            }

            return NoContent();
        }
        catch (Exception ex)
        {
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
                return BadRequest(new { message = "Failed to complete booking" });
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