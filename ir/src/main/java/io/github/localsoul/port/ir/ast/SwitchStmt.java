package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (switch expr {(case ...)} [(default ...)])}, ohne Durchfallen.
 *
 * @param value geprüfter Wert
 * @param cases Fälle
 * @param defaultCase Standardfall, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record SwitchStmt(
    Expr value, List<SwitchCase> cases, SwitchDefault defaultCase, NodeAttrs attrs, SourceSpan span)
    implements Stmt {

  /** Kopiert Listen in unveränderliche Listen. */
  public SwitchStmt {
    cases = List.copyOf(cases);
  }
}
