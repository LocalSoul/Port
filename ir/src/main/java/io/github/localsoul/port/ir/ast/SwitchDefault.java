package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (default {stmt})} in einem {@link SwitchStmt}.
 *
 * @param body Statements
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record SwitchDefault(List<Stmt> body, NodeAttrs attrs, SourceSpan span) implements IrNode {

  /** Kopiert Listen in unveränderliche Listen. */
  public SwitchDefault {
    body = List.copyOf(body);
  }
}
