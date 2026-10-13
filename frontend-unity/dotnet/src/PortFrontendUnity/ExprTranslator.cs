using System.Collections.Generic;
using ICSharpCode.Decompiler.CSharp.Resolver;
using ICSharpCode.Decompiler.CSharp.Syntax;
using ICSharpCode.Decompiler.Semantics;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>
/// Übersetzt C#-Ausdrücke des Dekompilers in IR-Ausdrücke (docs/ir-spec.md Abschnitt 7).
/// Grundlage ist der Syntaxknoten plus seine Semantik-Annotation (<c>GetResolveResult</c>).
/// </summary>
internal sealed class ExprTranslator
{
    public List<Sx> TranslateAll(IEnumerable<Expression> expressions)
    {
        var result = new List<Sx>();
        foreach (Expression expression in expressions)
        {
            result.Add(Translate(expression));
        }
        return result;
    }

    /// <summary>Reine Verteilertabelle über die Ausdrucksknoten.</summary>
    public Sx Translate(Expression? expression)
    {
        return expression switch
        {
            ParenthesizedExpression p => Translate(p.Expression),
            PrimitiveExpression p => Primitive(p),
            NullReferenceExpression => S.A("null"),
            ThisReferenceExpression => S.A("this"),
            BaseReferenceExpression => S.A("base"),
            IdentifierExpression i => Identifier(i),
            MemberReferenceExpression m => MemberAccess(m),
            InvocationExpression i => Invocation(i),
            ObjectCreateExpression o => ObjectCreate(o),
            BinaryOperatorExpression b => Binary(b),
            UnaryOperatorExpression u => Unary(u),
            AssignmentExpression a => Assignment(a),
            CastExpression c => Cast(c),
            AsExpression a => S.L(S.A("as"), S.A(TypeOf(a.Type)), Translate(a.Expression)),
            IsExpression i => S.L(S.A("is"), S.A(TypeOf(i.Type)), Translate(i.Expression)),
            ConditionalExpression c => S.L(S.A("cond"), Translate(c.Condition), Translate(c.TrueExpression), Translate(c.FalseExpression)),
            IndexerExpression i => Indexer(i),
            ArrayCreateExpression a => ArrayCreate(a),
            TypeOfExpression t => S.L(S.A("typeof"), S.A(TypeOf(t.Type))),
            DefaultValueExpression d => S.L(S.A("default"), S.A(TypeOf(d.Type))),
            CheckedExpression c => S.L(S.A("checked"), Translate(c.Expression)),
            UncheckedExpression u => S.L(S.A("unchecked"), Translate(u.Expression)),
            DirectionExpression d => Translate(d.Expression),
            null => throw new UnsupportedConstructException("fehlender Ausdruck"),
            _ => throw new UnsupportedConstructException("Ausdruck " + expression.GetType().Name),
        };
    }

    private static string TypeOf(AstType? type)
    {
        ResolveResult rr = type.Rr();
        return TypeNames.Of(rr.Type);
    }

