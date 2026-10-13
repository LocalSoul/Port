package io.github.localsoul.port.ir.ast;

/** Modifier (Spec Abschnitt 5). */
public enum Modifier {
  /** {@code public} */
  PUBLIC("public"),
  /** {@code private} */
  PRIVATE("private"),
  /** {@code protected} */
  PROTECTED("protected"),
  /** {@code internal} */
  INTERNAL("internal"),
  /** {@code static} */
  STATIC("static"),
  /** {@code abstract} */
  ABSTRACT("abstract"),
  /** {@code virtual} */
  VIRTUAL("virtual"),
  /** {@code override} */
  OVERRIDE("override"),
  /** {@code sealed} */
  SEALED("sealed"),
  /** {@code readonly} */
  READONLY("readonly"),
  /** {@code const} */
  CONST("const");

  private final String spelling;

  Modifier(String spelling) {
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
  public static Modifier fromSpelling(String text) {
    for (Modifier value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
