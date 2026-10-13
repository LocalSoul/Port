using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace PortFrontendUnity;

/// <summary>S-Expression der IR: Atom oder Liste. Rohausgabe, kanonisiert wird im Java-Printer.</summary>
internal abstract record Sx;

/// <summary>Fertig formatiertes Atom (Bezeichner, Typ, Member-Referenz, Literal, Keyword).</summary>
internal sealed record SAtom(string Text) : Sx;

/// <summary>Liste <c>( ... )</c>.</summary>
internal sealed record SList(IReadOnlyList<Sx> Items) : Sx;

/// <summary>Kurzschreibweisen zum Bauen von S-Expressions.</summary>
internal static class S
{
    public static SAtom A(string text) => new(text);

    public static SList L(params Sx[] items) => new(items);

    public static SList L(IEnumerable<Sx> items) => new(new List<Sx>(items));

    /// <summary>String-Literal mit den Escapes aus docs/ir-spec.md Abschnitt 2.</summary>
    public static SAtom Str(string value)
    {
        var sb = new StringBuilder(value.Length + 2);
        sb.Append('"');
        for (int i = 0; i < value.Length; i++)
        {
            char c = value[i];
            switch (c)
            {
                case '\\': sb.Append("\\\\"); break;
                case '"': sb.Append("\\\""); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': sb.Append("\\r"); break;
                case '\t': sb.Append("\\t"); break;
                default:
                    if (c < 0x20 || c == 0x7F || IsLoneSurrogate(value, i))
                    {
                        sb.Append("\\u").Append(((int)c).ToString("X4", CultureInfo.InvariantCulture));
                    }
                    else
                    {
                        sb.Append(c);
                    }
                    break;
            }
        }
        sb.Append('"');
        return new SAtom(sb.ToString());
    }

    private static bool IsLoneSurrogate(string value, int index)
    {
        char c = value[index];
        if (char.IsHighSurrogate(c))
        {
            return index + 1 >= value.Length || !char.IsLowSurrogate(value[index + 1]);
        }
        if (char.IsLowSurrogate(c))
        {
            return index == 0 || !char.IsHighSurrogate(value[index - 1]);
        }
        return false;
    }

    /// <summary>Gibt eine S-Expression aus: kurze Listen einzeilig, sonst ein Element je Zeile.</summary>
    public static string Render(Sx sx)
    {
        var sb = new StringBuilder();
        Render(sx, 0, sb);
        return sb.ToString();
    }

    private static void Render(Sx sx, int indent, StringBuilder sb)
    {
        string flat = Flat(sx);
        if (sx is SAtom || indent + flat.Length <= 100)
        {
            sb.Append(flat);
            return;
        }
        var list = (SList)sx;
        sb.Append('(');
        for (int i = 0; i < list.Items.Count; i++)
        {
            if (i > 0)
            {
                sb.Append('\n').Append(' ', indent + 2);
            }
            Render(list.Items[i], indent + 2, sb);
        }
        sb.Append(')');
    }

    private static string Flat(Sx sx)
    {
        if (sx is SAtom atom)
        {
            return atom.Text;
        }
        var sb = new StringBuilder("(");
        var list = (SList)sx;
        for (int i = 0; i < list.Items.Count; i++)
        {
            if (i > 0)
            {
                sb.Append(' ');
            }
            sb.Append(Flat(list.Items[i]));
        }
        return sb.Append(')').ToString();
    }
}
