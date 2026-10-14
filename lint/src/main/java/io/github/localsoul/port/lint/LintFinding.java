package io.github.localsoul.port.lint;

/**
 * Ein Befund.
 *
 * @param id Regel, z. B. {@code SH001}
 * @param severity Stufe (nach Konfiguration)
 * @param symbol betroffenes Symbol oder {@code null}
 * @param message Meldung
 * @param location {@code datei:zeile:spalte} oder {@code null}
 * @param hint Hinweis zur Behebung oder {@code null}
 */
public record LintFinding(
    String id, Severity severity, String symbol, String message, String location, String hint) {

  /**
   * Kopie mit anderer Stufe.
   *
   * @param changed neue Stufe
   * @return Befund
   */
  public LintFinding withSeverity(Severity changed) {
    return new LintFinding(id, changed, symbol, message, location, hint);
  }
}
