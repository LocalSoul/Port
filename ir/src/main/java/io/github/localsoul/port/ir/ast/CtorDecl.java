package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (ctor params [:mods] [:base-call ({expr})] body)}.
 *
 * @param parameters Parameter
 * @param modifiers Modifier
 * @param baseCallArguments Argumente des Basis-Konstruktoraufrufs, oder {@code null}
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CtorDecl(
    List<Param> parameters,
    List<Modifier> modifiers,
    List<Expr> baseCallArguments,
    Stmt body,
    NodeAttrs attrs,
    SourceSpan span)
    implements MemberDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public CtorDecl {
    parameters = List.copyOf(parameters);
    modifiers = List.copyOf(modifiers);
    if (baseCallArguments != null) {
      baseCallArguments = List.copyOf(baseCallArguments);
    }
  }
}
