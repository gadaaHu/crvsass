using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using CrvsAfr.Backend.Models;

namespace CrvsAfr.Backend.Services;

public class TokenService
{
    private readonly string _secretKey;
    private readonly int _ttlSeconds;

    public TokenService(IConfiguration config)
    {
        _secretKey = config["Jwt:Secret"] ?? "crvs_secure_hmac_secret_2026_diredawa_crvsafr_prod_key";
        _ttlSeconds = 7 * 24 * 60 * 60; // 7 days
    }

    public string GenerateToken(User user)
    {
        var tokenHandler = new JwtSecurityTokenHandler();
        var key = Encoding.UTF8.GetBytes(_secretKey);

        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(new[]
            {
                new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
                new Claim(ClaimTypes.Name, user.Username),
                new Claim(ClaimTypes.Role, user.Role),
                new Claim("fullname", user.FullName),
                new Claim("role", user.Role)
            }),
            Expires = DateTime.UtcNow.AddSeconds(_ttlSeconds),
            SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
        };

        var token = tokenHandler.CreateToken(tokenDescriptor);
        return tokenHandler.WriteToken(token);
    }
}
