package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (throw expr)}.
 *
 * @param exception geworfene Exception
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ThrowStmt(Expr exception, NodeAttrs attrs, SourceSpan span) implements Stmt {}
