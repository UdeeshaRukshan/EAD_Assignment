using Microsoft.AspNetCore.Mvc;
using EVChargingStation.Web.Services;
using EVChargingStation.Models;

namespace EVChargingStation.Web.Controllers;

public class StationsController : Controller
{
    private readonly IApiService _apiService;

    public StationsController(IApiService apiService)
    {
        _apiService = apiService;
    }

    public async Task<IActionResult> Index()
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        try
        {
            var stations = await _apiService.GetAsync<List<ChargingStation>>("/chargingstations", token) ?? new List<ChargingStation>();
            return View(stations);
        }
        catch (Exception ex)
        {
            ViewBag.ErrorMessage = "Failed to load stations";
            return View(new List<ChargingStation>());
        }
    }

    public async Task<IActionResult> Details(string id)
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        try
        {
            var station = await _apiService.GetAsync<ChargingStation>($"/chargingstations/{id}", token);
            if (station == null)
            {
                return NotFound();
            }

            // Get bookings for this station
            var bookings = await _apiService.GetAsync<List<Booking>>($"/bookings/station/{id}", token) ?? new List<Booking>();
            
            var model = new StationDetailsViewModel
            {
                Station = station,
                Bookings = bookings
            };

            return View(model);
        }
        catch (Exception ex)
        {
            ViewBag.ErrorMessage = "Failed to load station details";
            return RedirectToAction("Index");
        }
    }

    [HttpGet]
    public IActionResult Create()
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        return View(new CreateStationViewModel());
    }

    [HttpPost]
    public async Task<IActionResult> Create(CreateStationViewModel model)
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        if (!ModelState.IsValid)
        {
            return View(model);
        }

        try
        {
            var createRequest = new
            {
                Name = model.Name,
                Description = model.Description,
                Location = new Location { Latitude = model.Latitude, Longitude = model.Longitude },
                Address = model.Address,
                OperatorId = model.OperatorId,
                Connectors = model.Connectors.Select(c => new Connector
                {
                    Type = c.Type,
                    Power = c.Power,
                    IsAvailable = true,
                    Status = ConnectorStatus.Available
                }).ToList(),
                Amenities = model.Amenities.Split(',').Select(a => a.Trim()).ToList(),
                OpeningHours = model.OpeningHours,
                PricePerKWh = model.PricePerKWh
            };

            var response = await _apiService.PostAsync<ChargingStation>("/chargingstations", createRequest, token);

            if (response != null)
            {
                TempData["SuccessMessage"] = "Station created successfully!";
                return RedirectToAction("Index");
            }

            ModelState.AddModelError("", "Failed to create station");
        }
        catch (Exception ex)
        {
            ModelState.AddModelError("", "An error occurred while creating the station");
        }

        return View(model);
    }

    [HttpGet]
    public async Task<IActionResult> Edit(string id)
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        try
        {
            var station = await _apiService.GetAsync<ChargingStation>($"/chargingstations/{id}", token);
            if (station == null)
            {
                return NotFound();
            }

            var model = new EditStationViewModel
            {
                Id = station.Id,
                Name = station.Name,
                Description = station.Description,
                Latitude = station.Location.Latitude,
                Longitude = station.Location.Longitude,
                Address = station.Address,
                Status = station.Status,
                Amenities = string.Join(", ", station.Amenities),
                OpeningHours = station.OpeningHours,
                PricePerKWh = station.PricePerKWh,
                Connectors = station.Connectors.Select(c => new ConnectorViewModel
                {
                    Id = c.Id,
                    Type = c.Type,
                    Power = c.Power,
                    IsAvailable = c.IsAvailable,
                    Status = c.Status
                }).ToList()
            };

            return View(model);
        }
        catch (Exception ex)
        {
            ViewBag.ErrorMessage = "Failed to load station for editing";
            return RedirectToAction("Index");
        }
    }

    [HttpPost]
    public async Task<IActionResult> Edit(EditStationViewModel model)
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        if (!ModelState.IsValid)
        {
            return View(model);
        }

        try
        {
            var updateRequest = new
            {
                Name = model.Name,
                Description = model.Description,
                Location = new Location { Latitude = model.Latitude, Longitude = model.Longitude },
                Address = model.Address,
                Status = model.Status,
                Connectors = model.Connectors.Select(c => new Connector
                {
                    Id = c.Id,
                    Type = c.Type,
                    Power = c.Power,
                    IsAvailable = c.IsAvailable,
                    Status = c.Status
                }).ToList(),
                Amenities = model.Amenities.Split(',').Select(a => a.Trim()).ToList(),
                OpeningHours = model.OpeningHours,
                PricePerKWh = model.PricePerKWh
            };

            var response = await _apiService.PutAsync<ChargingStation>($"/chargingstations/{model.Id}", updateRequest, token);

            if (response != null)
            {
                TempData["SuccessMessage"] = "Station updated successfully!";
                return RedirectToAction("Details", new { id = model.Id });
            }

            ModelState.AddModelError("", "Failed to update station");
        }
        catch (Exception ex)
        {
            ModelState.AddModelError("", "An error occurred while updating the station");
        }

        return View(model);
    }

    [HttpPost]
    public async Task<IActionResult> Delete(string id)
    {
        var token = HttpContext.Session.GetString("AuthToken");
        if (string.IsNullOrEmpty(token))
        {
            return RedirectToAction("Login", "Auth");
        }

        try
        {
            var result = await _apiService.DeleteAsync($"/chargingstations/{id}", token);
            if (result)
            {
                TempData["SuccessMessage"] = "Station deleted successfully!";
            }
            else
            {
                TempData["ErrorMessage"] = "Failed to delete station";
            }
        }
        catch (Exception ex)
        {
            TempData["ErrorMessage"] = "An error occurred while deleting the station";
        }

        return RedirectToAction("Index");
    }
}

// ViewModels
public class StationDetailsViewModel
{
    public ChargingStation Station { get; set; } = new();
    public List<Booking> Bookings { get; set; } = new();
}

public class CreateStationViewModel
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string Address { get; set; } = string.Empty;
    public string OperatorId { get; set; } = string.Empty;
    public List<ConnectorViewModel> Connectors { get; set; } = new();
    public string Amenities { get; set; } = string.Empty;
    public string OpeningHours { get; set; } = "24/7";
    public decimal PricePerKWh { get; set; }
}

public class EditStationViewModel
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string Address { get; set; } = string.Empty;
    public StationStatus Status { get; set; }
    public List<ConnectorViewModel> Connectors { get; set; } = new();
    public string Amenities { get; set; } = string.Empty;
    public string OpeningHours { get; set; } = "24/7";
    public decimal PricePerKWh { get; set; }
}

public class ConnectorViewModel
{
    public string Id { get; set; } = string.Empty;
    public ConnectorType Type { get; set; }
    public decimal Power { get; set; }
    public bool IsAvailable { get; set; }
    public ConnectorStatus Status { get; set; }
}