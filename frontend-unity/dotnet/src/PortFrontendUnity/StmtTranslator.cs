using System.Collections.Generic;
using System.Linq;
using ICSharpCode.Decompiler.CSharp.Syntax;
using ICSharpCode.Decompiler.Semantics;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>
/// Übersetzt Statements (docs/ir-spec.md Abschnitt 6). Nicht übersetzbare Statements werden zu
/// <c>(raw cs "...")</c>; der Grund landet im <see cref="MemberContext"/> und damit in <c>:manual</c>.
/// </summary>
internal sealed class StmtTranslator
{
    private readonly MemberContext context;
    private readonly ExprTranslator expressions = new();

    public StmtTranslator(MemberContext context)
    {
        this.context = context;
    }

    public ExprTranslator Expressions => expressions;

    /// <summary>Übersetzt ein Statement; bei nicht unterstützten Konstrukten ein raw-Knoten.</summary>
    public Sx Translate(Statement? statement)
    {
        List<Sx> result = TranslateToList(statement.Required("Statement"));
        return result.Count == 1 ? result[0] : S.L(Prepend(S.A("block"), result));
    }

    private List<Sx> TranslateToList(Statement statement)
    {
        try
        {
            return TranslateUnchecked(statement);
        }
        catch (UnsupportedConstructException e)
        {
            context.Raw(e.Reason);
            return new List<Sx> { S.L(S.A("raw"), S.A("cs"), S.Str(CSharpText.Of(statement))) };
        }
    }

    /// <summary>Reine Verteilertabelle. Liefert eine Liste, weil eine Deklaration mehrere Locals erzeugen kann.</summary>
    private List<Sx> TranslateUnchecked(Statement statement)
    {
        return statement switch
        {
            BlockStatement b => One(Block(b.Statements)),
            VariableDeclarationStatement v => Locals(v),
            ExpressionStatement e => One(S.L(S.A("expr"), expressions.Translate(e.Expression))),
            IfElseStatement i => One(If(i)),
            WhileStatement w => One(S.L(S.A("while"), expressions.Translate(w.Condition), Translate(w.EmbeddedStatement))),
            DoWhileStatement d => One(S.L(S.A("do-while"), Translate(d.EmbeddedStatement), expressions.Translate(d.Condition))),
            ForStatement f => One(For(f)),
            ForeachStatement f => One(Foreach(f)),
            SwitchStatement s => One(Switch(s)),
            ReturnStatement r => One(r.Expression == null ? S.L(S.A("return")) : S.L(S.A("return"), expressions.Translate(r.Expression))),
            BreakStatement => One(S.L(S.A("break"))),
            ContinueStatement => One(S.L(S.A("continue"))),
            ThrowStatement t => One(Throw(t)),
            TryCatchStatement t => One(Try(t)),
            YieldReturnStatement y => One(S.L(S.A("yield-return"), expressions.Translate(y.Expression))),
            YieldBreakStatement => One(S.L(S.A("yield-break"))),
            LockStatement l => One(S.L(S.A("lock"), expressions.Translate(l.Expression), Translate(l.EmbeddedStatement))),
            UsingStatement u => One(Using(u)),
            LabelStatement l => One(S.L(S.A("label"), S.A(Names.Ident(l.Label)))),
            GotoStatement g => One(S.L(S.A("goto"), S.A(Names.Ident(g.Label)))),
            EmptyStatement => One(S.L(S.A("block"))),
            UnsafeStatement => throw new UnsupportedConstructException("unsafe-Block"),
            FixedStatement => throw new UnsupportedConstructException("fixed-Block (unsafe, Zeiger)"),
            _ => throw new UnsupportedConstructException("Statement " + statement.GetType().Name),
        };
    }

    public Sx Block(IEnumerable<Statement> statements)
    {
        var items = new List<Sx> { S.A("block") };
        foreach (Statement statement in statements)
        {
            items.AddRange(TranslateToList(statement));
        }
        return S.L(items);
    }

    private List<Sx> Locals(VariableDeclarationStatement declaration)
    {
        if (declaration.Modifiers != ICSharpCode.Decompiler.CSharp.Syntax.Modifiers.None)
        {
            throw new UnsupportedConstructException("lokale Konstante oder Modifier " + declaration.Modifiers);
        }
        var result = new List<Sx>();
        foreach (VariableInitializer variable in declaration.Variables)
        {
            IType type = VariableType(variable, declaration.Type);
            var items = new List<Sx> { S.A("local"), S.A(Names.Local(variable.Name)), S.A(TypeNames.Of(type)) };
            if (variable.Initializer != null)
            {
                items.Add(expressions.Translate(variable.Initializer));
            }
            result.Add(S.L(items));
        }
        return result;
    }

    private static IType VariableType(VariableInitializer variable, AstType? declaredType)
    {
        ResolveResult rr = variable.Rr();
        if (rr is ILVariableResolveResult or LocalResolveResult)
        {
            return rr.Type;
        }
        return declaredType.Rr().Type;
    }

