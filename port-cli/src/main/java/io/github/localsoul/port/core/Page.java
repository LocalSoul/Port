package io.github.localsoul.port.core;

import java.util.List;

/**
 * Ausschnitt einer sortierten Liste für {@code --limit}/{@code --cursor} (ADR-018). Der Cursor ist
 * die Position des ersten Eintrags, {@code next} die des nächsten Ausschnitts oder {@code null}.
 *
 * @param <T> Elementtyp
 * @param items Einträge des Ausschnitts
 * @param total Anzahl aller Einträge
 * @param next Cursor für den nächsten Ausschnitt oder {@code null}
 */
public record Page<T>(List<T> items, int total, String next) {

  /** Kopiert die Einträge. */
  public Page {
    items = List.copyOf(items);
  }

  /**
   * Schneidet einen Ausschnitt.
   *
   * @param <T> Elementtyp
   * @param all alle Einträge, sortiert
   * @param limit höchstens so viele, {@code 0} = alle
   * @param cursor Position aus {@code data.next} oder {@code null}
   * @return Ausschnitt
   * @throws PortException Exit 2 bei negativem Limit oder ungültigem Cursor
   */
  public static <T> Page<T> of(List<T> all, int limit, String cursor) throws PortException {
    if (limit < 0) {
      throw invalid("--limit muss >= 0 sein", null);
    }
    int start = 0;
    if (cursor != null) {
      try {
        start = Integer.parseInt(cursor);
      } catch (NumberFormatException e) {
        throw invalid("ungültiger --cursor '" + cursor + "'", "Wert aus data.next verwenden");
      }
      if (start < 0 || start > all.size()) {
        throw invalid("ungültiger --cursor '" + cursor + "'", "ohne --cursor neu beginnen");
      }
    }
    int end = limit == 0 ? all.size() : java.lang.Math.min(all.size(), start + limit);
    return new Page<>(
        all.subList(start, end), all.size(), end < all.size() ? Integer.toString(end) : null);
  }

  private static PortException invalid(String message, String hint) {
    return new PortException(
        ExitCode.INVALID, Diagnostic.error(DiagnosticIds.ARGS, message, null, hint));
  }
}
