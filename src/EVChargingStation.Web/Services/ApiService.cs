/*
 * File: ApiService.cs
 * Purpose: Provides API service methods for HTTP communication in the EV Charging Station Web application
 * Author: EAD Assignment
 * Date: 2025
 */

namespace EVChargingStation.Web.Services;

public class ApiSettings
{
    public string BaseUrl { get; set; } = string.Empty;
}

public interface IApiService
{
    Task<T?> GetAsync<T>(string endpoint, string? token = null);
    Task<T?> PostAsync<T>(string endpoint, object data, string? token = null);
    Task<T?> PutAsync<T>(string endpoint, object data, string? token = null);
    Task<bool> PatchAsync(string endpoint, object data, string? token = null);
    Task<bool> DeleteAsync(string endpoint, string? token = null);
}

public class ApiService : IApiService
{
    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;

    public ApiService(HttpClient httpClient, IConfiguration configuration)
    {
        _httpClient = httpClient;
        _baseUrl = configuration["ApiSettings:BaseUrl"] ?? "https://localhost:7000";
    }

    public async Task<T?> GetAsync<T>(string endpoint, string? token = null)
    {
        var request = new HttpRequestMessage(HttpMethod.Get, $"{_baseUrl}{endpoint}");
        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        var response = await _httpClient.SendAsync(request);
        if (response.IsSuccessStatusCode)
        {
            var content = await response.Content.ReadAsStringAsync();
            return Newtonsoft.Json.JsonConvert.DeserializeObject<T>(content);
        }

        return default(T);
    }

    public async Task<T?> PostAsync<T>(string endpoint, object data, string? token = null)
    {
        var request = new HttpRequestMessage(HttpMethod.Post, $"{_baseUrl}{endpoint}")
        {
            Content = new StringContent(
                Newtonsoft.Json.JsonConvert.SerializeObject(data),
                System.Text.Encoding.UTF8,
                "application/json")
        };

        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        var response = await _httpClient.SendAsync(request);
        if (response.IsSuccessStatusCode)
        {
            var content = await response.Content.ReadAsStringAsync();
            return Newtonsoft.Json.JsonConvert.DeserializeObject<T>(content);
        }

        return default(T);
    }

    public async Task<T?> PutAsync<T>(string endpoint, object data, string? token = null)
    {
        var request = new HttpRequestMessage(HttpMethod.Put, $"{_baseUrl}{endpoint}")
        {
            Content = new StringContent(
                Newtonsoft.Json.JsonConvert.SerializeObject(data),
                System.Text.Encoding.UTF8,
                "application/json")
        };

        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        var response = await _httpClient.SendAsync(request);
        if (response.IsSuccessStatusCode)
        {
            var content = await response.Content.ReadAsStringAsync();
            return Newtonsoft.Json.JsonConvert.DeserializeObject<T>(content);
        }

        return default(T);
    }

    public async Task<bool> PatchAsync(string endpoint, object data, string? token = null)
    {
        var request = new HttpRequestMessage(HttpMethod.Patch, $"{_baseUrl}{endpoint}")
        {
            Content = new StringContent(
                Newtonsoft.Json.JsonConvert.SerializeObject(data),
                System.Text.Encoding.UTF8,
                "application/json")
        };

        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        var response = await _httpClient.SendAsync(request);
        return response.IsSuccessStatusCode;
    }

    public async Task<bool> DeleteAsync(string endpoint, string? token = null)
    {
        var request = new HttpRequestMessage(HttpMethod.Delete, $"{_baseUrl}{endpoint}");
        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        var response = await _httpClient.SendAsync(request);
        return response.IsSuccessStatusCode;
    }
}