package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;

/**
 * {@code (sfield mref)}.
 *
 * @param field statisches Feld
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record StaticFieldExpr(MemberRef field, NodeAttrs attrs, SourceSpan span) implements Expr {}
