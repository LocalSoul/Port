package io.github.localsoul.port.core;

/** Schweregrad einer Diagnose ({@code docs/cli-contract.md}). */
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
   * Schreibweise in der Ausgabe.
   *
   * @return {@code error}, {@code warn} oder {@code info}
   */
  public String spelling() {
    return spelling;
  }
}
