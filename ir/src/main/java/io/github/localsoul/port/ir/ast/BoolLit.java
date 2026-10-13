package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code true} oder {@code false}.
 *
 * @param value Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record BoolLit(boolean value, NodeAttrs attrs, SourceSpan span) implements Expr {}
