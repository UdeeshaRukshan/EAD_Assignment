/*
 * File: DashboardController.cs
 * Purpose: Handles dashboard actions for the EV Charging Station Web application
 * Author: EAD Assignment
 * Date: 2025
 */

using Microsoft.AspNetCore.Mvc;
using EVChargingStation.Web.Services;
using EVChargingStation.Models;

namespace EVChargingStation.Web.Controllers;

public class DashboardController : Controller
{
    private readonly IApiService _apiService;

    public DashboardController(IApiService apiService)
    {
        _apiService = apiService;
    }

    public async Task<IActionResult> Index()
    {
        var token = HttpContext.Session.GetString("AuthToken");
        var role = HttpContext.Session.GetString("UserRole");

        if (string.IsNullOrEmpty(token) || (role != "Admin" && role != "BackofficeUser"))
        {
            return RedirectToAction("Login", "Auth");
        }

        try
        {
            // Get dashboard statistics
            var stations = await _apiService.GetAsync<List<ChargingStation>>("/chargingstations", token) ?? new List<ChargingStation>();
            var allBookings = await _apiService.GetAsync<List<Booking>>("/bookings", token) ?? new List<Booking>();

            var dashboardModel = new DashboardViewModel
            {
                TotalStations = stations.Count,
                ActiveStations = stations.Count(s => s.Status == StationStatus.Active),
                TotalBookings = allBookings.Count,
                PendingBookings = allBookings.Count(b => b.Status == BookingStatus.Pending),
                CompletedBookings = allBookings.Count(b => b.Status == BookingStatus.Completed),
                RecentBookings = allBookings.OrderByDescending(b => b.CreatedAt).Take(10).ToList(),
                Stations = stations.Take(5).ToList()
            };

            return View(dashboardModel);
        }
        catch (Exception ex)
        {
            ViewBag.ErrorMessage = "Failed to load dashboard data";
            return View(new DashboardViewModel());
        }
    }
}

public class DashboardViewModel
{
    public int TotalStations { get; set; }
    public int ActiveStations { get; set; }
    public int TotalBookings { get; set; }
    public int PendingBookings { get; set; }
    public int CompletedBookings { get; set; }
    public List<Booking> RecentBookings { get; set; } = new();
    public List<ChargingStation> Stations { get; set; } = new();
}