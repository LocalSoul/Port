package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (local ident type [expr])}, gilt bis Blockende.
 *
 * @param name Name
 * @param type Typ
 * @param initializer Startwert, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record LocalStmt(
    String name, TypeRef type, Expr initializer, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
