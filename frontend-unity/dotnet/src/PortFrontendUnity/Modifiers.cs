using System.Collections.Generic;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>Modifier nach docs/ir-spec.md Abschnitt 5, in fester Reihenfolge.</summary>
internal static class Modifiers
{
    public static IReadOnlyList<string> ForType(ITypeDefinition type)
    {
        var result = Accessibility(type.Accessibility);
        if (type.IsStatic)
        {
            result.Add("static");
        }
        else if (type.Kind == TypeKind.Class)
        {
            if (type.IsAbstract)
            {
                result.Add("abstract");
            }
            if (type.IsSealed)
            {
                result.Add("sealed");
            }
        }
        return result;
    }

    public static IReadOnlyList<string> ForField(IField field)
    {
        var result = Accessibility(field.Accessibility);
        if (field.IsConst)
        {
            result.Add("const");
            return result;
        }
        if (field.IsStatic)
        {
            result.Add("static");
        }
        if (field.IsReadOnly)
        {
            result.Add("readonly");
        }
        return result;
    }

    public static IReadOnlyList<string> ForMember(IMember member)
    {
        var result = Accessibility(member.Accessibility);
        if (member.IsStatic)
        {
            result.Add("static");
        }
        if (member.IsAbstract)
        {
            result.Add("abstract");
        }
        else if (member.IsOverride)
        {
            result.Add("override");
            if (member.IsSealed)
            {
                result.Add("sealed");
            }
        }
        else if (member.IsVirtual)
        {
            result.Add("virtual");
        }
        return result;
    }

    private static List<string> Accessibility(Accessibility accessibility)
    {
        return accessibility switch
        {
            ICSharpCode.Decompiler.TypeSystem.Accessibility.Public => new List<string> { "public" },
            ICSharpCode.Decompiler.TypeSystem.Accessibility.Private => new List<string> { "private" },
            ICSharpCode.Decompiler.TypeSystem.Accessibility.Protected => new List<string> { "protected" },
            ICSharpCode.Decompiler.TypeSystem.Accessibility.Internal => new List<string> { "internal" },
            ICSharpCode.Decompiler.TypeSystem.Accessibility.ProtectedOrInternal => new List<string> { "protected", "internal" },
            ICSharpCode.Decompiler.TypeSystem.Accessibility.ProtectedAndInternal => new List<string> { "private", "protected" },
            _ => new List<string>(),
        };
    }
}
