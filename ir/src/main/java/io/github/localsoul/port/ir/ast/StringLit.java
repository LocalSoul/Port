package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * String-Literal.
 *
 * @param value Inhalt mit aufgelösten Escapes
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record StringLit(String value, NodeAttrs attrs, SourceSpan span) implements Expr {}
