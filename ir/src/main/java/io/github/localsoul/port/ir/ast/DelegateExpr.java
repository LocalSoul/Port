package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;

/**
 * {@code (delegate expr mref)}, Methodengruppe zu Delegate.
 *
 * @param target Objekt
 * @param method Methode
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record DelegateExpr(Expr target, MemberRef method, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
