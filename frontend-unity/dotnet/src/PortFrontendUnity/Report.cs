using System.Collections.Generic;
using System.IO;

namespace PortFrontendUnity;

/// <summary>Zählt Ergebnisse und Lücken für die Zusammenfassung auf stderr (nie still verwerfen).</summary>
internal sealed class Report
{
    private readonly List<string> messages = new();

    public int Types { get; private set; }
    public int Members { get; private set; }
    public int RawSites { get; private set; }
    public int SkippedTypes { get; private set; }

    public void TypeTranslated() => Types++;

    public void MemberTranslated() => Members++;

    public void Raw(string symbol, string reason)
    {
        RawSites++;
        messages.Add("raw: " + symbol + ": " + reason);
    }

    public void SkippedType(string name, string reason)
    {
        SkippedTypes++;
        messages.Add("übersprungen: " + name + ": " + reason);
    }

    public void Note(string symbol, string text) => messages.Add("hinweis: " + symbol + ": " + text);

    public void WriteTo(TextWriter writer)
    {
        foreach (string message in messages)
        {
            writer.WriteLine(message);
        }
        writer.WriteLine(
            "port-frontend-unity: " + Types + " Typen, " + Members + " Mitglieder, " + RawSites
            + " raw-Stellen, " + SkippedTypes + " übersprungene Typen");
    }
}
