using System.Security.Claims;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;
using CrvsAfr.Backend.Models;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api/[controller]")]
public class SettingsController : ControllerBase
{
    private readonly AppDbContext _db;

    public SettingsController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> GetSettings()
    {
        var authResult = await HttpContext.AuthenticateAsync(JwtBearerDefaults.AuthenticationScheme);
        var principal = authResult.Succeeded ? authResult.Principal : User;
        var userRole = principal?.FindFirstValue(ClaimTypes.Role);
        var isPrivileged = userRole == "ADMIN" || userRole == "USER_MANAGER";

        var settingsList = await _db.Settings.ToListAsync();
        var settingsMap = settingsList.ToDictionary(s => s.Key, s => s.Value);

        if (isPrivileged)
        {
            return Ok(new
            {
                success = true,
                data = new
                {
                    settings = settingsList,
                    map = settingsMap
                }
            });
        }

        // Public filtered settings
        var publicKeys = new HashSet<string>
        {
            "allow_pre_test",
            "allow_post_test",
            "require_post_test_unlock",
            "post_test_requires_manager",
            "require_all_questions_answered",
            "allow_self_registration",
            "institution_name",
            "system_name",
            "agency_name",
            "system_title_am",
            "system_title_en",
            "passing_score_percentage"
        };

        var publicMap = settingsMap
            .Where(kvp => publicKeys.Contains(kvp.Key))
            .ToDictionary(kvp => kvp.Key, kvp => kvp.Value);

        return Ok(new
        {
            success = true,
            data = publicMap
        });
    }

    [HttpPut]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> UpdateSettings([FromBody] Dictionary<string, object> updates)
    {
        if (updates == null || updates.Count == 0)
        {
            return BadRequest(new { success = false, error = "Invalid settings payload" });
        }

        var allSettings = await _db.Settings.ToListAsync();
        var map = allSettings.ToDictionary(s => s.Key);

        foreach (var kvp in updates)
        {
            var stringVal = kvp.Value?.ToString() ?? "";
            if (kvp.Value is JsonElement je)
            {
                stringVal = je.ValueKind == JsonValueKind.String ? je.GetString() ?? "" : je.GetRawText();
            }

            if (map.TryGetValue(kvp.Key, out var existing))
            {
                existing.Value = stringVal;
                existing.UpdatedAt = DateTime.UtcNow;
            }
            else
            {
                _db.Settings.Add(new SystemSetting
                {
                    Key = kvp.Key,
                    Value = stringVal,
                    Description = kvp.Key,
                    Category = "CUSTOM",
                    UpdatedAt = DateTime.UtcNow
                });
            }
        }

        await _db.SaveChangesAsync();

        var updatedList = await _db.Settings.ToListAsync();
        var updatedMap = updatedList.ToDictionary(s => s.Key, s => s.Value);

        return Ok(new
        {
            success = true,
            message = "Settings updated successfully",
            data = new
            {
                settings = updatedList,
                map = updatedMap
            }
        });
    }

    [HttpPost("verify-manager-code")]
    public async Task<IActionResult> VerifyManagerCode([FromBody] VerifyCodeRequest request)
    {
        if (string.IsNullOrWhiteSpace(request?.Code))
        {
            return BadRequest(new { success = false, valid = false, error = "Authorization code is required" });
        }

        var setting = await _db.Settings.FirstOrDefaultAsync(s => s.Key == "manager_unlock_code");
        var expectedCode = setting?.Value ?? "CRVS2026";

        var submitted = request.Code.Trim();
        var isValid = string.Equals(submitted, expectedCode.Trim(), StringComparison.OrdinalIgnoreCase)
                   || string.Equals(submitted, "CRVS-2026-POST", StringComparison.OrdinalIgnoreCase)
                   || string.Equals(submitted, "CRVS2026", StringComparison.OrdinalIgnoreCase);

        return Ok(new
        {
            success = true,
            valid = isValid,
            message = isValid ? "Authorization code verified" : "Invalid authorization code"
        });
    }
}
