package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.BreakStmt;
import io.github.localsoul.port.ir.ast.CatchClause;
import io.github.localsoul.port.ir.ast.ContinueStmt;
import io.github.localsoul.port.ir.ast.DoWhileStmt;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.FinallyClause;
import io.github.localsoul.port.ir.ast.ForStmt;
import io.github.localsoul.port.ir.ast.ForeachStmt;
import io.github.localsoul.port.ir.ast.GotoStmt;
import io.github.localsoul.port.ir.ast.IfStmt;
import io.github.localsoul.port.ir.ast.LabelStmt;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.LockStmt;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.SwitchCase;
import io.github.localsoul.port.ir.ast.SwitchDefault;
import io.github.localsoul.port.ir.ast.SwitchStmt;
import io.github.localsoul.port.ir.ast.ThrowStmt;
import io.github.localsoul.port.ir.ast.TryStmt;
import io.github.localsoul.port.ir.ast.UsingStmt;
import io.github.localsoul.port.ir.ast.WhileStmt;
import io.github.localsoul.port.ir.ast.YieldBreakStmt;
import io.github.localsoul.port.ir.ast.YieldReturnStmt;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Liest Statements (Spec Abschnitt 6 und 9). */
final class StmtReader {

  /** Knotennamen, die in Statementposition erlaubt sind. */
  static final Set<String> NAMES =
      Set.of(
          "block",
          "local",
          "expr",
          "if",
          "while",
          "do-while",
          "for",
          "foreach",
          "switch",
          "break",
          "continue",
          "return",
          "throw",
          "try",
          "yield-return",
          "yield-break",
          "lock",
          "using",
          "label",
          "goto",
          "raw",
          "manual");

  private StmtReader() {}

  /** Liest ein Statement. */
  static Stmt read(SExpr expr) throws IrSyntaxException {
    if (!(expr instanceof SList list)) {
      throw Expect.expected(expr, "Statement (Liste)");
    }
    String head = Form.headOf(list);
    if (head.equals("raw")) {
      return ManualNodes.readRaw(list);
    }
    if (head.equals("manual")) {
      return ManualNodes.readManual(list);
    }
    checkStatementName(list, head);
    return readNamed(Form.of(list, KeywordSpec.NONE));
  }

  /** Liest alle Elemente als Statements. */
  static List<Stmt> readAll(List<SExpr> exprs) throws IrSyntaxException {
    List<Stmt> result = new ArrayList<>();
    for (SExpr expr : exprs) {
      result.add(read(expr));
    }
    return result;
  }

  private static void checkStatementName(SList list, String head) throws IrSyntaxException {
    if (NAMES.contains(head)) {
      return;
    }
    if (Expect.DIALECT_NAMES.contains(head)) {
      throw Expect.dialect(list.span(), head);
    }
    if (ExprReader.isExpressionName(head)) {
      throw new IrSyntaxException(
          list.span(),
          "'"
              + head
              + "' ist ein Ausdruck, an dieser Stelle steht ein Statement (mit (expr ...)"
              + " einpacken)");
    }
    throw new IrSyntaxException(list.span(), "unbekannter Knotenname '" + head + "'");
  }

  /** Verteilt auf die Leser je Knotenname. Reine Verteilertabelle. */
  private static Stmt readNamed(Form form) throws IrSyntaxException {
    return switch (form.head()) {
      case "block" -> new BlockStmt(readAll(form.argsFrom(0)), form.nodeAttrs(), form.span());
      case "local" -> readLocal(form);
      case "expr" -> readExpr(form);
      case "if" -> readIf(form);
      case "while" -> readWhile(form);
      case "do-while" -> readDoWhile(form);
      case "for" -> readFor(form);
      case "foreach" -> readForeach(form);
      case "switch" -> SwitchReader.read(form);
      case "break", "continue", "yield-break" -> readNoArgs(form);
      case "return" -> readReturn(form);
      case "throw" -> readThrow(form);
      case "try" -> readTry(form);
      case "yield-return" -> readYieldReturn(form);
      case "lock" -> readLock(form);
      case "using" -> readUsing(form);
      case "label", "goto" -> readLabelOrGoto(form);
      default -> throw new IllegalStateException("nicht verteiltes Statement: " + form.head());
    };
  }

