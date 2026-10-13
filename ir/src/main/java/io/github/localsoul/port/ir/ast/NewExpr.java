package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;
import java.util.List;

/**
 * {@code (new mref {expr})}.
 *
 * @param constructor {@code .ctor}-Referenz
 * @param arguments Argumente
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record NewExpr(MemberRef constructor, List<Expr> arguments, NodeAttrs attrs, SourceSpan span)
    implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public NewExpr {
    arguments = List.copyOf(arguments);
  }
}
