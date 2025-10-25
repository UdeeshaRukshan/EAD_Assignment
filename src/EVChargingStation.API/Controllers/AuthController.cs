/*
 * File: AuthController.cs
 * Purpose: Handles authentication endpoints for the EV Charging Station API
 * Author: EAD Assignment
 * Date: 2025
 */

using Microsoft.AspNetCore.Mvc;
using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using Microsoft.Extensions.Logging;

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<AuthController> _logger;

    public AuthController(IUserService userService, ILogger<AuthController> logger)
    {
        _userService = userService;
        _logger = logger;
        _logger.LogDebug("AuthController initialized");
    }

    // POST: api/auth/register
    // Handles user registration requests
    [HttpPost("register")]
    public async Task<ActionResult<AuthResponse>> Register(RegisterRequest request)
    {
        _logger.LogInformation("Register endpoint called with email: {Email}, NIC: {NIC}", request.Email, request.NIC);
        try
        {
            _logger.LogDebug("Checking if user exists for email: {Email}", request.Email);
            var existingUser = await _userService.GetUserByEmailAsync(request.Email);
            if (existingUser != null)
            {
                _logger.LogWarning("Registration failed: User with email {Email} already exists", request.Email);
                return BadRequest(new { message = "User with this email already exists" });
            }

            _logger.LogDebug("Creating new user for email: {Email}", request.Email);
            var user = new User
            {
                NIC = request.NIC,
                FirstName = request.FirstName,
                LastName = request.LastName,
                Email = request.Email,
                PhoneNumber = request.PhoneNumber,
                PasswordHash = request.Password,
                Role = request.Role,
                IsActive = true
            };

            await _userService.CreateUserAsync(user);
            _logger.LogDebug("User created successfully for email: {Email}", user.Email);
            var token = await _userService.GenerateJwtTokenAsync(user);
            _logger.LogInformation("User registered successfully: {Email}", user.Email);
            return Ok(new AuthResponse
            {
                Success = true,
                Token = token,
                User = new UserResponse
                {
                    Id = user.Id,
                    FirstName = user.FirstName,
                    LastName = user.LastName,
                    Email = user.Email,
                    Role = user.Role.ToString()
                }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Exception in Register endpoint for email: {Email}", request.Email);
            return BadRequest(new { message = ex.Message });
        }
        finally
        {
            _logger.LogDebug("Register endpoint finished for email: {Email}", request.Email);
        }
    }

    // POST: api/auth/login
    // Handles user login requests
    [HttpPost("login")]
    public async Task<ActionResult<AuthResponse>> Login(LoginRequest request)
    {
        _logger.LogCritical("Login endpoint invoked. Logger initialized: {LoggerInitialized}", _logger != null);
        _logger.LogInformation("Login endpoint called with email: {Email}", request.Email);
        try
        {
            _logger.LogDebug("Authenticating user for email: {Email}", request.Email);
            var isAuthenticated = await _userService.AuthenticateUserAsync(request.Email, request.Password);
            if (!isAuthenticated)
            {
                _logger.LogWarning("Login failed: Invalid credentials for email: {Email}", request.Email);
                return Unauthorized(new { message = "Invalid email or password" });
            }

            var user = await _userService.GetUserByEmailAsync(request.Email);
            if (user == null || !user.IsActive)
            {
                _logger.LogWarning("Login forbidden: Account inactive or user not found for email: {Email}", request.Email);
                return Forbid(); // Return 403 Forbidden for role/account based forbidden
            }

            var token = await _userService.GenerateJwtTokenAsync(user);
            _logger.LogInformation("Login successful for email: {Email}", request.Email);
            return Ok(new AuthResponse
            {
                Success = true,
                Token = token,
                User = new UserResponse
                {
                    Id = user.Id,
                    FirstName = user.FirstName,
                    LastName = user.LastName,
                    Email = user.Email,
                    Role = user.Role.ToString()
                }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Exception in Login endpoint for email: {Email}", request.Email);
            return BadRequest(new { message = ex.Message });
        }
        finally
        {
            _logger.LogDebug("Login endpoint finished for email: {Email}", request.Email);
        }
    }

    // POST: api/auth/refresh
    // Handles token refresh requests
    [HttpPost("refresh")]
    public async Task<ActionResult<AuthResponse>> RefreshToken()
    {
        _logger.LogInformation("RefreshToken endpoint called");
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            _logger.LogDebug("Extracted userId from claims: {UserId}", userId);
            if (string.IsNullOrEmpty(userId))
            {
                _logger.LogWarning("Token refresh failed: No user id found in claims");
                return Unauthorized();
            }

            var user = await _userService.GetUserByIdAsync(userId);
            if (user == null)
            {
                _logger.LogWarning("Token refresh failed: User not found for id {UserId}", userId);
                return NotFound();
            }

            var token = await _userService.GenerateJwtTokenAsync(user);
            _logger.LogInformation("Token refreshed for user id: {UserId}", userId);
            return Ok(new AuthResponse
            {
                Success = true,
                Token = token,
                User = new UserResponse
                {
                    Id = user.Id,
                    FirstName = user.FirstName,
                    LastName = user.LastName,
                    Email = user.Email,
                    Role = user.Role.ToString()
                }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Exception in RefreshToken endpoint");
            return BadRequest(new { message = ex.Message });
        }
        finally
        {
            _logger.LogDebug("RefreshToken endpoint finished");
        }
    }

    // GET: api/auth/profile
    // Retrieves the profile of the authenticated user
    [HttpGet("profile")]
    [Authorize]
    public async Task<ActionResult<UserResponse>> GetProfile()
    {
        _logger.LogInformation("GetProfile endpoint called");
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            _logger.LogDebug("Extracted userId from claims: {UserId}", userId);
            if (string.IsNullOrEmpty(userId))
            {
                _logger.LogWarning("Profile request failed: No user id found in claims");
                return Unauthorized();
            }

            var user = await _userService.GetUserByIdAsync(userId);
            if (user == null)
            {
                _logger.LogWarning("Profile request failed: User not found for id {UserId}", userId);
                return NotFound();
            }

            _logger.LogInformation("Profile data returned for user id: {UserId}", userId);
            return Ok(new UserResponse
            {
                Id = user.Id,
                NIC = user.NIC,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Email = user.Email,
                Role = user.Role.ToString(),
                PhoneNumber = user.PhoneNumber,
                IsActive = user.IsActive
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Exception in GetProfile endpoint");
            return BadRequest(new { message = ex.Message });
        }
        finally
        {
            _logger.LogDebug("GetProfile endpoint finished");
        }
    }

    // PATCH: api/auth/deactivate
    // Deactivates the current user's account (sets IsActive to false, does not delete)
    [HttpPatch("deactivate")]
    [Authorize]
    public async Task<IActionResult> DeactivateAccount()
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            _logger.LogWarning("DeactivateAccount failed: No user id found in claims");
            return Unauthorized();
        }
        var user = await _userService.GetUserByIdAsync(userId);
        if (user == null)
        {
            _logger.LogWarning("DeactivateAccount failed: User not found for id {UserId}", userId);
            return NotFound();
        }
        if (user.Role == UserRole.Admin)
        {
            _logger.LogWarning("DeactivateAccount forbidden: Admin cannot be deactivated (id: {UserId})", userId);
            return Forbid();
        }
        if (!user.IsActive)
        {
            _logger.LogInformation("DeactivateAccount: User already deactivated (id: {UserId})", userId);
            return Ok(new { message = "Account is already deactivated." });
        }
        user.IsActive = false;
        await _userService.UpdateUserAsync(user);
        _logger.LogInformation("Account deactivated for user id: {UserId}", userId);
        return Ok(new { message = "Account has been deactivated." });
    }

    // GET: api/auth/users
    // Retrieves all users (Admin only)
    [HttpGet("users")]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult<IEnumerable<UserResponse>>> GetAllUsers()
    {
        _logger.LogInformation("GetAllUsers endpoint called");
        try
        {
            var users = await _userService.GetAllUsersAsync();
            _logger.LogInformation("Retrieved {Count} users", users.Count());
            var result = users.Select(u => new UserResponse
            {
                Id = u.Id,
                NIC = u.NIC,
                FirstName = u.FirstName,
                LastName = u.LastName,
                Email = u.Email,
                Role = u.Role.ToString(),
                PhoneNumber = u.PhoneNumber,
                IsActive = u.IsActive
            });
            return Ok(result);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetAllUsers");
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/auth/users/{id}
    // Retrieves a user by ID (Admin only)
    [HttpGet("users/{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult<UserResponse>> GetUserById(string id)
    {
        _logger.LogInformation("GetUserById endpoint called for id: {Id}", id);
        try
        {
            var user = await _userService.GetUserByIdAsync(id);
            if (user == null)
            {
                _logger.LogWarning("User not found for id: {Id}", id);
                return NotFound();
            }
            _logger.LogInformation("User found for id: {Id}", id);
            return Ok(new UserResponse
            {
                Id = user.Id,
                NIC = user.NIC,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Email = user.Email,
                Role = user.Role.ToString(),
                PhoneNumber = user.PhoneNumber,
                IsActive = user.IsActive
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in GetUserById for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // PATCH: api/auth/users/{id}/reactivate
    // Reactivates a deactivated account (Admin only)
    [HttpPatch("users/{id}/reactivate")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> ReactivateUser(string id)
    {
        _logger.LogInformation("ReactivateUser endpoint called for id: {Id}", id);
        try
        {
            var user = await _userService.GetUserByIdAsync(id);
            if (user == null)
            {
                _logger.LogWarning("User not found for reactivation, id: {Id}", id);
                return NotFound();
            }
            if (user.IsActive)
            {
                _logger.LogInformation("User already active, id: {Id}", id);
                return Ok(new { message = "Account is already active." });
            }
            user.IsActive = true;
            await _userService.UpdateUserAsync(user);
            _logger.LogInformation("User reactivated successfully, id: {Id}", id);
            return Ok(new { message = "Account has been reactivated." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in ReactivateUser for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // PUT: api/auth/users/{id}
    // Updates user details (Admin only)
    [HttpPut("users/{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> UpdateUser(string id, [FromBody] RegisterRequest request)
    {
        _logger.LogInformation("UpdateUser endpoint called for id: {Id}", id);
        try
        {
            var user = await _userService.GetUserByIdAsync(id);
            if (user == null)
            {
                _logger.LogWarning("User not found for update, id: {Id}", id);
                return NotFound();
            }
            _logger.LogDebug("Updating user: {Id} with email: {Email}", id, request.Email);
            user.NIC = request.NIC;
            user.FirstName = request.FirstName;
            user.LastName = request.LastName;
            user.Email = request.Email;
            user.PhoneNumber = request.PhoneNumber;
            user.Role = request.Role;
            await _userService.UpdateUserAsync(user);
            _logger.LogInformation("User updated successfully, id: {Id}", id);
            return Ok(new { message = "User updated successfully." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in UpdateUser for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // PATCH: api/auth/users/{id}/deactivate
    // Deactivates a user account (Admin only)
    [HttpPatch("users/{id}/deactivate")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> DeactivateUserByAdmin(string id)
    {
        _logger.LogInformation("DeactivateUserByAdmin endpoint called for id: {Id}", id);
        try
        {
            var user = await _userService.GetUserByIdAsync(id);
            if (user == null)
            {
                _logger.LogWarning("User not found for deactivation, id: {Id}", id);
                return NotFound();
            }
            if (!user.IsActive)
            {
                _logger.LogInformation("User already inactive, id: {Id}", id);
                return Ok(new { message = "Account is already deactivated." });
            }
            user.IsActive = false;
            await _userService.UpdateUserAsync(user);
            _logger.LogInformation("User deactivated successfully by admin, id: {Id}", id);
            return Ok(new { message = "Account has been deactivated." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in DeactivateUserByAdmin for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }

    // DELETE: api/auth/users/{id}
    // Deletes a user (Admin only)
    [HttpDelete("users/{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> DeleteUser(string id)
    {
        _logger.LogInformation("DeleteUser endpoint called for id: {Id}", id);
        try
        {
            var user = await _userService.GetUserByIdAsync(id);
            if (user == null)
            {
                _logger.LogWarning("User not found for deletion, id: {Id}", id);
                return NotFound();
            }
            await _userService.DeleteUserAsync(id);
            _logger.LogInformation("User deleted successfully, id: {Id}", id);
            return Ok(new { message = "User deleted successfully." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in DeleteUser for id: {Id}", id);
            return BadRequest(new { message = ex.Message });
        }
    }
}

// DTOs
public class RegisterRequest
{
    public string NIC { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public UserRole Role { get; set; } = UserRole.EVOwner;
}

public class LoginRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class AuthResponse
{
    public bool Success { get; set; }
    public string Token { get; set; } = string.Empty;
    public UserResponse? User { get; set; }
}

public class UserResponse
{
    public string Id { get; set; } = string.Empty;
    public string NIC { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
    public bool IsActive { get; set; }
}