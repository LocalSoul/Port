using System.Globalization;
using System.Text;

namespace PortFrontendUnity;

/// <summary>
/// Bildet C#-Namen auf IR-Bezeichner <c>[A-Za-z_][A-Za-z0-9_]*</c> ab. Unzulässige Zeichen werden
/// deterministisch als <c>_uXXXX</c> kodiert (z. B. Umlaute, <c>&lt;</c> in Compiler-Namen), damit
/// Deklaration und Referenz denselben Namen tragen.
/// </summary>
internal static class Names
{
    public static string Ident(string? name)
    {
        if (name == null)
        {
            throw new UnsupportedConstructException("fehlender Name");
        }
        var sb = new StringBuilder(name.Length);
        for (int i = 0; i < name.Length; i++)
        {
            char c = name[i];
            bool letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
            bool digit = c >= '0' && c <= '9';
            if (letter || (digit && i > 0))
            {
                sb.Append(c);
            }
            else
            {
                sb.Append("_u").Append(((int)c).ToString("X4", CultureInfo.InvariantCulture));
            }
        }
        return sb.Length == 0 ? "_" : sb.ToString();
    }

    /// <summary>Lokale Variable oder Parameter (ohne <c>@</c>-Präfix).</summary>
    public static string Local(string? name) => Ident(name?.TrimStart('@'));
}
