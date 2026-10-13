package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.ArrayLitExpr;
import io.github.localsoul.port.ir.ast.BaseExpr;
import io.github.localsoul.port.ir.ast.BinaryExpr;
import io.github.localsoul.port.ir.ast.BinaryOp;
import io.github.localsoul.port.ir.ast.BoolLit;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.CastExpr;
import io.github.localsoul.port.ir.ast.CharLit;
import io.github.localsoul.port.ir.ast.CondExpr;
import io.github.localsoul.port.ir.ast.ConvExpr;
import io.github.localsoul.port.ir.ast.DefaultExpr;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.IndexExpr;
import io.github.localsoul.port.ir.ast.IsExpr;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.NewArrayExpr;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.NullLit;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticFieldExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.StringLit;
import io.github.localsoul.port.ir.ast.ThisExpr;
import io.github.localsoul.port.ir.ast.UnaryExpr;
import io.github.localsoul.port.ir.ast.UncheckedExpr;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.sexpr.NumberSuffix;
import java.util.List;
import java.util.Locale;

/**
 * Übersetzt IR-Ausdrücke in Java. Klammern nur, wo die Java-Rangfolge sie verlangt. Nicht
 * erzeugbare Konstrukte werfen {@link TodoException} (wird auf Member-Ebene zum Todo).
 */
final class ExprEmitter {

  /** Java-Ausdruck: Layout und Rangstufe (höher bindet stärker). */
  record JExpr(Doc doc, int precedence) {

    JExpr(String text, int precedence) {
      this(new Doc.Text(text), precedence);
    }

    /** Einzeilige Form. */
    String text() {
      return Doc.flat(doc);
    }
  }

  static final int ASSIGNMENT = 1;
  static final int TERNARY = 2;
  static final int UNARY = 13;
  static final int POSTFIX = 14;
  static final int PRIMARY = 15;

  private final TypeContext types;

  ExprEmitter(TypeContext types) {
    this.types = types;
  }

  /** Ausdruckstext ohne Rangstufe. */
  String text(Expr expr) {
    return emit(expr).text();
  }

  /** Reine Verteilertabelle über die in M3 unterstützten Ausdrücke. */
  JExpr emit(Expr expr) {
    return switch (expr) {
      case NumberLit n -> number(n);
      case StringLit s -> primary(JavaStrings.quote(s.value()));
      case BoolLit b -> primary(b.value() ? "true" : "false");
      case NullLit n -> primary("null");
      case CharLit c -> character(c.value());
      case IdentExpr i -> primary(JavaNames.ident(i.name()));
      case ThisExpr t -> primary("this");
      case BaseExpr b -> primary("super");
      case FieldExpr f -> field(f);
      case StaticFieldExpr f -> staticField(f.field());
      case PropExpr p -> property(p);
      case StaticPropExpr p -> staticProperty(p.property());
      case CallExpr c -> call(c);
      case StaticCallExpr c -> staticCall(c.method(), c.arguments());
      case NewExpr n -> construct(n);
      case NewArrayExpr n -> newArray(n);
      case ArrayLitExpr a -> arrayLiteral(a);
      case IndexExpr i -> index(i);
      case BinaryExpr b -> binary(b);
      case UnaryExpr u -> unary(u);
      case UncheckedExpr u -> emit(u.operand());
      case SetExpr s -> assignment(s);
      case SetOpExpr s -> compoundAssignment(s);
      case IncDecExpr i -> incDec(i);
      case ConvExpr c -> conversion(c.type(), c.operand());
      case CastExpr c -> cast(c.type(), c.operand());
      case IsExpr i -> instanceOf(i);
      case CondExpr c -> conditional(c);
      case DefaultExpr d -> defaultValue(d.type());
      case RawNode r -> throw new ManualException("raw " + r.language().spelling());
      case ManualNode m -> throw new ManualException(m.reason());
      default ->
          throw new TodoException("Ausdruck " + expr.getClass().getSimpleName() + " (nach M3)");
    };
  }

  // --- Literale ---

  private static JExpr primary(String text) {
    return new JExpr(text, PRIMARY);
  }

