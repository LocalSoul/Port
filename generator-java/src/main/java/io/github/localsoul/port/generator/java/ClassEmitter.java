package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.ParamMode;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Erzeugt zu einer IR-Klasse {@code FooBase.java} und, falls keine Handarbeit nötig oder vorhanden
 * ist, ein dünnes {@code Foo.java} (Nutzerentscheidung M3).
 *
 * <p>Modifier: C#-{@code private} wird paketsichtbar, damit {@code manual/Foo} im selben Paket
 * darauf zugreifen kann; {@code internal} wird {@code public} (C#-Namespaces einer Assembly sehen
 * einander, Java-Pakete nicht); {@code readonly} → {@code final}, {@code const} → {@code static
 * final}. Methoden, die eine {@code message}-Zeile des Mappings überschreiben, werden
 * {@code @Override protected}.
 */
final class ClassEmitter {

  private final GeneratorConfig config;
  private final Map<String, TypeDecl> ownTypes;
  private final List<GeneratedFile> files;
  private final List<Todo> todos;

  ClassEmitter(
      GeneratorConfig config,
      Map<String, TypeDecl> ownTypes,
      List<GeneratedFile> files,
      List<Todo> todos) {
    this.config = config;
    this.ownTypes = ownTypes;
    this.files = files;
    this.todos = todos;
  }

  /** Erzeugt die Dateien einer Klasse; Fehler im Kopf überspringen den Typ mit Todo. */
  void emit(ClassDecl decl) {
    String irName = decl.name();
    String javaPackage = JavaNames.packageOf(config.basePackage(), irName);
    String simpleName = JavaNames.simpleName(irName);
    TypeContext types = new TypeContext(config, ownTypes, javaPackage);
    types.addTypeParameters(decl.typeAttrs().generics());
    String typeParams = typeParameters(decl.typeAttrs().generics());
    String extendsClause;
    try {
      extendsClause = baseClause(decl, types);
    } catch (TodoException e) {
      todos.add(new Todo(irName, "Klasse nicht erzeugt: " + e.getMessage()));
      return;
    }
    if (!decl.typeAttrs().implementedTypes().isEmpty()) {
      todos.add(new Todo(irName, "Interfaces entfallen (Interfaces werden in M3 nicht erzeugt)"));
    }
    ClassState state = new ClassState(irName, types, messageNames(decl));
    JavaWriter body = new JavaWriter();
    body.push();
    List<CtorDecl> constructors = new ArrayList<>();
    boolean first = true;
    for (MemberDecl member : MemberOrder.sorted(irName, decl.members())) {
      JavaWriter memberOut = body.sibling();
      emitMember(member, state, memberOut, constructors);
      if (!memberOut.text().isEmpty()) {
        if (!first) {
          body.blank();
        }
        body.append(memberOut);
        first = false;
      }
    }
    String header = "public abstract class " + simpleName + "Base" + typeParams + extendsClause;
    files.add(file(javaPackage, simpleName + "Base", types, javadocBase(irName), header, body));
    emitThinClass(decl, javaPackage, simpleName, typeParams, constructors, state);
  }

  /** Zustand einer Klasse während der Erzeugung. */
  private static final class ClassState {
    final String irName;
    final TypeContext types;
    final Set<String> messages;
    final ExprEmitter expressions;
    final StmtEmitter statements;
    boolean needsManual;

    ClassState(String irName, TypeContext types, Set<String> messages) {
      this.irName = irName;
      this.types = types;
      this.messages = messages;
      this.expressions = new ExprEmitter(types);
      this.statements = new StmtEmitter(types, expressions);
    }
  }

  private String baseClause(ClassDecl decl, TypeContext types) {
    TypeRef base = decl.typeAttrs().baseType();
    if (base == null) {
      return "";
    }
    return " extends " + types.javaType(base);
  }

