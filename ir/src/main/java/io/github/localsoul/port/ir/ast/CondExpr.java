package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (cond expr expr expr)}, Ternär.
 *
 * @param condition Bedingung
 * @param whenTrue Wert bei wahr
 * @param whenFalse Wert bei falsch
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CondExpr(
    Expr condition, Expr whenTrue, Expr whenFalse, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
