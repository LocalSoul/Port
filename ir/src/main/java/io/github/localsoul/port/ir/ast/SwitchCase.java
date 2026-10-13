package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (case ({expr}) {stmt})} in einem {@link SwitchStmt}.
 *
 * @param labels Fallwerte
 * @param body Statements
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record SwitchCase(List<Expr> labels, List<Stmt> body, NodeAttrs attrs, SourceSpan span)
    implements IrNode {

  /** Kopiert Listen in unveränderliche Listen. */
  public SwitchCase {
    labels = List.copyOf(labels);
    body = List.copyOf(body);
  }
}
