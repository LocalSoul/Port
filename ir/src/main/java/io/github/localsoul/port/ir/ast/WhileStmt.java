package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (while expr stmt)}.
 *
 * @param condition Bedingung
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record WhileStmt(Expr condition, Stmt body, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
