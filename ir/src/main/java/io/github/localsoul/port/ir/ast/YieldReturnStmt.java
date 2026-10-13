package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (yield-return expr)}.
 *
 * @param value gelieferter Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record YieldReturnStmt(Expr value, NodeAttrs attrs, SourceSpan span) implements Stmt {}
