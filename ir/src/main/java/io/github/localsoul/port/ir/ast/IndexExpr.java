package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (index expr expr {expr})}.
 *
 * @param target Array oder Objekt mit Indexer
 * @param indices Indizes, mindestens einer
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IndexExpr(Expr target, List<Expr> indices, NodeAttrs attrs, SourceSpan span)
    implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public IndexExpr {
    indices = List.copyOf(indices);
  }
}
