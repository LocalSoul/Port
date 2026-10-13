package io.github.localsoul.port.ir.reader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.TypeDecl;

/** Bettet Ausschnitte in vollständige IR-Dateien ein. */
final class Snippets {

  static final String HEADER = "(ir :version 1 :assembly \"Test\")\n";

  private Snippets() {}

  static IrFile read(String text) throws IrSyntaxException {
    return new IrReader().read(text, "s.ir");
  }

  /** Typdeklaration ab Zeile 2, Spalte 1. */
  static TypeDecl type(String decl) throws IrSyntaxException {
    return read(HEADER + decl).types().get(0);
  }

  /** Statement ab Zeile 3, Spalte 1, als Rumpf einer Methode. */
  static Stmt stmt(String stmt) throws IrSyntaxException {
    String text = HEADER + "(class T (method m void ()\n" + stmt + "\n))";
    ClassDecl clazz = (ClassDecl) read(text).types().get(0);
    MethodDecl method = (MethodDecl) clazz.members().get(0);
    return method.body();
  }

  /** Ausdruck ab Zeile 4, Spalte 1, in einem {@code (expr ...)}. */
  static Expr expr(String expr) throws IrSyntaxException {
    Stmt stmt = stmt("(expr\n" + expr + "\n)");
    return ((ExprStmt) stmt).expression();
  }

  static BlockStmt block(String stmt) throws IrSyntaxException {
    return (BlockStmt) stmt(stmt);
  }

  /** Prüft, dass das Lesen mit einer Meldung scheitert, die {@code reason} enthält. */
  static IrSyntaxException rejects(ThrowingRead read, String reason) {
    IrSyntaxException e = assertThrows(IrSyntaxException.class, read::run);
    assertTrue(
        e.reason().contains(reason), "Meldung '" + e.reason() + "' enthält nicht '" + reason + "'");
    return e;
  }

  /** Lesevorgang, der einen Syntaxfehler werfen darf. */
  @FunctionalInterface
  interface ThrowingRead {
    void run() throws IrSyntaxException;
  }
}