  /** Signaturen ({@code Update#}) der Lifecycle-Nachrichten aus der Basiskette, mit Java-Namen. */
  private Set<String> messageNames(ClassDecl decl) {
    Set<String> result = new java.util.TreeSet<>();
    TypeRef base = decl.typeAttrs().baseType();
    int guard = 0;
    while (base instanceof NamedType named && guard < 64) {
      guard++;
      TypeDecl own = ownTypes.get(named.qualifiedName());
      if (own instanceof ClassDecl ownClass) {
        base = ownClass.typeAttrs().baseType();
        continue;
      }
      for (MappingEntry entry : config.mapping().messagesOf(named.qualifiedName())) {
        String signature = entry.irSymbol().substring(entry.irSymbol().indexOf("::") + 2);
        result.add(signature + "=" + entry.javaSymbol());
      }
      break;
    }
    return result;
  }

  // --- Member ---

  private void emitMember(
      MemberDecl member, ClassState state, JavaWriter out, List<CtorDecl> constructors) {
    String symbol = SymbolIds.of(state.irName, member);
    Map<String, String> savedImports = state.types.saveImports();
    try {
      switch (member) {
        case FieldDecl f -> field(f, state, out, symbol);
        case PropDecl p -> property(p, state, out, symbol);
        case MethodDecl m -> method(m, state, out, symbol);
        case CtorDecl c -> {
          constructor(c, state, out, symbol);
          if (!c.modifiers().contains(Modifier.STATIC)) {
            constructors.add(c);
          }
        }
        case EventDecl e -> todos.add(new Todo(symbol, "Events (nach M3)"));
      }
    } catch (TodoException | ManualException e) {
      state.types.restoreImports(savedImports);
      todos.add(new Todo(symbol, "Member nicht erzeugt: " + e.getMessage()));
    }
  }

  private void field(FieldDecl field, ClassState state, JavaWriter out, String symbol) {
    String type = state.types.javaType(field.type());
    String declaration =
        fieldModifiers(field.modifiers()) + type + " " + JavaNames.ident(field.name());
    Expr init = field.initializer();
    if (init != null) {
      Map<String, String> savedImports = state.types.saveImports();
      try {
        declaration += " = " + state.expressions.text(init);
      } catch (TodoException | ManualException e) {
        state.types.restoreImports(savedImports);
        todos.add(new Todo(symbol, "Initialisierer entfällt: " + e.getMessage()));
      }
    }
    out.line(declaration + ";");
  }

  private void property(PropDecl prop, ClassState state, JavaWriter out, String symbol) {
    String type = state.types.javaType(prop.type());
    String name = JavaNames.member(prop.name());
    String mods = memberModifiers(prop.modifiers(), false);
    boolean wrote = false;
    if (prop.getter() != null) {
      bodyMember(mods + type + " " + name + "()", prop.getter(), state, out, symbol + ":get", type);
      wrote = true;
    }
    if (prop.setter() != null) {
      if (wrote) {
        out.blank();
      }
      String header = mods + "void " + name + "(" + type + " value)";
      bodyMember(header, prop.setter(), state, out, symbol + ":set", "void");
      wrote = true;
    }
    if (!wrote) {
      todos.add(new Todo(symbol, "Property ohne Accessoren (abstrakt, Spec-Lücke)"));
    }
  }

  private void method(MethodDecl method, ClassState state, JavaWriter out, String symbol) {
    Set<String> savedParams = state.types.typeParameters();
    state.types.addTypeParameters(method.generics());
    try {
      String returnType = state.types.javaType(method.returnType());
      String signature = symbol.substring(symbol.indexOf("::") + 2);
      String message = messageName(state, signature);
      String name = message != null ? message : JavaNames.member(method.name());
      String generics = method.generics().isEmpty() ? "" : typeParameters(method.generics()) + " ";
      String params = parameters(method.parameters(), state.types);
      if (method.body() == null) {
        out.line(
            memberModifiers(method.modifiers(), true)
                + "abstract "
                + generics
                + returnType
                + " "
                + name
                + params
                + ";");
        return;
      }
      String prefix = message != null ? "protected " : memberModifiers(method.modifiers(), false);
      if (message != null || method.modifiers().contains(Modifier.OVERRIDE)) {
        out.line("@Override");
      }
      bodyMember(
          prefix + generics + returnType + " " + name + params,
          method.body(),
          state,
          out,
          symbol,
          returnType);
    } finally {
      state.types.restoreTypeParameters(savedParams);
    }
  }

