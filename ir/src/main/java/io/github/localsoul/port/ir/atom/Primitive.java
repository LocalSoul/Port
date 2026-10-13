package io.github.localsoul.port.ir.atom;

/** Primitive Typen der IR ({@code docs/ir-spec.md} Abschnitt 3). */
public enum Primitive {
  /** {@code bool} */
  BOOL("bool"),
  /** {@code char} */
  CHAR("char"),
  /** {@code string} */
  STRING("string"),
  /** {@code object} */
  OBJECT("object"),
  /** {@code void} */
  VOID("void"),
  /** {@code i8} */
  I8("i8"),
  /** {@code u8} */
  U8("u8"),
  /** {@code i16} */
  I16("i16"),
  /** {@code u16} */
  U16("u16"),
  /** {@code i32} */
  I32("i32"),
  /** {@code u32} */
  U32("u32"),
  /** {@code i64} */
  I64("i64"),
  /** {@code u64} */
  U64("u64"),
  /** {@code f32} */
  F32("f32"),
  /** {@code f64} */
  F64("f64"),
  /** Nativer 64-Bit-Zeiger (Spec Abschnitt 12). */
  PTR("ptr");

  private final String spelling;

  Primitive(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Liefert die Schreibweise im IR-Text, z. B. {@code "i32"}.
   *
   * @return die Schreibweise im IR-Text, z. B. {@code "i32"}
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht den primitiven Typ zu einer Schreibweise.
   *
   * @param text Schreibweise, z. B. {@code "string"}
   * @return der Typ oder {@code null}, wenn {@code text} kein primitiver Typ ist
   */
  public static Primitive fromSpelling(String text) {
    for (Primitive primitive : values()) {
      if (primitive.spelling.equals(text)) {
        return primitive;
      }
    }
    return null;
  }
}
