package io.github.localsoul.port.frontend.unity;

/**
 * Fehler des Unity-Frontends mit dem Exit-Code aus {@code docs/cli-contract.md}. Das CLI übersetzt
 * ihn ab M4 in {@code PortException}.
 */
public final class FrontendException extends Exception {

  /** Allgemeiner Fehler, z. B. Programm nicht gefunden. */
  public static final int GENERAL = 1;

  /** Eingabe nicht gefunden oder unlesbar. */
  public static final int INPUT_NOT_FOUND = 3;

  /** Transpiler-Fehler: Absturz des Frontends oder ungültige Ausgabe. */
  public static final int TRANSLATOR_FAILURE = 4;

  private static final long serialVersionUID = 1L;

  /** Exit-Code laut CLI-Vertrag. */
  private final int exitCode;

  /**
   * Erzeugt den Fehler.
   *
   * @param exitCode Exit-Code laut CLI-Vertrag
   * @param message Meldung, bei Prozessfehlern inklusive der Ausgabe auf stderr
   */
  public FrontendException(int exitCode, String message) {
    super(message);
    this.exitCode = exitCode;
  }

  /**
   * Liefert den Exit-Code laut CLI-Vertrag.
   *
   * @return Exit-Code
   */
  public int exitCode() {
    return exitCode;
  }
}
