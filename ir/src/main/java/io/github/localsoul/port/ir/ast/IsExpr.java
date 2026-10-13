package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (is type expr [ident])}.
 *
 * @param type geprüfter Typ
 * @param operand Operand
 * @param binding gebundene Variable, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IsExpr(TypeRef type, Expr operand, String binding, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
