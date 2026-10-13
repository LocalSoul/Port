package io.github.localsoul.port.ir.ast;

/** Übergabeart eines Parameters. */
public enum ParamMode {
  /** normale Übergabe, ohne Keyword */
  VALUE(""),
  /** {@code :ref} */
  REF("ref"),
  /** {@code :out} */
  OUT("out"),
  /** {@code :in} */
  IN("in");

  private final String spelling;

  ParamMode(String spelling) {
    this.spelling = spelling;
  }

  /** Liefert die Schreibweise im IR-Text. */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht den Wert zu einer Schreibweise.
   *
   * @param text Schreibweise
   * @return der Wert oder {@code null}, wenn es keinen gibt
   */
  public static ParamMode fromSpelling(String text) {
    for (ParamMode value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