    private Sx If(IfElseStatement statement)
    {
        var items = new List<Sx> { S.A("if"), expressions.Translate(statement.Condition), Translate(statement.TrueStatement) };
        if (statement.FalseStatement != null)
        {
            items.Add(Translate(statement.FalseStatement));
        }
        return S.L(items);
    }

    private Sx For(ForStatement statement)
    {
        var initializers = new List<Sx>();
        foreach (Statement initializer in statement.Initializers)
        {
            initializers.AddRange(TranslateUnchecked(initializer));
        }
        var updates = new List<Sx>();
        foreach (Statement iterator in statement.Iterators)
        {
            if (iterator is not ExpressionStatement expressionStatement)
            {
                throw new UnsupportedConstructException("for-Fortschaltung " + iterator.GetType().Name);
            }
            updates.Add(expressions.Translate(expressionStatement.Expression));
        }
        Sx condition = statement.Condition == null ? S.A("true") : expressions.Translate(statement.Condition);
        return S.L(S.A("for"), S.L(initializers), condition, S.L(updates), Translate(statement.EmbeddedStatement));
    }

    private Sx Foreach(ForeachStatement statement)
    {
        if (statement.VariableDesignation is not SingleVariableDesignation designation)
        {
            throw new UnsupportedConstructException("foreach mit Dekonstruktion");
        }
        IType type = designation.Rr().Type;
        return S.L(
            S.A("foreach"),
            S.A(Names.Local(designation.Identifier)),
            S.A(TypeNames.Of(type)),
            expressions.Translate(statement.InExpression),
            Translate(statement.EmbeddedStatement));
    }

    /// <summary>
    /// <c>switch</c> ohne Durchfallen. Ein Abschnitt mit <c>default</c>-Label wird zum default-Zweig
    /// (weitere Labels desselben Abschnitts sind dann ohne Wirkung) und ans Ende gestellt.
    /// </summary>
    private Sx Switch(SwitchStatement statement)
    {
        var items = new List<Sx> { S.A("switch"), expressions.Translate(statement.Expression) };
        Sx? defaultBranch = null;
        foreach (SwitchSection section in statement.SwitchSections)
        {
            var body = new List<Sx>();
            foreach (Statement inner in section.Statements)
            {
                body.AddRange(TranslateToList(inner));
            }
            var labels = new List<Sx>();
            bool isDefault = false;
            foreach (CaseLabel label in section.CaseLabels)
            {
                if (label.Expression == null)
                {
                    isDefault = true;
                }
                else
                {
                    labels.Add(expressions.Translate(label.Expression));
                }
            }
            if (isDefault)
            {
                defaultBranch = S.L(Prepend(S.A("default"), body));
            }
            else
            {
                var caseItems = new List<Sx> { S.A("case"), S.L(labels) };
                caseItems.AddRange(body);
                items.Add(S.L(caseItems));
            }
        }
        if (defaultBranch != null)
        {
            items.Add(defaultBranch);
        }
        return S.L(items);
    }

    private Sx Throw(ThrowStatement statement)
    {
        if (statement.Expression == null)
        {
            throw new UnsupportedConstructException("throw; (erneutes Werfen)");
        }
        return S.L(S.A("throw"), expressions.Translate(statement.Expression));
    }

    private Sx Try(TryCatchStatement statement)
    {
        var items = new List<Sx> { S.A("try"), Translate(statement.TryBlock) };
        foreach (CatchClause clause in statement.CatchClauses)
        {
            if (clause.Condition != null)
            {
                throw new UnsupportedConstructException("catch mit when-Filter");
            }
            string type = clause.Type == null ? "System.Exception" : TypeNames.Of(clause.Type.Rr().Type);
            string name = string.IsNullOrEmpty(clause.VariableName) ? "__ex" : Names.Local(clause.VariableName);
            items.Add(S.L(S.A("catch"), S.A(name), S.A(type), Translate(clause.Body)));
        }
        if (statement.FinallyBlock != null)
        {
            items.Add(S.L(S.A("finally"), Translate(statement.FinallyBlock)));
        }
        return S.L(items);
    }

    private Sx Using(UsingStatement statement)
    {
        if (statement.ResourceAcquisition is not VariableDeclarationStatement declaration
            || declaration.Variables.Count != 1)
        {
            throw new UnsupportedConstructException("using ohne genau eine Variablendeklaration");
        }
        VariableInitializer variable = declaration.Variables.FirstOrDefault().Required("using-Variable");
        IType type = VariableType(variable, declaration.Type);
        return S.L(
            S.A("using"),
            S.A(Names.Local(variable.Name)),
            S.A(TypeNames.Of(type)),
            expressions.Translate(variable.Initializer),
            Translate(statement.EmbeddedStatement));
    }

    private static List<Sx> One(Sx sx) => new() { sx };

    private static List<Sx> Prepend(Sx head, List<Sx> rest)
    {
        var result = new List<Sx> { head };
        result.AddRange(rest);
        return result;
    }
}
