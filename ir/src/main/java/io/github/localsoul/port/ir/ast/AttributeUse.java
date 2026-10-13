package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * Attribut {@code (qname {expr})} in {@code :attrs}.
 *
 * @param type qualifizierter Name des Attributtyps
 * @param arguments Argumente
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record AttributeUse(String type, List<Expr> arguments, NodeAttrs attrs, SourceSpan span)
    implements IrNode {

  /** Kopiert Listen in unveränderliche Listen. */
  public AttributeUse {
    arguments = List.copyOf(arguments);
  }
}
