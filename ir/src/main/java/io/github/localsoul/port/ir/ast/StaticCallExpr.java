package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;
import java.util.List;

/**
 * {@code (scall mref {expr})}.
 *
 * @param method statische Methode
 * @param arguments Argumente
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record StaticCallExpr(
    MemberRef method, List<Expr> arguments, NodeAttrs attrs, SourceSpan span) implements Expr {

  /** Kopiert Listen in unveränderliche Listen. */
  public StaticCallExpr {
    arguments = List.copyOf(arguments);
  }
}
