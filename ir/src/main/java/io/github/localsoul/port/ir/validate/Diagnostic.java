package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.Comparator;

/**
 * Eine Diagnose, Felder wie im JSON-Vertrag ({@code docs/cli-contract.md}).
 *
 * @param id Regel-ID, z. B. {@code IR002}
 * @param severity Schweregrad
 * @param symbol stabile Symbol-ID des betroffenen Typs oder Mitglieds, oder {@code null}, wenn es
 *     keines gibt (z. B. Syntaxfehler)
 * @param message Meldung
 * @param location Position; {@code location.toString()} ergibt {@code datei:zeile:spalte}
 * @param hint Lösungshinweis, oder {@code null}
 */
public record Diagnostic(
    String id, Severity severity, String symbol, String message, SourceSpan location, String hint) {

  /**
   * Reihenfolge für Ausgaben: Datei, Zeile, Spalte (numerisch), dann ID, dann Meldung.
   *
   * <p>Numerisch statt als Text, damit Zeile 9 vor Zeile 10 steht.
   */
  public static final Comparator<Diagnostic> ORDER = new DiagnosticOrder();

  private static final class DiagnosticOrder implements Comparator<Diagnostic> {

    @Override
    public int compare(Diagnostic a, Diagnostic b) {
      int result = a.location.file().compareTo(b.location.file());
      if (result == 0) {
        result = Integer.compare(a.location.line(), b.location.line());
      }
      if (result == 0) {
        result = Integer.compare(a.location.column(), b.location.column());
      }
      if (result == 0) {
        result = a.id.compareTo(b.id);
      }
      if (result == 0) {
        result = a.message.compareTo(b.message);
      }
      return result;
    }
  }
}
