using System.Text;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>
/// Member-Referenzen mit Signatur (docs/ir-spec.md Abschnitt 4, ADR-025): Typargumente der Instanz am
/// deklarierenden Typ, Parametertypen immer aus der Deklaration (offen).
/// </summary>
internal static class MemberRefs
{
    public static string Of(IMember member)
    {
        IType declaring = member.DeclaringType;
        var sb = new StringBuilder(TypeNames.QualifiedName(declaring));
        AppendTypeList(declaring.TypeArguments, sb);
        sb.Append("::").Append(MemberName(member.Name));
        if (member is IMethod method)
        {
            AppendTypeArguments(method, sb);
            AppendParameters((IMethod)method.MemberDefinition, sb);
        }
        return sb.ToString();
    }

    /// <summary>Konstruktornamen bleiben, alles andere wird zum IR-Bezeichner.</summary>
    public static string MemberName(string name)
    {
        return name is ".ctor" or ".cctor" ? name : Names.Ident(name);
    }

    private static void AppendTypeArguments(IMethod method, StringBuilder sb)
    {
        AppendTypeList(method.TypeArguments, sb);
    }

    /// <summary>Typargumente der Instanz (ADR-025), z. B. <c>&lt;UnityEngine.Transform&gt;</c>.</summary>
    private static void AppendTypeList(System.Collections.Generic.IReadOnlyList<IType> types, StringBuilder sb)
    {
        if (types.Count == 0)
        {
            return;
        }
        sb.Append('<');
        for (int i = 0; i < types.Count; i++)
        {
            if (i > 0)
            {
                sb.Append(',');
            }
            sb.Append(TypeNames.Of(types[i]));
        }
        sb.Append('>');
    }

    /// <summary>Parametertypen der Deklaration (nicht der Instanziierung), wie in der Symbol-ID.</summary>
    private static void AppendParameters(IMethod definition, StringBuilder sb)
    {
        sb.Append('#');
        for (int i = 0; i < definition.Parameters.Count; i++)
        {
            if (i > 0)
            {
                sb.Append(',');
            }
            sb.Append(TypeNames.Of(definition.Parameters[i].Type));
        }
    }
}
