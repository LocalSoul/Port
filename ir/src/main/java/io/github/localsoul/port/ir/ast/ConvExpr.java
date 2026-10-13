package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (conv type expr)}, numerische Umwandlung.
 *
 * @param type Zieltyp
 * @param operand Operand
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ConvExpr(TypeRef type, Expr operand, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
