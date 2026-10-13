using System.Collections.Generic;

namespace PortFrontendUnity;

/// <summary>Sammelt Gründe für <c>:manual</c> eines Members, während sein Rumpf übersetzt wird.</summary>
internal sealed class MemberContext
{
    private readonly List<string> reasons = new();
    private readonly Report report;

    public MemberContext(string symbol, Report report)
    {
        Symbol = symbol;
        this.report = report;
    }

    /// <summary>Stabile Symbol-ID des Members, für Meldungen.</summary>
    public string Symbol { get; }

    public void Manual(string reason)
    {
        if (!reasons.Contains(reason))
        {
            reasons.Add(reason);
        }
    }

    public void Raw(string reason)
    {
        Manual(reason);
        report.Raw(Symbol, reason);
    }

    public void Note(string text) => report.Note(Symbol, text);

    /// <summary>Text für <c>:manual</c>, oder <c>null</c> ohne offene Stellen.</summary>
    public string? ManualText => reasons.Count == 0 ? null : string.Join("; ", reasons);
}
