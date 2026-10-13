package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.BreakStmt;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.ContinueStmt;
import io.github.localsoul.port.ir.ast.DoWhileStmt;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.ForStmt;
import io.github.localsoul.port.ir.ast.ForeachStmt;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IfStmt;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.LockStmt;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.SwitchCase;
import io.github.localsoul.port.ir.ast.SwitchStmt;
import io.github.localsoul.port.ir.ast.ThisExpr;
import io.github.localsoul.port.ir.ast.WhileStmt;
import io.github.localsoul.port.ir.atom.MemberRef;
import java.util.List;

/**
 * Übersetzt Statements in Java (Blöcke immer mit geschweiften Klammern, google-java-format).
 * Zuweisungen an Properties gibt es nur als Statement: {@code name(wert)}.
 */
final class StmtEmitter {

  private final TypeContext types;
  private final ExprEmitter expressions;
  private final LineBreaker breaker = new LineBreaker();

  StmtEmitter(TypeContext types, ExprEmitter expressions) {
    this.types = types;
    this.expressions = expressions;
  }

  /** Inhalt eines Rumpfs: bei {@code block} dessen Statements, sonst das einzelne Statement. */
  void body(Stmt body, JavaWriter out) {
    if (body instanceof BlockStmt block) {
      for (Stmt stmt : block.statements()) {
        emit(stmt, out);
      }
    } else {
      emit(body, out);
    }
  }

  /** Reine Verteilertabelle über die in M3 unterstützten Statements. */
  void emit(Stmt stmt, JavaWriter out) {
    switch (stmt) {
      case BlockStmt b -> block(b, out);
      case LocalStmt l -> local(l, out);
      case ExprStmt e -> expressionStatement(e.expression(), out);
      case IfStmt i -> ifStmt(i, out);
      case WhileStmt w -> braced("while (" + expressions.text(w.condition()) + ")", w.body(), out);
      case DoWhileStmt d -> doWhile(d, out);
      case ForStmt f -> forStmt(f, out);
      case ForeachStmt f -> foreach(f, out);
      case SwitchStmt s -> switchStmt(s, out);
      case ReturnStmt r -> returnStmt(r, out);
      case BreakStmt b -> out.line("break;");
      case ContinueStmt c -> out.line("continue;");
      case LockStmt l ->
          braced("synchronized (" + expressions.text(l.lockObject()) + ")", l.body(), out);
      case RawNode r -> throw new ManualException("raw " + r.language().spelling());
      case ManualNode m -> throw new ManualException(m.reason());
      default ->
          throw new TodoException("Statement " + stmt.getClass().getSimpleName() + " (nach M3)");
    }
  }

  private void statement(String prefix, Doc expr, JavaWriter out) {
    out.line(breaker.statement(prefix, expr, ";", out.indent()));
  }

  private void returnStmt(ReturnStmt stmt, JavaWriter out) {
    if (stmt.value() == null) {
      out.line("return;");
      return;
    }
    statement("return ", expressions.emit(stmt.value()).doc(), out);
  }

  /** Ausdrucks-Statement mit Umbruch, wo das Layout es zulässt. */
  private void expressionStatement(Expr expr, JavaWriter out) {
    boolean plainCall =
        expr instanceof CallExpr || expr instanceof StaticCallExpr || expr instanceof NewExpr;
    if (plainCall) {
      statement("", expressions.emit(expr).doc(), out);
    } else {
      out.line(expressionStatement(expr) + ";");
    }
  }

  /** {@code throw new UnsupportedOperationException("PORT-TODO: ...")} mit Umbruch. */
  void todoThrow(String message, JavaWriter out) {
    Doc call =
        new Doc.Call(
            "new UnsupportedOperationException",
            java.util.List.of(new Doc.Text(JavaStrings.quote(message))));
    statement("throw ", call, out);
  }

  private void block(BlockStmt block, JavaWriter out) {
    out.line("{");
    out.push();
    body(block, out);
    out.close("}");
  }

  private void braced(String header, Stmt body, JavaWriter out) {
    JavaWriter inner = out.nested();
    body(body, inner);
    out.block(header, inner);
  }

  private void local(LocalStmt local, JavaWriter out) {
    String declaration = types.javaType(local.type()) + " " + JavaNames.ident(local.name());
    if (local.initializer() == null) {
      out.line(declaration + ";");
      return;
    }
    statement(declaration + " = ", expressions.emit(local.initializer()).doc(), out);
  }

  private void ifStmt(IfStmt stmt, JavaWriter out) {
    out.open("if (" + expressions.text(stmt.condition()) + ")");
    body(stmt.thenBranch(), out);
    Stmt elseBranch = stmt.elseBranch();
    while (elseBranch instanceof IfStmt nested) {
      out.close("} else if (" + expressions.text(nested.condition()) + ") {");
      out.push();
      body(nested.thenBranch(), out);
      elseBranch = nested.elseBranch();
    }
    if (elseBranch != null) {
      out.close("} else {");
      out.push();
      body(elseBranch, out);
    }
    out.close("}");
  }

