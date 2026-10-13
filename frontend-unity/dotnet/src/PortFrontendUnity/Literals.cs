using System;
using System.Globalization;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>Literale mit Pflicht-Typsuffix (docs/ir-spec.md Abschnitt 2).</summary>
internal static class Literals
{
    /// <summary>Übersetzt eine Konstante des angegebenen Typs.</summary>
    public static Sx Constant(object? value, IType type)
    {
        if (value == null)
        {
            return S.A("null");
        }
        switch (type.GetDefinition()?.KnownTypeCode ?? KnownTypeCode.None)
        {
            case KnownTypeCode.Boolean: return S.A((bool)value ? "true" : "false");
            case KnownTypeCode.Char: return S.L(S.A("char"), S.A(((int)(char)value).ToString(CultureInfo.InvariantCulture)));
            case KnownTypeCode.String: return S.Str((string)value);
            case KnownTypeCode.SByte: return Integer(value, "i8");
            case KnownTypeCode.Byte: return Integer(value, "u8");
            case KnownTypeCode.Int16: return Integer(value, "i16");
            case KnownTypeCode.UInt16: return Integer(value, "u16");
            case KnownTypeCode.Int32: return Integer(value, "i32");
            case KnownTypeCode.UInt32: return Integer(value, "u32");
            case KnownTypeCode.Int64: return Integer(value, "i64");
            case KnownTypeCode.UInt64: return Integer(value, "u64");
            case KnownTypeCode.Single: return Float((float)value);
            case KnownTypeCode.Double: return Double((double)value);
            default:
                throw new UnsupportedConstructException(
                    "Konstante vom Typ " + type.FullName + " hat keine IR-Literalform");
        }
    }

    /// <summary>Literal aus einem .NET-Wert, wenn keine Typinformation vorliegt.</summary>
    public static Sx FromClrValue(object? value)
    {
        return value switch
        {
            null => S.A("null"),
            bool b => S.A(b ? "true" : "false"),
            char c => S.L(S.A("char"), S.A(((int)c).ToString(CultureInfo.InvariantCulture))),
            string s => S.Str(s),
            sbyte => Integer(value, "i8"),
            byte => Integer(value, "u8"),
            short => Integer(value, "i16"),
            ushort => Integer(value, "u16"),
            int => Integer(value, "i32"),
            uint => Integer(value, "u32"),
            long => Integer(value, "i64"),
            ulong => Integer(value, "u64"),
            float f => Float(f),
            double d => Double(d),
            _ => throw new UnsupportedConstructException("Literal vom Typ " + value.GetType().Name),
        };
    }

    /// <summary>Prüft, ob ein .NET-Wert ein IR-Primitiv (Zahl, bool, char) ist.</summary>
    public static bool IsPrimitiveClrValue(object? value)
    {
        return value is bool or char or sbyte or byte or short or ushort or int or uint or long or ulong
            or float or double;
    }

    private static SAtom Integer(object value, string suffix)
    {
        string digits = Convert.ToString(value, CultureInfo.InvariantCulture)!;
        return S.A(digits + suffix);
    }

    /// <summary>
    /// <c>float</c> im kürzesten Rundreiseformat, immer mit Punkt oder Exponent, Exponent klein und ohne
    /// <c>+</c>. NaN und Unendlich haben keine Literalform und werden zu <c>sfield System.Single::...</c>.
    /// </summary>
    public static Sx Float(float value)
    {
        if (float.IsNaN(value)) return StaticField("System.Single", "NaN");
        if (float.IsPositiveInfinity(value)) return StaticField("System.Single", "PositiveInfinity");
        if (float.IsNegativeInfinity(value)) return StaticField("System.Single", "NegativeInfinity");
        return S.A(Normalize(value.ToString("R", CultureInfo.InvariantCulture)) + "f32");
    }

    /// <summary>Wie <see cref="Float"/> für <c>double</c>.</summary>
    public static Sx Double(double value)
    {
        if (double.IsNaN(value)) return StaticField("System.Double", "NaN");
        if (double.IsPositiveInfinity(value)) return StaticField("System.Double", "PositiveInfinity");
        if (double.IsNegativeInfinity(value)) return StaticField("System.Double", "NegativeInfinity");
        return S.A(Normalize(value.ToString("R", CultureInfo.InvariantCulture)) + "f64");
    }

    internal static string Normalize(string text)
    {
        string result = text.Replace("E+", "e").Replace("E", "e");
        if (!result.Contains('.') && !result.Contains('e'))
        {
            result += ".0";
        }
        return result;
    }

    private static Sx StaticField(string type, string name) => S.L(S.A("sfield"), S.A(type + "::" + name));
}
