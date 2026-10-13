using System.Collections.Generic;
using System.Linq;
using ICSharpCode.Decompiler.CSharp.Syntax;
using ICSharpCode.Decompiler.Semantics;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>
/// Übersetzt Typ- und Mitgliedsdeklarationen (docs/ir-spec.md Abschnitt 5). Mitglieder bleiben in
/// Dekompiler-Reihenfolge; die kanonische Sortierung macht der Java-Printer.
/// </summary>
internal sealed class DeclTranslator
{
    /// <summary>Präfix synthetischer Backing-Felder entzuckerter Auto-Properties (Nutzerentscheidung M2).</summary>
    public const string AutoFieldPrefix = "__auto_";

    private readonly Report report;

    public DeclTranslator(Report report)
    {
        this.report = report;
    }

    /// <summary>Übersetzt eine Typdeklaration samt Mitgliedern.</summary>
    public Sx Type(ITypeDefinition definition, TypeDeclaration declaration)
    {
        string name = TypeNames.QualifiedName(definition);
        if (definition.Kind == TypeKind.Enum)
        {
            return Enum(definition, declaration, name);
        }
        string head = definition.Kind switch
        {
            TypeKind.Class => "class",
            TypeKind.Struct => "struct",
            TypeKind.Interface => "interface",
            _ => throw new UnsupportedConstructException("Typart " + definition.Kind),
        };
        var items = new List<Sx> { S.A(head), S.A(name) };
        TypeHeader(definition, items);
        foreach (EntityDeclaration member in declaration.Members)
        {
            items.AddRange(MemberWithFallback(definition, member, name));
        }
        return S.L(items);
    }

    private void TypeHeader(ITypeDefinition definition, List<Sx> items)
    {
        var interfaces = new List<Sx>();
        foreach (IType baseType in definition.DirectBaseTypes)
        {
            if (baseType.Kind == TypeKind.Interface)
            {
                interfaces.Add(S.A(TypeNames.Of(baseType)));
            }
            else if (!IsImplicitBase(baseType))
            {
                items.Add(S.A(":base"));
                items.Add(S.A(TypeNames.Of(baseType)));
            }
        }
        if (interfaces.Count > 0)
        {
            items.Add(S.A(":implements"));
            items.Add(S.L(interfaces));
        }
        if (definition.TypeParameters.Count > 0)
        {
            items.Add(S.A(":generics"));
            items.Add(S.L(definition.TypeParameters.Select(p => (Sx)S.A(Names.Ident(p.Name)))));
        }
        AddModifiers(Modifiers.ForType(definition), items);
        var context = new MemberContext(TypeNames.QualifiedName(definition), report);
        AddAttributes(definition.GetAttributes(), items, context);
        AddManual(context, items);
    }

    private static bool IsImplicitBase(IType type)
    {
        return type.IsKnownType(KnownTypeCode.Object) || type.IsKnownType(KnownTypeCode.ValueType)
            || type.IsKnownType(KnownTypeCode.Enum);
    }

    private Sx Enum(ITypeDefinition definition, TypeDeclaration declaration, string name)
    {
        IType underlying = definition.EnumUnderlyingType
            ?? throw new UnsupportedConstructException("Enum ohne zugrunde liegenden Typ");
        var items = new List<Sx> { S.A("enum"), S.A(name), S.A(":underlying"), S.A(TypeNames.Of(underlying)) };
        foreach (EntityDeclaration member in declaration.Members)
        {
            if (member.Rr() is MemberResolveResult { Member: IField field } && field.IsConst)
            {
                items.Add(S.L(S.A(Names.Ident(field.Name)), Literals.Constant(field.GetConstantValue(), underlying)));
            }
        }
        report.MemberTranslated();
        return S.L(items);
    }

    /// <summary>Übersetzt ein Mitglied; ist das unmöglich, entsteht eine Platzhalter-Methode mit raw-Rumpf.</summary>
    private List<Sx> MemberWithFallback(ITypeDefinition type, EntityDeclaration declaration, string typeName)
    {
        if (declaration is TypeDeclaration nested)
        {
            report.SkippedType(typeName + "." + nested.Name, "verschachtelter Typ (Spec-Lücke)");
            return new List<Sx>();
        }
        string symbol = typeName + "::" + declaration.Name;
        var context = new MemberContext(symbol, report);
        try
        {
            List<Sx> result = Member(type, declaration, context);
            report.MemberTranslated();
            return result;
        }
        catch (UnsupportedConstructException e)
        {
            context.Raw(e.Reason);
            return new List<Sx> { Placeholder(declaration, context) };
        }
    }

    private static Sx Placeholder(EntityDeclaration declaration, MemberContext context)
    {
        string name = Names.Ident(declaration.Name.Length == 0 ? "__unsupported" : declaration.Name);
        return S.L(
            S.A("method"), S.A(name), S.A("void"), S.L(),
            S.A(":manual"), S.Str(context.ManualText ?? "nicht übersetzbar"),
            S.L(S.A("raw"), S.A("cs"), S.Str(CSharpText.Of(declaration))));
    }

