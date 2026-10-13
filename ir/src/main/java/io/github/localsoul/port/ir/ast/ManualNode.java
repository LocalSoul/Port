package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (manual string)}, Platzhalter für Handarbeit (Spec Abschnitt 9).
 *
 * @param reason Grund
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ManualNode(String reason, NodeAttrs attrs, SourceSpan span) implements Expr, Stmt {}
