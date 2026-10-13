using System.Collections.Generic;
using System.Text;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>Übersetzt Typen des Dekompilers in IR-Typ-Atome (docs/ir-spec.md Abschnitt 3).</summary>
internal static class TypeNames
{
    /// <summary>Platzhalter für nicht auflösbare Typen (Validator-Regel IR003).</summary>
    public const string Unknown = "unknown";

    public static string Of(IType type)
    {
        switch (type)
        {
            case ArrayType array:
                return Of(array.ElementType) + "[" + new string(',', array.Dimensions - 1) + "]";
            case ByReferenceType byRef:
                return Of(byRef.ElementType) + "&";
            case PointerType:
                throw new UnsupportedConstructException("Zeigertyp " + type.FullName + " (unsafe)");
            case ITypeParameter parameter:
                return Names.Ident(parameter.Name);
        }
        if (type.Kind == TypeKind.Unknown || type.Kind == TypeKind.None)
        {
            return Unknown;
        }
        if (type.Kind == TypeKind.Dynamic)
        {
            throw new UnsupportedConstructException("dynamic");
        }
        string? primitive = Primitive(type);
        if (primitive != null)
        {
            return primitive;
        }
        if (type.IsKnownType(KnownTypeCode.NullableOfT))
        {
            return Of(type.TypeArguments[0]) + "?";
        }
        return Named(type);
    }

    /// <summary>Qualifizierter Name ohne Typargumente, z. B. für deklarierende Typen.</summary>
    public static string QualifiedName(IType type)
    {
        if (type.DeclaringType != null)
        {
            throw new UnsupportedConstructException(
                "verschachtelter Typ " + type.FullName + " (Spec-Lücke: nicht von Namespaces unterscheidbar)");
        }
        if (type.Kind == TypeKind.Unknown)
        {
            return Unknown;
        }
        string name = Names.Ident(type.Name);
        if (string.IsNullOrEmpty(type.Namespace))
        {
            return name;
        }
        var parts = type.Namespace.Split('.');
        for (int i = 0; i < parts.Length; i++)
        {
            parts[i] = Names.Ident(parts[i]);
        }
        return string.Join(".", parts) + "." + name;
    }

    private static string Named(IType type)
    {
        var sb = new StringBuilder(QualifiedName(type));
        IReadOnlyList<IType> arguments = type.TypeArguments;
        if (arguments.Count > 0)
        {
            sb.Append('<');
            for (int i = 0; i < arguments.Count; i++)
            {
                if (i > 0)
                {
                    sb.Append(',');
                }
                sb.Append(Of(arguments[i]));
            }
            sb.Append('>');
        }
        return sb.ToString();
    }

    private static string? Primitive(IType type)
    {
        return type.GetDefinition()?.KnownTypeCode switch
        {
            KnownTypeCode.Boolean => "bool",
            KnownTypeCode.Char => "char",
            KnownTypeCode.String => "string",
            KnownTypeCode.Object => "object",
            KnownTypeCode.Void => "void",
            KnownTypeCode.SByte => "i8",
            KnownTypeCode.Byte => "u8",
            KnownTypeCode.Int16 => "i16",
            KnownTypeCode.UInt16 => "u16",
            KnownTypeCode.Int32 => "i32",
            KnownTypeCode.UInt32 => "u32",
            KnownTypeCode.Int64 => "i64",
            KnownTypeCode.UInt64 => "u64",
            KnownTypeCode.Single => "f32",
            KnownTypeCode.Double => "f64",
            KnownTypeCode.IntPtr => throw new UnsupportedConstructException("IntPtr (nativ)"),
            KnownTypeCode.UIntPtr => throw new UnsupportedConstructException("UIntPtr (nativ)"),
            KnownTypeCode.Decimal => throw new UnsupportedConstructException("decimal hat keinen IR-Typ"),
            _ => null,
        };
    }

    /// <summary>Prüft, ob der Typ in der IR ein Primitiv ist (Operatoren als opsym erlaubt).</summary>
    public static bool IsNumericOrBool(IType type)
    {
        var code = type.GetDefinition()?.KnownTypeCode;
        return code is KnownTypeCode.Boolean or KnownTypeCode.Char or KnownTypeCode.SByte or KnownTypeCode.Byte
            or KnownTypeCode.Int16 or KnownTypeCode.UInt16 or KnownTypeCode.Int32 or KnownTypeCode.UInt32
            or KnownTypeCode.Int64 or KnownTypeCode.UInt64 or KnownTypeCode.Single or KnownTypeCode.Double;
    }
}
