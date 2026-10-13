package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.BreakStmt;
import io.github.localsoul.port.ir.ast.CatchClause;
import io.github.localsoul.port.ir.ast.ContinueStmt;
import io.github.localsoul.port.ir.ast.DoWhileStmt;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.ForStmt;
import io.github.localsoul.port.ir.ast.ForeachStmt;
import io.github.localsoul.port.ir.ast.GotoStmt;
import io.github.localsoul.port.ir.ast.IfStmt;
import io.github.localsoul.port.ir.ast.LabelStmt;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.LockStmt;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.SwitchCase;
import io.github.localsoul.port.ir.ast.SwitchStmt;
import io.github.localsoul.port.ir.ast.ThrowStmt;
import io.github.localsoul.port.ir.ast.TryStmt;
import io.github.localsoul.port.ir.ast.UsingStmt;
import io.github.localsoul.port.ir.ast.WhileStmt;
import io.github.localsoul.port.ir.ast.YieldBreakStmt;
import io.github.localsoul.port.ir.ast.YieldReturnStmt;
import java.util.ArrayList;
import java.util.List;

/** Übersetzt Statements in {@link Doc}. */
final class StmtDocs {

  private StmtDocs() {}

  static List<Doc> of(List<Stmt> stmts) {
    List<Doc> result = new ArrayList<>();
    for (Stmt stmt : stmts) {
      result.add(of(stmt));
    }
    return result;
  }

  /** Reine Verteilertabelle über alle Statementknoten. */
  static Doc of(Stmt stmt) {
    return switch (stmt) {
      case BlockStmt b -> block(b);
      case LocalStmt l -> local(l);
      case ExprStmt e ->
          GroupBuilder.named("expr").doc(ExprDocs.of(e.expression())).attrs(e.attrs()).build();
      case IfStmt i -> ifStmt(i);
      case WhileStmt w ->
          GroupBuilder.named("while")
              .doc(ExprDocs.of(w.condition()))
              .doc(of(w.body()))
              .attrs(w.attrs())
              .build();
      case DoWhileStmt d ->
          GroupBuilder.named("do-while")
              .doc(of(d.body()))
              .doc(ExprDocs.of(d.condition()))
              .attrs(d.attrs())
              .build();
      case ForStmt f -> forStmt(f);
      case ForeachStmt f ->
          GroupBuilder.named("foreach")
              .atom(f.variable())
              .atom(f.type().toString())
              .doc(ExprDocs.of(f.collection()))
              .doc(of(f.body()))
              .attrs(f.attrs())
              .build();
      case SwitchStmt s -> switchStmt(s);
      case BreakStmt b -> GroupBuilder.named("break").attrs(b.attrs()).build();
      case ContinueStmt c -> GroupBuilder.named("continue").attrs(c.attrs()).build();
      case ReturnStmt r -> returnStmt(r);
      case ThrowStmt t ->
          GroupBuilder.named("throw").doc(ExprDocs.of(t.exception())).attrs(t.attrs()).build();
      case TryStmt t -> tryStmt(t);
      case YieldReturnStmt y ->
          GroupBuilder.named("yield-return").doc(ExprDocs.of(y.value())).attrs(y.attrs()).build();
      case YieldBreakStmt y -> GroupBuilder.named("yield-break").attrs(y.attrs()).build();
      case LockStmt l ->
          GroupBuilder.named("lock")
              .doc(ExprDocs.of(l.lockObject()))
              .doc(of(l.body()))
              .attrs(l.attrs())
              .build();
      case UsingStmt u ->
          GroupBuilder.named("using")
              .atom(u.variable())
              .atom(u.type().toString())
              .doc(ExprDocs.of(u.resource()))
              .doc(of(u.body()))
              .attrs(u.attrs())
              .build();
      case LabelStmt l -> GroupBuilder.named("label").atom(l.name()).attrs(l.attrs()).build();
      case GotoStmt g -> GroupBuilder.named("goto").atom(g.label()).attrs(g.attrs()).build();
      case RawNode r -> ExprDocs.raw(r);
      case ManualNode m -> ExprDocs.manual(m);
    };
  }

  /** Ein nicht-leerer Block steht immer mehrzeilig, jedes Statement auf eigener Zeile. */
  private static Doc block(BlockStmt block) {
    return GroupBuilder.named("block")
        .bodyStartsHere()
        .docs(of(block.statements()))
        .attrs(block.attrs())
        .forceBreak(!block.statements().isEmpty())
        .build();
  }

  private static Doc local(LocalStmt local) {
    GroupBuilder builder =
        GroupBuilder.named("local").atom(local.name()).atom(local.type().toString());
    if (local.initializer() != null) {
      builder.doc(ExprDocs.of(local.initializer()));
    }
    return builder.attrs(local.attrs()).build();
  }

  private static Doc ifStmt(IfStmt stmt) {
    GroupBuilder builder =
        GroupBuilder.named("if").doc(ExprDocs.of(stmt.condition())).doc(of(stmt.thenBranch()));
    if (stmt.elseBranch() != null) {
      builder.doc(of(stmt.elseBranch()));
    }
    return builder.attrs(stmt.attrs()).build();
  }

  private static Doc forStmt(ForStmt stmt) {
    Doc init = GroupBuilder.headless().docs(of(stmt.initializers())).build();
    Doc updates = GroupBuilder.headless().docs(ExprDocs.of(stmt.updates())).build();
    return GroupBuilder.named("for")
        .doc(init)
        .doc(ExprDocs.of(stmt.condition()))
        .doc(updates)
        .doc(of(stmt.body()))
        .attrs(stmt.attrs())
        .build();
  }

  private static Doc switchStmt(SwitchStmt stmt) {
    GroupBuilder builder =
        GroupBuilder.named("switch").doc(ExprDocs.of(stmt.value())).bodyStartsHere();
    for (SwitchCase branch : stmt.cases()) {
      Doc labels = GroupBuilder.headless().docs(ExprDocs.of(branch.labels())).build();
      Doc caseDoc =
          GroupBuilder.named("case")
              .doc(labels)
              .docs(of(branch.body()))
              .attrs(branch.attrs())
              .build();
      builder.doc(caseDoc);
    }
    if (stmt.defaultCase() != null) {
      Doc defaultDoc =
          GroupBuilder.named("default")
              .docs(of(stmt.defaultCase().body()))
              .attrs(stmt.defaultCase().attrs())
              .build();
      builder.doc(defaultDoc);
    }
    return builder.attrs(stmt.attrs()).build();
  }

  private static Doc returnStmt(ReturnStmt stmt) {
    GroupBuilder builder = GroupBuilder.named("return");
    if (stmt.value() != null) {
      builder.doc(ExprDocs.of(stmt.value()));
    }
    return builder.attrs(stmt.attrs()).build();
  }

  private static Doc tryStmt(TryStmt stmt) {
    GroupBuilder builder = GroupBuilder.named("try").doc(of(stmt.body()));
    for (CatchClause clause : stmt.catches()) {
      Doc catchDoc =
          GroupBuilder.named("catch")
              .atom(clause.variable())
              .atom(clause.type().toString())
              .doc(of(clause.body()))
              .attrs(clause.attrs())
              .build();
      builder.doc(catchDoc);
    }
    if (stmt.finallyClause() != null) {
      Doc finallyDoc =
          GroupBuilder.named("finally")
              .doc(of(stmt.finallyClause().body()))
              .attrs(stmt.finallyClause().attrs())
              .build();
      builder.doc(finallyDoc);
    }
    return builder.attrs(stmt.attrs()).build();
  }
}
