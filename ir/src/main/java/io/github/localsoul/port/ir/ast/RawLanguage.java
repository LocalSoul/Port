package io.github.localsoul.port.ir.ast;

/** Sprache eines {@code (raw ...)}-Knotens (Spec Abschnitt 9). */
public enum RawLanguage {
  /** Original-C# */
  CS("cs"),
  /** Disassembly-Text */
  ASM("asm");

  private final String spelling;

  RawLanguage(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Liefert die Schreibweise im IR-Text.
   *
   * @return die Schreibweise im IR-Text
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht den Wert zu einer Schreibweise.
   *
   * @param text Schreibweise
   * @return der Wert oder {@code null}, wenn es keinen gibt
   */
  public static RawLanguage fromSpelling(String text) {
    for (RawLanguage value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
