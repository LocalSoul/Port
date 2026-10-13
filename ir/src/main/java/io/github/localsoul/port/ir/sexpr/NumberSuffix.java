package io.github.localsoul.port.ir.sexpr;

/** Pflicht-Typsuffix einer Zahl ({@code docs/ir-spec.md} Abschnitt 2). */
public enum NumberSuffix {
  /** {@code i8} */
  I8("i8", false),
  /** {@code u8} */
  U8("u8", false),
  /** {@code i16} */
  I16("i16", false),
  /** {@code u16} */
  U16("u16", false),
  /** {@code i32} */
  I32("i32", false),
  /** {@code u32} */
  U32("u32", false),
  /** {@code i64} */
  I64("i64", false),
  /** {@code u64} */
  U64("u64", false),
  /** {@code f32} */
  F32("f32", true),
  /** {@code f64} */
  F64("f64", true);

  private final String spelling;
  private final boolean floatingPoint;

  NumberSuffix(String spelling, boolean floatingPoint) {
    this.spelling = spelling;
    this.floatingPoint = floatingPoint;
  }

  /**
   * Liefert die Schreibweise im IR-Text, z. B. {@code "f32"}.
   *
   * @return die Schreibweise im IR-Text, z. B. {@code "f32"}
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Liefert {@code true} für {@code f32} und {@code f64}.
   *
   * @return {@code true} für {@code f32} und {@code f64}
   */
  public boolean isFloatingPoint() {
    return floatingPoint;
  }

  /**
   * Sucht das Suffix zu einer Schreibweise.
   *
   * @param text Schreibweise, z. B. {@code "u64"}
   * @return das Suffix oder {@code null}, wenn {@code text} kein gültiges Suffix ist
   */
  public static NumberSuffix fromSpelling(String text) {
    for (NumberSuffix suffix : values()) {
      if (suffix.spelling.equals(text)) {
        return suffix;
      }
    }
    return null;
  }
}
