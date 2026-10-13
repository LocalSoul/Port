package io.github.localsoul.port.core;

/**
 * Eine Meldung in {@code diagnostics} ({@code docs/cli-contract.md}). IDs der CLI selbst beginnen
 * mit {@code PORT-} (Liste in {@link DiagnosticIds}).
 *
 * @param id Kennung, z. B. {@code PORT-HASH} oder eine Lint-Regel
 * @param severity Schweregrad
 * @param symbol betroffenes Symbol oder {@code null}
 * @param message Meldung
 * @param location Ort als {@code datei[:zeile[:spalte]]} relativ zum Projekt oder {@code null}
 * @param hint Hinweis zur Behebung oder {@code null}
 */
public record Diagnostic(
    String id, Severity severity, String symbol, String message, String location, String hint) {

  /**
   * Fehler ohne Symbol.
   *
   * @param id Kennung
   * @param message Meldung
   * @param location Ort oder {@code null}
   * @param hint Hinweis oder {@code null}
   * @return Diagnose
   */
  public static Diagnostic error(String id, String message, String location, String hint) {
    return new Diagnostic(id, Severity.ERROR, null, message, location, hint);
  }

  /**
   * Warnung ohne Symbol.
   *
   * @param id Kennung
   * @param message Meldung
   * @param location Ort oder {@code null}
   * @return Diagnose
   */
  public static Diagnostic warn(String id, String message, String location) {
    return new Diagnostic(id, Severity.WARN, null, message, location, null);
  }

  /**
   * Hinweis ohne Symbol und Ort.
   *
   * @param id Kennung
   * @param message Meldung
   * @return Diagnose
   */
  public static Diagnostic info(String id, String message) {
    return new Diagnostic(id, Severity.INFO, null, message, null, null);
  }
}
