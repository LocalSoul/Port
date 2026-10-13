using System.Collections.Generic;

namespace PortFrontendUnity;

/// <summary>Kommandozeile: <c>--assembly &lt;dll&gt; --out &lt;dir&gt; [--classes &lt;glob&gt;]... [--references &lt;dir&gt;]... [--no-sha256]</c>.</summary>
internal sealed class Options
{
    public string Assembly { get; private set; } = "";
    public string OutDir { get; private set; } = "";
    public List<string> Classes { get; } = new();
    public List<string> References { get; } = new();
    public bool IncludeSha256 { get; private set; } = true;

    /// <summary>Liest die Argumente; Fehler als Meldung, dann <c>null</c>.</summary>
    public static Options? Parse(string[] args, out string? error)
    {
        var options = new Options();
        error = null;
        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            if (arg == "--no-sha256")
            {
                options.IncludeSha256 = false;
                continue;
            }
            if (i + 1 >= args.Length)
            {
                error = "Wert fehlt für " + arg;
                return null;
            }
            string value = args[++i];
            switch (arg)
            {
                case "--assembly": options.Assembly = value; break;
                case "--out": options.OutDir = value; break;
                case "--classes": options.Classes.AddRange(value.Split(',')); break;
                case "--references": options.References.Add(value); break;
                default:
                    error = "unbekannte Option " + arg;
                    return null;
            }
        }
        if (options.Assembly.Length == 0 || options.OutDir.Length == 0)
        {
            error = "Aufruf: port-frontend-unity --assembly <dll> --out <dir> [--classes <glob>] [--references <dir>] [--no-sha256]";
            return null;
        }
        return options;
    }
}
