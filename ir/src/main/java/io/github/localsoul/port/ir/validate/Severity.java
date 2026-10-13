package io.github.localsoul.port.ir.validate;

/** Schweregrad einer Diagnose ({@code docs/cli-contract.md}). */
public enum Severity {
  /** {@code error} */
  ERROR("error"),
  /** {@code warn} */
  WARN("warn"),
  /** {@code info} */
  INFO("info");

  private final String spelling;

  Severity(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Liefert die Schreibweise im JSON-Vertrag: {@code error}, {@code warn} oder {@code info}.
   *
   * @return die Schreibweise im JSON-Vertrag: {@code error}, {@code warn} oder {@code info}
   */
  public String spelling() {
    return spelling;
  }
}
