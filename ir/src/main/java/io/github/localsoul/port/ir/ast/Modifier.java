package io.github.localsoul.port.ir.ast;

/** Modifier (Spec Abschnitt 5). */
public enum Modifier {
  PUBLIC("public"),
  PRIVATE("private"),
  PROTECTED("protected"),
  INTERNAL("internal"),
  STATIC("static"),
  ABSTRACT("abstract"),
  VIRTUAL("virtual"),
  OVERRIDE("override"),
  SEALED("sealed"),
  READONLY("readonly"),
  CONST("const");

  private final String spelling;

  Modifier(String spelling) {
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
  public static Modifier fromSpelling(String text) {
    for (Modifier value : values()) {
      if (value.spelling.equals(text)) {
        return value;
      }
    }
    return null;
  }
}