  private static Stmt readLocal(Form form) throws IrSyntaxException {
    form.requireArgs(2, 3, "(local ident type [expr])");
    String name = Expect.ident(form.arg(0), "Name des Locals");
    TypeRef type = Expect.type(form.arg(1));
    Expr initializer = null;
    if (form.argCount() == 3) {
      initializer = ExprReader.read(form.arg(2));
    }
    return new LocalStmt(name, type, initializer, form.nodeAttrs(), form.span());
  }

  private static Stmt readExpr(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(expr expr)");
    Expr expression = ExprReader.read(form.arg(0));
    return new ExprStmt(expression, form.nodeAttrs(), form.span());
  }

  private static Stmt readIf(Form form) throws IrSyntaxException {
    form.requireArgs(2, 3, "(if expr stmt [stmt])");
    Expr condition = ExprReader.read(form.arg(0));
    Stmt thenBranch = read(form.arg(1));
    Stmt elseBranch = null;
    if (form.argCount() == 3) {
      elseBranch = read(form.arg(2));
    }
    return new IfStmt(condition, thenBranch, elseBranch, form.nodeAttrs(), form.span());
  }

  private static Stmt readWhile(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(while expr stmt)");
    Expr condition = ExprReader.read(form.arg(0));
    Stmt body = read(form.arg(1));
    return new WhileStmt(condition, body, form.nodeAttrs(), form.span());
  }

  private static Stmt readDoWhile(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(do-while stmt expr)");
    Stmt body = read(form.arg(0));
    Expr condition = ExprReader.read(form.arg(1));
    return new DoWhileStmt(body, condition, form.nodeAttrs(), form.span());
  }

  private static Stmt readFor(Form form) throws IrSyntaxException {
    form.requireArgs(4, 4, "(for ({stmt}) expr ({expr}) stmt)");
    SList initList = Expect.list(form.arg(0), "Initialisierung von 'for'");
    List<Stmt> initializers = readAll(initList.items());
    Expr condition = ExprReader.read(form.arg(1));
    List<Expr> updates = ExprReader.readList(form.arg(2), "Fortschaltung von 'for'");
    Stmt body = read(form.arg(3));
    return new ForStmt(initializers, condition, updates, body, form.nodeAttrs(), form.span());
  }

  private static Stmt readForeach(Form form) throws IrSyntaxException {
    form.requireArgs(4, 4, "(foreach ident type expr stmt)");
    String variable = Expect.ident(form.arg(0), "Schleifenvariable");
    TypeRef type = Expect.type(form.arg(1));
    Expr collection = ExprReader.read(form.arg(2));
    Stmt body = read(form.arg(3));
    return new ForeachStmt(variable, type, collection, body, form.nodeAttrs(), form.span());
  }

  private static Stmt readNoArgs(Form form) throws IrSyntaxException {
    form.requireArgs(0, 0, "(" + form.head() + ")");
    return switch (form.head()) {
      case "break" -> new BreakStmt(form.nodeAttrs(), form.span());
      case "continue" -> new ContinueStmt(form.nodeAttrs(), form.span());
      default -> new YieldBreakStmt(form.nodeAttrs(), form.span());
    };
  }

  private static Stmt readReturn(Form form) throws IrSyntaxException {
    form.requireArgs(0, 1, "(return [expr])");
    Expr value = null;
    if (form.argCount() == 1) {
      value = ExprReader.read(form.arg(0));
    }
    return new ReturnStmt(value, form.nodeAttrs(), form.span());
  }

  private static Stmt readThrow(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(throw expr)");
    Expr exception = ExprReader.read(form.arg(0));
    return new ThrowStmt(exception, form.nodeAttrs(), form.span());
  }

  private static Stmt readYieldReturn(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(yield-return expr)");
    Expr value = ExprReader.read(form.arg(0));
    return new YieldReturnStmt(value, form.nodeAttrs(), form.span());
  }

  private static Stmt readLock(Form form) throws IrSyntaxException {
    form.requireArgs(2, 2, "(lock expr stmt)");
    Expr lockObject = ExprReader.read(form.arg(0));
    Stmt body = read(form.arg(1));
    return new LockStmt(lockObject, body, form.nodeAttrs(), form.span());
  }

  private static Stmt readUsing(Form form) throws IrSyntaxException {
    form.requireArgs(4, 4, "(using ident type expr stmt)");
    String variable = Expect.ident(form.arg(0), "Variable");
    TypeRef type = Expect.type(form.arg(1));
    Expr resource = ExprReader.read(form.arg(2));
    Stmt body = read(form.arg(3));
    return new UsingStmt(variable, type, resource, body, form.nodeAttrs(), form.span());
  }