  private void doWhile(DoWhileStmt stmt, JavaWriter out) {
    out.open("do");
    body(stmt.body(), out);
    out.close("} while (" + expressions.text(stmt.condition()) + ");");
  }

  private void forStmt(ForStmt stmt, JavaWriter out) {
    StringBuilder header = new StringBuilder("for (");
    List<Stmt> initializers = stmt.initializers();
    for (int i = 0; i < initializers.size(); i++) {
      header.append(i == 0 ? "" : ", ").append(forInitializer(initializers.get(i), i == 0));
    }
    header.append("; ").append(expressions.text(stmt.condition())).append("; ");
    for (int i = 0; i < stmt.updates().size(); i++) {
      header.append(i == 0 ? "" : ", ").append(expressionStatement(stmt.updates().get(i)));
    }
    braced(header.append(")").toString(), stmt.body(), out);
  }

  private String forInitializer(Stmt stmt, boolean first) {
    if (stmt instanceof LocalStmt local && local.initializer() != null) {
      String type = first ? types.javaType(local.type()) + " " : "";
      return type + JavaNames.ident(local.name()) + " = " + expressions.text(local.initializer());
    }
    if (stmt instanceof ExprStmt expr) {
      return expressionStatement(expr.expression());
    }
    throw new TodoException("for-Initialisierung " + stmt.getClass().getSimpleName());
  }

  /** Java-for-each funktioniert für Arrays und (ab M3a) für {@code Iterable} aus runtime-system. */
  private void foreach(ForeachStmt stmt, JavaWriter out) {
    String header =
        "for ("
            + types.javaType(stmt.type())
            + " "
            + JavaNames.ident(stmt.variable())
            + " : "
            + expressions.text(stmt.collection())
            + ")";
    braced(header, stmt.body(), out);
  }

  /** {@code switch} ohne Durchfallen: jeder Zweig endet laut IR mit break/return/throw/continue. */
  private void switchStmt(SwitchStmt stmt, JavaWriter out) {
    out.open("switch (" + expressions.text(stmt.value()) + ")");
    for (SwitchCase branch : stmt.cases()) {
      for (Expr label : branch.labels()) {
        out.line("case " + expressions.text(label) + ":");
      }
      caseBody(branch.body(), out);
    }
    if (stmt.defaultCase() != null) {
      out.line("default:");
      caseBody(stmt.defaultCase().body(), out);
    }
    out.close("}");
  }

  private void caseBody(List<Stmt> body, JavaWriter out) {
    out.push();
    for (Stmt stmt : body) {
      emit(stmt, out);
    }
    out.pop();
  }

  /**
   * Ausdruck als Java-Statement. Zulässig sind Zuweisungen, Inkrement/Dekrement, Aufrufe und {@code
   * new}; Property-Zuweisungen werden zu Setter-Aufrufen.
   */
  String expressionStatement(Expr expr) {
    return switch (expr) {
      case SetExpr s when isProperty(s.target()) -> setter(s.target(), expressions.text(s.value()));
      case SetOpExpr s when isProperty(s.target()) -> compoundSetter(s);
      case SetExpr s -> expressions.text(s);
      case SetOpExpr s -> expressions.text(s);
      case IncDecExpr i -> expressions.text(i);
      case CallExpr c -> expressions.text(c);
      case StaticCallExpr c -> expressions.text(c);
      case NewExpr n -> expressions.text(n);
      default -> throw new TodoException("Ausdruck als Statement: " + ExprEmitter.describe(expr));
    };
  }

  private static boolean isProperty(Expr target) {
    return target instanceof PropExpr || target instanceof StaticPropExpr;
  }

  private String setter(Expr target, String value) {
    MemberRef ref;
    String receiver;
    if (target instanceof PropExpr prop) {
      ref = prop.property();
      receiver = expressions.text(prop.target());
    } else {
      ref = ((StaticPropExpr) target).property();
      receiver = types.className(ref.declaringType());
    }
    if (!types.isOwnType(ref.declaringType())) {
      throw new TodoException("Setter von " + ref.symbolId() + " ohne Mapping");
    }
    return receiver + "." + JavaNames.member(ref.name()) + "(" + value + ")";
  }

  /**
   * {@code p += v} auf eine Property: {@code p(p() + v)}; nur bei einfachem Ziel (keine
   * Seiteneffekte).
   */
  private String compoundSetter(SetOpExpr set) {
    if (set.target() instanceof PropExpr prop && !isSimple(prop.target())) {
      throw new TodoException("zusammengesetzte Zuweisung an Property mit komplexem Ziel");
    }
    String current = expressions.text(set.target());
    String value = expressions.text(set.value());
    return setter(set.target(), current + " " + set.op().spelling() + " " + value);
  }

  private static boolean isSimple(Expr target) {
    return target instanceof ThisExpr
        || target instanceof IdentExpr
        || (target instanceof FieldExpr field && isSimple(field.target()));
  }
}
