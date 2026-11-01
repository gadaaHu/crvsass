using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;
using CrvsAfr.Backend.Models;
using CrvsAfr.Backend.Services;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
public class UsersController : ControllerBase
{
    private readonly AppDbContext _context;

    public UsersController(AppDbContext context)
    {
        _context = context;
    }

    [HttpGet("api/users")]
    [HttpGet("api/auth/users")]
    public async Task<IActionResult> ListUsers([FromQuery] string? role, [FromQuery] string? search, [FromQuery] string? status)
    {
        var query = _context.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(role) && role != "ALL")
        {
            query = query.Where(u => u.Role == role);
        }

        if (!string.IsNullOrWhiteSpace(status) && status != "ALL")
        {
            query = query.Where(u => u.Status == status);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim().ToLower();
            query = query.Where(u =>
                u.Username.ToLower().Contains(term) ||
                u.FullName.ToLower().Contains(term) ||
                (u.ParticipantCode != null && u.ParticipantCode.ToLower().Contains(term)) ||
                (u.WeredaKebele != null && u.WeredaKebele.ToLower().Contains(term))
            );
        }

        var users = await query.OrderByDescending(u => u.Id)
            .Select(u => new UserDto(
                u.Id, u.Username, u.FullName, u.Role,
                u.ParticipantCode, u.WeredaKebele, u.Gender,
                u.ExperienceYears, u.Status, u.CreatedAt
            ))
            .ToListAsync();

        return Ok(new { success = true, data = users });
    }

    [HttpGet("api/users/stats")]
    public async Task<IActionResult> GetUserStats()
    {
        var users = await _context.Users.ToListAsync();
        var stats = new UserStatsDto(
            TotalUsers: users.Count,
            Admins: users.Count(u => u.Role == "ADMIN"),
            Managers: users.Count(u => u.Role == "USER_MANAGER"),
            Examinees: users.Count(u => u.Role == "EXAMINEE"),
            ActiveUsers: users.Count(u => u.Status == "ACTIVE")
        );

        return Ok(new { success = true, data = stats });
    }

    [HttpGet("api/users/{id}")]
    public async Task<IActionResult> GetUser(int id)
    {
        var user = await _context.Users.FindAsync(id);
        if (user == null)
            return NotFound(new { success = false, error = "User not found" });

        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return Ok(new { success = true, data = userDto });
    }

    [HttpPost("api/users")]
    public async Task<IActionResult> CreateUser([FromBody] CreateUserRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.Username) || string.IsNullOrWhiteSpace(req.Password) || string.IsNullOrWhiteSpace(req.FullName))
        {
            return BadRequest(new { success = false, error = "Username, password, and full name are required" });
        }

        var username = req.Username.Trim().ToLowerInvariant();
        if (await _context.Users.AnyAsync(u => u.Username.ToLower() == username))
        {
            return BadRequest(new { success = false, error = "Username is already taken" });
        }

        var salt = PasswordHasher.GenerateSalt();
        var user = new User
        {
            Username = username,
            PasswordHash = PasswordHasher.HashPassword(req.Password, salt),
            Salt = salt,
            FullName = req.FullName.Trim(),
            Role = req.Role == "ADMIN" ? "ADMIN" : req.Role == "USER_MANAGER" ? "USER_MANAGER" : "EXAMINEE",
            ParticipantCode = req.ParticipantCode,
            WeredaKebele = req.WeredaKebele,
            Gender = req.Gender,
            ExperienceYears = req.ExperienceYears,
            Status = req.Status ?? "ACTIVE",
            CreatedAt = DateTime.UtcNow
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return StatusCode(201, new { success = true, data = userDto });
    }

    [HttpPut("api/users/{id}")]
    public async Task<IActionResult> UpdateUser(int id, [FromBody] UpdateUserRequest req)
    {
        var user = await _context.Users.FindAsync(id);
        if (user == null)
            return NotFound(new { success = false, error = "User not found" });

        if (!string.IsNullOrWhiteSpace(req.FullName))
            user.FullName = req.FullName.Trim();

        if (!string.IsNullOrWhiteSpace(req.Role) && (req.Role == "ADMIN" || req.Role == "USER_MANAGER" || req.Role == "EXAMINEE"))
            user.Role = req.Role;

        if (req.ParticipantCode != null)
            user.ParticipantCode = req.ParticipantCode.Trim();

        if (req.WeredaKebele != null)
            user.WeredaKebele = req.WeredaKebele.Trim();

        if (req.Gender != null)
            user.Gender = req.Gender;

        if (req.ExperienceYears.HasValue)
            user.ExperienceYears = req.ExperienceYears.Value;

        if (!string.IsNullOrWhiteSpace(req.Status))
            user.Status = req.Status;

        await _context.SaveChangesAsync();

        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return Ok(new { success = true, data = userDto });
    }

    [HttpPatch("api/users/{id}/role")]
    [HttpPatch("api/auth/users/{id}/role")]
    public async Task<IActionResult> UpdateRole(int id, [FromBody] UpdateRoleRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.Role) || (req.Role != "ADMIN" && req.Role != "USER_MANAGER" && req.Role != "EXAMINEE"))
        {
            return BadRequest(new { success = false, error = "Valid role (ADMIN, USER_MANAGER, EXAMINEE) is required" });
        }

        var user = await _context.Users.FindAsync(id);
        if (user == null)
            return NotFound(new { success = false, error = "User not found" });

        user.Role = req.Role;
        await _context.SaveChangesAsync();

        var userDto = new UserDto(
            user.Id, user.Username, user.FullName, user.Role,
            user.ParticipantCode, user.WeredaKebele, user.Gender,
            user.ExperienceYears, user.Status, user.CreatedAt
        );

        return Ok(new { success = true, data = userDto });
    }

    [HttpPatch("api/users/{id}/password")]
    public async Task<IActionResult> ResetPassword(int id, [FromBody] ResetPasswordRequest req)
    {
        if (string.IsNullOrWhiteSpace(req.NewPassword) || req.NewPassword.Length < 4)
        {
            return BadRequest(new { success = false, error = "Password must be at least 4 characters" });
        }

        var user = await _context.Users.FindAsync(id);
        if (user == null)
            return NotFound(new { success = false, error = "User not found" });

        var salt = PasswordHasher.GenerateSalt();
        user.PasswordHash = PasswordHasher.HashPassword(req.NewPassword, salt);
        user.Salt = salt;

        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Password reset successfully" });
    }

    [HttpDelete("api/users/{id}")]
    [HttpDelete("api/auth/users/{id}")]
    public async Task<IActionResult> DeleteUser(int id)
    {
        var user = await _context.Users.FindAsync(id);
        if (user == null)
            return NotFound(new { success = false, message = "User not found" });

        _context.Users.Remove(user);
        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "User deleted successfully" });
    }
}
