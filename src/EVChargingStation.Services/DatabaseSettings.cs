namespace EVChargingStation.Services;

public class DatabaseSettings
{
    public string ConnectionString { get; set; } = string.Empty;
    public string DatabaseName { get; set; } = string.Empty;
    public string UsersCollectionName { get; set; } = "Users";
    public string ChargingStationsCollectionName { get; set; } = "ChargingStations";
    public string BookingsCollectionName { get; set; } = "Bookings";
}