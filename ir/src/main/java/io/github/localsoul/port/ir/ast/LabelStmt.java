package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (label ident)}.
 *
 * @param name Sprungmarke
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record LabelStmt(String name, NodeAttrs attrs, SourceSpan span) implements Stmt {}
