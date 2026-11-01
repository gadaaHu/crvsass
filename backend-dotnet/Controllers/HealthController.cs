using Microsoft.AspNetCore.Mvc;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api/[controller]")]
public class HealthController : ControllerBase
{
    [HttpGet]
    public IActionResult GetHealth()
    {
        return Ok(new
        {
            status = "ok",
            service = "CRVS Assessment & Question Gathering System (.NET Edition)",
            runtime = ".NET 8.0 / C#",
            version = "2.0.0",
            timestamp = DateTime.UtcNow.ToString("o")
        });
    }
}
