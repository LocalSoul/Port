package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (try stmt {(catch ...)} [(finally ...)])}.
 *
 * @param body geschützter Block
 * @param catches catch-Zweige
 * @param finallyClause finally-Zweig, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record TryStmt(
    Stmt body,
    List<CatchClause> catches,
    FinallyClause finallyClause,
    NodeAttrs attrs,
    SourceSpan span)
    implements Stmt {

  /** Kopiert Listen in unveränderliche Listen. */
  public TryStmt {
    catches = List.copyOf(catches);
  }
}
