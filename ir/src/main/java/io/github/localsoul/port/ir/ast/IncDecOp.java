package io.github.localsoul.port.ir.ast;

/** Inkrement und Dekrement (Spec Abschnitt 7). */
public enum IncDecOp {
  /** {@code pre-inc} */
  PRE_INC("pre-inc"),
  /** {@code pre-dec} */
  PRE_DEC("pre-dec"),
  /** {@code post-inc} */
  POST_INC("post-inc"),
  /** {@code post-dec} */
  POST_DEC("post-dec");

  private final String spelling;

  IncDecOp(String spelling) {
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
  public static IncDecOp fromSpelling(String text) {
    for (IncDecOp value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
