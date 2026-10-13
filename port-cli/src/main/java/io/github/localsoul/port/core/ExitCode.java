package io.github.localsoul.port.core;

/**
 * Exit-Codes laut {@code docs/cli-contract.md}. Die Reihenfolge der Konstanten entspricht den
 * Codes.
 */
public enum ExitCode {
  /** Erfolg. */
  SUCCESS(0),
  /** Allgemeiner Fehler. */
  GENERAL(1),
  /** Ungültige Argumente, Manifest oder IR. */
  INVALID(2),
  /** Eingabe nicht gefunden oder Hash stimmt nicht. */
  INPUT(3),
  /** Transpiler-Fehler. */
  TRANSLATOR(4),
  /** Offene {@code @manual}-Stellen (nur mit {@code --strict}). */
  MANUAL_OPEN(5),
  /** Merge-Konflikt bei {@code ir --update} (ab M7). */
  MERGE_CONFLICT(6),
  /** Kompilierfehler im generierten Java. */
  COMPILE(7),
  /** Abhängigkeit nicht auflösbar. */
  DEPENDENCY(8),
  /** Lint-Fehler (ab M6). */
  LINT(9),
  /** Fix oder Patch besteht die Prüfung nicht (ab M6a/M7a). */
  FIX_REJECTED(10),
  /** Konformitätstest weicht ab (ab M11a). */
  CONFORMANCE(11);

  private final int code;

  ExitCode(int code) {
    this.code = code;
  }

  /**
   * Zahlenwert für den Prozess.
   *
   * @return Exit-Code
   */
  public int code() {
    return code;
  }

  /**
   * Sucht die Konstante zu einem Zahlenwert, z. B. aus {@code FrontendException}.
   *
   * @param code Zahlenwert
   * @return Konstante, {@link #GENERAL} bei unbekanntem Wert
   */
  public static ExitCode of(int code) {
    for (ExitCode exitCode : values()) {
      if (exitCode.code == code) {
        return exitCode;
      }
    }
    return GENERAL;
  }
}