    /// <summary>Reine Verteilertabelle über die Mitgliedsarten.</summary>
    private List<Sx> Member(ITypeDefinition type, EntityDeclaration declaration, MemberContext context)
    {
        return declaration switch
        {
            FieldDeclaration f => Fields(type, f, context),
            PropertyDeclaration p => Property(type, p, context),
            MethodDeclaration m => One(Method(m, context)),
            OperatorDeclaration o => One(Method(o, context)),
            ConstructorDeclaration c => One(Constructor(c, context)),
            EventDeclaration e => Events(type, e),
            _ => throw new UnsupportedConstructException("Mitgliedsart " + declaration.GetType().Name),
        };
    }

    private List<Sx> Fields(ITypeDefinition type, FieldDeclaration declaration, MemberContext context)
    {
        var result = new List<Sx>();
        foreach (VariableInitializer variable in declaration.Variables)
        {
            IField field = type.Fields.First(f => f.Name == variable.Name);
            var items = new List<Sx> { S.A("field"), S.A(Names.Ident(field.Name)), S.A(TypeNames.Of(field.Type)) };
            AddModifiers(Modifiers.ForField(field), items);
            AddAttributes(field.GetAttributes(), items, context);
            Sx? initializer = FieldInitializer(field, variable, context);
            if (initializer != null)
            {
                items.Add(S.A(":init"));
                items.Add(initializer);
            }
            AddManual(context, items);
            result.Add(S.L(items));
        }
        return result;
    }

    private static Sx? FieldInitializer(IField field, VariableInitializer variable, MemberContext context)
    {
        if (field.IsConst)
        {
            return Literals.Constant(field.GetConstantValue(), field.Type);
        }
        if (variable.Initializer == null)
        {
            return null;
        }
        return new StmtTranslator(context).Expressions.Translate(variable.Initializer);
    }

    private List<Sx> Property(ITypeDefinition type, PropertyDeclaration declaration, MemberContext context)
    {
        var property = (IProperty)((MemberResolveResult)declaration.Rr()).Member;
        bool hasGetterBody = declaration.Getter?.Body != null;
        bool hasSetterBody = declaration.Setter?.Body != null;
        if (property.IsAbstract || type.Kind == TypeKind.Interface)
        {
            context.Manual("abstrakte Property: Accessoren ohne Rumpf sind nicht darstellbar (Spec-Lücke)");
            return One(PropertyHead(property, context, null, null));
        }
        if (!hasGetterBody && !hasSetterBody)
        {
            return AutoProperty(type, property, declaration, context);
        }
        var statements = new StmtTranslator(context);
        Sx? getter = hasGetterBody ? statements.Translate(declaration.Getter!.Body) : null;
        Sx? setter = hasSetterBody ? statements.Translate(declaration.Setter!.Body) : null;
        return One(PropertyHead(property, context, getter, setter));
    }

    /// <summary>
    /// Entzuckert eine Auto-Property: privates Feld <c>__auto_Name</c> plus explizite Accessoren.
    /// </summary>
    private List<Sx> AutoProperty(ITypeDefinition type, IProperty property, PropertyDeclaration declaration, MemberContext context)
    {
        string fieldName = AutoFieldPrefix + Names.Ident(property.Name);
        string fieldRef = TypeNames.QualifiedName(type) + "::" + fieldName;
        Sx access = property.IsStatic ? S.L(S.A("sfield"), S.A(fieldRef)) : S.L(S.A("field"), S.A("this"), S.A(fieldRef));
        var fieldItems = new List<Sx> { S.A("field"), S.A(fieldName), S.A(TypeNames.Of(property.ReturnType)) };
        AddModifiers(property.IsStatic ? new[] { "private", "static" } : new[] { "private" }, fieldItems);
        if (declaration.Initializer != null)
        {
            fieldItems.Add(S.A(":init"));
            fieldItems.Add(new StmtTranslator(context).Expressions.Translate(declaration.Initializer));
        }
        Sx getter = S.L(S.A("return"), access);
        Sx? setter = property.CanSet ? S.L(S.A("block"), S.L(S.A("expr"), S.L(S.A("set"), access, S.A("value")))) : null;
        return new List<Sx> { S.L(fieldItems), PropertyHead(property, context, getter, setter) };
    }

    private static Sx PropertyHead(IProperty property, MemberContext context, Sx? getter, Sx? setter)
    {
        var items = new List<Sx> { S.A("prop"), S.A(Names.Ident(property.Name)), S.A(TypeNames.Of(property.ReturnType)) };
        AddModifiers(Modifiers.ForMember(property), items);
        if (getter != null)
        {
            items.Add(S.A(":get"));
            items.Add(getter);
        }
        if (setter != null)
        {
            items.Add(S.A(":set"));
            items.Add(setter);
        }
        AddManual(context, items);
        return S.L(items);
    }

