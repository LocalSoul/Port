package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (set-op opsym lvalue expr)}.
 *
 * @param op Operator
 * @param target Ziel, ein L-Wert
 * @param value Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record SetOpExpr(BinaryOp op, Expr target, Expr value, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
