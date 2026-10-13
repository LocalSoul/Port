package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (opsym expr expr)}, nur für Primitive.
 *
 * @param op Operator
 * @param left linker Operand
 * @param right rechter Operand
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record BinaryExpr(BinaryOp op, Expr left, Expr right, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
