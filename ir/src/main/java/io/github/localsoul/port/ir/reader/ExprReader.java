package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.Identifiers;
import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.ArrayLitExpr;
import io.github.localsoul.port.ir.ast.AsExpr;
import io.github.localsoul.port.ir.ast.BaseExpr;
import io.github.localsoul.port.ir.ast.BinaryExpr;
import io.github.localsoul.port.ir.ast.BinaryOp;
import io.github.localsoul.port.ir.ast.BoolLit;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.CastExpr;
import io.github.localsoul.port.ir.ast.CharLit;
import io.github.localsoul.port.ir.ast.CheckedExpr;
import io.github.localsoul.port.ir.ast.CoalesceExpr;
import io.github.localsoul.port.ir.ast.CondExpr;
import io.github.localsoul.port.ir.ast.ConvExpr;
import io.github.localsoul.port.ir.ast.CopyExpr;
import io.github.localsoul.port.ir.ast.DefaultExpr;
import io.github.localsoul.port.ir.ast.DelegateExpr;
import io.github.localsoul.port.ir.ast.EventAddExpr;
import io.github.localsoul.port.ir.ast.EventRemoveExpr;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.IncDecOp;
import io.github.localsoul.port.ir.ast.IndexExpr;
import io.github.localsoul.port.ir.ast.IsExpr;
import io.github.localsoul.port.ir.ast.LambdaExpr;
import io.github.localsoul.port.ir.ast.NewArrayExpr;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.ast.NullLit;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticFieldExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.StringLit;
import io.github.localsoul.port.ir.ast.ThisExpr;
import io.github.localsoul.port.ir.ast.TypeofExpr;
import io.github.localsoul.port.ir.ast.UnaryExpr;
import io.github.localsoul.port.ir.ast.UnaryOp;
import io.github.localsoul.port.ir.ast.UncheckedExpr;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.sexpr.SBareInteger;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SNumber;
import io.github.localsoul.port.ir.sexpr.SString;
import io.github.localsoul.port.ir.sexpr.SSymbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Liest Ausdrücke (Spec Abschnitt 7 und 9). */
final class ExprReader {

  /** Knotennamen, die in Ausdrucksposition erlaubt sind (ohne Operatoren). */
  static final Set<String> NAMES =
      Set.of(
          "field",
          "sfield",
          "prop",
          "sprop",
          "call",
          "scall",
          "new",
          "new-array",
          "array-lit",
          "index",
          "neg",
          "not",
          "bnot",
          "checked",
          "unchecked",
          "set",
          "set-op",
          "pre-inc",
          "pre-dec",
          "post-inc",
          "post-dec",
          "conv",
          "cast",
          "as",
          "is",
          "cond",
          "coalesce",
          "typeof",
          "default",
          "lambda",
          "delegate",
          "event-add",
          "event-remove",
          "copy",
          "char",
          "raw",
          "manual");

  private ExprReader() {}

  /** Liefert {@code true}, wenn {@code head} ein Ausdrucksknoten oder Operator ist. */
  static boolean isExpressionName(String head) {
    return NAMES.contains(head) || BinaryOp.fromSpelling(head) != null;
  }

  /** Liest einen Ausdruck. */
  static Expr read(SExpr expr) throws IrSyntaxException {
    return switch (expr) {
      case SList list -> readList(list);
      case SSymbol symbol -> readSymbol(symbol);
      case SNumber number -> readNumber(number);
      case SString string -> new StringLit(string.value(), NodeAttrs.NONE, string.span());
      case SBareInteger bare ->
          throw new IrSyntaxException(
              bare.span(), "Zahl ohne Typsuffix '" + bare.text() + "' (z. B. 0i32)");
      default -> throw Expect.expected(expr, "Ausdruck");
    };
  }

  /** Liest alle Elemente als Ausdrücke. */
  static List<Expr> readAll(List<SExpr> exprs) throws IrSyntaxException {
    List<Expr> result = new ArrayList<>();
    for (SExpr expr : exprs) {
      result.add(read(expr));
    }
    return result;
  }

  /** {@code ( {expr} )}. */
  static List<Expr> readList(SExpr expr, String what) throws IrSyntaxException {
    SList list = Expect.list(expr, what);
    return readAll(list.items());
  }

