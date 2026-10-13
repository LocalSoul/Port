using ICSharpCode.Decompiler.CSharp.Syntax;

namespace PortFrontendUnity;

/// <summary>Binäre Operatoren auf Primitiven als <c>opsym</c> (docs/ir-spec.md Abschnitt 2).</summary>
internal static class Operators
{
    public static string Binary(BinaryOperatorType op)
    {
        return op switch
        {
            BinaryOperatorType.Add => "+",
            BinaryOperatorType.Subtract => "-",
            BinaryOperatorType.Multiply => "*",
            BinaryOperatorType.Divide => "/",
            BinaryOperatorType.Modulus => "%",
            BinaryOperatorType.BitwiseAnd => "&",
            BinaryOperatorType.BitwiseOr => "|",
            BinaryOperatorType.ExclusiveOr => "^",
            BinaryOperatorType.ShiftLeft => "<<",
            BinaryOperatorType.ShiftRight => ">>",
            BinaryOperatorType.Equality => "==",
            BinaryOperatorType.InEquality => "!=",
            BinaryOperatorType.LessThan => "<",
            BinaryOperatorType.LessThanOrEqual => "<=",
            BinaryOperatorType.GreaterThan => ">",
            BinaryOperatorType.GreaterThanOrEqual => ">=",
            BinaryOperatorType.ConditionalAnd => "&&",
            BinaryOperatorType.ConditionalOr => "||",
            _ => throw new UnsupportedConstructException("Operator " + op),
        };
    }
}
