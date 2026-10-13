package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * Eine Liste {@code ( ... )}.
 *
 * @param items Elemente in Eingabereihenfolge (unveränderlich)
 * @param span Position der öffnenden Klammer
 */
public record SList(List<SExpr> items, SourceSpan span) implements SExpr {

  /** Kopiert die Elemente in eine unveränderliche Liste. */
  public SList {
    items = List.copyOf(items);
  }
}