  private static NumberLit readNumber(SNumber number) throws IrSyntaxException {
    NumberLiterals.checkRange(number);
    return new NumberLit(number.text(), number.suffix(), NodeAttrs.NONE, number.span());
  }

  private static Expr readSymbol(SSymbol symbol) throws IrSyntaxException {
    String text = symbol.text();
    SourceSpan span = symbol.span();
    switch (text) {
      case "true":
        return new BoolLit(true, NodeAttrs.NONE, span);
      case "false":
        return new BoolLit(false, NodeAttrs.NONE, span);
      case "null":
        return new NullLit(NodeAttrs.NONE, span);
      case "this":
        return new ThisExpr(NodeAttrs.NONE, span);
      case "base":
        return new BaseExpr(NodeAttrs.NONE, span);
      default:
        break;
    }
    if (!Identifiers.isIdent(text)) {
      throw Expect.expected(symbol, "Ausdruck (Bezeichner, Literal oder Liste)");
    }
    return new IdentExpr(text, NodeAttrs.NONE, span);
  }

  private static Expr readList(SList list) throws IrSyntaxException {
    String head = Form.headOf(list);
    BinaryOp binaryOp = BinaryOp.fromSpelling(head);
    if (binaryOp != null) {
      return readBinary(Form.of(list, KeywordSpec.NONE), binaryOp);
    }
    UnaryOp unaryOp = UnaryOp.fromSpelling(head);
    if (unaryOp != null) {
      Form form = Form.of(list, KeywordSpec.NONE);
      form.requireArgs(1, 1, "(" + head + " expr)");
      return new UnaryExpr(unaryOp, read(form.arg(0)), form.nodeAttrs(), form.span());
    }
    IncDecOp incDecOp = IncDecOp.fromSpelling(head);
    if (incDecOp != null) {
      Form form = Form.of(list, KeywordSpec.NONE);
      form.requireArgs(1, 1, "(" + head + " lvalue)");
      Expr target = lvalue(form.arg(0));
      return new IncDecExpr(incDecOp, target, form.nodeAttrs(), form.span());
    }
    if (head.equals("raw")) {
      return ManualNodes.readRaw(list);
    }
    if (head.equals("manual")) {
      return ManualNodes.readManual(list);
    }
    checkExpressionName(list, head);
    return readNamed(Form.of(list, KeywordSpec.NONE));
  }

  private static void checkExpressionName(SList list, String head) throws IrSyntaxException {
    if (NAMES.contains(head)) {
      return;
    }
    if (Expect.DIALECT_NAMES.contains(head)) {
      throw Expect.dialect(list.span(), head);
    }
    if (StmtReader.NAMES.contains(head)) {
      throw new IrSyntaxException(
          list.span(), "'" + head + "' ist ein Statement, an dieser Stelle steht ein Ausdruck");
    }
    throw new IrSyntaxException(list.span(), "unbekannter Knotenname '" + head + "'");
  }

  /** Verteilt auf die Leser je Knotenname. Reine Verteilertabelle. */
  private static Expr readNamed(Form form) throws IrSyntaxException {
    return switch (form.head()) {
      case "field" -> readField(form);
      case "sfield" -> readStaticField(form);
      case "prop" -> readProp(form);
      case "sprop" -> readStaticProp(form);
      case "call" -> readCall(form);
      case "scall" -> readStaticCall(form);
      case "new" -> readNew(form);
      case "new-array" -> readNewArray(form);
      case "array-lit" -> readArrayLit(form);
      case "index" -> readIndex(form);
      case "checked", "unchecked", "copy" -> readWrapper(form);
      case "set" -> readSet(form);
      case "set-op" -> readSetOp(form);
      case "conv", "cast", "as" -> readTypeConversion(form);
      case "is" -> readIs(form);
      case "cond" -> readCond(form);
      case "coalesce" -> readCoalesce(form);
      case "typeof", "default" -> readTypeOperand(form);
      case "lambda" -> readLambda(form);
      case "delegate" -> readDelegate(form);
      case "event-add", "event-remove" -> readEvent(form);
      case "char" -> readChar(form);
      default -> throw new IllegalStateException("nicht verteilter Ausdruck: " + form.head());
    };
  }

