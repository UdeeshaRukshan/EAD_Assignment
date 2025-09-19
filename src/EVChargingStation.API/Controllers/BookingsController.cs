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

    [HttpGet]
    [Authorize(Roles = "Admin,BackofficeUser")]
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
                else if (userRole == "Operator")
                {
                    var station = await _stationService.GetStationByIdAsync(booking.StationId);
                    if (station == null || station.OperatorId != userId)
                    {
                        return Forbid();
                    }
                }
            }

            return Ok(booking);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

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

            // Operators can only confirm bookings for their stations
            if (userRole == "Operator")
            {
                var station = await _stationService.GetStationByIdAsync(booking.StationId);
                if (station == null || station.OperatorId != operatorId)
                {
                    return Forbid();
                }
            }

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

            if (userRole == "Operator")
            {
                var station = await _stationService.GetStationByIdAsync(booking.StationId);
                if (station == null || station.OperatorId != operatorId)
                {
                    return Forbid();
                }
            }

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

public class CompleteBookingRequest
{
    public decimal EnergyConsumed { get; set; }
}