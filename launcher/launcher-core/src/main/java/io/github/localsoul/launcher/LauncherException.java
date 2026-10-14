package io.github.localsoul.launcher;

/**
 * Fehler des Launchers mit Exit-Code laut {@code docs/cli-contract.md}, Diagnose-ID, Ort und
 * vorgeschlagenem nächsten Schritt (ADR-018). Das CLI übersetzt ihn in seine Ausgabe.
 */
public final class LauncherException extends Exception {

  /** Allgemeiner Fehler. */
  public static final int GENERAL = 1;

  /** Ungültige Argumente, Manifest, Profil oder Kombination. */
  public static final int INVALID = 2;

  /** Eingabe nicht gefunden. */
  public static final int INPUT = 3;

  /** Abhängigkeit (Engine, Plattform) nicht auflösbar. */
  public static final int DEPENDENCY = 8;

  private static final long serialVersionUID = 1L;

  private final int exitCode;
  private final String id;
  private final String location;
  private final String hint;

  /**
   * Erzeugt den Fehler.
   *
   * @param exitCode Exit-Code laut Vertrag
   * @param id Diagnose-ID, z. B. {@code PORT-MANIFEST}
   * @param message Meldung
   * @param location Ort ({@code datei[:zeile:spalte]}) oder {@code null}
   * @param hint nächster Schritt oder {@code null}
   */
  public LauncherException(int exitCode, String id, String message, String location, String hint) {
    super(message);
    this.exitCode = exitCode;
    this.id = id;
    this.location = location;
    this.hint = hint;
  }

  /**
   * Exit-Code laut Vertrag.
   *
   * @return Exit-Code
   */
  public int exitCode() {
    return exitCode;
  }

  /**
   * Diagnose-ID.
   *
   * @return ID
   */
  public String id() {
    return id;
  }

  /**
   * Ort des Fehlers.
   *
   * @return Ort oder {@code null}
   */
  public String location() {
    return location;
  }

  /**
   * Vorgeschlagener nächster Schritt.
   *
   * @return Hinweis oder {@code null}
   */
  public String hint() {
    return hint;
  }
}