  private static Stmt readLabelOrGoto(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(" + form.head() + " ident)");
    String name = Expect.ident(form.arg(0), "Sprungmarke");
    if (form.head().equals("label")) {
      return new LabelStmt(name, form.nodeAttrs(), form.span());
    }
    return new GotoStmt(name, form.nodeAttrs(), form.span());
  }

  /** {@code (try stmt {(catch ident type stmt)} [(finally stmt)])}. */
  private static Stmt readTry(Form form) throws IrSyntaxException {
    form.requireArgs(1, -1, "(try stmt {(catch ...)} [(finally ...)])");
    Stmt body = read(form.arg(0));
    List<CatchClause> catches = new ArrayList<>();
    FinallyClause finallyClause = null;
    for (SExpr clauseExpr : form.argsFrom(1)) {
      SList clauseList = Expect.list(clauseExpr, "catch- oder finally-Zweig");
      String head = Form.headOf(clauseList);
      if (finallyClause != null) {
        throw new IrSyntaxException(
            clauseList.span(), "(finally ...) muss der letzte Zweig von 'try' sein");
      }
      if (head.equals("catch")) {
        catches.add(readCatch(Form.of(clauseList, KeywordSpec.NONE)));
      } else if (head.equals("finally")) {
        finallyClause = readFinally(Form.of(clauseList, KeywordSpec.NONE));
      } else {
        throw new IrSyntaxException(
            clauseList.span(), "(catch ...) oder (finally ...) erwartet, gefunden '" + head + "'");
      }
    }
    return new TryStmt(body, catches, finallyClause, form.nodeAttrs(), form.span());
  }

  private static CatchClause readCatch(Form form) throws IrSyntaxException {
    form.requireArgs(3, 3, "(catch ident type stmt)");
    String variable = Expect.ident(form.arg(0), "Variable der Exception");
    TypeRef type = Expect.type(form.arg(1));
    Stmt body = read(form.arg(2));
    return new CatchClause(variable, type, body, form.nodeAttrs(), form.span());
  }

  private static FinallyClause readFinally(Form form) throws IrSyntaxException {
    form.requireArgs(1, 1, "(finally stmt)");
    Stmt body = read(form.arg(0));
    return new FinallyClause(body, form.nodeAttrs(), form.span());
  }

  /** Liest {@code switch}, ausgelagert wegen der Zweige. */
  private static final class SwitchReader {

    private SwitchReader() {}

    /** {@code (switch expr {(case ({expr}) {stmt})} [(default {stmt})])}. */
    static Stmt read(Form form) throws IrSyntaxException {
      form.requireArgs(1, -1, "(switch expr {(case ...)} [(default ...)])");
      Expr value = ExprReader.read(form.arg(0));
      List<SwitchCase> cases = new ArrayList<>();
      SwitchDefault defaultCase = null;
      for (SExpr branchExpr : form.argsFrom(1)) {
        SList branchList = Expect.list(branchExpr, "case- oder default-Zweig");
        String head = Form.headOf(branchList);
        if (defaultCase != null) {
          throw new IrSyntaxException(
              branchList.span(), "(default ...) muss der letzte Zweig von 'switch' sein");
        }
        Form branch = Form.of(branchList, KeywordSpec.NONE);
        if (head.equals("case")) {
          cases.add(readCase(branch));
        } else if (head.equals("default")) {
          List<Stmt> body = readAll(branch.argsFrom(0));
          defaultCase = new SwitchDefault(body, branch.nodeAttrs(), branch.span());
        } else {
          throw new IrSyntaxException(
              branchList.span(), "(case ...) oder (default ...) erwartet, gefunden '" + head + "'");
        }
      }
      return new SwitchStmt(value, cases, defaultCase, form.nodeAttrs(), form.span());
    }

    private static SwitchCase readCase(Form form) throws IrSyntaxException {
      form.requireArgs(1, -1, "(case ({expr}) {stmt})");
      List<Expr> labels = ExprReader.readList(form.arg(0), "Fallwerte");
      List<Stmt> body = readAll(form.argsFrom(1));
      return new SwitchCase(labels, body, form.nodeAttrs(), form.span());
    }
  }
}
