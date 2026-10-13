package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (if expr stmt [stmt])}.
 *
 * @param condition Bedingung
 * @param thenBranch Zweig bei wahr
 * @param elseBranch Zweig bei falsch, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IfStmt(
    Expr condition, Stmt thenBranch, Stmt elseBranch, NodeAttrs attrs, SourceSpan span)
    implements Stmt {}
