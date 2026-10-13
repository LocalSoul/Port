package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (continue)}.
 *
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ContinueStmt(NodeAttrs attrs, SourceSpan span) implements Stmt {}
