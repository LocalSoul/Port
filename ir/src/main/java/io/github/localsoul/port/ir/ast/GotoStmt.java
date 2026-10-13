package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (goto ident)}.
 *
 * @param label Ziel-Sprungmarke
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record GotoStmt(String label, NodeAttrs attrs, SourceSpan span) implements Stmt {}
