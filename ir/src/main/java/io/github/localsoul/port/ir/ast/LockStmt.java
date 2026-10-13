package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (lock expr stmt)}.
 *
 * @param lockObject Sperrobjekt
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record LockStmt(Expr lockObject, Stmt body, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
