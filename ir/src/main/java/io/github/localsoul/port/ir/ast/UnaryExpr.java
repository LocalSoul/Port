package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (neg|not|bnot expr)}.
 *
 * @param op Operator
 * @param operand Operand
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record UnaryExpr(UnaryOp op, Expr operand, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
