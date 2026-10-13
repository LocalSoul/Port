package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;

/**
 * {@code (field expr mref)}.
 *
 * @param target Objekt
 * @param field Feld
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record FieldExpr(Expr target, MemberRef field, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