    private Sx Method(EntityDeclaration declaration, MemberContext context)
    {
        var method = (IMethod)((MemberResolveResult)declaration.Rr()).Member;
        var items = new List<Sx>
        {
            S.A("method"), S.A(Names.Ident(method.Name)), S.A(TypeNames.Of(method.ReturnType)), Parameters(method, context),
        };
        AddModifiers(Modifiers.ForMember(method), items);
        if (method.TypeParameters.Count > 0)
        {
            items.Add(S.A(":generics"));
            items.Add(S.L(method.TypeParameters.Select(p => (Sx)S.A(Names.Ident(p.Name)))));
        }
        NoteDroppedAttributes(method, context);
        BlockStatement? body = declaration switch
        {
            MethodDeclaration m => m.Body,
            OperatorDeclaration o => o.Body,
            _ => null,
        };
        if (body == null)
        {
            items.Add(S.A(":abstract"));
        }
        else
        {
            items.Add(new StmtTranslator(context).Translate(body));
        }
        AddManual(context, items);
        return S.L(items);
    }

    private Sx Constructor(ConstructorDeclaration declaration, MemberContext context)
    {
        var constructor = (IMethod)((MemberResolveResult)declaration.Rr()).Member;
        var items = new List<Sx> { S.A("ctor"), Parameters(constructor, context) };
        AddModifiers(Modifiers.ForMember(constructor), items);
        var statements = new StmtTranslator(context);
        ConstructorInitializer? initializer = declaration.Initializer;
        if (initializer != null)
        {
            if (initializer.ConstructorInitializerType != ConstructorInitializerType.Base)
            {
                throw new UnsupportedConstructException("Konstruktor-Verkettung mit this(...)");
            }
            items.Add(S.A(":base-call"));
            items.Add(S.L(statements.Expressions.TranslateAll(initializer.Arguments)));
        }
        items.Add(statements.Translate(declaration.Body.Required("Konstruktorrumpf")));
        AddManual(context, items);
        return S.L(items);
    }

    private List<Sx> Events(ITypeDefinition type, EventDeclaration declaration)
    {
        var result = new List<Sx>();
        foreach (VariableInitializer variable in declaration.Variables)
        {
            IEvent @event = type.Events.First(e => e.Name == variable.Name);
            var items = new List<Sx> { S.A("event"), S.A(Names.Ident(@event.Name)), S.A(TypeNames.Of(@event.ReturnType)) };
            AddModifiers(Modifiers.ForMember(@event), items);
            result.Add(S.L(items));
        }
        return result;
    }

    private static Sx Parameters(IMethod method, MemberContext context)
    {
        var list = new List<Sx>();
        foreach (IParameter parameter in method.Parameters)
        {
            IType type = parameter.Type is ByReferenceType byRef ? byRef.ElementType : parameter.Type;
            var items = new List<Sx> { S.A(Names.Local(parameter.Name)), S.A(TypeNames.Of(type)) };
            switch (parameter.ReferenceKind)
            {
                case ReferenceKind.Ref: items.Add(S.A(":ref")); break;
                case ReferenceKind.Out: items.Add(S.A(":out")); break;
                case ReferenceKind.In: items.Add(S.A(":in")); break;
            }
            if (parameter.HasConstantValueInSignature)
            {
                items.Add(S.A(":default"));
                items.Add(Literals.Constant(parameter.GetConstantValue(), type));
            }
            if (parameter.IsParams)
            {
                context.Note("params-Parameter " + parameter.Name + " als normales Array übersetzt");
            }
            list.Add(S.L(items));
        }
        return S.L(list);
    }

    private static void NoteDroppedAttributes(IMethod method, MemberContext context)
    {
        foreach (IAttribute attribute in method.GetAttributes())
        {
            if (!Attributes.IsCompilerAttribute(attribute))
            {
                context.Note("Attribut " + attribute.AttributeType.FullName + " an Methode entfällt (Spec-Lücke)");
            }
        }
    }

    private static void AddModifiers(IReadOnlyList<string> modifiers, List<Sx> items)
    {
        if (modifiers.Count == 0)
        {
            return;
        }
        items.Add(S.A(":mods"));
        items.Add(S.L(modifiers.Select(m => (Sx)S.A(m))));
    }

    private static void AddAttributes(IEnumerable<IAttribute> attributes, List<Sx> items, MemberContext context)
    {
        var translated = new List<Sx>();
        foreach (IAttribute attribute in attributes)
        {
            if (Attributes.IsCompilerAttribute(attribute))
            {
                continue;
            }
            try
            {
                translated.Add(Attributes.Translate(attribute));
            }
            catch (UnsupportedConstructException e)
            {
                context.Manual("Attribut " + attribute.AttributeType.FullName + " nicht übersetzt: " + e.Reason);
            }
        }
        if (translated.Count > 0)
        {
            items.Add(S.A(":attrs"));
            items.Add(S.L(translated));
        }
    }

    private static void AddManual(MemberContext context, List<Sx> items)
    {
        string? text = context.ManualText;
        if (text != null)
        {
            items.Add(S.A(":manual"));
            items.Add(S.Str(text));
        }
    }

    private static List<Sx> One(Sx sx) => new() { sx };
}
