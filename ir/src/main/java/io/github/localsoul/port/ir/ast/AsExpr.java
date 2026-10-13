package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (as type expr)}.
 *
 * @param type Zieltyp
 * @param operand Operand
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record AsExpr(TypeRef type, Expr operand, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
