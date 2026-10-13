using System;

namespace PortFrontendUnity;

/// <summary>
/// Ein Konstrukt lässt sich (noch) nicht in IR abbilden. Wird auf Statement- bzw. Member-Ebene
/// gefangen und dort zu <c>(raw cs "...")</c> plus <c>:manual "Grund"</c> (docs/ir-spec.md Abschnitt 9).
/// </summary>
internal sealed class UnsupportedConstructException : Exception
{
    public UnsupportedConstructException(string reason)
        : base(reason)
    {
    }

    /// <summary>Grund für <c>:manual</c>.</summary>
    public string Reason => Message;
}