  private JExpr number(NumberLit number) {
    NumberSuffix suffix = number.suffix();
    String text = number.text();
    String digits = text.substring(0, text.length() - suffix.spelling().length());
    int precedence = digits.startsWith("-") ? UNARY : PRIMARY;
    return switch (suffix) {
      case I8 -> new JExpr("(byte) " + digits, UNARY);
      case I16 -> new JExpr("(short) " + digits, UNARY);
      case I32 -> new JExpr(digits, precedence);
      case I64 -> new JExpr(digits + "L", precedence);
      case F32 -> new JExpr(digits + "f", precedence);
      case F64 ->
          new JExpr(
              digits.contains(".") || digits.contains("e") ? digits : digits + "d", precedence);
      case U8, U16, U32, U64 ->
          throw new TodoException("vorzeichenloses Literal " + text + " (ADR-019)");
    };
  }

  private static JExpr character(char value) {
    boolean printable = value >= 0x20 && value < 0x7F && value != '\'' && value != '\\';
    if (printable) {
      return primary("'" + value + "'");
    }
    return new JExpr("(char) " + (int) value, UNARY);
  }

  // --- Member ---

  private JExpr target(Expr target) {
    JExpr emitted = emit(target);
    return emitted.precedence() < PRIMARY ? primary("(" + emitted.text() + ")") : emitted;
  }

  /** Hängt ein Glied an den Empfänger an; Ketten werden flach gehalten. */
  private static JExpr member(JExpr receiver, Doc segment) {
    if (receiver.doc() instanceof Doc.Chain chain) {
      List<Doc> segments = new java.util.ArrayList<>(chain.segments());
      segments.add(segment);
      return new JExpr(new Doc.Chain(chain.first(), segments), PRIMARY);
    }
    return new JExpr(new Doc.Chain(receiver.doc(), List.of(segment)), PRIMARY);
  }

  private List<Doc> argumentDocs(List<Expr> args) {
    List<Doc> result = new java.util.ArrayList<>();
    for (Expr arg : args) {
      result.add(emit(arg).doc());
    }
    return result;
  }

  private JExpr field(FieldExpr field) {
    MemberRef ref = field.field();
    JExpr receiver = target(field.target());
    if (types.isOwnType(ref.declaringType())) {
      return member(receiver, new Doc.Text("." + JavaNames.ident(ref.name())));
    }
    MappingEntry entry = external(ref, MappingForm.GETTER);
    return member(receiver, new Doc.Call("." + entry.javaSymbol(), List.of()));
  }

  private JExpr staticField(MemberRef ref) {
    String owner = types.className(ref.declaringType());
    if (types.isOwnType(ref.declaringType())) {
      return primary(owner + "." + JavaNames.ident(ref.name()));
    }
    MappingEntry entry = external(ref, MappingForm.STATIC_GETTER);
    return primary(owner + "." + entry.javaSymbol() + "()");
  }

  private JExpr property(PropExpr property) {
    MemberRef ref = property.property();
    if (isArrayLength(ref)) {
      return member(target(property.target()), new Doc.Text(".length"));
    }
    JExpr receiver = target(property.target());
    return member(receiver, new Doc.Call("." + getterName(ref, MappingForm.GETTER), List.of()));
  }

  private JExpr staticProperty(MemberRef ref) {
    String owner = types.className(ref.declaringType());
    return primary(owner + "." + getterName(ref, MappingForm.STATIC_GETTER) + "()");
  }

  /** {@code System.Array::Length} ist in Java das Sprachmittel {@code array.length}. */
  private static boolean isArrayLength(MemberRef ref) {
    return ref.declaringType().equals("System.Array") && ref.name().equals("Length");
  }

  /** Getter-Name: eigene Properties {@code name()}, externe laut Mapping. */
  String getterName(MemberRef ref, MappingForm form) {
    if (types.isOwnType(ref.declaringType())) {
      return JavaNames.member(ref.name());
    }
    return external(ref, form).javaSymbol();
  }

  private JExpr call(CallExpr call) {
    MemberRef ref = call.method();
    JExpr receiver = target(call.target());
    String name;
    if (types.isOwnType(ref.declaringType())) {
      name = JavaNames.member(ref.name());
    } else {
      name = external(ref, MappingForm.METHOD).javaSymbol();
    }
    return member(receiver, new Doc.Call("." + name, argumentDocs(call.arguments())));
  }

