/*
 * File: UserService.cs
 * Purpose: Implements user-related business logic for the EV Charging Station application
 * Author: EAD Assignment
 * Date: 2025
 */

using EVChargingStation.Models;
using EVChargingStation.Services.Interfaces;
using MongoDB.Driver;
using Microsoft.Extensions.Options;
using Microsoft.Extensions.Configuration;
using System.Security.Cryptography;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace EVChargingStation.Services.Implementations;

public class UserService : IUserService
{
    private readonly IMongoCollection<User> _users;
    private readonly string _jwtSecret;

    public UserService(IOptions<DatabaseSettings> databaseSettings, IConfiguration configuration)
    {
        var mongoClient = new MongoClient(databaseSettings.Value.ConnectionString);
        var mongoDatabase = mongoClient.GetDatabase(databaseSettings.Value.DatabaseName);
        _users = mongoDatabase.GetCollection<User>(databaseSettings.Value.UsersCollectionName);

        var indexKeys = Builders<User>.IndexKeys.Ascending(u => u.NIC);
        var indexOptions = new CreateIndexOptions { Unique = true, Sparse = true };
        var indexModel = new CreateIndexModel<User>(indexKeys, indexOptions);
        _users.Indexes.CreateOne(indexModel);

        _jwtSecret = configuration["JwtSettings:Secret"] ?? "your-secret-key-here";
    }

    public async Task<IEnumerable<User>> GetAllUsersAsync()
    {
        return await _users.Find(user => !user.IsDeleted).ToListAsync();
    }

    public async Task<User?> GetUserByIdAsync(string nic)
    {
        return await _users.Find(user => user.NIC == nic && !user.IsDeleted).FirstOrDefaultAsync();
    }

    public async Task<User?> GetUserByNICAsync(string nic)
    {
        return await _users.Find(user => user.NIC == nic && !user.IsDeleted).FirstOrDefaultAsync();
    }

    public async Task<User?> GetUserByEmailAsync(string email)
    {
        return await _users.Find(user => user.Email == email && !user.IsDeleted).FirstOrDefaultAsync();
    }

    public async Task<User> CreateUserAsync(User user)
    {
        user.PasswordHash = HashPassword(user.PasswordHash);
        await _users.InsertOneAsync(user);
        return user;
    }

    public async Task<User> UpdateUserAsync(User user)
    {
        user.UpdatedAt = DateTime.UtcNow;
       await _users.ReplaceOneAsync(u => u.NIC == user.NIC, user);
        return user;
    }

    public async Task<bool> DeleteUserAsync(string nic)
    {
        var update = Builders<User>.Update.Set(u => u.IsDeleted, true).Set(u => u.UpdatedAt, DateTime.UtcNow);
        var result = await _users.UpdateOneAsync(u => u.NIC == nic, update);
        return result.ModifiedCount > 0;
    }

    public async Task<bool> AuthenticateUserAsync(string email, string password)
    {
        var user = await GetUserByEmailAsync(email);
        if (user == null || !user.IsActive) return false;
        
        return VerifyPassword(password, user.PasswordHash);
    }

    public Task<string> GenerateJwtTokenAsync(User user)
    {
        var tokenHandler = new JwtSecurityTokenHandler();
        var key = Encoding.ASCII.GetBytes(_jwtSecret);
        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(new[]
            {
                new Claim(ClaimTypes.NameIdentifier, user.NIC),
                new Claim(ClaimTypes.Email, user.Email),
                new Claim(ClaimTypes.Name, $"{user.FirstName} {user.LastName}"),
                new Claim(ClaimTypes.Role, user.Role.ToString())
            }),
            Expires = DateTime.UtcNow.AddDays(7),
            SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
        };
        var token = tokenHandler.CreateToken(tokenDescriptor);
        return Task.FromResult(tokenHandler.WriteToken(token));
    }

    private string HashPassword(string password)
    {
        using var sha256 = SHA256.Create();
        var hashedBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(password));
        return Convert.ToBase64String(hashedBytes);
    }

    private bool VerifyPassword(string password, string hashPassword)
    {
        var hashedInputPassword = HashPassword(password);
        return hashedInputPassword == hashPassword;
    }
}