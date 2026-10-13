package io.github.localsoul.port.ir.ast;

/** Unäre Operatoren (Spec Abschnitt 7). */
public enum UnaryOp {
  /** arithmetische Negation */
  NEG("neg"),
  /** logisches Nicht */
  NOT("not"),
  /** bitweises Nicht */
  BNOT("bnot");

  private final String spelling;

  UnaryOp(String spelling) {
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
  public static UnaryOp fromSpelling(String text) {
    for (UnaryOp value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