  private static Expr readBinary(Form form, BinaryOp op) throws IrSyntaxException {
    form.requireArgs(2, 2, "(" + op.spelling() + " expr expr)");
    Expr left = read(form.arg(0));
    Expr right = read(form.arg(1));
    return new BinaryExpr(op, left, right, form.nodeAttrs(), form.span());
  }

  private static Expr readField(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(field expr mref)");
    Expr target = read(form.arg(0));
    return new FieldExpr(target, Expect.member(form.arg(1)), form.nodeAttrs(), form.span());
  }

  private static Expr readStaticField(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(sfield mref)");
    return new StaticFieldExpr(Expect.member(form.arg(0)), form.nodeAttrs(), form.span());
  }

  private static Expr readProp(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(prop expr mref)");
    Expr target = read(form.arg(0));
    return new PropExpr(target, Expect.member(form.arg(1)), form.nodeAttrs(), form.span());
  }

  private static Expr readStaticProp(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(sprop mref)");
    return new StaticPropExpr(Expect.member(form.arg(0)), form.nodeAttrs(), form.span());
  }

  private static Expr readCall(Form form) throws IrSyntaxException {
    form.requireArgs(2, -1, "(call expr mref {expr})");
    Expr target = read(form.arg(0));
    MemberRef method = Expect.member(form.arg(1));
    List<Expr> arguments = readAll(form.argsFrom(2));
    return new CallExpr(target, method, arguments, form.nodeAttrs(), form.span());
  }

  private static Expr readStaticCall(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(scall mref {expr})");
    MemberRef method = Expect.member(form.arg(0));
    List<Expr> arguments = readAll(form.argsFrom(1));
    return new StaticCallExpr(method, arguments, form.nodeAttrs(), form.span());
  }

  private static Expr readNew(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(new mref {expr})");
    MemberRef constructor = Expect.member(form.arg(0));
    if (!constructor.name().equals(".ctor")) {
      throw new IrSyntaxException(
          form.arg(0).span(), "'new' erwartet eine .ctor-Referenz, gefunden '" + constructor + "'");
    }
    List<Expr> arguments = readAll(form.argsFrom(1));
    return new NewExpr(constructor, arguments, form.nodeAttrs(), form.span());
  }

  private static Expr readNewArray(Form form) throws IrSyntaxException {
    form.requireArgs(2, -1, "(new-array type expr {expr})");
    TypeRef elementType = Expect.type(form.arg(0));
    List<Expr> lengths = readAll(form.argsFrom(1));
    return new NewArrayExpr(elementType, lengths, form.nodeAttrs(), form.span());
  }

  private static Expr readArrayLit(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(array-lit type {expr})");
    TypeRef elementType = Expect.type(form.arg(0));
    List<Expr> elements = readAll(form.argsFrom(1));
    return new ArrayLitExpr(elementType, elements, form.nodeAttrs(), form.span());
  }

  private static Expr readIndex(Form form) throws IrSyntaxException {
    form.requireArgs(2, -1, "(index expr expr {expr})");
    Expr target = read(form.arg(0));
    List<Expr> indices = readAll(form.argsFrom(1));
    return new IndexExpr(target, indices, form.nodeAttrs(), form.span());
  }

  private static Expr readWrapper(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(" + form.head() + " expr)");
    Expr operand = read(form.arg(0));
    NodeAttrs attrs = form.nodeAttrs();
    return switch (form.head()) {
      case "checked" -> new CheckedExpr(operand, attrs, form.span());
      case "unchecked" -> new UncheckedExpr(operand, attrs, form.span());
      default -> new CopyExpr(operand, attrs, form.span());
    };
  }

  private static Expr readSet(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(set lvalue expr)");
    Expr target = lvalue(form.arg(0));
    Expr value = read(form.arg(1));
    return new SetExpr(target, value, form.nodeAttrs(), form.span());
  }

  private static Expr readSetOp(Form form) throws IrSyntaxException {
    form.requireArgs(3, 3, "(set-op opsym lvalue expr)");
    SExpr opExpr = form.arg(0);
    BinaryOp op = null;
    if (opExpr instanceof SSymbol symbol) {
      op = BinaryOp.fromSpelling(symbol.text());
    }
    if (op == null) {
      throw Expect.expected(opExpr, "Operator");
    }
    Expr target = lvalue(form.arg(1));
    Expr value = read(form.arg(2));
    return new SetOpExpr(op, target, value, form.nodeAttrs(), form.span());
  }