  private JExpr staticCall(MemberRef ref, List<Expr> args) {
    String owner = types.className(ref.declaringType());
    String name;
    if (types.isOwnType(ref.declaringType())) {
      name = JavaNames.member(ref.name());
    } else {
      MappingEntry entry = types.mapping().entry(ref.symbolId());
      boolean usable =
          entry != null
              && (entry.form() == MappingForm.STATIC_METHOD
                  || entry.form() == MappingForm.OPERATOR);
      if (!usable) {
        throw new TodoException(
            "externes Symbol " + ref.symbolId() + " ohne Mapping (static-method/operator)");
      }
      name = entry.javaSymbol();
    }
    return new JExpr(new Doc.Call(owner + "." + name, argumentDocs(args)), PRIMARY);
  }

  private JExpr construct(NewExpr construct) {
    MemberRef ref = construct.constructor();
    if (!types.isOwnType(ref.declaringType())) {
      external(ref, MappingForm.CONSTRUCTOR);
    }
    String type = types.className(ref.declaringType());
    String typeArguments = ref.declaringTypeArguments().isEmpty() ? "" : "<>";
    Doc call = new Doc.Call("new " + type + typeArguments, argumentDocs(construct.arguments()));
    return new JExpr(call, PRIMARY);
  }

  private MappingEntry external(MemberRef ref, MappingForm form) {
    MappingEntry entry = types.mapping().entry(ref.symbolId());
    if (entry == null || entry.form() != form) {
      throw new TodoException(
          "externes Symbol " + ref.symbolId() + " ohne Mapping (" + form.spelling() + ")");
    }
    return entry;
  }

  // --- Arrays ---

  private JExpr newArray(NewArrayExpr array) {
    if (array.lengths().size() != 1) {
      throw new TodoException("mehrdimensionales Array (SM008)");
    }
    String element = types.javaType(array.elementType());
    return primary("new " + element + "[" + emit(array.lengths().get(0)).text() + "]");
  }

  private JExpr arrayLiteral(ArrayLitExpr array) {
    String element = types.javaType(array.elementType());
    StringBuilder out = new StringBuilder("new " + element + "[] {");
    for (int i = 0; i < array.elements().size(); i++) {
      out.append(i == 0 ? "" : ", ").append(emit(array.elements().get(i)).text());
    }
    return primary(out.append('}').toString());
  }

  private JExpr index(IndexExpr index) {
    if (index.indices().size() != 1) {
      throw new TodoException("mehrdimensionaler Indexzugriff (SM008)");
    }
    return primary(target(index.target()).text() + "[" + emit(index.indices().get(0)).text() + "]");
  }

  // --- Operatoren ---

  private JExpr binary(BinaryExpr binary) {
    int precedence = precedence(binary.op());
    JExpr left = emit(binary.left());
    JExpr right = emit(binary.right());
    String leftText = left.precedence() < precedence ? "(" + left.text() + ")" : left.text();
    String rightText = right.precedence() <= precedence ? "(" + right.text() + ")" : right.text();
    return new JExpr(leftText + " " + binary.op().spelling() + " " + rightText, precedence);
  }

  /** Java-Rangstufen der binären Operatoren. */
  private static int precedence(BinaryOp op) {
    return switch (op) {
      case MUL, DIV, REM -> 12;
      case ADD, SUB -> 11;
      case SHL, SHR -> 10;
      case LT, LE, GT, GE -> 9;
      case EQ, NE -> 8;
      case AND -> 7;
      case XOR -> 6;
      case OR -> 5;
      case LOGICAL_AND -> 4;
      case LOGICAL_OR -> 3;
    };
  }

  private JExpr unary(UnaryExpr unary) {
    String symbol =
        switch (unary.op()) {
          case NEG -> "-";
          case NOT -> "!";
          case BNOT -> "~";
        };
    JExpr operand = emit(unary.operand());
    boolean needsParens = operand.precedence() < UNARY || operand.text().startsWith(symbol);
    String text = needsParens ? "(" + operand.text() + ")" : operand.text();
    return new JExpr(symbol + text, UNARY);
  }