  private static String messageName(ClassState state, String signature) {
    for (String entry : state.messages) {
      int split = entry.indexOf('=');
      if (entry.substring(0, split).equals(signature)) {
        return entry.substring(split + 1);
      }
    }
    return null;
  }

  private void constructor(CtorDecl ctor, ClassState state, JavaWriter out, String symbol) {
    if (ctor.modifiers().contains(Modifier.STATIC)) {
      JavaWriter staticBody = out.nested();
      state.statements.body(ctor.body(), staticBody);
      out.block("static", staticBody);
      return;
    }
    String simple = JavaNames.simpleName(state.irName) + "Base";
    String header = "protected " + simple + parameters(ctor.parameters(), state.types);
    JavaWriter inner = out.nested();
    if (ctor.baseCallArguments() != null && !ctor.baseCallArguments().isEmpty()) {
      inner.line("super" + argumentList(ctor.baseCallArguments(), state) + ";");
    }
    try {
      state.statements.body(ctor.body(), inner);
    } catch (ManualException e) {
      throw new TodoException("Handarbeit im Konstruktor (" + e.getMessage() + ")");
    }
    out.block(header, inner);
  }

  private String argumentList(List<Expr> args, ClassState state) {
    StringBuilder out = new StringBuilder("(");
    for (int i = 0; i < args.size(); i++) {
      out.append(i == 0 ? "" : ", ").append(state.expressions.text(args.get(i)));
    }
    return out.append(')').toString();
  }

  /**
   * Member mit Rumpf. Nicht erzeugbarer Rumpf → {@code throw ... PORT-TODO}; {@code raw}/{@code
   * manual} → abstrakte Methode, Handarbeit nötig.
   */
  private void bodyMember(
      String header,
      io.github.localsoul.port.ir.ast.Stmt body,
      ClassState state,
      JavaWriter out,
      String symbol,
      String returnType) {
    JavaWriter inner = out.nested();
    Map<String, String> savedImports = state.types.saveImports();
    try {
      state.statements.body(body, inner);
    } catch (ManualException e) {
      state.types.restoreImports(savedImports);
      state.needsManual = true;
      todos.add(new Todo(symbol, "Handarbeit in manual/: " + e.getMessage()));
      out.line(abstractHeader(header) + ";");
      return;
    } catch (TodoException e) {
      state.types.restoreImports(savedImports);
      todos.add(new Todo(symbol, e.getMessage()));
      inner = out.nested();
      state.statements.todoThrow("PORT-TODO: " + symbol + ": " + e.getMessage(), inner);
    }
    out.block(header, inner);
  }

  /** Macht aus einem Methodenkopf eine abstrakte Deklaration (private/static entfallen). */
  private static String abstractHeader(String header) {
    String result = header.replace("static ", "");
    if (result.startsWith("public ") || result.startsWith("protected ")) {
      int space = result.indexOf(' ');
      return result.substring(0, space) + " abstract" + result.substring(space);
    }
    return "protected abstract " + result;
  }

  private static String parameters(List<Param> params, TypeContext types) {
    StringBuilder out = new StringBuilder("(");
    for (int i = 0; i < params.size(); i++) {
      Param param = params.get(i);
      if (param.mode() != ParamMode.VALUE) {
        throw new TodoException(
            "Parameter "
                + param.name()
                + " mit :"
                + param.mode().spelling()
                + " (Java hat keine Referenzparameter)");
      }
      out.append(i == 0 ? "" : ", ")
          .append(types.javaType(param.type()))
          .append(' ')
          .append(JavaNames.ident(param.name()));
    }
    return out.append(')').toString();
  }

  private static String typeParameters(List<String> generics) {
    if (generics.isEmpty()) {
      return "";
    }
    return "<" + String.join(", ", generics) + ">";
  }

