package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (new-array type expr {expr})}.
 *
 * @param elementType Elementtyp
 * @param lengths Länge je Dimension, mindestens eine
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record NewArrayExpr(
    TypeRef elementType, List<Expr> lengths, NodeAttrs attrs, SourceSpan span) implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public NewArrayExpr {
    lengths = List.copyOf(lengths);
  }
}
