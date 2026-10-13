package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.AttributeUse;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.ParamMode;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Liest Typ- und Mitgliedsdeklarationen (Spec Abschnitt 5). */
final class DeclReader {

  private static final KeywordSpec TYPE_KEYWORDS =
      KeywordSpec.values("base", "implements", "generics", "mods", "attrs");
  private static final KeywordSpec ENUM_KEYWORDS = KeywordSpec.values("underlying");
  private static final KeywordSpec FIELD_KEYWORDS = KeywordSpec.values("mods", "attrs", "init");
  private static final KeywordSpec PROP_KEYWORDS = KeywordSpec.values("mods", "get", "set");
  private static final KeywordSpec METHOD_KEYWORDS =
      new KeywordSpec(Set.of("mods", "generics"), Set.of("abstract"));
  private static final KeywordSpec CTOR_KEYWORDS = KeywordSpec.values("mods", "base-call");
  private static final KeywordSpec EVENT_KEYWORDS = KeywordSpec.values("mods");
  private static final KeywordSpec PARAM_KEYWORDS =
      new KeywordSpec(Set.of("default"), Set.of("ref", "out", "in"));

  private DeclReader() {}

  /** Liest {@code class}, {@code struct}, {@code interface} oder {@code enum}. */
  static TypeDecl readType(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Typdeklaration");
    String head = Form.headOf(list);
    return switch (head) {
      case "class", "struct", "interface" -> readClassLike(Form.of(list, TYPE_KEYWORDS));
      case "enum" -> readEnum(Form.of(list, ENUM_KEYWORDS));
      default ->
          throw new IrSyntaxException(
              list.span(),
              "Typdeklaration erwartet (class, struct, interface, enum), gefunden '" + head + "'");
    };
  }

  private static TypeDecl readClassLike(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(" + form.head() + " qname typeattrs {member})");
    String name = Expect.qualifiedName(form.arg(0), "Typname");
    TypeAttrs typeAttrs = readTypeAttrs(form);
    List<MemberDecl> members = new ArrayList<>();
    for (SExpr memberExpr : form.argsFrom(1)) {
      members.add(readMember(memberExpr));
    }
    NodeAttrs attrs = form.nodeAttrs();
    return switch (form.head()) {
      case "class" -> new ClassDecl(name, typeAttrs, members, attrs, form.span());
      case "struct" -> new StructDecl(name, typeAttrs, members, attrs, form.span());
      default -> new InterfaceDecl(name, typeAttrs, members, attrs, form.span());
    };
  }

  private static TypeAttrs readTypeAttrs(Form form) throws IrSyntaxException {
    TypeRef baseType = null;
    SExpr baseExpr = form.value("base");
    if (baseExpr != null) {
      baseType = Expect.type(baseExpr);
    }
    List<TypeRef> implemented = new ArrayList<>();
    SExpr implementsExpr = form.value("implements");
    if (implementsExpr != null) {
      implemented = Expect.typeList(implementsExpr, ":implements");
    }
    List<String> generics = readGenerics(form);
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    List<AttributeUse> attributes = readAttributes(form.value("attrs"));
    return new TypeAttrs(baseType, implemented, generics, modifiers, attributes);
  }

  private static TypeDecl readEnum(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(enum qname :underlying type {(ident number)})");
    String name = Expect.qualifiedName(form.arg(0), "Typname");
    TypeRef underlying = Expect.type(form.requiredValue("underlying"));
    List<EnumMember> members = new ArrayList<>();
    for (SExpr memberExpr : form.argsFrom(1)) {
      members.add(readEnumMember(memberExpr));
    }
    return new EnumDecl(name, underlying, members, form.nodeAttrs(), form.span());
  }

  private static EnumMember readEnumMember(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Enum-Wert (ident number)");
    Form form = Form.of(list, KeywordSpec.NONE);
    String name = Expect.ident(list.items().get(0), "Name des Enum-Werts");
    form.requireArgs(1, 1, "(ident number)");
    SExpr valueExpr = form.arg(0);
    if (!(valueExpr instanceof SNumber number)) {
      throw Expect.expected(valueExpr, "Zahl mit Typsuffix");
    }
    NumberLiterals.checkRange(number);
    NumberLit value = new NumberLit(number.text(), number.suffix(), NodeAttrs.NONE, number.span());
    return new EnumMember(name, value, form.nodeAttrs(), form.span());
  }

  private static MemberDecl readMember(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Mitglied");
    String head = Form.headOf(list);
    return switch (head) {
      case "field" -> readField(Form.of(list, FIELD_KEYWORDS));
      case "prop" -> readProp(Form.of(list, PROP_KEYWORDS));
      case "method" -> readMethod(Form.of(list, METHOD_KEYWORDS));
      case "ctor" -> readCtor(Form.of(list, CTOR_KEYWORDS));
      case "event" -> readEvent(Form.of(list, EVENT_KEYWORDS));
      default ->
          throw new IrSyntaxException(
              list.span(),
              "Mitglied erwartet (field, prop, method, ctor, event), gefunden '" + head + "'");
    };
  }