  private JExpr assignment(SetExpr set) {
    if (set.target() instanceof PropExpr || set.target() instanceof StaticPropExpr) {
      throw new TodoException("Zuweisung an Property als Ausdruck (nur als Statement)");
    }
    JExpr value = emit(set.value());
    return new JExpr(lvalue(set.target()) + " = " + value.text(), ASSIGNMENT);
  }

  private JExpr compoundAssignment(SetOpExpr set) {
    if (set.target() instanceof PropExpr || set.target() instanceof StaticPropExpr) {
      throw new TodoException(
          "zusammengesetzte Zuweisung an Property als Ausdruck (nur als Statement)");
    }
    JExpr value = emit(set.value());
    return new JExpr(
        lvalue(set.target()) + " " + set.op().spelling() + "= " + value.text(), ASSIGNMENT);
  }

  private JExpr incDec(IncDecExpr expr) {
    if (expr.target() instanceof PropExpr || expr.target() instanceof StaticPropExpr) {
      throw new TodoException(expr.op().spelling() + " auf Property");
    }
    String target = lvalue(expr.target());
    return switch (expr.op()) {
      case PRE_INC -> new JExpr("++" + target, UNARY);
      case PRE_DEC -> new JExpr("--" + target, UNARY);
      case POST_INC -> new JExpr(target + "++", POSTFIX);
      case POST_DEC -> new JExpr(target + "--", POSTFIX);
    };
  }

  /** Ziel einer Zuweisung: Local, Feld, statisches Feld oder Array-Element. */
  String lvalue(Expr target) {
    return switch (target) {
      case IdentExpr i -> JavaNames.ident(i.name());
      case FieldExpr f when types.isOwnType(f.field().declaringType()) -> emit(f).text();
      case StaticFieldExpr f when types.isOwnType(f.field().declaringType()) -> emit(f).text();
      case IndexExpr i -> emit(i).text();
      default -> throw new TodoException("Zuweisungsziel " + target.getClass().getSimpleName());
    };
  }

  private JExpr conversion(TypeRef type, Expr operand) {
    if (!(type instanceof PrimitiveType)) {
      throw new TodoException("Umwandlung in " + type);
    }
    return cast(type, operand);
  }

  private JExpr cast(TypeRef type, Expr operand) {
    JExpr emitted = emit(operand);
    String text = emitted.precedence() < UNARY ? "(" + emitted.text() + ")" : emitted.text();
    return new JExpr("(" + types.javaType(type) + ") " + text, UNARY);
  }

  private JExpr instanceOf(IsExpr is) {
    if (is.binding() != null) {
      throw new TodoException("is mit Bindung");
    }
    JExpr operand = emit(is.operand());
    String text = operand.precedence() < 9 ? "(" + operand.text() + ")" : operand.text();
    return new JExpr(text + " instanceof " + types.javaType(is.type()), 9);
  }

  private JExpr conditional(CondExpr cond) {
    JExpr condition = emit(cond.condition());
    JExpr whenTrue = emit(cond.whenTrue());
    JExpr whenFalse = emit(cond.whenFalse());
    String c = condition.precedence() <= TERNARY ? "(" + condition.text() + ")" : condition.text();
    String t = whenTrue.precedence() < TERNARY ? "(" + whenTrue.text() + ")" : whenTrue.text();
    String f = whenFalse.precedence() < TERNARY ? "(" + whenFalse.text() + ")" : whenFalse.text();
    return new JExpr(c + " ? " + t + " : " + f, TERNARY);
  }

  private JExpr defaultValue(TypeRef type) {
    if (!(type instanceof PrimitiveType primitive)) {
      return primary("null");
    }
    String text =
        switch (primitive.primitive()) {
          case BOOL -> "false";
          case CHAR -> "(char) 0";
          case STRING, OBJECT -> "null";
          case F32 -> "0.0f";
          case F64 -> "0.0";
          case I64 -> "0L";
          case I8 -> "(byte) 0";
          case I16 -> "(short) 0";
          case I32 -> "0";
          default -> throw new TodoException("default(" + type + ")");
        };
    return new JExpr(text, text.startsWith("(") ? UNARY : PRIMARY);
  }

  /** Nur für Meldungen. */
  static String describe(Expr expr) {
    return expr.getClass().getSimpleName().toLowerCase(Locale.ROOT);
  }
}
