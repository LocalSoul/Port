package io.github.localsoul.port.index;

/** Fehler des Index mit Exit-Code laut {@code docs/cli-contract.md} und Ort. */
public final class IndexException extends Exception {

  /** Ein-/Ausgabe- oder Datenbankfehler. */
  public static final int GENERAL = 1;

  /** Ungültige IR (Syntaxfehler). */
  public static final int INVALID = 2;

  private static final long serialVersionUID = 1L;

  private final int exitCode;
  private final String location;

  /**
   * Erzeugt den Fehler.
   *
   * @param exitCode Exit-Code laut Vertrag
   * @param message Meldung
   * @param location Ort ({@code datei[:zeile:spalte]}) oder {@code null}
   * @param cause Ursache oder {@code null}
   */
  public IndexException(int exitCode, String message, String location, Throwable cause) {
    super(message, cause);
    this.exitCode = exitCode;
    this.location = location;
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
   * Ort des Fehlers.
   *
   * @return Ort oder {@code null}
   */
  public String location() {
    return location;
  }
}
