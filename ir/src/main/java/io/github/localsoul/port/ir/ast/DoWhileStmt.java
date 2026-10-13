package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (do-while stmt expr)}.
 *
 * @param body Rumpf
 * @param condition Bedingung
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record DoWhileStmt(Stmt body, Expr condition, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
