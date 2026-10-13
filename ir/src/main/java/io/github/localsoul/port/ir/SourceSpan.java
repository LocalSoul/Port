package io.github.localsoul.port.ir;

/**
 * Quellposition eines Tokens oder Knotens.
 *
 * <p>Zeile und Spalte zählen ab 1. Die Spalte zählt Unicode-Codepoints, ein Tabulator zählt als
 * eine Spalte.
 *
 * @param file Dateiname, wie er dem Reader übergeben wurde (nie {@code null})
 * @param line Zeile ab 1
 * @param column Spalte ab 1
 */
public record SourceSpan(String file, int line, int column) {

  /**
   * Prüft die Werte.
   *
   * @throws IllegalArgumentException wenn {@code file} {@code null} ist oder Zeile bzw. Spalte
   *     kleiner als 1 sind
   */
  public SourceSpan {
    if (file == null) {
      throw new IllegalArgumentException("file darf nicht null sein");
    }
    if (line < 1 || column < 1) {
      throw new IllegalArgumentException("Zeile und Spalte beginnen bei 1: " + line + ":" + column);
    }
  }

  /** Liefert die Position im Format {@code datei:zeile:spalte}. */
  @Override
  public String toString() {
    return file + ":" + line + ":" + column;
  }
}
