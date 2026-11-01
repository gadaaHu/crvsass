using System.Security.Cryptography;
using System.Text;

namespace CrvsAfr.Backend.Services;

public static class PasswordHasher
{
    public static string GenerateSalt()
    {
        var bytes = RandomNumberGenerator.GetBytes(16);
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    public static string HashPassword(string password, string salt, int iterations = 100000)
    {
        var passwordBytes = Encoding.UTF8.GetBytes(password);
        var saltBytes = Encoding.UTF8.GetBytes(salt);
        var hash = Rfc2898DeriveBytes.Pbkdf2(passwordBytes, saltBytes, iterations, HashAlgorithmName.SHA512, 64);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    public static bool VerifyPassword(string password, string storedHash, string salt)
    {
        var calcHash100k = HashPassword(password, salt, 100000);
        if (CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(calcHash100k),
            Encoding.UTF8.GetBytes(storedHash.ToLowerInvariant())))
        {
            return true;
        }

        // Check legacy 1000 iterations
        var calcHash1k = HashPassword(password, salt, 1000);
        return CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(calcHash1k),
            Encoding.UTF8.GetBytes(storedHash.ToLowerInvariant()));
    }
}
