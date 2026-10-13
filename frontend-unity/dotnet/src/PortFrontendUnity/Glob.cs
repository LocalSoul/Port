using System.Collections.Generic;

namespace PortFrontendUnity;

/// <summary>Einfache Muster für <c>--classes</c>: <c>*</c> beliebig viele, <c>?</c> genau ein Zeichen.</summary>
internal static class Glob
{
    public static bool MatchesAny(IReadOnlyList<string> patterns, string name)
    {
        if (patterns.Count == 0)
        {
            return true;
        }
        foreach (string pattern in patterns)
        {
            if (Matches(pattern, 0, name, 0))
            {
                return true;
            }
        }
        return false;
    }

    private static bool Matches(string pattern, int p, string text, int t)
    {
        while (p < pattern.Length)
        {
            char c = pattern[p];
            if (c == '*')
            {
                for (int i = t; i <= text.Length; i++)
                {
                    if (Matches(pattern, p + 1, text, i))
                    {
                        return true;
                    }
                }
                return false;
            }
            if (t >= text.Length || (c != '?' && c != text[t]))
            {
                return false;
            }
            p++;
            t++;
        }
        return t == text.Length;
    }
}