  private static MemberDecl readField(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(field ident type [:mods] [:attrs] [:init expr])");
    String name = Expect.ident(form.arg(0), "Feldname");
    TypeRef type = Expect.type(form.arg(1));
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    List<AttributeUse> attributes = readAttributes(form.value("attrs"));
    Expr initializer = null;
    SExpr initExpr = form.value("init");
    if (initExpr != null) {
      initializer = ExprReader.read(initExpr);
    }
    return new FieldDecl(
        name, type, modifiers, attributes, initializer, form.nodeAttrs(), form.span());
  }

  private static MemberDecl readProp(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(prop ident type [:mods] [:get body] [:set body])");
    String name = Expect.ident(form.arg(0), "Property-Name");
    TypeRef type = Expect.type(form.arg(1));
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    Stmt getter = readOptionalBody(form.value("get"));
    Stmt setter = readOptionalBody(form.value("set"));
    return new PropDecl(name, type, modifiers, getter, setter, form.nodeAttrs(), form.span());
  }

  private static MemberDecl readMethod(Form form) throws IrSyntaxException {
    form.requireArgs(3, 4, "(method ident type params [:mods] [:generics] (body | :abstract))");
    String name = Expect.ident(form.arg(0), "Methodenname");
    TypeRef returnType = Expect.type(form.arg(1));
    List<Param> parameters = readParams(form.arg(2));
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    List<String> generics = readGenerics(form);
    boolean hasBody = form.argCount() == 4;
    boolean isAbstract = form.hasFlag("abstract");
    if (hasBody == isAbstract) {
      String reason =
          hasBody ? "Methode hat Rumpf und :abstract" : "Methode braucht Rumpf oder :abstract";
      throw new IrSyntaxException(form.span(), reason);
    }
    Stmt body = hasBody ? StmtReader.read(form.arg(3)) : null;
    return new MethodDecl(
        name, returnType, parameters, modifiers, generics, body, form.nodeAttrs(), form.span());
  }

  private static MemberDecl readCtor(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(ctor params [:mods] [:base-call ({expr})] body)");
    List<Param> parameters = readParams(form.arg(0));
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    List<Expr> baseCallArguments = null;
    SExpr baseCallExpr = form.value("base-call");
    if (baseCallExpr != null) {
      baseCallArguments = ExprReader.readList(baseCallExpr, ":base-call");
    }
    Stmt body = StmtReader.read(form.arg(1));
    return new CtorDecl(
        parameters, modifiers, baseCallArguments, body, form.nodeAttrs(), form.span());
  }

  private static MemberDecl readEvent(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(event ident type [:mods])");
    String name = Expect.ident(form.arg(0), "Event-Name");
    TypeRef type = Expect.type(form.arg(1));
    List<Modifier> modifiers = Expect.modifiers(form.value("mods"));
    return new EventDecl(name, type, modifiers, form.nodeAttrs(), form.span());
  }

  /** {@code ( { (ident type [:ref|:out|:in] [:default expr]) } )}. */
  static List<Param> readParams(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Parameterliste");
    List<Param> result = new ArrayList<>();
    for (SExpr paramExpr : list.items()) {
      result.add(readParam(paramExpr));
    }
    return result;
  }

  private static Param readParam(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Parameter (ident type)");
    Form form = Form.of(list, PARAM_KEYWORDS);
    String name = Expect.ident(list.items().get(0), "Parametername");
    form.requireArgs(1, 1, "(ident type [:ref|:out|:in] [:default expr])");
    TypeRef type = Expect.type(form.arg(0));
    ParamMode mode = readParamMode(form);
    Expr defaultValue = null;
    SExpr defaultExpr = form.value("default");
    if (defaultExpr != null) {
      defaultValue = ExprReader.read(defaultExpr);
    }
    return new Param(name, type, mode, defaultValue, form.nodeAttrs(), form.span());
  }

  private static ParamMode readParamMode(Form form) throws IrSyntaxException {
    ParamMode mode = ParamMode.VALUE;
    int count = 0;
    for (ParamMode candidate : ParamMode.values()) {
      if (candidate != ParamMode.VALUE && form.hasFlag(candidate.spelling())) {
        mode = candidate;
        count++;
      }
    }
    if (count > 1) {
      throw new IrSyntaxException(form.span(), "nur eines von :ref, :out, :in ist erlaubt");
    }
    return mode;
  }

  /** {@code :attrs ( {(qname {expr})} )}, leer, wenn {@code expr} {@code null} ist. */
  private static List<AttributeUse> readAttributes(SExpr expr) throws IrSyntaxException {
    List<AttributeUse> result = new ArrayList<>();
    if (expr == null) {
      return result;
    }
    SList list = Expect.list(expr, ":attrs");
    for (SExpr item : list.items()) {
      SList attrList = Expect.list(item, "Attribut (qname {expr})");
      Form form = Form.of(attrList, KeywordSpec.NONE);
      String type = Expect.qualifiedName(attrList.items().get(0), "Attributtyp");
      List<Expr> arguments = ExprReader.readAll(form.argsFrom(0));
      result.add(new AttributeUse(type, arguments, form.nodeAttrs(), form.span()));
    }
    return result;
  }

  private static List<String> readGenerics(Form form) throws IrSyntaxException {
    SExpr genericsExpr = form.value("generics");
    if (genericsExpr == null) {
      return new ArrayList<>();
    }
    return Expect.identList(genericsExpr, ":generics");
  }

  private static Stmt readOptionalBody(SExpr expr) throws IrSyntaxException {
    if (expr == null) {
      return null;
    }
    return StmtReader.read(expr);
  }
}
