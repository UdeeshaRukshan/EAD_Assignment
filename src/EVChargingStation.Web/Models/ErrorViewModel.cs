/*
 * File: ErrorViewModel.cs
 * Purpose: Represents error information for the EV Charging Station Web application
 * Author: EAD Assignment
 * Date: 2025
 */

namespace EVChargingStation.Web.Models;

public class ErrorViewModel
{
    public string? RequestId { get; set; }

    public bool ShowRequestId => !string.IsNullOrEmpty(RequestId);
}
