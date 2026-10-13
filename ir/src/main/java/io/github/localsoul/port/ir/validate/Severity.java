package io.github.localsoul.port.ir.validate;

/** Schweregrad einer Diagnose ({@code docs/cli-contract.md}). */
public enum Severity {
  ERROR("error"),
  WARN("warn"),
  INFO("info");

  private final String spelling;

  Severity(String spelling) {
    this.spelling = spelling;
  }

  /** Liefert die Schreibweise im JSON-Vertrag: {@code error}, {@code warn} oder {@code info}. */
  public String spelling() {
    return spelling;
  }
}
