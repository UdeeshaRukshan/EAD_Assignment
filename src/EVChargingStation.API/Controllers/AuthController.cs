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

namespace EVChargingStation.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IUserService _userService;

    public AuthController(IUserService userService)
    {
        _userService = userService;
    }

    // POST: api/auth/register
    // Handles user registration requests
    [HttpPost("register")]
    public async Task<ActionResult<AuthResponse>> Register(RegisterRequest request)
    {
        try
        {
            // Check if user already exists
            var existingUser = await _userService.GetUserByEmailAsync(request.Email);
            if (existingUser != null)
            {
                return BadRequest(new { message = "User with this email already exists" });
            }

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
            var token = await _userService.GenerateJwtTokenAsync(user);

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
            return BadRequest(new { message = ex.Message });
        }
    }

    // POST: api/auth/login
    // Handles user login requests
    [HttpPost("login")]
    public async Task<ActionResult<AuthResponse>> Login(LoginRequest request)
    {
        try
        {
            var isAuthenticated = await _userService.AuthenticateUserAsync(request.Email, request.Password);
            if (!isAuthenticated)
            {
                return Unauthorized(new { message = "Invalid email or password" });
            }

            var user = await _userService.GetUserByEmailAsync(request.Email);
            if (user == null || !user.IsActive)
            {
                return Unauthorized(new { message = "Account is inactive" });
            }

            var token = await _userService.GenerateJwtTokenAsync(user);

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
            return BadRequest(new { message = ex.Message });
        }
    }

    // POST: api/auth/refresh
    // Handles token refresh requests
    [HttpPost("refresh")]
    public async Task<ActionResult<AuthResponse>> RefreshToken()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized();
            }

            var user = await _userService.GetUserByIdAsync(userId);
            if (user == null)
            {
                return NotFound();
            }

            var token = await _userService.GenerateJwtTokenAsync(user);

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
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET: api/auth/profile
    // Retrieves the profile of the authenticated user
    [HttpGet("profile")]
    [Authorize]
    public async Task<ActionResult<UserResponse>> GetProfile()
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized();
            }

            var user = await _userService.GetUserByIdAsync(userId);
            if (user == null)
            {
                return NotFound();
            }

            return Ok(new UserResponse
            {
                Id = user.Id,
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
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
    public bool IsActive { get; set; }
}