using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;
using CrvsAfr.Backend.Models;
using CrvsAfr.Backend.Services;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext _context;
    private readonly TokenService _tokenService;

    public AuthController(AppDbContext context, TokenService tokenService)
    {
        _context = context;
        _tokenService = tokenService;
    }

    [HttpPost("signin")]
    [HttpPost("login")]
    public async Task<IActionResult> SignIn([FromBody] LoginRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.Username) || string.IsNullOrWhiteSpace(req.Password))
        {
            return BadRequest(new { success = false, error = "Username and password are required" });
        }

        var username = req.Username.Trim().ToLowerInvariant();
        var user = await _context.Users.FirstOrDefaultAsync(u => u.Username.ToLower() == username);

        if (user == null || !PasswordHasher.VerifyPassword(req.Password, user.PasswordHash, user.Salt))
        {
            return Unauthorized(new { success = false, error = "Invalid username or password" });
        }

        var token = _tokenService.GenerateToken(user);
        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return Ok(new { success = true, data = new AuthResponse(userDto, token) });
    }

    [HttpPost("signup")]
    public async Task<IActionResult> SignUp([FromBody] SignupRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.Username) || req.Username.Length < 3)
            return BadRequest(new { success = false, error = "Username must be at least 3 characters" });
        if (string.IsNullOrWhiteSpace(req.Password) || req.Password.Length < 4)
            return BadRequest(new { success = false, error = "Password must be at least 4 characters" });
        if (string.IsNullOrWhiteSpace(req.FullName))
            return BadRequest(new { success = false, error = "Full name is required" });

        var username = req.Username.Trim().ToLowerInvariant();
        if (await _context.Users.AnyAsync(u => u.Username.ToLower() == username))
        {
            return BadRequest(new { success = false, error = "Username is already taken" });
        }

        var role = req.Role == "ADMIN" ? "ADMIN" : req.Role == "USER_MANAGER" ? "USER_MANAGER" : "EXAMINEE";
        var partCode = req.ParticipantCode;
        if (role == "EXAMINEE" && string.IsNullOrWhiteSpace(partCode))
        {
            partCode = $"DD-REG-{Random.Shared.Next(100, 999)}";
        }

        var salt = PasswordHasher.GenerateSalt();
        var user = new User
        {
            Username = username,
            PasswordHash = PasswordHasher.HashPassword(req.Password, salt),
            Salt = salt,
            FullName = req.FullName.Trim(),
            Role = role,
            ParticipantCode = partCode,
            WeredaKebele = req.WeredaKebele ?? "Kebele 01",
            Gender = req.Gender ?? "Not specified",
            ExperienceYears = req.ExperienceYears,
            Status = "ACTIVE",
            CreatedAt = DateTime.UtcNow
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        var token = _tokenService.GenerateToken(user);
        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return StatusCode(201, new { success = true, data = new AuthResponse(userDto, token) });
    }

    [Authorize]
    [HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        var idClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(idClaim) || !int.TryParse(idClaim, out var userId))
        {
            return Unauthorized(new { success = false, error = "Invalid token claims" });
        }

        var user = await _context.Users.FindAsync(userId);
        if (user == null)
        {
            return NotFound(new { success = false, error = "User not found" });
        }

        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return Ok(new { success = true, data = new { user = userDto } });
    }
}
