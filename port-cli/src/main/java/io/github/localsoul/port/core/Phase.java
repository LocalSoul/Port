package io.github.localsoul.port.core;

/**
 * Phasen der Pipeline in Ausführungsreihenfolge ({@code docs/cli-contract.md}, Lifecycle). Index
 * und Lint folgen in M5/M6.
 */
public enum Phase {
  /** Eingabe prüfen. */
  EXTRACT("extract"),
  /** IR erzeugen. */
  IR("ir"),
  /** Java erzeugen. */
  GEN("gen"),
  /** Java kompilieren. */
  BUILD("build");

  private final String spelling;

  Phase(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Name in Befehlen und Ausgaben.
   *
   * @return z. B. {@code ir}
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht die Phase zu einem Namen.
   *
   * @param text Name
   * @return Phase oder {@code null}
   */
  public static Phase fromSpelling(String text) {
    for (Phase phase : values()) {
      if (phase.spelling.equals(text)) {
        return phase;
      }
    }
    return null;
  }
}
