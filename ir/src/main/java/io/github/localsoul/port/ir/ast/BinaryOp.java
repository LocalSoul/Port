package io.github.localsoul.port.ir.ast;

/** Binäre Operatoren {@code opsym} (Spec Abschnitt 2), nur für Primitive. */
public enum BinaryOp {
  ADD("+"),
  SUB("-"),
  MUL("*"),
  DIV("/"),
  REM("%"),
  AND("&"),
  OR("|"),
  XOR("^"),
  SHL("<<"),
  SHR(">>"),
  EQ("=="),
  NE("!="),
  LT("<"),
  LE("<="),
  GT(">"),
  GE(">="),
  LOGICAL_AND("&&"),
  /** \|| */
  LOGICAL_OR("||");

  private final String spelling;

  BinaryOp(String spelling) {
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
  public static BinaryOp fromSpelling(String text) {
    for (BinaryOp value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
