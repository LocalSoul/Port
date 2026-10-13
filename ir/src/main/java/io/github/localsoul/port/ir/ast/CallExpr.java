package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;
import java.util.List;

/**
 * {@code (call expr mref {expr})}.
 *
 * @param target Objekt
 * @param method Methode
 * @param arguments Argumente
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CallExpr(
    Expr target, MemberRef method, List<Expr> arguments, NodeAttrs attrs, SourceSpan span)
    implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public CallExpr {
    arguments = List.copyOf(arguments);
  }
}
