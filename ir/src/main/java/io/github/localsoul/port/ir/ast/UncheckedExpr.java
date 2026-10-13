package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (unchecked expr)}.
 *
 * @param operand Ausdruck
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record UncheckedExpr(Expr operand, NodeAttrs attrs, SourceSpan span) implements Expr {}
