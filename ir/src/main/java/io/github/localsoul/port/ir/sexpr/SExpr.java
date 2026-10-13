package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/** Knoten der Listen-/Atom-Struktur, die der {@link SExprReader} liefert. */
public sealed interface SExpr permits SList, SSymbol, SKeyword, SString, SNumber, SBareInteger {

  /**
   * Liefert die Position des ersten Zeichens (bei Listen: der öffnenden Klammer).
   *
   * @return die Position des ersten Zeichens (bei Listen: der öffnenden Klammer)
   */
  SourceSpan span();
}
