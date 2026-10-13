package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (array-lit type {expr})}.
 *
 * @param elementType Elementtyp
 * @param elements Elemente
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ArrayLitExpr(
    TypeRef elementType, List<Expr> elements, NodeAttrs attrs, SourceSpan span) implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public ArrayLitExpr {
    elements = List.copyOf(elements);
  }
}
