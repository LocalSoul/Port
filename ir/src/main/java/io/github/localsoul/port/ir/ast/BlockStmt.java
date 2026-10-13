package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (block {stmt})}.
 *
 * @param statements Statements
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record BlockStmt(List<Stmt> statements, NodeAttrs attrs, SourceSpan span) implements Stmt {

  /** Kopiert Listen in unveränderliche Listen. */
  public BlockStmt {
    statements = List.copyOf(statements);
  }
}
