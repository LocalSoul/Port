package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (coalesce expr expr)}.
 *
 * @param left Wert
 * @param right Ersatz, wenn {@code left} null ist
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CoalesceExpr(Expr left, Expr right, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
