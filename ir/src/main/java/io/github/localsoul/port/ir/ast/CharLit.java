package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (char 97)}.
 *
 * @param value UTF-16-Code-Unit
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CharLit(char value, NodeAttrs attrs, SourceSpan span) implements Expr {}
