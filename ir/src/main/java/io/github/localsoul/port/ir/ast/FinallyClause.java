package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (finally stmt)} in einem {@link TryStmt}.
 *
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record FinallyClause(Stmt body, NodeAttrs attrs, SourceSpan span) implements IrNode {}
