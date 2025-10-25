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
                _logger.LogWarning("Login failed: Account inactive or user not found for email: {Email}", request.Email);
                return Unauthorized(new { message = "Account is inactive" });
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