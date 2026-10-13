using System.Collections.Generic;
using System.Reflection.Metadata;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>Attribute für <c>:attrs</c>: nur positionale Argumente, Compiler-Attribute entfallen.</summary>
internal static class Attributes
{
    private static readonly string[] CompilerNamespaces =
    {
        "System.Runtime.CompilerServices",
        "System.Diagnostics",
        "System.Reflection",
        "Microsoft.CodeAnalysis",
    };

    public static bool IsCompilerAttribute(IAttribute attribute)
    {
        string ns = attribute.AttributeType.Namespace;
        foreach (string compilerNamespace in CompilerNamespaces)
        {
            if (ns == compilerNamespace)
            {
                return true;
            }
        }
        return false;
    }

    public static Sx Translate(IAttribute attribute)
    {
        if (attribute.NamedArguments.Length > 0)
        {
            throw new UnsupportedConstructException("benannte Attributargumente");
        }
        var items = new List<Sx> { S.A(TypeNames.QualifiedName(attribute.AttributeType)) };
        foreach (CustomAttributeTypedArgument<IType> argument in attribute.FixedArguments)
        {
            items.Add(Argument(argument));
        }
        return S.L(items);
    }

    private static Sx Argument(CustomAttributeTypedArgument<IType> argument)
    {
        if (argument.Value is IType type)
        {
            return S.L(S.A("typeof"), S.A(TypeNames.Of(type)));
        }
        if (argument.Type.Kind == TypeKind.Enum)
        {
            throw new UnsupportedConstructException("Enum-Wert als Attributargument");
        }
        if (argument.Type.Kind == TypeKind.Array)
        {
            throw new UnsupportedConstructException("Array als Attributargument");
        }
        return Literals.Constant(argument.Value, argument.Type);
    }
}
