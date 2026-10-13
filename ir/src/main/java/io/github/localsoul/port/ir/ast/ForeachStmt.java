package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (foreach ident type expr stmt)}.
 *
 * @param variable Schleifenvariable
 * @param type Typ der Variable
 * @param collection Sammlung
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record ForeachStmt(
    String variable, TypeRef type, Expr collection, Stmt body, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
