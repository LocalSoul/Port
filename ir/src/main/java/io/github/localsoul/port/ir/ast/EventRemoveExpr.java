package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;

/**
 * {@code (event-remove expr mref expr)}.
 *
 * @param target Objekt
 * @param event Event
 * @param handler Handler
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record EventRemoveExpr(
    Expr target, MemberRef event, Expr handler, NodeAttrs attrs, SourceSpan span) implements Expr {}
