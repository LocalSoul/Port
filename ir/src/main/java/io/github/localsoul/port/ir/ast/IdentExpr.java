package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Local oder Parameter.
 *
 * @param name Bezeichner
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IdentExpr(String name, NodeAttrs attrs, SourceSpan span) implements Expr {}