  private static String fieldModifiers(List<Modifier> modifiers) {
    StringBuilder out = new StringBuilder(access(modifiers));
    if (modifiers.contains(Modifier.CONST)) {
      out.append("static final ");
    } else {
      if (modifiers.contains(Modifier.STATIC)) {
        out.append("static ");
      }
      if (modifiers.contains(Modifier.READONLY)) {
        out.append("final ");
      }
    }
    return out.toString();
  }

  private static String memberModifiers(List<Modifier> modifiers, boolean forAbstract) {
    StringBuilder out = new StringBuilder(access(modifiers));
    if (forAbstract && out.isEmpty()) {
      out.append("protected ");
    }
    if (modifiers.contains(Modifier.STATIC) && !forAbstract) {
      out.append("static ");
    }
    if (modifiers.contains(Modifier.SEALED) && modifiers.contains(Modifier.OVERRIDE)) {
      out.append("final ");
    }
    return out.toString();
  }

  /** Zugriff: siehe Klassenkommentar. */
  private static String access(List<Modifier> modifiers) {
    if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.INTERNAL)) {
      return "public ";
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      return "protected ";
    }
    return "";
  }

  // --- Dateien ---

  private void emitThinClass(
      ClassDecl decl,
      String javaPackage,
      String simpleName,
      String typeParams,
      List<CtorDecl> constructors,
      ClassState state) {
    String irName = decl.name();
    if (config.manualClasses().contains(irName)) {
      return;
    }
    if (state.needsManual) {
      todos.add(
          new Todo(
              irName,
              "Klasse braucht Handarbeit: manual/"
                  + simpleName
                  + ".java extends "
                  + simpleName
                  + "Base"));
      return;
    }
    TypeContext types = new TypeContext(config, ownTypes, javaPackage);
    types.addTypeParameters(decl.typeAttrs().generics());
    JavaWriter body = new JavaWriter();
    body.push();
    boolean first = true;
    for (CtorDecl ctor : constructors) {
      try {
        String params = parameters(ctor.parameters(), types);
        if (!first) {
          body.blank();
        }
        body.open("public " + simpleName + params);
        body.line("super(" + parameterNames(ctor.parameters()) + ");");
        body.close("}");
        first = false;
      } catch (TodoException e) {
        // Schon beim Basis-Konstruktor gemeldet.
      }
    }
    String finalMod = decl.typeAttrs().modifiers().contains(Modifier.SEALED) ? "final " : "";
    String header =
        "public "
            + finalMod
            + "class "
            + simpleName
            + typeParams
            + " extends "
            + simpleName
            + "Base"
            + typeParams;
    String doc =
        LineBreaker.javadoc(
            "Generiert, solange es keine Handarbeit in {@code manual/"
                + simpleName
                + ".java} gibt.",
            0);
    files.add(file(javaPackage, simpleName, types, doc, header, body));
  }

  private static String parameterNames(List<Param> params) {
    List<String> names = new ArrayList<>();
    for (Param param : params) {
      names.add(JavaNames.ident(param.name()));
    }
    return String.join(", ", names);
  }

  private static String javadocBase(String irName) {
    return LineBreaker.javadoc(
        "Generiert aus dem IR-Typ {@code "
            + irName
            + "}. Nicht bearbeiten, Handarbeit in der Unterklasse.",
        0);
  }

  private static GeneratedFile file(
      String javaPackage,
      String simpleName,
      TypeContext types,
      String doc,
      String header,
      JavaWriter body) {
    StringBuilder out = new StringBuilder();
    out.append("package ").append(javaPackage).append(";\n\n");
    Set<String> imports = types.imports();
    for (String imported : imports) {
      out.append("import ").append(imported).append(";\n");
    }
    if (!imports.isEmpty()) {
      out.append('\n');
    }
    out.append(doc).append('\n');
    String bodyText = body.text();
    if (bodyText.isEmpty()) {
      out.append(header).append(" {}\n");
    } else {
      out.append(header).append(" {\n").append(bodyText).append("}\n");
    }
    String path = javaPackage.replace('.', '/') + "/" + simpleName + ".java";
    return new GeneratedFile(path, out.toString());
  }
}
