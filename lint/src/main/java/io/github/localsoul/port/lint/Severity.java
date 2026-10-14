package io.github.localsoul.port.lint;

/** Stufe eines Befunds ({@code docs/cli-contract.md}). */
public enum Severity {
  /** Fehler. */
  ERROR("error"),
  /** Warnung. */
  WARN("warn"),
  /** Hinweis. */
  INFO("info");

  private final String spelling;

  Severity(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Schreibweise in {@code port.toml} und Ausgaben.
   *
   * @return {@code error}, {@code warn} oder {@code info}
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht die Stufe zu einer Schreibweise.
   *
   * @param text Schreibweise
   * @return Stufe oder {@code null}
   */
  public static Severity fromSpelling(String text) {
    for (Severity severity : values()) {
      if (severity.spelling.equals(text)) {
        return severity;
      }
    }
    return null;
  }
}