    private Sx Primitive(PrimitiveExpression expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr is ConstantResolveResult constant)
        {
            return Literals.Constant(constant.ConstantValue, constant.Type);
        }
        // Teil eines konstant gefalteten Ausdrucks ohne eigene Annotation: Typ aus dem .NET-Wert.
        return Literals.FromClrValue(expression.Value);
    }

    private Sx Identifier(IdentifierExpression expression)
    {
        ResolveResult rr = expression.Rr();
        switch (rr)
        {
            case ILVariableResolveResult variable:
                return S.A(Names.Local(variable.Variable.Name));
            case LocalResolveResult local:
                return S.A(Names.Local(local.Variable.Name));
            case MemberResolveResult member:
                return MemberValue(member, member.Member.IsStatic ? null : S.A("this"));
            default:
                throw new UnsupportedConstructException("Bezeichner " + expression.Identifier);
        }
    }

    private Sx MemberAccess(MemberReferenceExpression expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr is not MemberResolveResult member)
        {
            throw new UnsupportedConstructException("Memberzugriff " + expression.MemberName + " (z. B. Methodengruppe)");
        }
        Sx? target = member.Member.IsStatic ? null : Translate(expression.Target);
        return MemberValue(member, target);
    }

    /// <summary>Feld oder Property lesen; <paramref name="target"/> ist <c>null</c> bei statischen Membern.</summary>
    private static Sx MemberValue(MemberResolveResult rr, Sx? target)
    {
        IMember member = rr.Member;
        string reference = MemberRefs.Of(member);
        switch (member)
        {
            case IField:
                return target == null ? S.L(S.A("sfield"), S.A(reference)) : S.L(S.A("field"), target, S.A(reference));
            case IProperty property when property.Parameters.Count == 0:
                return target == null ? S.L(S.A("sprop"), S.A(reference)) : S.L(S.A("prop"), target, S.A(reference));
            default:
                throw new UnsupportedConstructException("Memberzugriff auf " + member.SymbolKind + " " + member.Name);
        }
    }

    private Sx Invocation(InvocationExpression expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr is not CSharpInvocationResolveResult call || call.Member is not IMethod method)
        {
            throw new UnsupportedConstructException("Aufruf ohne aufgelöste Methode (z. B. Delegate-Aufruf)");
        }
        List<Sx> arguments = TranslateAll(expression.Arguments);
        string reference = MemberRefs.Of(method);
        if (method.IsStatic)
        {
            var items = new List<Sx> { S.A("scall"), S.A(reference) };
            items.AddRange(arguments);
            return S.L(items);
        }
        var callItems = new List<Sx> { S.A("call"), CallTarget(expression.Target), S.A(reference) };
        callItems.AddRange(arguments);
        return S.L(callItems);
    }

    /// <summary>Ziel eines Instanzaufrufs; ohne expliziten Empfänger ist es <c>this</c>.</summary>
    private Sx CallTarget(Expression? target)
    {
        if (target is MemberReferenceExpression member)
        {
            return Translate(member.Target);
        }
        if (target is IdentifierExpression)
        {
            return S.A("this");
        }
        throw new UnsupportedConstructException("Aufrufziel " + (target?.GetType().Name ?? "fehlt"));
    }

    private Sx ObjectCreate(ObjectCreateExpression expression)
    {
        if (expression.Initializer != null)
        {
            throw new UnsupportedConstructException("Objekt-Initialisierer");
        }
        ResolveResult rr = expression.Rr();
        if (rr is not CSharpInvocationResolveResult call)
        {
            throw new UnsupportedConstructException("new ohne aufgelösten Konstruktor");
        }
        var items = new List<Sx> { S.A("new"), S.A(MemberRefs.Of(call.Member)) };
        items.AddRange(TranslateAll(expression.Arguments));
        return S.L(items);
    }

    private Sx Binary(BinaryOperatorExpression expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr is CSharpInvocationResolveResult call && call.Member is IMethod method)
        {
            if (IsStringConcat(method))
            {
                return Concat(Translate(expression.Left), Translate(expression.Right), expression);
            }
            return S.L(S.A("scall"), S.A(MemberRefs.Of(method)), Translate(expression.Left), Translate(expression.Right));
        }
        if (expression.Operator == BinaryOperatorType.NullCoalescing)
        {
            return S.L(S.A("coalesce"), Translate(expression.Left), Translate(expression.Right));
        }
        if (expression.Operator == BinaryOperatorType.Add
            && (IsStringTyped(expression.Left) || IsStringTyped(expression.Right)))
        {
            // Teil einer verketteten String-Addition ohne eigene Annotation.
            return Concat(Translate(expression.Left), Translate(expression.Right), expression);
        }
        if (rr is OperatorResolveResult { IsLiftedOperator: true })
        {
            throw new UnsupportedConstructException("Operator auf Nullable (lifted)");
        }
        if (IsReferenceEquality(expression))
        {
            Sx equals = S.L(S.A("scall"), S.A("System.Object::ReferenceEquals#object,object"), Translate(expression.Left), Translate(expression.Right));
            return expression.Operator == BinaryOperatorType.Equality ? equals : S.L(S.A("not"), equals);
        }
        CheckPrimitiveOperands(expression.Left, expression.Right);
        return S.L(S.A(Operators.Binary(expression.Operator)), Translate(expression.Left), Translate(expression.Right));
    }

    private static bool IsStringConcat(IMethod method)
    {
        return method.Name == "Concat" && method.DeclaringType.IsKnownType(KnownTypeCode.String);
    }

    /// <summary>
    /// String-Verkettung paarweise: <c>Concat#string,string</c>, wenn beide Seiten Strings sind, sonst
    /// <c>Concat#object,object</c> (ruft ToString, null wird zu ""). Verschachtelte Verkettungen ohne
    /// eigene Annotation bleiben Strings.
    /// </summary>
    private Sx Concat(Sx left, Sx right, BinaryOperatorExpression expression)
    {
        bool bothStrings = IsStringTyped(expression.Left) && IsStringTyped(expression.Right);
        string reference = bothStrings ? "System.String::Concat#string,string" : "System.String::Concat#object,object";
        return S.L(S.A("scall"), S.A(reference), left, right);
    }

    private static bool IsStringTyped(Expression? expression)
    {
        if (expression is BinaryOperatorExpression { Operator: BinaryOperatorType.Add } nested
            && nested.Rr() is not OperatorResolveResult)
        {
            return true;
        }
        return expression.Rr().Type.IsKnownType(KnownTypeCode.String);
    }

    /// <summary><c>==</c>/<c>!=</c> zwischen Referenztypen ohne eigenen Operator (inklusive <c>null</c>).</summary>
    private static bool IsReferenceEquality(BinaryOperatorExpression expression)
    {
        if (expression.Operator is not (BinaryOperatorType.Equality or BinaryOperatorType.InEquality))
        {
            return false;
        }
        IType left = expression.Left.Rr().Type;
        IType right = expression.Right.Rr().Type;
        bool leftReference = left.IsReferenceType == true || left.Kind == TypeKind.Null;
        bool rightReference = right.IsReferenceType == true || right.Kind == TypeKind.Null;
        return leftReference && rightReference && !(left.Kind == TypeKind.Null && right.Kind == TypeKind.Null);
    }

    private static void CheckPrimitiveOperands(Expression? left, Expression? right)
    {
        if (!IsPrimitiveValued(left) || !IsPrimitiveValued(right))
        {
            throw new UnsupportedConstructException(
                "Operator auf Nicht-Primitiv (" + left.Rr().Type.FullName + ", " + right.Rr().Type.FullName + ")");
        }
    }

    /// <summary>
    /// Prüft, ob ein Ausdruck einen primitiven Wert liefert. Verschachtelte Rechenausdrücke tragen nicht
    /// immer eine Semantik-Annotation; dann entscheiden die Operanden.
    /// </summary>
    private static bool IsPrimitiveValued(Expression? expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr.Type.Kind == TypeKind.Null)
        {
            return true;
        }
        if (rr.Type.Kind != TypeKind.Unknown)
        {
            return TypeNames.IsNumericOrBool(rr.Type);
        }
        return expression switch
        {
            ParenthesizedExpression p => IsPrimitiveValued(p.Expression),
            BinaryOperatorExpression b => IsPrimitiveValued(b.Left) && IsPrimitiveValued(b.Right),
            UnaryOperatorExpression u => IsPrimitiveValued(u.Expression),
            PrimitiveExpression p => Literals.IsPrimitiveClrValue(p.Value),
            _ => false,
        };
    }

    private Sx Unary(UnaryOperatorExpression expression)
    {
        ResolveResult rr = expression.Rr();
        if (rr is CSharpInvocationResolveResult call && call.Member is IMethod method && !IsIncrement(expression.Operator))
        {
            return S.L(S.A("scall"), S.A(MemberRefs.Of(method)), Translate(expression.Expression));
        }
        Sx operand = Translate(expression.Expression);
        switch (expression.Operator)
        {
            case UnaryOperatorType.Not: return S.L(S.A("not"), operand);
            case UnaryOperatorType.Minus: return S.L(S.A("neg"), operand);
            case UnaryOperatorType.BitNot: return S.L(S.A("bnot"), operand);
            case UnaryOperatorType.Plus: return operand;
            case UnaryOperatorType.Increment: return IncDec("pre-inc", expression);
            case UnaryOperatorType.Decrement: return IncDec("pre-dec", expression);
            case UnaryOperatorType.PostIncrement: return IncDec("post-inc", expression);
            case UnaryOperatorType.PostDecrement: return IncDec("post-dec", expression);
            default:
                throw new UnsupportedConstructException("unärer Operator " + expression.Operator);
        }
    }

    private static bool IsIncrement(UnaryOperatorType op)
    {
        return op is UnaryOperatorType.Increment or UnaryOperatorType.Decrement
            or UnaryOperatorType.PostIncrement or UnaryOperatorType.PostDecrement;
    }

    private Sx IncDec(string head, UnaryOperatorExpression expression)
    {
        if (!TypeNames.IsNumericOrBool(expression.Expression.Rr().Type))
        {
            throw new UnsupportedConstructException(head + " auf Nicht-Primitiv");
        }
        return S.L(S.A(head), Translate(expression.Expression));
    }

    private Sx Assignment(AssignmentExpression expression)
    {
        Sx target = Translate(expression.Left);
        Sx value = Translate(expression.Right);
        if (expression.Operator == AssignmentOperatorType.Assign)
        {
            return S.L(S.A("set"), target, value);
        }
        IType targetType = expression.Left.Rr().Type;
        if (targetType.IsKnownType(KnownTypeCode.String) && expression.Operator == AssignmentOperatorType.Add)
        {
            bool rightIsString = IsStringTyped(expression.Right);
            string reference = rightIsString ? "System.String::Concat#string,string" : "System.String::Concat#object,object";
            return S.L(S.A("set"), target, S.L(S.A("scall"), S.A(reference), target, value));
        }
        if (!TypeNames.IsNumericOrBool(targetType))
        {
            throw new UnsupportedConstructException("zusammengesetzte Zuweisung auf " + targetType.FullName);
        }
        string op = Operators.Binary(AssignmentExpression.GetCorrespondingBinaryOperator(expression.Operator)!.Value);
        return S.L(S.A("set-op"), S.A(op), target, value);
    }

    private Sx Cast(CastExpression expression)
    {
        ResolveResult rr = expression.Rr();
        Sx operand = Translate(expression.Expression);
        string type = TypeOf(expression.Type);
        if (rr is not ConversionResolveResult conversionResult)
        {
            return S.L(S.A("cast"), S.A(type), operand);
        }
        Conversion conversion = conversionResult.Conversion;
        if (conversion.IsUserDefined)
        {
            return S.L(S.A("scall"), S.A(MemberRefs.Of(conversion.Method)), operand);
        }
        if (conversion.IsNumericConversion || conversion.IsEnumerationConversion)
        {
            return S.L(S.A("conv"), S.A(type), operand);
        }
        if (conversion.IsIdentityConversion || (conversion.IsImplicit && (conversion.IsReferenceConversion || conversion.IsBoxingConversion)))
        {
            return operand;
        }
        if (conversion.IsNullableConversion)
        {
            throw new UnsupportedConstructException("Nullable-Umwandlung");
        }
        return S.L(S.A("cast"), S.A(type), operand);
    }

    private Sx Indexer(IndexerExpression expression)
    {
        IType targetType = expression.Target.Rr().Type;
        if (targetType.Kind != TypeKind.Array)
        {
            throw new UnsupportedConstructException("Indexer auf " + targetType.FullName + " (nur Arrays in M2)");
        }
        var items = new List<Sx> { S.A("index"), Translate(expression.Target) };
        items.AddRange(TranslateAll(expression.Arguments));
        return S.L(items);
    }

    private Sx ArrayCreate(ArrayCreateExpression expression)
    {
        if (expression.Rr().Type is not ArrayType arrayType)
        {
            throw new UnsupportedConstructException("Array ohne Typinformation");
        }
        string elementType = TypeNames.Of(arrayType.ElementType);
        if (expression.Initializer != null)
        {
            if (arrayType.Dimensions != 1)
            {
                throw new UnsupportedConstructException("mehrdimensionaler Array-Initialisierer");
            }
            var literal = new List<Sx> { S.A("array-lit"), S.A(elementType) };
            literal.AddRange(TranslateAll(expression.Initializer.Elements));
            return S.L(literal);
        }
        if (expression.AdditionalArraySpecifiers.Count > 0)
        {
            throw new UnsupportedConstructException("verschachtelte Arrays");
        }
        var items = new List<Sx> { S.A("new-array"), S.A(elementType) };
        items.AddRange(TranslateAll(expression.Arguments));
        return S.L(items);
    }
}
