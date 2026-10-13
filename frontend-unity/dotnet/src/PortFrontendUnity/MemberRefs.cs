using System.Text;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>Member-Referenzen mit Signatur, zugleich stabile Symbol-IDs (docs/ir-spec.md Abschnitt 4).</summary>
internal static class MemberRefs
{
    public static string Of(IMember member)
    {
        IType declaring = member.DeclaringType;
        if (declaring.TypeArguments.Count > 0)
        {
            throw new UnsupportedConstructException(
                "Member eines generischen Typs " + declaring.FullName + " (Spec-Lücke vor M3a)");
        }
        var sb = new StringBuilder(TypeNames.QualifiedName(declaring));
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
        if (method.TypeArguments.Count == 0)
        {
            return;
        }
        sb.Append('<');
        for (int i = 0; i < method.TypeArguments.Count; i++)
        {
            if (i > 0)
            {
                sb.Append(',');
            }
            sb.Append(TypeNames.Of(method.TypeArguments[i]));
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
