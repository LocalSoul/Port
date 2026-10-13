package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;

/**
 * {@code (prop expr mref)}.
 *
 * @param target Objekt
 * @param property Property
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record PropExpr(Expr target, MemberRef property, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
