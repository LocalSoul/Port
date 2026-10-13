package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (lambda params stmt)}.
 *
 * @param parameters Parameter
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record LambdaExpr(List<Param> parameters, Stmt body, NodeAttrs attrs, SourceSpan span)
    implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public LambdaExpr {
    parameters = List.copyOf(parameters);
  }
}
