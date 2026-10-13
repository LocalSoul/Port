package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/** Gemeinsame Sicht auf alle AST-Knoten: Position und Attribute {@code :loc :manual :note}. */
public sealed interface IrNode
    permits Expr,
        Stmt,
        TypeDecl,
        MemberDecl,
        SwitchCase,
        SwitchDefault,
        CatchClause,
        FinallyClause,
        Param,
        AttributeUse,
        EnumMember,
        IrHeader {

  /** Liefert die Position der öffnenden Klammer bzw. des Atoms. */
  SourceSpan span();

  /** Liefert die Attribute {@code :loc}, {@code :manual} und {@code :note}. */
  NodeAttrs attrs();
}
