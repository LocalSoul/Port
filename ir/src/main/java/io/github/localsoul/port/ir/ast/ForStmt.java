package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (for ({stmt}) expr ({expr}) stmt)}.
 *
 * @param initializers Initialisierung
 * @param condition Bedingung
 * @param updates Fortschaltung
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ForStmt(
    List<Stmt> initializers,
    Expr condition,
    List<Expr> updates,
    Stmt body,
    NodeAttrs attrs,
    SourceSpan span)
    implements Stmt {

  /** Kopiert Listen in unveränderliche Listen. */
  public ForStmt {
    initializers = List.copyOf(initializers);
    updates = List.copyOf(updates);
  }
}