  private static Expr readTypeConversion(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(" + form.head() + " type expr)");
    TypeRef type = Expect.type(form.arg(0));
    Expr operand = read(form.arg(1));
    NodeAttrs attrs = form.nodeAttrs();
    return switch (form.head()) {
      case "conv" -> new ConvExpr(type, operand, attrs, form.span());
      case "cast" -> new CastExpr(type, operand, attrs, form.span());
      default -> new AsExpr(type, operand, attrs, form.span());
    };
  }

  private static Expr readIs(Form form) throws IrSyntaxException {
    form.requireArgs(2, 3, "(is type expr [ident])");
    TypeRef type = Expect.type(form.arg(0));
    Expr operand = read(form.arg(1));
    String binding = null;
    if (form.argCount() == 3) {
      binding = Expect.ident(form.arg(2), "gebundene Variable");
    }
    return new IsExpr(type, operand, binding, form.nodeAttrs(), form.span());
  }

  private static Expr readCond(Form form) throws IrSyntaxException {
    form.requireArgs(3, 3, "(cond expr expr expr)");
    Expr condition = read(form.arg(0));
    Expr whenTrue = read(form.arg(1));
    Expr whenFalse = read(form.arg(2));
    return new CondExpr(condition, whenTrue, whenFalse, form.nodeAttrs(), form.span());
  }

  private static Expr readCoalesce(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(coalesce expr expr)");
    Expr left = read(form.arg(0));
    Expr right = read(form.arg(1));
    return new CoalesceExpr(left, right, form.nodeAttrs(), form.span());
  }

  private static Expr readTypeOperand(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(" + form.head() + " type)");
    TypeRef type = Expect.type(form.arg(0));
    if (form.head().equals("typeof")) {
      return new TypeofExpr(type, form.nodeAttrs(), form.span());
    }
    return new DefaultExpr(type, form.nodeAttrs(), form.span());
  }

  private static Expr readLambda(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(lambda params stmt)");
    List<Param> parameters = DeclReader.readParams(form.arg(0));
    Stmt body = StmtReader.read(form.arg(1));
    return new LambdaExpr(parameters, body, form.nodeAttrs(), form.span());
  }

  private static Expr readDelegate(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(delegate expr mref)");
    Expr target = read(form.arg(0));
    MemberRef method = Expect.member(form.arg(1));
    return new DelegateExpr(target, method, form.nodeAttrs(), form.span());
  }

  private static Expr readEvent(Form form) throws IrSyntaxException {
    form.requireArgs(3, 3, "(" + form.head() + " expr mref expr)");
    Expr target = read(form.arg(0));
    MemberRef event = Expect.member(form.arg(1));
    Expr handler = read(form.arg(2));
    if (form.head().equals("event-add")) {
      return new EventAddExpr(target, event, handler, form.nodeAttrs(), form.span());
    }
    return new EventRemoveExpr(target, event, handler, form.nodeAttrs(), form.span());
  }

  private static Expr readChar(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(char nat)");
    SExpr value = form.arg(0);
    if (!(value instanceof SBareInteger bare)) {
      throw Expect.expected(value, "Zahl ohne Suffix (UTF-16-Code-Unit)");
    }
    char codeUnit = NumberLiterals.charValue(bare);
    return new CharLit(codeUnit, form.nodeAttrs(), form.span());
  }

  /** Liest einen L-Wert: ident, field, sfield, prop, sprop oder index. */
  private static Expr lvalue(SExpr expr) throws IrSyntaxException {
    Expr target = read(expr);
    boolean assignable =
        switch (target) {
          case IdentExpr _ -> true;
          case FieldExpr _ -> true;
          case StaticFieldExpr _ -> true;
          case PropExpr _ -> true;
          case StaticPropExpr _ -> true;
          case IndexExpr _ -> true;
          default -> false;
        };
    if (!assignable) {
      throw new IrSyntaxException(
          expr.span(), "L-Wert erwartet (ident, field, sfield, prop, sprop oder index)");
    }
    return target;
  }
}
