package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (return [expr])}.
 *
 * @param value Rückgabewert, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ReturnStmt(Expr value, NodeAttrs attrs, SourceSpan span) implements Stmt {}
