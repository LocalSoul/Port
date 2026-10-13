package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (copy expr)}, explizite Kopie eines struct-Werts (Spec Abschnitt 7).
 *
 * @param operand kopierter Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CopyExpr(Expr operand, NodeAttrs attrs, SourceSpan span) implements Expr {}
