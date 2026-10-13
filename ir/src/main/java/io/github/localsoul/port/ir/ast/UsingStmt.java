package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (using ident type expr stmt)}.
 *
 * @param variable Variable
 * @param type Typ der Variable
 * @param resource Ressource
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record UsingStmt(
    String variable, TypeRef type, Expr resource, Stmt body, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
