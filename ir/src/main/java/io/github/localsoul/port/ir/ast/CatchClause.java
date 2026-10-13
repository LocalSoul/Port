package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * {@code (catch ident type stmt)} in einem {@link TryStmt}.
 *
 * @param variable Variable der Exception
 * @param type gefangener Typ
 * @param body Rumpf
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record CatchClause(
    String variable, TypeRef type, Stmt body, NodeAttrs attrs, SourceSpan span) implements IrNode {}
