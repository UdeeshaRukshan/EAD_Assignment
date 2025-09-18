using Microsoft.AspNetCore.Mvc;
using EVChargingStation.Web.Services;
using EVChargingStation.Models;

namespace EVChargingStation.Web.Controllers;

public class AuthController : Controller
{
    private readonly IApiService _apiService;

    public AuthController(IApiService apiService)
    {
        _apiService = apiService;
    }

    [HttpGet]
    public IActionResult Login()
    {
        return View();
    }

    [HttpPost]
    public async Task<IActionResult> Login(LoginViewModel model)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        try
        {
            var loginRequest = new
            {
                Email = model.Email,
                Password = model.Password
            };

            var response = await _apiService.PostAsync<AuthResponse>("/auth/login", loginRequest);

            if (response != null && response.Success)
            {
                // Store token and user info in session
                HttpContext.Session.SetString("AuthToken", response.Token);
                HttpContext.Session.SetString("UserRole", response.User?.Role ?? "");
                HttpContext.Session.SetString("UserId", response.User?.Id ?? "");
                HttpContext.Session.SetString("UserName", $"{response.User?.FirstName} {response.User?.LastName}");

                // Redirect based on role
                if (response.User?.Role == "Admin" || response.User?.Role == "BackofficeUser")
                {
                    return RedirectToAction("Index", "Dashboard");
                }
                else if (response.User?.Role == "Operator")
                {
                    return RedirectToAction("Index", "Operator");
                }
            }

            ModelState.AddModelError("", "Invalid email or password");
        }
        catch (Exception ex)
        {
            ModelState.AddModelError("", "An error occurred during login");
        }

        return View(model);
    }

    [HttpGet]
    public IActionResult Register()
    {
        return View();
    }

    [HttpPost]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        try
        {
            var registerRequest = new
            {
                FirstName = model.FirstName,
                LastName = model.LastName,
                Email = model.Email,
                PhoneNumber = model.PhoneNumber,
                Password = model.Password,
                Role = model.Role
            };

            var response = await _apiService.PostAsync<AuthResponse>("/auth/register", registerRequest);

            if (response != null && response.Success)
            {
                TempData["SuccessMessage"] = "Registration successful! Please login.";
                return RedirectToAction("Login");
            }

            ModelState.AddModelError("", "Registration failed");
        }
        catch (Exception ex)
        {
            ModelState.AddModelError("", "An error occurred during registration");
        }

        return View(model);
    }

    public IActionResult Logout()
    {
        HttpContext.Session.Clear();
        return RedirectToAction("Login");
    }
}

// ViewModels
public class LoginViewModel
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class RegisterViewModel
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string ConfirmPassword { get; set; } = string.Empty;
    public UserRole Role { get; set; } = UserRole.BackofficeUser;
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