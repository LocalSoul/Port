package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (pre-inc|pre-dec|post-inc|post-dec lvalue)}.
 *
 * @param op Art
 * @param target Ziel, ein L-Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IncDecExpr(IncDecOp op, Expr target, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
